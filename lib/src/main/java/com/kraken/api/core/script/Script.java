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

    public ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
    public ScheduledFuture<?> scheduledFuture;
    public ScheduledFuture<?> mainScheduledFuture;

    private Instant startTime; // when the script started

    @Getter
    private volatile boolean running = false;
    private Thread thread;

    @Inject
    public Script(final Context context) {
        this.context = context;

        if(!this.context.isRegistered()) {
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
     *         Return less than or equal to 0 to stop execution.
     */
    public abstract long loop();

    /**
     * Called once when the script is stopped (manually or by returning less than or equal to 0 from loop()).
     * Override this to clean up resources or reset services.
     */
    public abstract void onEnd();

    public final void start() {
        if (running) {
            return; // Already running
        }

        if(!this.context.isRegistered()) {
            this.context.register();
        }

        running = true;
        thread = new Thread(() -> {
            try {
                onStart();

                while (running) {
                    long delay = loop();

                    if (delay <= 0) {
                        break; // negative return means exit loop
                    }

                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            } finally {
                onEnd();
                running = false;
            }
        });

        thread.start();
    }

    public final void stop() {
        running = false;
        if(this.context.isRegistered()) {
            this.context.destroy();
        }

        if (thread != null) {
            thread.interrupt();
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public final boolean isRunning() {
        return running;
    }

    /**
     * Get the runtime formatted as HH:mm:ss
     * @return The runtime of the script formatted as a string
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
