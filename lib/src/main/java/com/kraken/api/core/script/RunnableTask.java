package com.kraken.api.core.script;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class RunnableTask implements Runnable {

    @Getter
    private static volatile boolean canceled = false;

    @Getter
    private static volatile RunnableTask task = null;

    private final Runnable runnable;

    @Setter
    @Getter
    private boolean shouldYield = false;

    @Getter
    private final AtomicBoolean await = new AtomicBoolean(false);

    @Getter
    private long threadId = 0L;

    public RunnableTask(@NonNull Runnable runnable) {
        super();
        this.runnable = runnable;
    }

    public static void cancel() {
        canceled = true;
    }

    public static void dispose() {
        canceled = false;
        task = null;
    }

    @Override
    public void run() {
        threadId = Thread.currentThread().getId();
        task = this;

        if (runnable == null)
            return;
        try {
            runnable.run();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Runnable Task threw an exception: ", e);
        } finally {
            task = null;
            canceled = false;
        }
    }
}