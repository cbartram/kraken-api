package com.kraken.api.service.util;

import com.kraken.api.Context;
import com.kraken.api.core.script.RunnableTask;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.client.RuneLite;

import javax.inject.Singleton;
import java.util.concurrent.Callable;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Slf4j
@Singleton
public class SleepService {

    private static final Context ctx = RuneLite.getInjector().getInstance(Context.class);

    /**
     * Waits until the specified condition is true.
     * @param condition the condition to be met
     */
    public static void sleepUntil(Supplier<Boolean> condition) {
        while(!condition.get()) {
            if(Thread.currentThread().isInterrupted() || RunnableTask.isCanceled()) {
                throw new RuntimeException();
            }
            sleep(100);
        }
    }

    /**
     * sleeps until the specified condition is true or the timeout is reached.
     * @param condition the condition to be met
     * @param timeoutMS the maximum time to sleep in milliseconds
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean sleepUntil(Supplier<Boolean> condition, long timeoutMS) {
        long start = System.currentTimeMillis();
        while(!condition.get()) {
            if(System.currentTimeMillis() - start > timeoutMS) {
                return false;
            }

            if(Thread.currentThread().isInterrupted() || RunnableTask.isCanceled()) {
                throw new RuntimeException();
            }

            sleep(100);
        }
        return true;
    }

    /**
     * sleeps until the specified condition is true or the timeout is reached.
     * @param condition the condition to be met
     * @param ticks the maximum time to sleep in game ticks
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean sleepUntil(Supplier<Boolean> condition, int ticks) {
        int end = ctx.getClient().getTickCount() + ticks;
        while(!condition.get()) {
            if(ctx.getClient().getTickCount() >= end) {
                return false;
            }

            if(Thread.currentThread().isInterrupted() || RunnableTask.isCanceled()) {
                throw new RuntimeException();
            }

            sleep(100);
        }
        return true;
    }

    /**
     * Sleeps until the local player's animation is idle.
     */
    public static void sleepUntilIdle() {
        do {
            tick();
        } while (!ctx.players().local().isIdle());
    }

    /**
     * Sleeps until the local player reaches the specified world tile.
     * @param worldX the x-coordinate of the target tile
     * @param worldY the y-coordinate of the target tile
     */
    public static void sleepUntilTile(int worldX, int worldY) {
        Player player = ctx.players().local().raw();
        while((player.getWorldLocation().getX() != worldX || player.getWorldLocation().getY() != worldY)) {
            tick();
        }
    }

    /**
     * Sleeps for a specified amount of time in milliseconds. This sleep is interruptible by script cancellation.
     * @param duration the duration to sleep in milliseconds
     */
    public static void sleep(int duration) {
        // Prevent sleeping on the client thread to avoid freezing the game
        if (ctx.getClient().isClientThread()) return;
        sleep((long) duration);
    }

    /**
     * Sleeps for a specified amount of time in milliseconds. This sleep is interruptible by script cancellation.
     * @param time the duration to sleep in milliseconds
     */
    public static void sleep(long time) {
        if (time <= 0) {
            return;
        }
        long end = System.currentTimeMillis() + time;
        while (System.currentTimeMillis() < end) {
            if (RunnableTask.isCanceled()) {
                throw new RuntimeException("Script stopped during sleep");
            }
            try {
                long remaining = end - System.currentTimeMillis();
                if (remaining > 0) {
                    Thread.sleep(Math.min(remaining, 50)); // Check for cancellation every 50ms
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Sleep interrupted", e);
            }
        }
    }

    /**
     * Sleeps for a random duration between start and end milliseconds.
     * @param start the minimum sleep time
     * @param end the maximum sleep time
     */
    public static void sleep(long start, long end) {
        if (start < 0 || end < 0 || start > end) {
            throw new IllegalArgumentException("Invalid sleep range: " + start + " to " + end);
        }
        long randomSleep = RandomService.between((int) start, (int) end);
        sleep(randomSleep);
    }

    /**
     * Sleeps for a random duration between start and end milliseconds.
     * @param start the minimum sleep time
     * @param end the maximum sleep time
     */
    public static void sleep(int start, int end) {
        int randomSleep = RandomService.between(start, end);
        sleep(randomSleep);
    }

    /**
     * Sleeps for a duration based on a Gaussian distribution.
     * @param mean the mean sleep time
     * @param stddev the standard deviation of the sleep time
     */
    public static void sleepGaussian(int mean, int stddev) {
        int randomSleep = RandomService.randomGaussian(mean, stddev);
        sleep(randomSleep);
    }

    /**
     * Repeatedly calls a method until it returns a non-null value, or a timeout is reached.
     * @param method the method to call
     * @param timeoutMillis the maximum time to wait in milliseconds
     * @param sleepMillis the time to sleep between calls
     * @param <T> the return type of the method
     * @return the non-null value, or null if the timeout was reached
     */
    @SneakyThrows
    public static <T> T sleepUntilNotNull(Callable<T> method, int timeoutMillis, int sleepMillis) {
        if (ctx.getClient().isClientThread()) return null;
        boolean done;
        T methodResponse;
        final long endTime = System.currentTimeMillis()+timeoutMillis;
        do {
            if (RunnableTask.isCanceled()) {
                throw new RuntimeException("Script stopped");
            }
            methodResponse = method.call();
            done = methodResponse != null;
            if (!done) {
                sleep(sleepMillis);
            }
        } while (!done && System.currentTimeMillis() < endTime);
        return methodResponse;
    }

    /**
     * Repeatedly calls a method until it returns a non-null value, or a timeout is reached.
     * Sleeps 100ms between calls.
     * @param method the method to call
     * @param timeoutMillis the maximum time to wait in milliseconds
     * @param <T> the return type of the method
     * @return the non-null value, or null if the timeout was reached
     */
    public static <T> T sleepUntilNotNull(Callable<T> method, int timeoutMillis) {
        return sleepUntilNotNull(method, timeoutMillis, 100);
    }

    /**
     * Waits until the specified condition is true, with a default timeout of 5000ms.
     * @param awaitedCondition the condition to be met
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean sleepUntil(BooleanSupplier awaitedCondition) {
        return sleepUntil(awaitedCondition, 5000);
    }

    /**
     * Waits until the specified condition is true, or a timeout is reached.
     * @param awaitedCondition the condition to be met
     * @param time the maximum time to wait in milliseconds
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean sleepUntil(BooleanSupplier awaitedCondition, int time) {
        if (ctx.getClient().isClientThread()) return false;
        boolean done;
        long startTime = System.currentTimeMillis();
        do {
            if (RunnableTask.isCanceled()) {
                throw new RuntimeException("Script stopped");
            }
            done = awaitedCondition.getAsBoolean();
            if (!done) {
                sleep(100);
            }
        } while (!done && System.currentTimeMillis() - startTime < time);
        return done;
    }

    /**
     * Sleeps the current thread for one game tick
     */
    public static void tick() {
        sleepFor(1);
    }

    /**
     * Sleeps the current thread by the specified number of game ticks
     * @param ticks ticks
     */
    public static void sleepFor(int ticks) {
        int tick = ctx.getClient().getTickCount() + ticks;
        int start = ctx.getClient().getTickCount();
        while(ctx.getClient().getTickCount() < tick && ctx.getClient().getTickCount() >= start) {
            if(Thread.currentThread().isInterrupted() || RunnableTask.isCanceled()) {
                throw new RuntimeException();
            }
            sleep(20);
        }
    }

    /**
     * Waits until the specified condition is true, checking at a given interval, until a timeout is reached.
     * @param awaitedCondition the condition to be met
     * @param time the time to sleep between checks in milliseconds
     * @param timeout the maximum time to wait in milliseconds
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean sleepUntilTrue(BooleanSupplier awaitedCondition, int time, int timeout) {
        if (ctx.getClient().isClientThread()) return false;
        long startTime = System.currentTimeMillis();
        do {
            if (RunnableTask.isCanceled()) {
                throw new RuntimeException("Script stopped");
            }
            if (awaitedCondition.getAsBoolean()) {
                return true;
            }
            sleep(time);
        } while (System.currentTimeMillis() - startTime < timeout);
        return false;
    }

    /**
     * Waits until the specified condition is true, checking every 100ms, until a timeout is reached.
     * @param awaitedCondition the condition to be met
     * @param timeout the maximum time to wait in milliseconds
     * @return true if the condition was met, false if the timeout was reached
     */
    public static boolean sleepUntilTrue(BooleanSupplier awaitedCondition, int timeout) {
        if (ctx.getClient().isClientThread()) return false;
        long startTime = System.currentTimeMillis();
        do {
            if (RunnableTask.isCanceled()) {
                throw new RuntimeException("Script stopped");
            }
            if (awaitedCondition.getAsBoolean()) {
                return true;
            }
            sleep(100);
        } while (System.currentTimeMillis() - startTime < timeout);
        return false;
    }

    /**
     * Sleeps while the specified condition is true or until the timeout is reached.
     * @param condition the condition to be met
     * @param timeout the maximum time to sleep in milliseconds
     * @return true if the condition became false, false if the timeout was reached
     */
    public static boolean sleepWhile(BooleanSupplier condition, int timeout) {
        long start = System.currentTimeMillis();
        while (condition.getAsBoolean()) {
            if (System.currentTimeMillis() - start > timeout) {
                return false;
            }

            if (Thread.currentThread().isInterrupted() || RunnableTask.isCanceled()) {
                throw new RuntimeException();
            }

            sleep(100);
        }
        return true;
    }
}