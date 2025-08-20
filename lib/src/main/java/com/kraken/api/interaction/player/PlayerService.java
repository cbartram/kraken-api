package com.kraken.api.interaction.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.widget.WidgetService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import java.util.Optional;

@Slf4j
@Singleton
public class PlayerService extends AbstractService {

    @Inject
    private WidgetService widgetService;

    /**
     * Checks if the player is currently moving based on their pose animation.
     * A player is considered moving if their pose animation is different from their idle pose animation.
     *
     * @return {@code true} if the player is moving, {@code false} if they are idle.
     */
    public boolean isMoving() {
        return context.runOnClientThreadOptional(() -> {
            Player localPlayer = client.getLocalPlayer();
            if (localPlayer == null) {
                return false;
            }
            return localPlayer.getPoseAnimation() != localPlayer.getIdlePoseAnimation();
        }).orElse(false);
    }

    /**
     * Gets the current player position safely
     */
    public WorldPoint getPlayerPosition() {
        Player player = client.getLocalPlayer();
        return player != null ? player.getWorldLocation() : null;
    }

    /**
     * Checks if the player is currently interacting with another entity (NPC, player, or object).
     *
     * @return {@code true} if the player is interacting with another entity, {@code false} otherwise.
     */
    public boolean isInteracting() {
        return Optional.of(client.getLocalPlayer().isInteracting()).orElse(false);
    }


    /**
     * Sets the special attack state if currentSpecEnergy >= specialAttackEnergyRequired
     *
     * @param energyRequired int, 1000 = 100%
     * @return boolean, whether the action succeeded
     */
    public void toggleSpecialAttack(int energyRequired) {
        int currentSpecEnergy = client.getVarpValue(300); // Spec percent
        // spec enabled
        if (currentSpecEnergy >= energyRequired && (client.getVarpValue(301) == 0)) {
            widgetService.clickWidget("special attack");
        }
    }


    /**
     * Checks if the player is within 5 tiles of a given {@link WorldPoint}.
     *
     * @param worldPoint The {@link WorldPoint} to check proximity to.
     * @return {@code true} if the player is within the specified distance, {@code false} otherwise.
     */
    public boolean isInArea(WorldPoint worldPoint) {
        return isInArea(worldPoint, 5);
    }

    /**
     * Checks if the player is within a specified distance of a given {@link WorldPoint}.
     *
     * @param worldPoint The {@link WorldPoint} to check proximity to.
     * @param radius   The radius (in tiles) around the {@code worldPoint} to check.
     * @return {@code true} if the player is within the specified distance, {@code false} otherwise.
     */
    public boolean isInArea(WorldPoint worldPoint, int radius) {
        return isInArea(worldPoint, radius, radius);
    }

    /**
     * Checks if the player is within a specified area around a given {@link WorldPoint}.
     *
     * @param worldPoint The {@link WorldPoint} to check proximity to.
     * @param xRadius    The horizontal radius (in tiles) around the {@code worldPoint}.
     * @param yRadius    The vertical radius (in tiles) around the {@code worldPoint}.
     * @return {@code true} if the player is within the specified area, {@code false} otherwise.
     */
    public boolean isInArea(WorldPoint worldPoint, int xRadius, int yRadius) {
        // Null check for world point
        if (worldPoint == null) {
            return false;
        }

        // Validate radius parameters (should be non-negative)
        if (xRadius < 0 || yRadius < 0) {
            return false;
        }

        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();

        // Null check for player location
        if (playerLocation == null) {
            return false;
        }

        // Ensure both points are on the same plane
        if (worldPoint.getPlane() != playerLocation.getPlane()) {
            return false;
        }

        // Simple distance check - check if player is within the rectangular radius
        int deltaX = Math.abs(playerLocation.getX() - worldPoint.getX());
        int deltaY = Math.abs(playerLocation.getY() - worldPoint.getY());

        return deltaX <= xRadius && deltaY <= yRadius;
    }
}
