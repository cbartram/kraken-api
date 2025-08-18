package com.kraken.api.core;

import com.google.inject.Inject;
import com.kraken.api.Context;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public abstract class Script implements Scriptable {
    // Any time a user extends this class, context will be injected registering the API Classes with the eventbus without
    // the user needing to manually register them. This API expects to be running only within the context of a RuneLite client.
    private final Context context;

    protected ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);
    protected ScheduledFuture<?> scheduledFuture;
    private ScheduledFuture<?> mainScheduledFuture;

    @Getter
    private volatile boolean running = false;

    @Inject
    public Script(final Context context) {
        this.context = context;
        this.context.register();
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
        if(!this.context.isRegistered()) {
            this.context.register();
        }

        onStart();

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                if (!running) {
                    stop();
                    return;
                }

                long delay = loop();

                // If loop() signals to stop
                if (delay <= 0) {
                    stop();
                    return;
                }

                // Reschedule with user-specified delay
                reschedule(delay);
            } catch (Exception e) {
                log.error("Exception in script loop: {}", this.getClass().getSimpleName(), e);
                stop();
            }
        }, 0, 100, TimeUnit.MILLISECONDS); // Initial 100ms default, will be overridden by reschedule
    }

    /**
     * Stops the script loop and calls {@link #onEnd()}.
     */
    public void stop() {
        if (!running) {
            return;
        }

        running = false;

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
     * Reschedules the main loop with a new delay.
     *
     * @param delay Delay in milliseconds between executions.
     */
    private void reschedule(long delay) {
        if (mainScheduledFuture != null && !mainScheduledFuture.isCancelled()) {
            mainScheduledFuture.cancel(false);
        }

        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            try {
                long nextDelay = loop();
                if (nextDelay <= 0) {
                    stop();
                } else {
                    reschedule(nextDelay);
                }
            } catch (Exception e) {
                log.error("Exception in script loop: {}", this.getClass().getSimpleName(), e);
                stop();
            }
        }, delay, delay, TimeUnit.MILLISECONDS);
    }
}
