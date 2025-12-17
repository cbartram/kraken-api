package com.kraken.api.core.script;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;

import java.lang.reflect.Method;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Singleton
public class Script extends Plugin implements Scriptable {
    private Future<?> future = null;
    private final static ExecutorService executor = Executors.newCachedThreadPool();

    /**
     * This method is intended to be overridden by subclasses to define the script's main logic.
     * It is called repeatedly on a separate thread by the script's internal game tick handler.
     */
    public void loop() throws Exception {
        // Intentionally left empty for subclasses to override.
        // Subclasses should implement their script's main logic here.
    }

    /**
     * Checks if a method with the given name and argument types is overridden in the provided object's class
     * compared to its superclass.
     *
     * @param obj The object whose class's method implementation is to be checked.
     * @param methodName The name of the method to check.
     * @param argTypes An array of {@code Class} objects representing the parameter types of the method.
     *                 If the method has no parameters, an empty array should be used.
     * @return {@code true} if the method is overridden in {@code obj}'s class; {@code false} otherwise,
     *         or if {@code obj} or {@code methodName} is null, or if the method does not exist.
     */
    private boolean isOverridden(Object obj, String methodName, Class<?>... argTypes) {
        if (obj == null || methodName == null) {
            return false;
        }

        Class<?> objClass = obj.getClass();
        Class<?> superClass = objClass.getSuperclass();

        try {
            Method objMethod = objClass.getDeclaredMethod(methodName, argTypes);
            Method superMethod = superClass.getMethod(methodName, argTypes);

            return !objMethod.equals(superMethod);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Subscriber to the game tick event to handle dealing with starting new futures for the loop() method. Game ticks
     * execute every 0.6 seconds.
     */
    @Subscribe
    public final void _onGameTick(GameTick event) {
        if (!isOverridden(this, "loop")) return;

        if(future != null && !future.isDone()) return;
        future = executor.submit(new RunnableTask(() -> {
            try {
                loop();
            } catch (RuntimeException e) {
                log.error("[{}] loop() has been interrupted: ", this.getName(), e);
            } catch (Throwable e) {
                log.error("[{}] Error in loop():", this.getName(), e);
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

        log.info("Stopping loop: {}", getName());
        RunnableTask.cancel();
        executor.submit(() -> {
            try {
                while(!future.isDone()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {}
                }
                log.info("loop stopped: {}", getName());
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