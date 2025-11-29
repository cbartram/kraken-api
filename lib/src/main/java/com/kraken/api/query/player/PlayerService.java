package com.kraken.api.query.player;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.service.ui.UIService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class PlayerService extends AbstractService {

    private static final int VENOM_VALUE_CUTOFF = -38;
    private static final int VENOM_THRESHOLD = 1000000;

    @Inject
    private UIService uiService;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private WidgetPackets widgetPackets;

    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    private int antiVenomTime = -1;
    private int antiPoisonTime = -1;

    @Getter
    private boolean isPoisoned = false;

    @Getter
    private boolean isVenomed = false;

    /**
     * The poisoned status of the player, with negative values indicating the duration of poison or venom protection and
     * positive values representing the amount of poison or venom damage the player will be taking.
     * @param event The varbit changed event from RuneLite
     *    - (-inf, -38): Venom immune for a duration of {@code (abs(val) - 38) * 30} game ticks (18 seconds per poison tick), after which point the value will have increased to {@code -38} and be representing poison immunity rather than venom immunity
     *    - [-38, 0): Poison immune for a duration of {@code abs(val) * 30} game ticks (18 seconds per poison tick)
     *    - 0: Not poisoned or immune to poison
     *    - [1, 100]: Poisoned for an amount of {@code ceil(val / 5.0f)}
     *    - [1000000, inf): Venomed for an amount of {@code min(20, (val - 999997) * 2)}, that is, an amount starting at 6 damage, increasing by 2 each time the value increases by one, and capped at 20
     */
    @Subscribe
    public void onVarbitChanged(VarbitChanged event) {
        if (event.getVarpId() == VarPlayerID.POISON) {
            final int poisonValue = event.getValue();
            if (poisonValue >= VENOM_VALUE_CUTOFF) {
                antiVenomTime = 0;
            } else {
                antiVenomTime = poisonValue;
            }

            if(poisonValue == 0) {
                antiPoisonTime = -1;
            } else {
                antiPoisonTime = poisonValue;
            }

            isVenomed = poisonValue >= VENOM_THRESHOLD;
            isPoisoned = poisonValue >= 0 || poisonValue < VENOM_VALUE_CUTOFF;
        }
    }

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
     * @return The current players position as a world point
     */
    public WorldPoint getPlayerPosition() {
        return context.runOnClientThread(() -> {
            Player p = client.getLocalPlayer();
            return p != null ? p.getWorldLocation() : null;
        });
    }

    /**
     * Wrapper method for returning the players current world location
     * @return WorldPoint players location
     */
    public WorldPoint getLocation() {
        return getPlayerPosition();
    }

    /**
     * Returns the local location of the player
     * @return LocalPoint the players local location.
     */
    public LocalPoint getLocalLocation() {
        return LocalPoint.fromWorld(client.getTopLevelWorldView(), getLocation());
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
     * Returns the current local player.
     * @return The current player object.
     */
    public Player getPlayer() {
        return context.runOnClientThread(() -> client.getLocalPlayer());
    }

    /**
     * Returns the Actor that the player is currently interacting with or null if the player
     * is not interacting with anything.
     * @return Actor the Actor being interacted with.
     */
    public Actor getInteracting() {
        if(!isInteracting()) return null;
        return context.runOnClientThread(() -> client.getLocalPlayer().getInteracting());
    }

    /**
     * Calculates the player's current health as a percentage of their real (base) health.
     * If the player has 40 hp total and has 36 hp remaining this will return ~85.0 showing that roughly 85% of the
     * players health is remaining.
     *
     * @return the health percentage as a double. For example:
     *         150.0 if boosted, 80.0 if drained, or 100.0 if unchanged.
     */
    public double getHealthPercentage() {
        return (double) (client.getBoostedSkillLevel(Skill.HITPOINTS) * 100) / client.getRealSkillLevel(Skill.HITPOINTS);
    }

    /**
     * Returns the players current health remaining as an integer.
     * @return the remaining amount of hitpoints the player currently has.
     */
    public int getHealthRemaining() {
        return client.getBoostedSkillLevel(Skill.HITPOINTS);
    }

    /**
     * Returns the players maximum health as an integer.
     * @return The total amount of hitpoints the player has.
     */
    public int getMaxHealth() {
        return client.getRealSkillLevel(Skill.HITPOINTS);
    }

    /**
     * Returns true when the spec is enabled and false otherwise
     * @return True if spec is enabled and false otherwise
     */
    public boolean isSpecEnabled() {
        return client.getVarpValue(301) == 1;
    }

    /**
     * Returns true when the spec is disabled and false otherwise
     * @return True if speci is disabled and false otherwise
     */
    public boolean isSpecDisabled() {
        return client.getVarpValue(301) == 0;
    }

    /**
     * Returns true if the player has anti-venom protection currently active
     * @return True if the player has anti-venom active and false otherwise
     */
    private boolean hasAntiVenomActive() {
        return antiVenomTime < VENOM_VALUE_CUTOFF;
    }

    /**
     * Returns true if the player has anti-poison protection currently active
     * @return True if the player has anti-poison active and false otherwise
     */
    private boolean hasAntiPoisonActive() {
        return antiPoisonTime > 0;
    }

    /**
     * Returns true if the player's anti-poison protection is about to expire (within 10 ticks of expiration)
     * @return True if the anti-poison is about to expire within 10 game ticks.
     */
    private boolean antiPoisonAboutToExpire() {
        return hasAntiPoisonActive() && (Math.abs(antiPoisonTime) * 30) < 10;
    }

    /**
     * Returns true if the players anti-venom protection is about to expire (within 10 ticks of expiration)
     * @return True if the anti-venom is about to expire within 10 game ticks.
     */
    private boolean antiVenomAboutToExpire() {
        int ticks = (Math.abs(antiVenomTime) - 38) * 30;
        return hasAntiVenomActive() && ticks < 10;
    }

    /**
     * Sets the special attack state if current special attack energy is greater than or equal to the required special
     * attack energy
     *
     * @param energyRequired int, 100 = 100%
     */
    public void toggleSpecialAttack(int energyRequired) {
        toggleSpecialAttack(energyRequired, 300);
    }

    /**
     * Sets the special attack state if the current special attack energy is greater than or equal to the required special attack energy using reflection instead of mouse events.
     *
     * @param energyRequired int, 100 = 100%
     * @param delay int a set delay before the spec button is pressed. This can't happen instantaneously because the server needs to process
     *              the weapon equip before it can toggle on spec. Otherwise, the game would see you toggle on spec for nothing, then spec weapon gets equipped with spec disabled.
     */
    public void toggleSpecialAttack(int energyRequired, int delay) {
        if(!context.isPacketsLoaded()) return;
        int currentSpecEnergy = client.getVarpValue(300) / 10;
        if (currentSpecEnergy >= energyRequired && !isSpecEnabled()) {
            executor.schedule(() -> {
                Widget w = context.runOnClientThread(() -> client.getWidget(10485796));
                Point pt = uiService.getClickbox(w);
                mousePackets.queueClickPacket(pt.getX(), pt.getY());
                widgetPackets.queueWidgetAction(w, "Use"); // or Use Special Attack
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Toggles the players run energy. If it is on it will turn it off and vice versa. Use
     * {activateRun} and {deactivateRun} if a specific state is required.
     */
    public void toggleRun() {
        if(!context.isPacketsLoaded()) return;
        executor.schedule(() -> {
            Widget w = context.runOnClientThread(() -> client.getWidget(10485788));
            Point pt = uiService.getClickbox(w);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(w, "Toggle Run");
        }, 50, TimeUnit.MILLISECONDS);
    }

    /**
     * Activates run if it is deactivated.
     */
    public void activateRun() {
        if(isRunEnabled()) return;
        toggleRun();
    }

    /**
     * Deactivates run if it is enabled.
     */
    public void deactivateRun() {
        if(isRunEnabled()) toggleRun();
    }

    /**
     * Returns true when a players run is enabled and false otherwise.
     * @return boolean
     */
    public boolean isRunEnabled() {
        return client.getVarpValue(173) == 1;
    }

    /**
     * Returns the amount of run energy remaining for the player as an integer between 0 and 100.
     * @return int
     */
    public int currentRunEnergy() {
        return this.client.getEnergy() / 100;
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
