package com.kraken.api.interaction.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.widget.WidgetService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class PlayerService extends AbstractService {

    @Inject
    private WidgetService widgetService;

    @Inject
    private ReflectionService reflectionService;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

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
     * Gets the amount of special attack energy remaining as a percent (0-100).
     * @return int the amount of special attack energy the player has remaining.
     */
    public int getSpecialAttackEnergy() {
        return client.getVarpValue(300) / 10;
    }

    /**
     * Gets the current player position safely
     */
    public WorldPoint getPlayerPosition() {
        return context.runOnClientThreadOptional(() -> {
            Player p = client.getLocalPlayer();
            return p != null ? p.getWorldLocation() : null;
        }).orElse(null);
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
     * @param energyRequired int, 100 = 100%
     */
    public void toggleSpecialAttack(int energyRequired) {
        int currentSpecEnergy = client.getVarpValue(300) / 10;
        if (currentSpecEnergy >= energyRequired && !isSpecEnabled()) {
            widgetService.clickWidget(10485795);
        }
    }

    /**
     * Returns true when the spec is enabled and false otherwise
     * @return
     */
    public boolean isSpecEnabled() {
        return client.getVarpValue(301) == 1;
    }

    /**
     * Returns true when the spec is disabled and false otherwise
     * @return
     */
    public boolean isSpecDisabled() {
        return client.getVarpValue(301) == 0;
    }

    /**
     * Sets the special attack state if currentSpecEnergy >= specialAttackEnergyRequired using reflection instead of mouse events.
     *
     * @param energyRequired int, 100 = 100%
     * @param delay int a set delay before the spec button is pressed. This can't happen instantaneously because the server needs to process
     *              the weapon equip before it can toggle on spec. Otherwise, the game would see you toggle on spec for nothing, then spec weapon gets equipped with spec disabled.
     */
    public void toggleSpecialAttackReflect(int energyRequired, int delay) {
        int currentSpecEnergy = client.getVarpValue(300) / 10;
        if (currentSpecEnergy >= energyRequired && !isSpecEnabled()) {
            executor.schedule(() -> reflectionService.invokeMenuAction(-1, 38862886, MenuAction.CC_OP.getId(), 1, -1), delay, TimeUnit.MILLISECONDS);
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

        WorldPoint playerLocation = getPlayerPosition();

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
