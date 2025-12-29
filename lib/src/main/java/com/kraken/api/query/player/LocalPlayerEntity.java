package com.kraken.api.query.player;

import com.kraken.api.Context;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.ui.tab.InterfaceTab;
import com.kraken.api.service.ui.tab.TabService;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LocalPlayerEntity extends PlayerEntity {
    private static final int VENOM_VALUE_CUTOFF = -38;
    private static final int VENOM_THRESHOLD = 1000000;
    private static final int LOGOUT_WIDGET_ID = 11927560;

    private final  ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    public LocalPlayerEntity(Context ctx) {
        super(ctx, ctx.getClient().getLocalPlayer());
    }

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

    @Override
    public Player raw() {
        return ctx.getClient().getLocalPlayer();
    }

    /**
     * Checks if the player is currently moving based on their pose animation.
     * A player is considered moving if their pose animation is different from their idle pose animation.
     *
     * @return {@code true} if the player is moving, {@code false} if they are idle.
     */
    public boolean isMoving() {
        return ctx.runOnClientThreadOptional(() -> {
            Player localPlayer = ctx.getClient().getLocalPlayer();
            if (localPlayer == null) {
                return false;
            }
            return localPlayer.getPoseAnimation() != localPlayer.getIdlePoseAnimation();
        }).orElse(false);
    }


    /**
     * Checks if the player is currently idle based on their pose animation.
     * A player is considered idle if their pose animation is the same as their idle pose animation.
     * @return {@code true} if the player is idle, {@code false} if they are moving.
     */
    public boolean isIdle() {
        return ctx.runOnClientThreadOptional(() -> {
            Player localPlayer = ctx.getClient().getLocalPlayer();
            if (localPlayer == null) {
                return false;
            }
            return localPlayer.getPoseAnimation() == localPlayer.getIdlePoseAnimation() && localPlayer.getAnimation() == -1;
        }).orElse(false);
    }

    /**
     * Gets the amount of special attack energy remaining as a percent (0-100).
     * @return int the amount of special attack energy the player has remaining.
     */
    public int getSpecialAttackEnergy() {
        return ctx.getVarpValue(300) / 10;
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
        return (double) (ctx.getClient().getBoostedSkillLevel(Skill.HITPOINTS) * 100) / ctx.getClient().getRealSkillLevel(Skill.HITPOINTS);
    }

    /**
     * Returns the players current health remaining as an integer.
     * @return the remaining amount of hitpoints the player currently has.
     */
    public int getHealthRemaining() {
        return ctx.getClient().getBoostedSkillLevel(Skill.HITPOINTS);
    }

    /**
     * Returns the players maximum health as an integer.
     * @return The total amount of hitpoints the player has.
     */
    public int getMaxHealth() {
        return ctx.getClient().getRealSkillLevel(Skill.HITPOINTS);
    }

    /**
     * Returns true when the spec is enabled and false otherwise
     * @return True if spec is enabled and false otherwise
     */
    public boolean isSpecEnabled() {
        return ctx.getVarpValue(301) == 1;
    }

    /**
     * Returns true when the spec is disabled and false otherwise
     * @return True if speci is disabled and false otherwise
     */
    public boolean isSpecDisabled() {
        return ctx.getVarpValue(301) == 0;
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
        if(!ctx.isPacketsLoaded()) return;
        int currentSpecEnergy = ctx.getVarpValue(300) / 10;
        if (currentSpecEnergy >= energyRequired && !isSpecEnabled()) {
            executor.schedule(() -> {
                Widget w = ctx.runOnClientThread(() -> ctx.getClient().getWidget(10485796));
                ctx.getInteractionManager().interact(w, "Use");
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Toggles the players run energy. If it is on it will turn it off and vice versa. Use
     * {activateRun} and {deactivateRun} if a specific state is required.
     */
    public void toggleRun() {
        if(!ctx.isPacketsLoaded()) return;
        executor.schedule(() -> {
            Widget w = ctx.runOnClientThread(() -> ctx.getClient().getWidget(10485788));
            ctx.getInteractionManager().interact(w, "Toggle Run");
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
        return ctx.getVarpValue(173) == 1;
    }

    /**
     * Returns the amount of run energy remaining for the player as an integer between 0 and 100.
     * @return int
     */
    public int currentRunEnergy() {
        return ctx.getClient().getEnergy() / 100;
    }


    public boolean logout() {
        ctx.runOnClientThread(() -> {
            Player localPlayer = ctx.getClient().getLocalPlayer();
            if (localPlayer == null) {
                return false;
            }
            TabService tabService = ctx.getService(TabService.class);
            return tabService.switchTo(InterfaceTab.LOGOUT);
        });

        return ctx.runOnClientThread(() -> {
            WidgetEntity logoutButton = ctx.widgets().withId(LOGOUT_WIDGET_ID).first();

            if (logoutButton == null) {
                logoutButton = ctx.widgets().withAction("Logout").first();
            }

            if (logoutButton == null || !logoutButton.isVisible()) {
                return false;
            }

            return logoutButton.interact("Logout");
        });
    }

    /**
     * Calculates the total skill level of the player by summing up the real levels of all their skills.
     * <p>
     * This method iterates over all available skills and retrieves their real levels from the game client.
     * It then calculates the total by summing up these levels.
     * </p>
     *
     * @return The total skill level of the player as an integer.
     */
    public int totalSkillLevel() {
        return ctx.runOnClientThread(() -> {
            int totalLevel = 0;
            for (Skill skill : Skill.values()) {
                totalLevel += ctx.getClient().getRealSkillLevel(skill);
            }
            return totalLevel;
        });
    }

    /**
     * Retrieves the base level of the specified skill.
     * <p>
     * This method executes a client-side operation to obtain the real (unboosted) level
     * of the given skill. The skill must be a valid {@code Skill} object.
     * </p>
     *
     * @param skill The {@code Skill} for which the base level is being queried.
     *              Represents one of the player's in-game skills.
     * @return The base level of the specified skill as an integer. If the skill is
     *         invalid or cannot be retrieved, the method may return 0 or throw
     *         an exception depending on the implementation details.
     */
    public int getLevel(Skill skill) {
        return ctx.runOnClientThread(() -> ctx.getClient().getRealSkillLevel(skill));
    }

    /**
     * Retrieves the boosted level of a specified skill.
     * <p>
     * This method executes on the client thread to safely fetch the current boosted level
     * of the provided skill from the client.
     *
     * @param skill The {@literal @}Skill object representing the skill for which the boosted level is needed.
     *              Must not be {@literal null}.
     * @return The boosted level of the specified skill as an integer.
     */
    public int getBoostedLevel(Skill skill) {
        return ctx.runOnClientThread(() -> ctx.getClient().getBoostedSkillLevel(skill));
    }

    /**
     * Retrieves the total experience of the specified skill for the current client.
     *
     * <p>
     * This method executes a thread-safe operation to fetch the experience points
     * associated with the given {@link Skill} object by running on the client thread.
     * </p>
     *
     * @param skill the skill whose experience is to be retrieved.
     *              This parameter must not be {@literal null}.
     * @return the total experience points of the specified skill.
     */
    public int getExperience(Skill skill) {
        Client client = ctx.getClient();
        return ctx.runOnClientThread(() -> client.getSkillExperience(skill));
    }

    /**
     * Calculates the total experience across all skills for the current client.
     *
     * <p>This method iterates through all the skill categories, retrieves their individual
     * experience values, and then sums them up to return the total accumulated experience.</p>
     *
     * @return the total experience points of all skills combined for the client.
     */
    public int getTotalExperience() {
        Client client = ctx.getClient();
        return ctx.runOnClientThread(() -> {
            int totalExperience = 0;
            for (Skill skill : Skill.values()) {
                totalExperience += client.getSkillExperience(skill);
            }
            return totalExperience;
        });
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
        return ctx.runOnClientThread(() -> {
            Player localPlayer = raw();
            if (worldPoint == null || localPlayer == null) {
                return false;
            }

            if (xRadius < 0 || yRadius < 0) {
                return false;
            }

            WorldPoint playerLocation = localPlayer.getWorldLocation();

            if (playerLocation == null) {
                return false;
            }

            if (worldPoint.getPlane() != playerLocation.getPlane()) {
                return false;
            }

            int deltaX = Math.abs(playerLocation.getX() - worldPoint.getX());
            int deltaY = Math.abs(playerLocation.getY() - worldPoint.getY());

            return deltaX <= xRadius && deltaY <= yRadius;
        });
    }
}
