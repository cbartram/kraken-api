package com.kraken.api.core.script.breakhandler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.script.Script;
import com.kraken.api.service.ui.login.LoginService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class BreakManager {

    private final Client client;
    private final EventBus eventBus;
    private final Context ctx;
    private final LoginService loginService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("hh:mm a")
            .withZone(ZoneId.systemDefault());

    @Getter
    private final BreakState state = new BreakState();

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private Script activeScript;
    private BreakProfile activeProfile;
    private boolean enabled = false;

    @Inject
    public BreakManager(EventBus eventBus, Context ctx, LoginService loginService) {
        this.eventBus = eventBus;
        this.ctx = ctx;
        this.loginService = loginService;
        this.client = ctx.getClient();
        eventBus.register(this);
        enabled = true;
        log.info("Break Manager initialized");
    }

    /**
     * Shuts down the break handler and cleans up resources.
     */
    public void shutdown() {
        if (enabled) {
            eventBus.unregister(this);
            scheduler.shutdownNow();
            enabled = false;
            state.reset();
            log.info("Break Manager shut down");
        }
    }

    /**
     * Attaches a script to the break handler with a specific profile.
     */
    public void attachScript(Script script, BreakProfile profile) {
        if (activeScript != null) {
            log.warn("Detaching previous script before attaching new one");
            detachScript();
        }

        this.activeScript = script;
        this.activeProfile = profile;
        this.state.reset();
        this.state.setScriptStartTime(Instant.now());

        scheduleNextBreak();
        log.info("Attached script with profile: {}", profile.getName());
    }

    /**
     * Detaches the current script from the break handler.
     */
    public void detachScript() {
        if (state.isOnBreak()) {
            endBreak();
        }
        this.activeScript = null;
        this.activeProfile = null;
        this.state.reset();
        log.info("Script detached from Break Manager");
    }

    /**
     * Schedules the next break based on the profile settings.
     */
    private void scheduleNextBreak() {
        if (activeProfile == null) return;

        Duration nextRunDuration = activeProfile.getNextRunDuration();
        Instant nextBreakTime = Instant.now().plus(nextRunDuration);
        state.setNextBreakTime(nextBreakTime);

        String formattedTime = TIME_FORMATTER.format(nextBreakTime);
        log.info("Next break scheduled in {} minutes at {}", nextRunDuration.toMinutes(), formattedTime);
    }

    /**
     * Checks on each client tick if a break should be triggered.
     */
    @Subscribe
    public void onGameTick(GameTick e) {
        if (!enabled || activeScript == null || activeProfile == null) return;

        if (state.isOnBreak()) return;

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
     * Initiates a break by pausing the script and optionally logging out.
     */
    private void startBreak(String reason) {
        if (activeScript == null) return;

        log.info("Starting break: {}", reason);
        state.setOnBreak(true);
        state.setBreakStartTime(Instant.now());
        state.setBreakReason(reason);

        // Pause the script
        activeScript.pause();

        // Logout if configured
        if (activeProfile.isLogoutDuringBreak() && client.getLocalPlayer() != null) {
            try {
                ctx.players().local().logout();
                log.info("Logged out for break");
            } catch (Exception e) {
                log.error("Failed to logout during break", e);
            }
        }

        // Schedule break end
        Duration breakDuration = activeProfile.getNextBreakDuration();
        scheduler.schedule(this::endBreak, breakDuration.toMillis(), TimeUnit.MILLISECONDS);
        log.info("Break will end in {} minutes", breakDuration.toMinutes());
    }

    /**
     * Ends the current break and resumes the script.
     */
    private void endBreak() {
        if (!state.isOnBreak() || activeScript == null) return;

        log.info("Ending break");
        state.setOnBreak(false);
        state.setBreakStartTime(null);

        if (activeProfile.isLogoutDuringBreak()) {
            try {
                loginService.login();
                log.info("Logged back in after break");
            } catch (Exception e) {
                log.error("Failed to login after break", e);
            }
        }

        activeScript.resume();
        scheduleNextBreak();
    }

    /**
     * Manually triggers a break.
     */
    public void triggerBreak(String reason) {
        if (activeScript != null && !state.isOnBreak()) {
            startBreak(reason);
        }
    }

    /**
     * Checks if the handler is currently managing a script.
     */
    public boolean isActive() {
        return activeScript != null;
    }
}