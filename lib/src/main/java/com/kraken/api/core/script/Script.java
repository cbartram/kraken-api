package com.kraken.api.core.script;

import com.google.inject.Singleton;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Singleton
public class Script implements Scriptable {
    private Future<?> future = null;
    private final static ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * This field is intended to be set in another class to define the script's main logic.
     * It is called repeatedly on a separate thread by the script's internal game tick handler. This
     * should be used over the onGameTick() method for core script logic because it runs in a separate thread
     * so sleeps and script delays can be used.
     */
    @Setter
    private Runnable loopTask;

    /**
     * Subscriber to the game tick event to handle dealing with starting new futures for the loop() method. Game ticks
     * execute every 0.6 seconds.
     * @param event the game tick event
     */
    public final void onGameTick(GameTick event) {
        if(loopTask == null) {
            log.error("Loop Task is not set. use script.setLoopTask(yourLoopRunnable) before starting the script.");
            return;
        }

        if(future != null && !future.isDone()) return;
        future = executor.submit(new RunnableTask(() -> {
            try {
                loopTask.run();
            } catch (RuntimeException e) {
                log.error("loop() has been interrupted: ", e);
            } catch (Throwable e) {
                log.error("Error in loop():", e);
            } finally {
                RunnableTask.dispose();
            }
        }));
    }

    /**
     * Gracefully stops a running asynchronous loop.
     * @param callback callback function to execute once the loop() is stopped
     */
    public void stop(Runnable callback) {
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