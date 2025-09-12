package com.kraken.api.core.script;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public abstract class Script implements Scriptable {
    // Any time a user extends this class, context will be injected registering the API Classes with the eventbus without
    // the user needing to manually register them. This API expects to be running only within the context of a RuneLite client.
    private final Context context;

    protected ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
    protected ScheduledFuture<?> scheduledFuture;
    private ScheduledFuture<?> mainScheduledFuture;

    private Instant startTime; // when the script started

    @Getter
    private volatile boolean running = false;

    @Inject
    public Script(final Context context) {
        this.context = context;

        if(!this.context.isRegistered()) {
            log.info("Registering services classes with the eventbus.");
            this.context.register();
        }
    }

    /**
     * Called once before the script starts running.
     * Override this to initialize resources or set up services.
     */
    public abstract void onStart();

    /**
     * Called on every scheduled iteration of the script loop.
     * This is where the user's main logic goes.
     *
     * @return Delay in milliseconds before the next loop iteration.
     *         Return <= 0 to stop execution.
     */
    public abstract long loop();

    /**
     * Called once when the script is stopped (manually or by returning <= 0 from loop()).
     * Override this to clean up resources or reset services.
     */
    public abstract void onEnd();

    /**
     * Starts the script loop.
     * Will call {@link #onStart()} before the first loop execution.
     */
    public void start() {
        if (running) {
            log.warn("Script already running: {}", this.getClass().getSimpleName());
            return;
        }

        running = true;
        startTime = Instant.now();

        if(!this.context.isHooksLoaded()) {
            this.context.loadHooks();
        }

        if(!this.context.isRegistered()) {
            log.info("Registering services classes with the eventbus.");
            this.context.register();
        }

        onStart();

        // Simple recursive scheduling
        scheduleNext(0);
    }

    /**
     * Reschedules the main loop with a new delay.
     *
     * @param delay Delay in milliseconds between executions.
     */
    private void scheduleNext(long delay) {
        if (!running) {
            return;
        }

        mainScheduledFuture = scheduledExecutorService.schedule(() -> {
            try {
                if (!running) {
                    stop();
                    return;
                }

                log.info("Executing loop");
                long nextDelay = loop();

                if (nextDelay <= 0) {
                    stop();
                } else {
                    scheduleNext(nextDelay); // Schedule the next execution
                }
            } catch (Exception e) {
                log.error("Exception in script loop: {}", this.getClass().getSimpleName(), e);
                stop();
            }
        }, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * Stops the script loop and calls {@link #onEnd()}.
     */
    public void stop() {
        if (!running) {
            return;
        }

        running = false;
        startTime = null;

        if (mainScheduledFuture != null && !mainScheduledFuture.isDone()) {
            mainScheduledFuture.cancel(true);
        }

        if (scheduledFuture != null && !scheduledFuture.isDone()) {
            scheduledFuture.cancel(true);
        }

        try {
            onEnd();
        } catch (Exception e) {
            log.error("Exception in script onEnd: {}", this.getClass().getSimpleName(), e);
        }

        if(this.context.isRegistered()) {
            this.context.destroy();
        }
    }

    /**
     * Get the runtime formatted as HH:mm:ss
     */
    public String getRuntimeString() {
        if (startTime == null) {
            return "00:00:00";
        }
        Duration runtime = Duration.between(startTime, Instant.now());
        long hours = runtime.toHours();
        long minutes = runtime.toMinutesPart();
        long seconds = runtime.toSecondsPart();
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
}
