package com.kraken.api.interaction.movement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.core.SleepService;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.MovementPackets;
import com.kraken.api.interaction.player.PlayerService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.tile.TileService;
import com.kraken.api.interaction.ui.UIService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Singleton
public class WalkService extends AbstractService {

    @Inject
    private ShortestPathService shortestPathService;

    @Inject
    private SleepService sleepService;

    @Inject
    private PlayerService playerService;

    @Inject
    private TileService tileService;

    @Inject
    private ReflectionService reflectionService;

    @Inject
    private UIService uiService;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private MovementPackets movementPackets;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
    private final AtomicReference<MovementTask> currentTask = new AtomicReference<>();

    /**
     * Initiates walking to a destination point asynchronously.
     * @param destination The target WorldPoint
     * @return CompletableFuture that completes when movement finishes
     */
    public CompletableFuture<MovementState> walkToAsync(WorldPoint destination) {
        cancelCurrentMovement();

        if (client.getLocalPlayer().getWorldLocation().getPlane() != destination.getPlane()) {
            log.warn("Player is on different plane from destination point.");
            return CompletableFuture.completedFuture(MovementState.BLOCKED);
        }

        MovementTask task = new MovementTask(destination);
        currentTask.set(task);

        CompletableFuture<MovementState> future = new CompletableFuture<>();
        task.setFuture(future);

        executorService.submit(() -> executeMovement(task));

        return future;
    }

    /**
     * Synchronous version that blocks until movement completes.
     * @param point The world point to walk to
     * @return The current movement state. This is an enum value representing
     * the current state of the players path traversal i.e blocked, walking, completed, etc...
     */
    public MovementState walkTo(WorldPoint point) {
        try {
            return walkToAsync(point).get();
        } catch (Exception e) {
            log.error("Error during synchronous walk", e);
            return MovementState.FAILED;
        }
    }

    /**
     * Gets the current movement status.
     * @return The current movement state. This is an enum value representing
     * the current state of the players path traversal i.e blocked, walking, completed, etc...
     */
    public MovementState getCurrentMovementState() {
        MovementTask task = currentTask.get();
        return task != null ? task.getState() : MovementState.IDLE;
    }

    /**
     * Gets the current destination if movement is active.
     * @return The current world point for the destination tile
     */
    public WorldPoint getCurrentDestination() {
        MovementTask task = currentTask.get();
        return task != null ? task.getDestination() : null;
    }

    /**
     * Cancels the current movement task.
     */
    public void cancelCurrentMovement() {
        MovementTask task = currentTask.getAndSet(null);
        if (task != null) {
            task.cancel();
        }
    }

    /**
     * Main movement execution logic.
     */
    private void executeMovement(MovementTask task) {
        try {
            while (!task.isCancelled() && task.getState() == MovementState.WALKING) {
                WorldPoint currentLocation = client.getLocalPlayer().getWorldLocation();
                WorldPoint destination = task.getDestination();

                // Check if we've arrived
                if (currentLocation.distanceTo(destination) <= 1) {
                    task.setState(MovementState.ARRIVED);
                    task.complete(MovementState.ARRIVED);
                    return;
                }

                // Compute path to destination and wait for it to complete
                List<WorldPoint> path = computePathWithTimeout(destination, 2000);

                if (path == null) {
                    log.warn("Path computation timed out");
                    task.setState(MovementState.FAILED);
                    task.complete(MovementState.FAILED);
                    return;
                }

                if (path.isEmpty()) {
                    log.warn("No path found to destination");
                    task.setState(MovementState.BLOCKED);
                    task.complete(MovementState.BLOCKED);
                    return;
                }

                // If destination is close enough, walk directly
                if (path.size() <= 15) {
                    moveTo(destination);
                    waitForMovementComplete(task, destination);
                } else {
                    // Find intermediate point within walking distance
                    WorldPoint intermediatePoint = findIntermediatePoint(path, currentLocation);
                    if (intermediatePoint != null) {
                        moveTo(intermediatePoint);
                        waitForMovementComplete(task, intermediatePoint);
                    } else {
                        log.warn("Could not find valid intermediate point");
                        task.setState(MovementState.BLOCKED);
                        task.complete(MovementState.BLOCKED);
                        return;
                    }
                }

                // Brief pause between movement attempts
                sleepService.sleep(50, 100);
            }
        } catch (Exception e) {
            log.error("Error during movement execution", e);
            task.setState(MovementState.FAILED);
            task.complete(MovementState.FAILED);
        }
    }

    /**
     * Waits for the player to reach or get close to a target point.
     */
    private void waitForMovementComplete(MovementTask task, WorldPoint target) {
        long startTime = System.currentTimeMillis();
        long timeout = 10000; // 10 second timeout

        while (!task.isCancelled() && System.currentTimeMillis() - startTime < timeout) {
            WorldPoint currentLocation = client.getLocalPlayer().getWorldLocation();

            // Check if we're close enough to the target
            if (currentLocation.distanceTo(target) <= 3) {
                return;
            }

            // Check if player is still moving
            if (!playerService.isMoving()) {
                // Player stopped moving but didn't reach target - might be blocked
                sleepService.sleep(100, 200);
                if (!playerService.isMoving()) {
                    // Still not moving after a brief wait
                    break;
                }
            }

            sleepService.sleep(100, 150);
        }
    }

    /**
     * Computes a path to the destination with a timeout.
     * @param destination Target WorldPoint
     * @param timeoutMs Timeout in milliseconds
     * @return List of WorldPoints representing the path, or null if timeout
     */
    private List<WorldPoint> computePathWithTimeout(WorldPoint destination, long timeoutMs) {
        // Start pathfinding
        shortestPathService.setTarget(destination);

        long startTime = System.currentTimeMillis();
        List<WorldPoint> previousPath = null;

        // Wait for path to be computed with timeout
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            List<WorldPoint> currentPath = shortestPathService.getCurrentPath();

            // Check if we have a valid path result
            if (currentPath != null) {
                // If we got the same path twice in a row, pathfinding is likely complete
                if (currentPath.equals(previousPath)) {
                    return currentPath;
                }
                previousPath = currentPath;
            }

            // Short sleep to avoid busy waiting
            try {
                Thread.sleep(25);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }

        // Timeout occurred - return the last path we got, if any
        List<WorldPoint> finalPath = shortestPathService.getCurrentPath();
        if (finalPath != null) {
            log.warn("Path computation timed out after {}ms, using last computed path with {} points",
                    timeoutMs, finalPath.size());
            return finalPath;
        }

        log.warn("Path computation timed out after {}ms with no valid path", timeoutMs);
        return null;
    }

    /**
     * Finds an intermediate point on the path that's within walking distance.
     */
    private WorldPoint findIntermediatePoint(List<WorldPoint> path, WorldPoint currentLocation) {
        // Start from the end and work backwards to find the furthest reachable point
        for (int i = Math.min(path.size() - 1, 12); i >= 0; i--) {
            WorldPoint point = path.get(i);
            if (isWithinScene(point, currentLocation)) {
                return point;
            }
        }
        return null;
    }

    /**
     * Checks if a point is within the current scene/walking distance.
     */
    private boolean isWithinScene(WorldPoint target, WorldPoint current) {
        return current.distanceTo(target) <= 12; // Conservative walking distance
    }

    /**
     * Moves the player to a specific world point. This takes into account when it is called
     * from within an instance and will convert the world point from an instance world point into a
     * normal world point which is moveable.
     * @param point The world point to move towards
     */
    public void moveTo(WorldPoint point) {
        WorldPoint convertedPoint;
        if (client.getTopLevelWorldView().isInstance()) {
            // multiple conversions here: 1 which takes WP and creates instanced LP and
            // 2 which converts a LP to WP
            convertedPoint = WorldPoint.fromLocal(client, tileService.fromWorldInstance(point));
        } else {
            convertedPoint = point;
        }

        Point clickingPoint = uiService.getClickbox(convertedPoint);
        mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
        movementPackets.queueMovement(convertedPoint);
    }

    /**
     * Moves the player to a given local point using packets. This method assumes that if the local point passed
     * is in an instance it
     * @param point The local point to move to
     */
    public void moveTo(LocalPoint point) {
        WorldPoint converted;
        if(client.getTopLevelWorldView().isInstance()) {
            // TODO May not work right
            converted = WorldPoint.fromLocalInstance(client, point);
            LocalPoint lp = tileService.fromWorldInstance(converted);
            converted = WorldPoint.fromLocal(client, lp);
        } else {
            converted = WorldPoint.fromLocal(client, point);
        }


        Point clickingPoint = uiService.getClickbox(converted);
        mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
        movementPackets.queueMovement(converted);
    }

    /**
     * Moves to the specified world point using reflection
     * @param point The world point to move towards.
     */
    public void moveToReflect(WorldPoint point) {
        moveToReflect(LocalPoint.fromWorld(client.getTopLevelWorldView(), point));
    }

    /**
     * Moves to the specified local point using reflection. Note: TODO this function
     * will not correctly move to the point. It only moves to the last place your mouse left
     * the canvas
     * @param point Local point to move towards
     */
    public void moveToReflect(LocalPoint point) {
        if(point == null) return;

        Point canv = Perspective.localToCanvas(client, point, client.getTopLevelWorldView().getPlane());
        int canvasX = canv != null ? canv.getX() : -1;
        int canvasY = canv != null ? canv.getY() : -1;

        log.debug("Canvas: ({}, {}), Point: ({}, {})", canvasX, canvasY, point.getX(), point.getY());
        reflectionService.invokeMenuAction(canvasX, canvasY, MenuAction.WALK.getId(), 0, -1, -1, "Walk here", "", canvasX, canvasY);
    }

    /**
     * Internal class to track movement state.
     */
    private static class MovementTask {
        @Getter
        private final WorldPoint destination;
        @Getter
        @Setter
        private volatile MovementState state = MovementState.WALKING;
        @Getter
        private volatile boolean cancelled = false;
        @Setter
        private CompletableFuture<MovementState> future;

        public MovementTask(WorldPoint destination) {
            this.destination = destination;
        }

        public void cancel() {
            this.cancelled = true;
            if (future != null && !future.isDone()) {
                future.complete(MovementState.FAILED);
            }
        }

        public void complete(MovementState result) {
            if (future != null && !future.isDone()) {
                future.complete(result);
            }
            this.state = result;
        }
    }

    /**
     * Cleanup resources when service is destroyed.
     */
    public void destroy() {
        cancelCurrentMovement();
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}