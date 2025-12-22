package com.kraken.api.service.util;

import com.kraken.api.Context;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;

@Slf4j
public class TaskChain {

    private final Context ctx;
    private final Queue<Task> tasks = new LinkedList<>();
    
    private interface Task {
        boolean execute() throws InterruptedException;
    }

    private TaskChain(Context ctx) {
        this.ctx = ctx;
    }

    /**
     * Creates a new instance of {@literal TaskChain} using the specified {@literal Context}.
     * <p>
     * This method serves as a static factory for initializing a {@literal TaskChain}
     * with the given {@literal Context}. The resulting {@literal TaskChain} can be used
     * to construct and execute a sequence of tasks.
     *
     * @param ctx the {@literal Context} used to initialize the {@literal TaskChain}.
     *            This context is typically required for operations that depend on
     *            thread-safe execution and client interactions. Cannot be {@code null}.
     *            Passing {@code null} will result in a {@code NullPointerException}.
     *
     * @return a new {@literal TaskChain} instance initialized with the specified {@literal Context}.
     */
    public static TaskChain builder(Context ctx) {
        return new TaskChain(ctx);
    }

    /**
     * Adds a task to the chain that executes the specified {@literal @}Runnable action 
     * on the client thread and immediately proceeds to the next task in the chain.
     *
     * <p>The action is executed synchronously on the client thread using the 
     * {@literal @}Context's {@code runOnClientThread} method. This ensures the action is 
     * performed in a thread-safe manner with respect to client operations.</p>
     *
     * @param action the {@literal @}Runnable task to be executed on the client thread.
     *               Cannot be null. Throws a {@literal @}NullPointerException if null is passed.
     *
     * @return the current instance of {@literal @}TaskChain, allowing for 
     *         method chaining of additional tasks.
     */
    public TaskChain run(Runnable action) {
        tasks.add(() -> {
            ctx.runOnClientThread(action);
            return true; // Move to next step immediately
        });
        return this;
    }


    /**
     * Walks to a specific WorldPoint using the LocalPathfinder and MovementService.
     * <p>
     * This method utilizes {@code findSparsePath} to calculate an efficient route
     * and processes the path sequentially. It will wait for the player to reach
     * each waypoint (within 1 tile) before proceeding to the next.
     * </p>
     *
     * @param target The destination WorldPoint.
     * @return The current TaskChain instance.
     */
    public TaskChain walkTo(WorldPoint target) {
        tasks.add(() -> {
            MovementService movement = ctx.getService(MovementService.class);
            LocalPathfinder pathfinder = ctx.getService(LocalPathfinder.class);
            Client client = ctx.getClient();

            // Guardrail: Prevent infinite loops if pathing fails repeatedly
            int maxRefreshes = 50;

            for (int i = 0; i < maxRefreshes; i++) {
                WorldPoint currentLoc = client.getLocalPlayer().getWorldLocation();

                // 1. Check completion condition (Are we there yet?)
                if (currentLoc.distanceTo(target) <= 3) {
                    return true;
                }

                // 2. Calculate path (Client Thread)
                // This might return a path to the TARGET (if close) or the EDGE (if far)
                AtomicReference<List<WorldPoint>> pathRef = new AtomicReference<>();
                ctx.runOnClientThread(() -> pathRef.set(pathfinder.findPath(currentLoc, target)));
                List<WorldPoint> densePath = pathRef.get();

                // If path is empty but we aren't at the target, we are stuck/unreachable
                if (densePath == null || densePath.isEmpty()) {
                    log.warn("TaskChain: Path calculation failed or target unreachable: {}", target);
                    return false;
                }

                // 3. Filter for variable stride (human-like steps)
                List<WorldPoint> stridedPath = movement.applyVariableStride(densePath);

                // 4. Walk this segment, this blocks the for loop until the current path is traversed
                // If traverse returns false (timeout/stuck), we abort the whole chain
                if (!movement.traversePath(client, movement, stridedPath)) {
                    return false;
                }

                // If traverse succeeded, the loop restarts.
                // We are now at the end of the previous path (likely the scene edge).
                // The next iteration will re-run pathfinder.findPath from the NEW location.
            }

            log.error("TaskChain: Exceeded max scene refreshes walking to {}", target);
            return false;
        });
        return this;
    }

    /**
     * Walks to an approximate location within a radius of the target.
     * Useful for banking or interacting with large objects where exact tile precision isn't needed.
     *
     * @param target The center WorldPoint.
     * @param radius The radius to search for a walkable tile.
     * @return The current TaskChain instance.
     */
    public TaskChain walkTo(WorldPoint target, int radius) {
        tasks.add(() -> {
            MovementService movement = ctx.getService(MovementService.class);
            LocalPathfinder pathfinder = ctx.getService(LocalPathfinder.class);
            Client client = ctx.getClient();

            int maxRefreshes = 50;

            for (int i = 0; i < maxRefreshes; i++) {
                WorldPoint currentLoc = client.getLocalPlayer().getWorldLocation();

                // 2. Calculate path to the area
                AtomicReference<List<WorldPoint>> pathRef = new AtomicReference<>();
                ctx.runOnClientThread(() -> {
                    // Note: findApproximatePath handles logic for "nearest tile in area"
                    pathRef.set(pathfinder.findApproximatePath(currentLoc, target, radius));
                });

                List<WorldPoint> densePath = pathRef.get();

                if (densePath == null || densePath.isEmpty()) {
                    log.warn("TaskChain: No path found to target {}", target);
                    return false;
                }

                List<WorldPoint> stridedPath = movement.applyVariableStride(densePath);
                if (!movement.traversePath(client, movement, stridedPath)) {
                    return false;
                }
            }
            return false;
        });
        return this;
    }

    /**
     * Walks to a random reachable point inside a specific WorldArea.
     *
     * @param area The WorldArea to walk into.
     * @return The current TaskChain instance.
     */
    public TaskChain walkTo(WorldArea area) {
        tasks.add(() -> {
            MovementService movement = ctx.getService(MovementService.class);
            LocalPathfinder pathfinder = ctx.getService(LocalPathfinder.class);
            Client client = ctx.getClient();

            int maxRefreshes = 50;

            for (int i = 0; i < maxRefreshes; i++) {
                WorldPoint currentLoc = client.getLocalPlayer().getWorldLocation();

                // 1. Check if we are inside the area (Goal reached)
                if (area.contains(currentLoc)) {
                    return true;
                }

                // 2. Calculate path to the area
                AtomicReference<List<WorldPoint>> pathRef = new AtomicReference<>();
                ctx.runOnClientThread(() -> {
                    // Note: findApproximatePath handles logic for "nearest tile in area"
                    pathRef.set(pathfinder.findApproximatePath(currentLoc, area));
                });

                List<WorldPoint> densePath = pathRef.get();

                if (densePath == null || densePath.isEmpty()) {
                    log.warn("TaskChain: No path found to area {}", area);
                    return false;
                }

                List<WorldPoint> stridedPath = movement.applyVariableStride(densePath);

                if (!movement.traversePath(client, movement, stridedPath)) {
                    return false;
                }
            }
            return false;
        });
        return this;
    }

    /**
     * Retries a specific action until a condition is met or the maximum number of retries is reached.
     * <p>
     * This is useful for "flaky" interactions, such as clicking a bank booth (which might miss)
     * or attacking an NPC (which might be interrupted).
     * </p>
     *
     * @param action The {@link Runnable} action to perform (e.g., {@code () -> object.interact("Open")}).
     * This is automatically executed on the client thread.
     * @param successCondition A {@link BooleanSupplier} that determines if the action succeeded
     * (e.g., {@code () -> player.getAnimation() != IDLE}).
     * Checked on the client thread.
     * @param maxRetries The maximum number of times to retry the action if the condition returns false.
     * @param retryDelayMs The time to wait (in ms) between the action and the check (and before the next retry).
     * This should be at least 600ms (1 tick) for most game actions.
     * @return The current {@link TaskChain} instance.
     */
    public TaskChain retryUntil(Runnable action, BooleanSupplier successCondition, int maxRetries, int retryDelayMs) {
        tasks.add(() -> {
            for (int attempt = 0; attempt <= maxRetries; attempt++) {

                ctx.runOnClientThread(action);
                Thread.sleep(retryDelayMs);

                AtomicBoolean success = new AtomicBoolean(false);
                ctx.runOnClientThread(() -> success.set(successCondition.getAsBoolean()));

                if (success.get()) {
                    return true;
                }

                if (attempt < maxRetries) {
                    log.info("TaskChain: Condition not met, retrying... (Attempt {}/{})", attempt + 1, maxRetries);
                }
            }

            log.warn("TaskChain: retryUntil failed after {} attempts.", maxRetries + 1);
            return false;
        });
        return this;
    }

    /**
     * Retries an action until a condition is met using default settings (3 retries, 600ms delay).
     *
     * @param action The action to perform.
     * @param successCondition The condition to check for success.
     * @return The current {@link TaskChain} instance.
     */
    public TaskChain retryUntil(Runnable action, BooleanSupplier successCondition) {
        return retryUntil(action, successCondition, 3, 600);
    }

    /**
     * Waits until the given condition is met or a timeout occurs, checking the condition
     * periodically at the specified interval. If the condition is not met within the timeout period,
     * the task chain will stop execution and log a warning.
     *
     * <p>This method continuously evaluates the {@code condition} on the client thread using the
     * {@code runOnClientThread} method provided by the {@code Context}. The evaluation is performed
     * in a thread-safe manner, with the specified delay between checks.</p>
     *
     * @param condition a {@literal @}BooleanSupplier representing the condition to wait for. The condition
     *                  is evaluated on the client thread. Cannot be null. A null value may result in
     *                  unexpected runtime behavior.
     * @param checkDelayMs the delay (in milliseconds) between subsequent evaluations of the {@code condition}.
     *                     This determines how frequently the condition is checked.
     * @param timeoutMs the maximum time (in milliseconds) to wait for the {@code condition} to evaluate to {@code true}.
     *                  If the condition is not met within this duration, the chain execution terminates.
     *
     * @return the current instance of {@literal @}TaskChain, allowing for further chaining of tasks.
     */
    public TaskChain waitUntil(BooleanSupplier condition, int checkDelayMs, int timeoutMs) {
        tasks.add(() -> {
            long start = System.currentTimeMillis();
            while (true) {
                if (System.currentTimeMillis() - start > timeoutMs) {
                    log.warn("TaskChain timed out waiting for condition.");
                    return false;
                }

                AtomicBoolean result = new AtomicBoolean(false);
                ctx.runOnClientThread(() -> result.set(condition.getAsBoolean()));

                if (result.get()) {
                    return true; // Condition met, move next
                }

                // Sleep the background thread to wait for next check
                Thread.sleep(checkDelayMs);
            }
        });
        return this;
    }

    /**
     * Waits until the given condition is met, using default delay and timeout values.
     * If the condition is not met within the default timeout period, the task chain will stop execution
     * and log a warning.
     *
     * <p>This method evaluates the {@code condition} on the client thread and continues to the next task
     * in the chain when the condition is satisfied. The evaluations are performed in a thread-safe manner.</p>
     *
     * @param condition a {@literal @}BooleanSupplier representing the condition to wait for.
     *                  The condition is evaluated on the client thread. Cannot be null. A null value
     *                  may result in unexpected runtime behavior.
     *
     * @return the current instance of {@literal @}TaskChain, allowing for method chaining of additional tasks.
     */
    public TaskChain waitUntil(BooleanSupplier condition) {
        return waitUntil(condition, 100, 10000);
    }

    /**
     * Waits until the given condition is met or a timeout occurs. The condition is checked
     * periodically at a default interval. If the condition is not met within the timeout period,
     * the task chain will stop execution and log a warning.
     *
     * <p>This method evaluates the {@code condition} on the client thread and continues to the next
     * task in the chain when the condition is satisfied. The evaluations are performed in a
     * thread-safe manner using the {@code runOnClientThread} method of the {@code Context}.</p>
     *
     * @param condition a {@literal @}BooleanSupplier representing the condition to wait for. The condition
     *                  is evaluated on the client thread. Cannot be null. A null value may result in
     *                  unintended runtime behavior.
     * @param timeout the maximum time (in milliseconds) to wait for the {@code condition} to evaluate to
     *                {@code true}. If the condition is not met within this duration, the chain execution
     *                terminates.
     *
     * @return the current instance of {@literal @}TaskChain, allowing for method chaining of additional tasks.
     */
    public TaskChain waitUntil(BooleanSupplier condition, int timeout) {
        return waitUntil(condition, 100, timeout);
    }

    /**
     * Adds a delay to the task chain, pausing execution for the specified duration in milliseconds
     * before proceeding to the next task.
     *
     * <p>This method schedules a task that introduces the delay by invoking {@literal Thread.sleep(ms)}.
     * As a result, chain progression is paused during the delay duration.</p>
     *
     * @param ms The duration of the delay in milliseconds. Must be a non-negative integer;
     *           a value less than 0 may result in unexpected behavior.
     *
     * @return The current instance of {@literal TaskChain}, allowing for method chaining of additional tasks.
     */
    public TaskChain delay(int ms) {
        tasks.add(() -> {
            Thread.sleep(ms);
            return true;
        });
        return this;
    }

    /**
     * Adds a delay to the task chain, pausing execution for the specified duration measured in game ticks
     * before proceeding to the next task.
     *
     * <p>A tick is typically defined as the basic unit of game time, depending on the underlying game architecture.
     * This method schedules a task that introduces a delay by syncing with the {@literal @}SleepService, which
     * ensures proper integration with the game's scheduling system.</p>
     *
     * @param ticks The number of game ticks to delay execution. Must be a positive integer; providing a value
     *              less than or equal to 0 may result in undefined behavior.
     *
     * @return The current instance of {@literal @}TaskChain, allowing for method chaining of additional tasks.
     */
    public TaskChain delayTicks(int ticks) {
        if(ticks <= 0) {
            return this;
        }

        tasks.add(() -> {
            SleepService sleep = ctx.getService(SleepService.class);
            sleep.tick(ticks);
            return true;
        });
        return this;
    }

    /**
     * Executes the task chain sequentially, proceeding through each task until all tasks are complete,
     * an error occurs, or the chain is interrupted.
     *
     * <p>The method polls tasks from an internal task queue and executes them in the order they are added.
     * If any task fails (e.g., returns {@code false}) or an {@link InterruptedException} occurs, the chain
     * halts execution and returns {@code false}.
     *
     * <p>This method performs tasks synchronously, blocking until the completion or failure of all tasks
     * in the chain.
     *
     * @return {@code true} if all tasks in the chain are executed successfully;
     *         {@code false} if any task fails or the chain is interrupted.
     */
    public boolean execute() {
        try {
            while (!tasks.isEmpty()) {
                Task task = tasks.poll();
                if (!task.execute()) {
                    return false; // Task failed (e.g. timeout)
                }
            }
            return true;
        } catch (InterruptedException e) {
            log.debug("Chain interrupted");
            return false;
        }
    }

    /**
     * Adds all tasks from the specified {@literal @}TaskChain to the current task chain.
     *
     * <p>This method incorporates all tasks from the given {@literal @}TaskChain instance,
     * ensuring they are executed sequentially as part of the current task chain. The added
     * tasks are executed in the order they exist in {@code otherChain}, maintaining their
     * original sequence within the chain.</p>
     *
     * @param otherChain the {@literal @}TaskChain whose tasks are to be added to the current chain.
     *                   Cannot be null. If {@code null} is passed, the behavior is undefined and
     *                   may result in a runtime exception.
     *
     * @return the current instance of {@literal @}TaskChain, allowing for method chaining of additional tasks.
     */
    public TaskChain add(TaskChain otherChain) {
        tasks.add(otherChain::execute);
        return this;
    }
}
