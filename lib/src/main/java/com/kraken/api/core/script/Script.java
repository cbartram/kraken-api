package com.kraken.api.core.script;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public abstract class Script implements Scriptable {

    @Inject
    private EventBus eventBus;

    private Future<?> future = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isRunning = false;

    /**
     * This field is intended to be overridden in the plugins subclass defining the script's main logic.
     * It is called repeatedly on a separate thread by the script's internal game tick handler. This
     * should be used over the onGameTick() method for core script logic because it runs in a separate thread
     * so sleeps and script delays can be used.
     */
    public abstract int loop();

    /**
     * Optional: Called when the script starts.
     */
    public void onStart() {}

    /**
     * Optional: Called when the script stops.
     */
    public void onStop() {}

    /**
     * Starts the script, registers event listeners, and calls onStart.
     */
    public final void start() {
        if (isRunning) return;
        isRunning = true;
        eventBus.register(this);
        log.info("Script started");
        onStart();
    }

    /**
     * Subscriber to the game tick event to handle dealing with starting new futures for the loop() method. Game ticks
     * execute every 0.6 seconds.
     * @param event the game tick event
     */
    @Subscribe
    public final void onGameTick(GameTick event) {
        if (!isRunning) return;

        if (future != null && !future.isDone()) return;

        future = executor.submit(() -> {
            try {
                int delay = loop();
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            } catch (InterruptedException e) {
                // Thread interrupted, likely due to stop() being called
            } catch (Exception e) {
                log.error("Error in script loop:", e);
            } finally {
                RunnableTask.dispose();
            }
        });
    }

    /**
     * Gracefully stops a running asynchronous loop.
     * @param callback callback function to execute once the loop() is stopped
     */
    public void stop(Runnable callback) {
        if (!isRunning) return;
        isRunning = false;
        eventBus.unregister(this);

        if(future == null || future.isDone()) callback.run();

        log.info("Stopping loop");
        RunnableTask.cancel();
        executor.submit(() -> {
            try {
                while(!future.isDone()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {}
                }
                log.info("loop stopped");
                onStop();
                if(callback != null) callback.run();
            } catch (Exception e) {
                System.err.println("Task execution failed: " + e.getMessage());
            }
        });
    }

    /**
     * Gracefully stops a running asynchronous loop.
     */
    public void stop() {
        stop(() -> {});
    }
}