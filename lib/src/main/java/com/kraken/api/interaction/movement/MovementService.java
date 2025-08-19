package com.kraken.api.interaction.movement;

import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.widget.WidgetService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
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
    private SpriteManager spriteManager;
    
    @Inject
    private WidgetService widgetService;

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

        // If target is within scene, use direct scene walking
        LocalPoint targetLocal = LocalPoint.fromWorld(client.getTopLevelWorldView(), target);
        if (targetLocal != null && targetLocal.isInScene()) {
//            sceneWalk(targetLocal);
            walkMiniMap(target, 5);
            currentState = MovementState.WALKING;
            stateDescription = "Walking within scene";
            log.info("Walking within scene");
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
        WorldPoint currentWaypoint = currentPath.peek();
        nextWaypoint = currentWaypoint; // Update for state tracking

        // Check if we've reached the current waypoint
        if (playerPos.distanceTo(currentWaypoint) <= 3) {
            currentPath.poll(); // Remove completed waypoint
            completedWaypoints++;
            lastMovementTime = System.currentTimeMillis();
            stateDescription = String.format("Reached waypoint %d/%d", completedWaypoints, fullCalculatedPath.size());
            log.info("Reached waypoint {}/{}", completedWaypoints, fullCalculatedPath.size());
            
            // Check if we've completed the entire path
            if (currentPath.isEmpty()) {
                if (playerPos.distanceTo(finalTarget) <= distance) {
                    stopMovement();
                    currentState = MovementState.ARRIVED;
                    stateDescription = "Arrived at final destination";
                    log.info("Arrived at final destination");
                    return currentState;
                } else {
                    // Path completed but not at final target - might need new path
                    isExecutingPath = false;
                    stateDescription = "Path completed, recalculating...";
                    log.info("Path completed, recalculating path to final target: {}", finalTarget);
                    return walkWithPathfinding(finalTarget, distance);
                }
            }

            // Move to next waypoint
            currentWaypoint = currentPath.peek();
            nextWaypoint = currentWaypoint;
        }

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

        // Try to walk to current waypoint
        LocalPoint waypointLocal = LocalPoint.fromWorld(client.getTopLevelWorldView(), currentWaypoint);
        if (waypointLocal != null && waypointLocal.isInScene()) {
//            sceneWalk(waypointLocal);
            walkMiniMap(currentWaypoint, 5);
            currentState = MovementState.WALKING;
            stateDescription = String.format("Walking to waypoint %d/%d", completedWaypoints + 1, fullCalculatedPath.size());
            log.info("Walking to waypoint (in scene) {}/{}", completedWaypoints + 1, fullCalculatedPath.size());
            return currentState;
        } else {
            // Waypoint not in scene, try to find intermediate point that is
            WorldPoint intermediatePoint = findIntermediatePoint(playerPos, currentWaypoint);
            if (intermediatePoint != null) {
//                sceneWalk(intermediatePoint);
                walkMiniMap(intermediatePoint, 5);
                currentState = MovementState.WALKING;
                stateDescription = "Walking to intermediate point";
                log.info("Walking to intermediate point: {}", intermediatePoint);
                return currentState;
            }
        }

        currentState = MovementState.BLOCKED;
        stateDescription = "Waypoint not accessible";
        return currentState;
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

        // Try points at different distances
        for (int step = 5; step < distance && step < 50; step += 5) {
            int x = from.getX() + (int)(dx * step);
            int y = from.getY() + (int)(dy * step);
            WorldPoint testPoint = new WorldPoint(x, y, from.getPlane());

            LocalPoint testLocal = LocalPoint.fromWorld(wv, testPoint);
            if (testLocal != null && testLocal.isInScene()) {
                return testPoint;
            }
        }

        return null;
    }

    public boolean walkMiniMap(WorldPoint worldPoint, double zoomDistance) {
        if (client.getMinimapZoom() != zoomDistance)
            client.setMinimapZoom(zoomDistance);

        Point point = worldToMinimap(worldPoint);

        if (point == null) return false;
        if (!isPointInsideMinimap(point)) return false;

        context.getMouse().click(point);
        return true;
    }

    /**
     * Checks if a given point is inside the minimap clipping area.
     *
     * @param point The point to check.
     * @return {@code true} if the point is within the minimap bounds, {@code false} otherwise.
     */
    public boolean isPointInsideMinimap(Point point) {
        Shape minimapClipArea = getMinimapClipArea();
        return minimapClipArea != null && minimapClipArea.contains(point.getX(), point.getY());
    }

    /**
     * Converts a {@link WorldPoint} to a minimap coordinate {@link Point}.
     *
     * @param worldPoint The world point to convert.
     * @return The corresponding minimap point, or {@code null} if conversion fails.
     */
    @Nullable
    public Point worldToMinimap(WorldPoint worldPoint) {
        if (worldPoint == null) return null;

        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
        if (localPoint == null) {
            log.info("Tried to walk worldpoint " + worldPoint + " using the canvas but localpoint returned null");
            return null;
        }

        final LocalPoint lp = localPoint;
        return context.runOnClientThreadOptional(() -> Perspective.localToMinimap(client, lp)).orElse(null);
    }

    /**
     * Returns a simple elliptical clip area for the minimap.
     *
     * @return A {@link Shape} representing the minimap clip area.
     */
    private Shape getMinimapClipAreaSimple() {
        Widget minimapDrawArea = getMinimapDrawWidget();
        if (minimapDrawArea == null) {
            return null;
        }
        Rectangle bounds = minimapDrawArea.getBounds();
        return new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    /**
     * Retrieves the minimap clipping area as a polygon derived from the minimap alpha mask sprite,
     * and scales it inward to avoid overlapping edge elements.
     *
     * @param scale The scale factor to shrink the polygon (e.g., 0.94 for 94% of the original size).
     * @return A {@link Shape} representing the scaled minimap clickable area, or a fallback shape if the sprite is unavailable.
     */
    public Shape getMinimapClipArea(double scale) {
        Widget minimapWidget = getMinimapDrawWidget();
        if (minimapWidget == null) {
            return null;
        }

        boolean isResized = client.isResized();

        BufferedImage minimapSprite = context.runOnClientThreadOptional(() ->
                spriteManager.getSprite(isResized ? SpriteID.RESIZEABLE_MODE_MINIMAP_ALPHA_MASK : SpriteID.FIXED_MODE_MINIMAP_ALPHA_MASK, 0)).orElse(null);

        if (minimapSprite == null) {
            return getMinimapClipAreaSimple();
        }

        Shape rawClipArea = bufferedImageToPolygon(minimapSprite, minimapWidget.getBounds());
        return shrinkShape(rawClipArea, scale);
    }

    /**
     * Retrieves the minimap draw widget based on the current game view mode.
     *
     * @return The minimap draw widget, or {@code null} if not found.
     */
    public Widget getMinimapDrawWidget() {
        if (client.isResized()) {
            if (client.getVarbitValue(Varbits.SIDE_PANELS) == 1) {
                return widgetService.getWidget(ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA);
            }
            return widgetService.getWidget(ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA);
        }
        return widgetService.getWidget(ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA);
    }

    /**
     * Retrieves the minimap clipping area as a {@link Shape}, scaled to slightly reduce its size.
     * <p>
     * This is useful for rendering overlays within the minimap without overlapping UI elements such as the globe icon.
     *
     * @return A {@link Shape} representing the scaled minimap clip area, or {@code null} if the minimap widget is unavailable.
     */
    public Shape getMinimapClipArea() {
        return getMinimapClipArea(client.isResized() ? 0.94 : 1.0);
    }

    /**
     * Converts a BufferedImage to a polygon by detecting the border based on the outside color.
     *
     * @param image         The image to convert.
     * @param minimapBounds The bounds of the minimap widget.
     * @return A polygon representing the minimap's clickable area.
     */
    private Polygon bufferedImageToPolygon(BufferedImage image, Rectangle minimapBounds) {
        Color outsideColour = null;
        Color previousColour;
        final int width = image.getWidth();
        final int height = image.getHeight();
        List<java.awt.Point> points = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            previousColour = outsideColour;
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb & 0xff000000) >>> 24;
                int r = (rgb & 0x00ff0000) >> 16;
                int g = (rgb & 0x0000ff00) >> 8;
                int b = (rgb & 0x000000ff);
                Color colour = new Color(r, g, b, a);
                if (x == 0 && y == 0) {
                    outsideColour = colour;
                    previousColour = colour;
                }
                if (!colour.equals(outsideColour) && previousColour.equals(outsideColour)) {
                    points.add(new java.awt.Point(x, y));
                }
                if ((colour.equals(outsideColour) || x == (width - 1)) && !previousColour.equals(outsideColour)) {
                    points.add(0, new java.awt.Point(x, y));
                }
                previousColour = colour;
            }
        }

        int offsetX = minimapBounds.x;
        int offsetY = minimapBounds.y;
        Polygon polygon = new Polygon();
        for (java.awt.Point point : points) {
            polygon.addPoint(point.x + offsetX, point.y + offsetY);
        }
        return polygon;
    }

    /**
     * Shrinks the given shape toward its center by the specified scale factor.
     *
     * @param shape The original shape to shrink.
     * @param scale The scale factor (e.g., 0.94 = 94% size). Must be > 0 and < 1 to reduce the shape.
     * @return A new {@link Shape} that is scaled inward toward its center.
     */
    private Shape shrinkShape(Shape shape, double scale) {
        Rectangle bounds = shape.getBounds();
        double centerX = bounds.getCenterX();
        double centerY = bounds.getCenterY();

        AffineTransform shrink = AffineTransform.getTranslateInstance(centerX, centerY);
        shrink.scale(scale, scale);
        shrink.translate(-centerX, -centerY);

        return shrink.createTransformedShape(shape);
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
     *
     * @param sceneX The X coordinate within the loaded scene grid.
     * @param sceneY The Y coordinate within the loaded scene grid.
     */
    public void sceneWalk(int sceneX, int sceneY) {
        log.info("Scene walking to coordinates: ({}, {})", sceneX, sceneY);

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