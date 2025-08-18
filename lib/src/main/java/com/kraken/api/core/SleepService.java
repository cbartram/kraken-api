package com.kraken.api.core;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Singleton;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;

@Slf4j
@Singleton
public class SleepService extends AbstractService {
     ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(10);
     ScheduledFuture<?> scheduledFuture;

    public  ScheduledFuture<?> awaitExecutionUntil(Runnable callback, BooleanSupplier awaitedCondition, int time) {
        scheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (awaitedCondition.getAsBoolean()) {
                scheduledFuture.cancel(true);
                scheduledFuture = null;
                callback.run();
            }
        }, 0, time, TimeUnit.MILLISECONDS);
        return scheduledFuture;
    }

    public void sleep(int start) {
        if (client.isClientThread()) return;
        try {
            Thread.sleep(start);
        } catch (InterruptedException ignored) {
            // ignore interrupted
        }
    }

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ignored) {
            // ignore interrupted
        }
    }

    public void sleep(int start, int end) {
        int randomSleep = Random.between(start, end);
        sleep(randomSleep);
    }

    public void sleepGaussian(int mean, int stddev) {
        int randomSleep = Random.randomGaussian(mean, stddev);
        sleep(randomSleep);
    }

    @SneakyThrows
    public <T> T sleepUntilNotNull(Callable<T> method, int timeoutMillis, int sleepMillis) {
        if (client.isClientThread()) return null;
        boolean done;
        T methodResponse;
        final long endTime = System.currentTimeMillis()+timeoutMillis;
        do {
            methodResponse = method.call();
            done = methodResponse != null;
            sleep(sleepMillis);
        } while (!done && System.currentTimeMillis() < endTime);
        return methodResponse;
    }

    public <T> T sleepUntilNotNull(Callable<T> method, int timeoutMillis) {
        return sleepUntilNotNull(method, timeoutMillis, 100);
    }

    public boolean sleepUntil(BooleanSupplier awaitedCondition) {
        return sleepUntil(awaitedCondition, 5000);
    }

    public boolean sleepUntil(BooleanSupplier awaitedCondition, int time) {
        if (client.isClientThread()) return false;
        boolean done = false;
        long startTime = System.currentTimeMillis();
        try {
            do {
                done = awaitedCondition.getAsBoolean();
                sleep(100);
            } while (!done && System.currentTimeMillis() - startTime < time);
        } catch (Exception e) {
            log.error("failed to sleep: {}", e.getMessage());
        }
        return done;
    }

    public boolean sleepUntil(BooleanSupplier awaitedCondition, Runnable action, long timeoutMillis, int sleepMillis) {
        if (client.isClientThread()) return false;
        long startTime = System.nanoTime();
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(timeoutMillis);
        try {
            while (System.nanoTime() - startTime < timeoutNanos) {
                if (awaitedCondition.getAsBoolean()) {
                    return true;
                }
                action.run();
                sleep(sleepMillis);
            }
        } catch (Exception e) {
            log.error("failed to sleep: {}", e.getMessage());
        }
        return false;
    }

    public boolean sleepUntilTrue(BooleanSupplier awaitedCondition) {
        if (client.isClientThread()) return false;
        long startTime = System.currentTimeMillis();
        try {
            do {
                if (awaitedCondition.getAsBoolean()) {
                    return true;
                }
                sleep(100);
            } while (System.currentTimeMillis() - startTime < 5000);
        } catch (Exception e) {
            log.error("failed to sleep: {}", e.getMessage());
        }
        return false;
    }

    public boolean sleepUntilTrue(BooleanSupplier awaitedCondition, int time, int timeout) {
        if (client.isClientThread()) return false;
        long startTime = System.currentTimeMillis();
        try {
            do {
                if (awaitedCondition.getAsBoolean()) {
                    return true;
                }
                sleep(time);
            } while (System.currentTimeMillis() - startTime < timeout);
        } catch (Exception e) {
            log.error("failed to sleep: {}", e.getMessage());
        }
        return false;
    }

    public boolean sleepUntilTrue(BooleanSupplier awaitedCondition, BooleanSupplier resetCondition, int time, int timeout) {
        if (client.isClientThread()) return false;
        long startTime = System.currentTimeMillis();
        try {
            do {
                if (resetCondition.getAsBoolean()) {
                    startTime = System.currentTimeMillis();
                }
                if (awaitedCondition.getAsBoolean()) {
                    return true;
                }
                sleep(time);
            } while (System.currentTimeMillis() - startTime < timeout);
        } catch (Exception e) {
            log.error("failed to sleep: {}", e.getMessage());
        }
        return false;
    }

    public void sleepUntilOnClientThread(BooleanSupplier awaitedCondition) {
        sleepUntilOnClientThread(awaitedCondition, Random.between(2500, 5000));
    }

    public void sleepUntilOnClientThread(BooleanSupplier awaitedCondition, int time) {
        if (client.isClientThread()) return;
        boolean done;
        long startTime = System.currentTimeMillis();
        try {
            do {
                done = context.runOnClientThreadOptional(awaitedCondition::getAsBoolean).orElse(false);
            } while (!done && System.currentTimeMillis() - startTime < time);
        } catch (Exception e) {
            
        }
    }

    public boolean sleepUntilTick(int ticksToWait) {
        int startTick = client.getTickCount();
        return sleepUntil(() -> client.getTickCount() >= startTick + ticksToWait, ticksToWait * 600 + 2000);
    }
}
