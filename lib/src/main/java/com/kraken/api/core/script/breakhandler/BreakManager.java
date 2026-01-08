package com.kraken.api.core.script.breakhandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.script.Script;
import com.kraken.api.service.ui.login.LoginService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class BreakManager {

    @Inject
    private Client client;

    @Inject
    private EventBus eventBus;

    @Inject
    private Context ctx;

    @Inject
    private LoginService loginService;

    @Inject
    private BreakState state;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm:ss a").withZone(ZoneId.systemDefault());

    private Script activeScript;
    private BreakProfile activeProfile;
    private boolean initialized = false;
    private boolean breakScheduled = false;
    private ScheduledFuture<?> scheduledBreakEnd;

    /**
     * Initializes the break handler and registers it to the event bus.
     * Safe to call multiple times - will only initialize once.
     */
    public void initialize() {
        if (!initialized) {
            eventBus.register(this);
            initialized = true;
            log.info("Break Manager initialized");
        }
    }

    /**
     * Shuts down the break handler and cleans up resources.
     */
    public void shutdown() {
        if (initialized) {
            eventBus.unregister(this);
            if (scheduledBreakEnd != null && !scheduledBreakEnd.isDone()) {
                scheduledBreakEnd.cancel(false);
            }
            scheduler.shutdownNow();
            initialized = false;
            breakScheduled = false;
            state.reset();
            log.info("Break Manager shut down");
        }
    }

    /**
     * Attaches a script to the break handler with a specific profile.
     * Prevents duplicate attachments and handles resuming from breaks.
     */
    public void attachScript(Script script, BreakProfile profile) {
        // If same script is already attached, don't re-attach (plugin restart scenario)
        if (activeScript == script && activeProfile == profile) {
            // If we were on break and logged back in, handle resume logic
            if (state.shouldResumeAfterLogin()) {
                log.info("Resuming script after break ended during logout");
                completeBreakResume();
            }
            return;
        }

        if (activeScript != null && activeScript != script) {
            log.warn("Detaching previous script before attaching new one");
            detachScript();
        }

        this.activeScript = script;
        this.activeProfile = profile;

        // Only reset state if we're not currently managing a break
        if (!state.isOnBreak() && !state.isAwaitingLogin()) {
            this.state.reset();
        } else {
            log.info("Reattached script while break is active or pending");
        }

        log.info("Attached script: {} with profile: {}", script.getClass().getName(), profile.getName());
    }

    /**
     * Detaches the current script from the break handler.
     */
    public void detachScript() {
        // Don't clear state if we're on break - preserve it for potential resume
        if (!state.isOnBreak() && !state.isAwaitingLogin()) {
            log.info("Script: {} detached", activeScript.getClass().getName());
            this.activeScript = null;
            this.activeProfile = null;
            this.breakScheduled = false;
            this.state.reset();
        } else {
            log.info("Script: {} detached but break state preserved", activeScript.getClass().getName());
            this.activeScript = null;
        }
    }

    /**
     * Schedules the next break based on the profile settings.
     */
    private void scheduleNextBreak() {
        if (activeProfile == null) return;

        Duration nextRunDuration = activeProfile.getNextRunDuration();
        Instant nextBreakTime = Instant.now().plus(nextRunDuration);
        state.setNextBreakTime(nextBreakTime);
        breakScheduled = true;

        String formattedTime = TIME_FORMATTER.format(nextBreakTime);
        log.info("Next break scheduled in {} minutes at: {}", nextRunDuration.toMinutes(), formattedTime);
    }

    /**
     * Checks on each game tick if a break should be triggered.
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        if (!initialized || activeScript == null || activeProfile == null) return;

        // If we haven't scheduled the first break yet and we're logged in and not on break, schedule it now
        if (!breakScheduled && !state.isOnBreak() && !state.isAwaitingLogin() &&
                client.getGameState() == GameState.LOGGED_IN) {
            log.info("Player logged in, starting break schedule");
            state.setScriptStartTime(Instant.now());
            scheduleNextBreak();
            return;
        }

        // Don't check conditions while on break
        if (state.isOnBreak() || state.isAwaitingLogin()) return;

        if (state.getNextBreakTime() != null && Instant.now().isAfter(state.getNextBreakTime())) {
            startBreak("Scheduled break time reached");
            return;
        }

        // Check custom conditions
        for (BreakCondition condition : activeProfile.getBreakConditions()) {
            if (condition.shouldBreak()) {
                startBreak(condition.getDescription());
                return;
            }
        }
    }

    /**
     * Listen for login events to handle break resume after logout.
     */
    @Subscribe
    public void onGameStateChanged(GameStateChanged event) {
        if (!initialized) return;

        if (event.getGameState() == GameState.LOGGED_IN) {
            if (state.isAwaitingLogin()) {
                if (state.shouldResumeAfterLogin()) {
                    log.info("Break period ended while logged out, resuming script");
                    completeBreakResume();
                } else {
                    log.info("Break still active, remaining logged in but paused");
                }
            }
        }
    }

    /**
     * Initiates a break by pausing the script and optionally logging out.
     * Returns false if break couldn't be started (e.g., logout failed when required).
     */
    private boolean startBreak(String reason) {
        if (activeScript == null) return false;
        log.info("Attempting to start break: {}", reason);

        // If logout is required, try it first before committing to the break
        if (activeProfile.isLogoutDuringBreak()) {
            if (client.getGameState() == GameState.LOGGED_IN) {
                try {
                    boolean loggedOut = ctx.players().local().logout();
                    if (!loggedOut) {
                        log.error("Failed to logout for break - aborting break attempt");
                        return false;
                    }
                } catch (Exception e) {
                    log.error("Exception during logout for break - aborting break", e);
                    return false;
                }
            } else {
                log.warn("Not logged in, cannot logout for break - aborting break attempt");
                return false;
            }
        }

        state.setOnBreak(true);
        state.setBreakStartTime(Instant.now());
        state.setBreakReason(reason);

        activeScript.pause();

        Duration breakDuration = activeProfile.getNextBreakDuration();
        Instant breakEndTime = Instant.now().plus(breakDuration);
        state.setBreakEndTime(breakEndTime);

        if (activeProfile.isLogoutDuringBreak()) {
            state.setAwaitingLogin(true);
            log.info("Break started with logout - will resume after login at: {}", TIME_FORMATTER.format(breakEndTime));
        }

        scheduledBreakEnd = scheduler.schedule(this::endBreak, breakDuration.toMillis(), TimeUnit.MILLISECONDS);
        String formattedTime = TIME_FORMATTER.format(breakEndTime);
        log.info("Break will end in {} minutes at: {}", breakDuration.toMinutes(), formattedTime);
        return true;
    }

    /**
     * Ends the current break and resumes the script (if logged in).
     */
    private void endBreak() {
        if (!state.isOnBreak()) return;
        log.info("Break period ended");

        if (state.isAwaitingLogin() && client.getGameState() != GameState.LOGGED_IN) {
            try {
                loginService.login();
                log.info("Attempting to log back in after break");
                // Don't call completeBreakResume here - let game state change event handle it
            } catch (Exception e) {
                log.error("Failed to login after break", e);
            }
        } else if (client.getGameState() == GameState.LOGGED_IN) {
            // If we're already logged in, complete the resume immediately
            completeBreakResume();
        }
    }

    /**
     * Completes the break resume process - resuming script and scheduling next break.
     * Should only be called once per break resume.
     */
    private void completeBreakResume() {
        // Guard against duplicate calls
        if (!state.isOnBreak() && !state.isAwaitingLogin()) {
            log.info("Resume already completed, ignoring duplicate call");
            return;
        }

        if (activeScript == null) {
            log.warn("Cannot resume break - no active script");
            state.setOnBreak(false);
            state.setAwaitingLogin(false);
            return;
        }

        // Make sure we're logged in before resuming
        if (client.getGameState() != GameState.LOGGED_IN) {
            log.warn("Cannot complete resume - not logged in");
            return;
        }

        log.info("Completing break resume");

        state.setOnBreak(false);
        state.setAwaitingLogin(false);
        state.setBreakStartTime(null);
        state.setBreakEndTime(null);

        activeScript.resume();
        log.info("Script resumed after break");

        scheduleNextBreak();
    }

    /**
     * Manually triggers a break.
     */
    public boolean triggerBreak(String reason) {
        if (activeScript != null && !state.isOnBreak()) {
            return startBreak(reason);
        }
        return false;
    }

    /**
     * Checks if the handler is currently managing a script.
     */
    public boolean isActive() {
        return activeScript != null;
    }

    /**
     * Checks if currently on break.
     */
    public boolean isOnBreak() {
        return state.isOnBreak();
    }
}