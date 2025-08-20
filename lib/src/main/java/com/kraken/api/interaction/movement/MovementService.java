package com.kraken.api.interaction.movement;

import com.kraken.api.core.AbstractService;
import com.kraken.api.core.SleepService;
import com.kraken.api.interaction.camera.CameraService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.model.NewMenuEntry;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

@Slf4j
@Singleton
public class MovementService extends AbstractService {

    @Inject
    private ReflectionService reflectionService;

    @Inject
    private ShortestPathService shortestPathService;
    
    @Inject
    private MinimapService minimapService;

    @Getter
    @Setter
    private WorldPoint currentTarget;

    private Queue<WorldPoint> currentPath;
    private List<WorldPoint> fullCalculatedPath; // For visualization
    private boolean isExecutingPath = false;
    private long lastMovementTime = 0;

    @Getter
    private MovementState currentState = MovementState.IDLE;

    @Getter
    private String stateDescription = "";

    @Getter
    private WorldPoint nextWaypoint;

    @Getter
    private int completedWaypoints = 0;
    private static final int MOVEMENT_TIMEOUT = 5000; // 5 seconds
    private static final int MIN_DISTANCE_FOR_PATH = 20; // Tiles

    public boolean walkTo(WorldPoint target) {
        return walkTo(target, 5);
    }

    public boolean walkTo(WorldPoint target, int distance) {
        return walkWithState(target, distance) == MovementState.ARRIVED;
    }

    public MovementState walkWithState(WorldPoint target, int distance) {
        return walkWithStateInternal(target, distance);
    }

    /**
     * Enhanced internal walking logic with cross-world pathfinding support
     */
    private MovementState walkWithStateInternal(WorldPoint target, int distance) {
        if (target == null) {
            currentState = MovementState.FAILED;
            stateDescription = "Target is null";
            log.warn("Target is null, movement failed");
            return currentState;
        }

        WorldPoint playerPos = getPlayerPosition();
        if (playerPos == null) {
            currentState = MovementState.FAILED;
            stateDescription = "Cannot get player position";
            log.warn("Cannot get player position, movement failed");
            return currentState;
        }

        // Check if we're already at the target
        if (playerPos.distanceTo(target) <= distance) {
            stopMovement();
            currentState = MovementState.ARRIVED;
            stateDescription = "Arrived at destination";
            log.info("Arrived at target position: {}", target);
            return currentState;
        }

        // If target is within scene and close enough, use direct scene walking
        LocalPoint targetLocal = LocalPoint.fromWorld(client.getTopLevelWorldView(), target);
        if (targetLocal != null && targetLocal.isInScene() && playerPos.distanceTo(target) <= 15) {
            log.info("Target is within scene and close enough, walking directly to {}", target);
            // Wait for player to stop moving before clicking
            if (isPlayerMoving()) {
                currentState = MovementState.WALKING;
                stateDescription = "Waiting for player to stop moving";
                return currentState;
            }

            minimapService.walkMiniMap(target);
            currentState = MovementState.WALKING;
            lastMovementTime = System.currentTimeMillis();
            stateDescription = "Walking within scene";
            log.info("Walking within scene to target");
            return currentState;
        }

        return walkWithPathfinding(target, distance);
    }

    /**
     * Handles long-distance walking using pathfinding
     */
    private MovementState walkWithPathfinding(WorldPoint target, int distance) {
        WorldPoint playerPos = getPlayerPosition();

        // Check if we need to generate a new path
        if (!isExecutingPath || currentPath == null || currentPath.isEmpty()) {
            shortestPathService.setTarget(target);
            List<WorldPoint> path = shortestPathService.getCurrentPath();

            if (path == null || path.isEmpty()) {
                log.warn("No path found, or path is still being calculated from {} to {}", playerPos, target);
                currentState = MovementState.BLOCKED;
                stateDescription = "No path found to destination";
                return currentState;
            }

            currentPath = new LinkedList<>(path);
            fullCalculatedPath = new ArrayList<>(path); // Store full path for visualization
            isExecutingPath = true;
            lastMovementTime = System.currentTimeMillis();
            completedWaypoints = 0;
            currentState = MovementState.WALKING;
            stateDescription = String.format("Following path with %d waypoints", path.size());
            log.info("Generated path with {} waypoints", path.size());
        }

        // Execute the current path
        return executePathStep(target, distance);
    }

    /**
     * Executes a single step in the pathfinding sequence
     */
    private MovementState executePathStep(WorldPoint finalTarget, int distance) {
        if (currentPath == null || currentPath.isEmpty()) {
            stopMovement();
            currentState = MovementState.FAILED;
            stateDescription = "Path execution failed - no waypoints";
            return currentState;
        }

        WorldPoint playerPos = getPlayerPosition();

        // If player is moving, wait for them to stop
        if (isPlayerMoving()) {
            currentState = MovementState.WALKING;
            stateDescription = "Player is moving, waiting...";
            lastMovementTime = System.currentTimeMillis(); // Update movement time while moving
            return currentState;
        }

        // Find the next waypoint that's 7-11 tiles away
        WorldPoint targetWaypoint = findOptimalWaypoint(playerPos);

        if (targetWaypoint == null) {
            // No suitable waypoint found, might be close to destination
            if (playerPos.distanceTo(finalTarget) <= distance) {
                stopMovement();
                currentState = MovementState.ARRIVED;
                stateDescription = "Arrived at final destination";
                log.info("Arrived at final destination");
                return currentState;
            } else {
                // Regenerate path
                isExecutingPath = false;
                currentPath = null;
                fullCalculatedPath = null;
                stateDescription = "Recalculating path...";
                log.info("No suitable waypoint found, recalculating path");
                return walkWithPathfinding(finalTarget, distance);
            }
        }

        nextWaypoint = targetWaypoint; // Update for state tracking

        // Check for movement timeout (stuck detection)
        if (System.currentTimeMillis() - lastMovementTime > MOVEMENT_TIMEOUT) {
            log.info("Movement timeout detected, regenerating path");
            isExecutingPath = false;
            currentPath = null;
            fullCalculatedPath = null;
            currentState = MovementState.BLOCKED;
            stateDescription = "Player is stuck, regenerating path...";
            return walkWithPathfinding(finalTarget, distance);
        }

        // Try to walk to the target waypoint
        LocalPoint waypointLocal = LocalPoint.fromWorld(client.getTopLevelWorldView(), targetWaypoint);
        if (waypointLocal != null && waypointLocal.isInScene()) {
            minimapService.walkMiniMap(targetWaypoint);
            lastMovementTime = System.currentTimeMillis();
            currentState = MovementState.WALKING;
            stateDescription = String.format("Walking to waypoint (distance: %d)", playerPos.distanceTo(targetWaypoint));
            log.info("Walking to waypoint at distance: {}", playerPos.distanceTo(targetWaypoint));

            // Remove waypoints that we've passed or are close to
            removePassedWaypoints(playerPos);

            return currentState;
        } else {
            // Waypoint not in scene, try to find intermediate point that is
            WorldPoint intermediatePoint = findIntermediatePoint(playerPos, targetWaypoint);
            if (intermediatePoint != null) {
                minimapService.walkMiniMap(intermediatePoint);
                lastMovementTime = System.currentTimeMillis();
                currentState = MovementState.WALKING;
                stateDescription = "Walking to intermediate point";
                log.info("Walking to intermediate point: {}", intermediatePoint);
                return currentState;
            }
        }

        currentState = MovementState.BLOCKED;
        stateDescription = "Waypoint not accessible";
        log.info("Movement blocked, waypoint not accessible: {}", targetWaypoint);
        return currentState;
    }

    /**
     * Finds the optimal waypoint that's 7-11 tiles away from current position
     */
    private WorldPoint findOptimalWaypoint(WorldPoint playerPos) {
        if (currentPath == null || currentPath.isEmpty()) {
            return null;
        }

        // Look for a waypoint that's 7-11 tiles away
        for (WorldPoint waypoint : currentPath) {
            double distance = playerPos.distanceTo(waypoint);
            if (distance >= 7 && distance <= 11) {
                return waypoint;
            }
        }

        // If no waypoint in ideal range, find the farthest one that's still accessible
        WorldPoint farthestAccessible = null;
        double maxDistance = 0;

        for (WorldPoint waypoint : currentPath) {
            double distance = playerPos.distanceTo(waypoint);
            if (distance <= 15) { // Max scene walk distance
                LocalPoint waypointLocal = LocalPoint.fromWorld(client.getTopLevelWorldView(), waypoint);
                if (waypointLocal != null && waypointLocal.isInScene()) {
                    if (distance > maxDistance) {
                        maxDistance = distance;
                        farthestAccessible = waypoint;
                    }
                }
            }
        }

        return farthestAccessible;
    }

    /**
     * Removes waypoints that have been passed or are very close to current position
     */
    private void removePassedWaypoints(WorldPoint playerPos) {
        if (currentPath == null) return;

        Iterator<WorldPoint> iterator = currentPath.iterator();
        while (iterator.hasNext()) {
            WorldPoint waypoint = iterator.next();
            if (playerPos.distanceTo(waypoint) <= 3) {
                iterator.remove();
                completedWaypoints++;
                log.info("Removed passed waypoint, total completed: {}", completedWaypoints);
            } else {
                // Once we hit a waypoint that's not passed, stop removing
                // (assuming path is ordered)
                break;
            }
        }
    }

    /**
     * Checks if the player is currently moving
     */
    private boolean isPlayerMoving() {
        return client.getLocalPlayer().getPoseAnimation() == client.getLocalPlayer().getWalkAnimation()
                || client.getLocalPlayer().getPoseAnimation() == client.getLocalPlayer().getRunAnimation();
    }

    /**
     * Finds an intermediate point between current position and target that is within the loaded scene
     */
    private WorldPoint findIntermediatePoint(WorldPoint from, WorldPoint to) {
        WorldView wv = client.getTopLevelWorldView();

        // Try points along the line between from and to
        double dx = to.getX() - from.getX();
        double dy = to.getY() - from.getY();
        double distance = Math.sqrt(dx * dx + dy * dy);

        // Normalize the direction
        dx /= distance;
        dy /= distance;

        // Try points at different distances, prioritizing 7-11 tile range
        for (int step = 11; step >= 7; step--) {
            if (step < distance) {
                int x = from.getX() + (int)(dx * step);
                int y = from.getY() + (int)(dy * step);
                WorldPoint testPoint = new WorldPoint(x, y, from.getPlane());

                LocalPoint testLocal = LocalPoint.fromWorld(wv, testPoint);
                if (testLocal != null && testLocal.isInScene()) {
                    return testPoint;
                }
            }
        }

        // If no point in ideal range, try shorter distances
        for (int step = 6; step >= 3; step--) {
            if (step < distance) {
                int x = from.getX() + (int)(dx * step);
                int y = from.getY() + (int)(dy * step);
                WorldPoint testPoint = new WorldPoint(x, y, from.getPlane());

                LocalPoint testLocal = LocalPoint.fromWorld(wv, testPoint);
                if (testLocal != null && testLocal.isInScene()) {
                    return testPoint;
                }
            }
        }

        return null;
    }

    /**
     * Gets the current player position safely
     */
    private WorldPoint getPlayerPosition() {
        Player player = client.getLocalPlayer();
        return player != null ? player.getWorldLocation() : null;
    }

    /**
     * Stops any current pathfinding operation
     */
    public void stopMovement() {
        isExecutingPath = false;
        currentPath = null;
        fullCalculatedPath = null;
        currentTarget = null;
        nextWaypoint = null;
        completedWaypoints = 0;
        currentState = MovementState.IDLE;
        stateDescription = "Movement stopped";
    }

    /**
     * Checks if currently executing a path
     */
    public boolean isMoving() {
        return isExecutingPath && currentPath != null && !currentPath.isEmpty();
    }

    /**
     * Gets the current movement progress (0.0 to 1.0)
     */
    public double getMovementProgress() {
        if (!isMoving() || fullCalculatedPath == null || fullCalculatedPath.isEmpty()) {
            return currentState == MovementState.ARRIVED ? 1.0 : 0.0;
        }

        return (double) completedWaypoints / fullCalculatedPath.size();
    }

    /**
     * Gets the full calculated path for visualization
     */
    public List<WorldPoint> getCalculatedPath() {
        return fullCalculatedPath != null ? Collections.unmodifiableList(fullCalculatedPath) : Collections.emptyList();
    }

    /**
     * Gets the remaining waypoints in the current path
     */
    public List<WorldPoint> getRemainingPath() {
        if (currentPath == null || currentPath.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<>(currentPath));
    }

    /**
     * Gets the total number of waypoints in the current path
     */
    public int getTotalWaypoints() {
        return fullCalculatedPath != null ? fullCalculatedPath.size() : 0;
    }

    /**
     * Gets detailed movement statistics for debugging/display
     */
    public MovementStats getMovementStats() {
        WorldPoint playerPos = getPlayerPosition();
        double distanceToTarget = currentTarget != null && playerPos != null ?
                playerPos.distanceTo(currentTarget) : 0.0;
        double distanceToNextWaypoint = nextWaypoint != null && playerPos != null ?
                playerPos.distanceTo(nextWaypoint) : 0.0;

        return new MovementStats(
                currentState,
                stateDescription,
                getMovementProgress(),
                completedWaypoints,
                getTotalWaypoints(),
                distanceToTarget,
                distanceToNextWaypoint,
                System.currentTimeMillis() - lastMovementTime,
                currentTarget,
                nextWaypoint
        );
    }

    /**
     * Attempts to walk to a given {@link WorldPoint} by converting it to a scene coordinate
     * and setting the internal RuneLite walking fields via reflection.
     *
     * @param worldPoint         The absolute in-game world coordinates to walk to.
     * @param convertForInstance Whether to convert the world point into an instanced coordinate.
     *                           Used when the player is inside a dynamically generated region (e.g., dungeons, raids).
     */
    public void sceneWalk(WorldPoint worldPoint, boolean convertForInstance) {
        if (worldPoint == null) {
            return; // Nothing to do if no target point is given.
        }

        // Retrieve the top-level world view from the client.
        WorldView wv = client.getTopLevelWorldView();

        if (convertForInstance) {
            // Convert world coordinates to their local equivalents inside an instanced scene.
            Collection<WorldPoint> localWorldPoints = WorldPoint.toLocalInstance(wv.getScene(), worldPoint);

            // Ensure we have exactly one matching point; otherwise, walking becomes ambiguous.
            if (localWorldPoints.size() != 1) {
                return;
            }

            // Convert the single matching WorldPoint to LocalPoint and initiate walking.
            for (WorldPoint localWorld : localWorldPoints) {
                sceneWalk(LocalPoint.fromWorld(wv, localWorld));
                return;
            }
        } else {
            // Normal world coordinate → local coordinate conversion.
            sceneWalk(LocalPoint.fromWorld(wv, worldPoint));
        }
    }

    /**
     * Checks if the player's current location is within the specified area defined by the given world points.
     *
     * @param worldPoints an array of two world points of the NW and SE corners of the area
     * @return true if the player's current location is within the specified area, false otherwise
     */
    public boolean isInArea(WorldPoint... worldPoints) {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        return playerLocation.getX() <= worldPoints[0].getX() &&   // NW corner x
                playerLocation.getY() >= worldPoints[0].getY() &&   // NW corner y
                playerLocation.getX() >= worldPoints[1].getX() &&   // SE corner x
                playerLocation.getY() <= worldPoints[1].getY();     // SE corner Y
        // draws box from 2 points to check against all variations of player X,Y from said points.
    }

    /**
     * Convenience overload for walking to a {@link WorldPoint} without instance conversion.
     *
     * @param worldPoint The world coordinates to walk to.
     */
    public void sceneWalk(WorldPoint worldPoint) {
        sceneWalk(worldPoint, false);
    }

    /**
     * Convenience overload for walking to a specific (x, y, plane) world coordinate.
     *
     * @param worldPointX The X world coordinate.
     * @param worldPointY The Y world coordinate.
     * @param plane       The Z level (0 = ground floor).
     */
    public void sceneWalk(int worldPointX, int worldPointY, int plane) {
        WorldPoint point = new WorldPoint(worldPointX, worldPointY, plane);
        sceneWalk(point);
    }

    /**
     * Walks to a {@link LocalPoint}, which is a coordinate relative to the currently loaded scene.
     *
     * @param localPoint The scene-relative coordinates to walk to.
     */
    public void sceneWalk(LocalPoint localPoint) {
        // Ensure the target is inside the currently loaded scene to avoid invalid clicks.
        if (localPoint == null || !localPoint.isInScene()) {
            return;
        }

        // Translate local coordinates into scene grid coordinates and walk.
        sceneWalk(localPoint.getSceneX(), localPoint.getSceneY());
    }

    /**
     * Core walking method for scene coordinates.
     * Sets internal RuneLite scene walking variables via reflection to simulate a click-to-walk action.
     * TODO When in fixed mode no stretch plugin SceneWalk actually walks to where your actual mouse last left the canvas. This needs a real mouse to work it seems
     * @param sceneX The X coordinate within the loaded scene grid.
     * @param sceneY The Y coordinate within the loaded scene grid.
     */
    public void sceneWalk(int sceneX, int sceneY) {
        setXCoordinate(sceneX);     // Set the target X tile.
        setYCoordinate(sceneY);     // Set the target Y tile.
        setCheckClick();            // Mark the click as valid for pathfinding.
        setViewportWalking();       // Allow walking to be processed by the viewport logic.

        // Update last movement time for timeout detection
        lastMovementTime = System.currentTimeMillis();
    }

    /**
     * Sets the internal X coordinate for scene walking via reflection.
     *
     * @param x The X scene coordinate to set.
     */
    private void setXCoordinate(int x) {
        Field xField = reflectionService.getField(
                ReflectionService.getSceneSelectedXClassName(),
                ReflectionService.getSceneSelectedXFieldName()
        );
        ReflectionService.setFieldIntValue(
                xField,
                client.getTopLevelWorldView().getScene(),
                x,
                "Failed to set scene selected X coordinate."
        );
    }

    /**
     * Sets the internal Y coordinate for scene walking via reflection.
     *
     * @param y The Y scene coordinate to set.
     */
    private void setYCoordinate(int y) {
        Field yField = reflectionService.getField(
                ReflectionService.getSceneSelectedYClassName(),
                ReflectionService.getSceneSelectedYFieldName()
        );
        ReflectionService.setFieldIntValue(
                yField,
                client.getTopLevelWorldView().getScene(),
                y,
                "Failed to set scene selected Y coordinate."
        );
    }

    /**
     * Marks the "check click" boolean internally to true, indicating that
     * the click should be processed for walking.
     */
    private void setCheckClick() {
        Field checkClick = reflectionService.getField(
                ReflectionService.getCheckClickClassName(),
                ReflectionService.getCheckClickFieldName()
        );
        ReflectionService.setFieldBooleanValue(
                checkClick,
                client.getTopLevelWorldView().getScene(),
                true,
                "Failed to set check click walking boolean."
        );
    }

    /**
     * Enables viewport walking internally, allowing the walking action
     * to actually be processed after coordinates are set.
     */
    private void setViewportWalking() {
        Field viewport = reflectionService.getField(
                ReflectionService.getViewportWalkingClassName(),
                ReflectionService.getViewportWalkingFieldName()
        );
        String errorMsg = "Failed to set scene viewport walking boolean.";
        ReflectionService.setFieldBooleanValue(
                viewport,
                client.getTopLevelWorldView().getScene(),
                true,
                errorMsg
        );
    }
}