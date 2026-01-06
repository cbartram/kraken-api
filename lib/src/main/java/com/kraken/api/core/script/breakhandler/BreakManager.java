package com.kraken.api.core.script.breakhandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.script.Script;
import com.kraken.api.service.ui.login.LoginService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
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
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private Script activeScript;
    private BreakProfile activeProfile;
    private boolean initialized = false;
    private ScheduledFuture<?> scheduledBreakEnd;

    /**
     * Initializes the break handler and registers it to the event bus.
     * Safe to call multiple times - will only initialize once.
     */
    public void initialize() {
        if (!initialized) {
            eventBus.register(this);
            initialized = true;
            log.info("BreakHandler initialized");
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
            state.reset();
            log.info("BreakHandler shut down");
        }
    }

    /**
     * Attaches a script to the break handler with a specific profile.
     * Prevents duplicate attachments and handles resuming from breaks.
     */
    public void attachScript(Script script, BreakProfile profile) {
        // If same script is already attached, don't re-attach (plugin restart scenario)
        if (activeScript == script && activeProfile == profile) {
            log.debug("Script already attached, checking break state...");

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
            this.state.setScriptStartTime(Instant.now());
            scheduleNextBreak();
        } else {
            log.info("Reattached script while break is active or pending");
        }

        log.info("Attached script with profile: {}", profile.getName());
    }

    /**
     * Detaches the current script from the break handler.
     */
    public void detachScript() {
        // Don't clear state if we're on break - preserve it for potential resume
        if (!state.isOnBreak() && !state.isAwaitingLogin()) {
            this.activeScript = null;
            this.activeProfile = null;
            this.state.reset();
            log.info("Script detached from BreakHandler");
        } else {
            log.info("Script detached but break state preserved");
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

        log.info("Next break scheduled in {} minutes", nextRunDuration.toMinutes());
    }

    /**
     * Checks on each game tick if a break should be triggered.
     */
    @Subscribe
    public void onGameTick(GameTick event) {
        if (!initialized || activeScript == null || activeProfile == null) return;

        // Don't check conditions while on break
        if (state.isOnBreak() || state.isAwaitingLogin()) return;

        // Check time-based break
        if (state.getNextBreakTime() != null && Instant.now().isAfter(state.getNextBreakTime())) {
            startBreak("Scheduled break time reached");
            return;
        }

        // Check custom conditions
        for (BreakCondition condition : activeProfile.getCustomConditions()) {
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
        if (event.getGameState() == net.runelite.api.GameState.LOGGED_IN && state.isAwaitingLogin()) {
            log.info("Logged back in during break period");

            if (state.shouldResumeAfterLogin()) {
                log.info("Break period ended while logged out, resuming script");
                completeBreakResume();
            } else {
                log.info("Break still active, remaining logged in but paused");
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
            if (client.getGameState() == net.runelite.api.GameState.LOGGED_IN) {
                try {
                    boolean loggedOut = ctx.players().local().logout();
                    if (!loggedOut) {
                        log.error("Failed to logout for break - aborting break attempt");
                        return false;
                    }
                    log.info("Successfully logged out for break");
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
            log.info("Break started with logout - will resume after login at: {}",
                    TIME_FORMATTER.format(breakEndTime));
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

        if (state.isAwaitingLogin() && client.getGameState() != net.runelite.api.GameState.LOGGED_IN) {
            log.info("Break ended but still logged out - waiting for login to resume");
            return;
        }

        completeBreakResume();
    }

    /**
     * Completes the break resume process - logging in if needed and resuming script.
     */
    private void completeBreakResume() {
        if (activeScript == null) {
            log.warn("Cannot resume break - no active script");
            state.setOnBreak(false);
            state.setAwaitingLogin(false);
            return;
        }

        // Login if we're logged out and need to log back in
        if (state.isAwaitingLogin() && client.getGameState() != net.runelite.api.GameState.LOGGED_IN) {
            try {
                loginService.login();
                log.info("Attempting to log back in after break");
                return;
            } catch (Exception e) {
                log.error("Failed to login after break", e);
            }
        }

        state.setOnBreak(false);
        state.setAwaitingLogin(false);
        state.setBreakStartTime(null);
        state.setBreakEndTime(null);

        // Resume the script
        activeScript.resume();
        log.info("Script resumed after break");

        // Schedule next break
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