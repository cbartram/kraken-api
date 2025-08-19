package com.kraken.api.interaction.movement;

import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.reflect.ReflectionService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.Collection;

@Slf4j
@Singleton
public class MovementService extends AbstractService {

    @Inject
    private ReflectionService reflectionService;

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
        setXCoordinate(sceneX);     // Set the target X tile.
        setYCoordinate(sceneY);     // Set the target Y tile.
        setCheckClick();            // Mark the click as valid for pathfinding.
        setViewportWalking();       // Allow walking to be processed by the viewport logic.
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
