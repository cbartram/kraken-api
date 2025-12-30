package com.kraken.api.query.npc;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.service.tile.GameArea;
import lombok.SneakyThrows;
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;
import net.runelite.api.widgets.Widget;

public class NpcEntity extends AbstractEntity<NPC> {
    public NpcEntity(Context ctx, NPC raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        NPC n = raw();
        return n != null ? ctx.runOnClientThread(n::getName) : null;
    }

    @Override
    public int getId() {
        NPC n = raw();
        return n != null ? ctx.runOnClientThread(n::getId) : -1;
    }

    /**
     * Gets the health percentage of the NPC.
     * @return Health percentage (0-100), or -1 if unknown
     */
    public double getHealthPercentage() {
        NPC raw = raw();
        int ratio = raw.getHealthRatio();
        int scale = raw.getHealthScale();

        if (scale == 0) return -1;
        return (double) ratio / (double) scale * 100.0;
    }

    /**
     * Retrieves the head icon associated with the NPC, if it exists.
     * <p>
     * A head icon represents an overhead visual indicator, such as combat prayers or effects
     * like Hunllef's prayers or Nex's deflect melee. This is determined from the NPC's overhead sprite IDs.
     * </p>
     *
     * <ul>
     *   <li>If no head icons are defined for the NPC, this will return {@code null}.</li>
     *   <li>If a valid head icon is found, it will be returned as a {@code HeadIcon} enum.</li>
     * </ul>
     *
     * @return The {@code HeadIcon} for the NPC, or {@code null} if no valid head icon exists.
     */
    @SneakyThrows
    public HeadIcon getHeadIcon() {
        NPC raw = raw();
        if (raw.getOverheadSpriteIds() == null) return null;

        for (int i = 0; i < raw.getOverheadSpriteIds().length; i++) {
            int overheadSpriteId = raw.getOverheadSpriteIds()[i];
            if (overheadSpriteId == -1) continue;
            return HeadIcon.values()[overheadSpriteId];
        }

        return null;
    }

    /**
     * Calculates the distance between the NPC and the local player within the game world.
     * <p>
     * The method retrieves the NPC's local location and the local player's location
     * from the game client, then computes the distance between the two positions.
     * If the distance cannot be calculated (e.g., due to a timeout on the client thread),
     * {@code Integer.MAX_VALUE} is returned as a fallback.
     * </p>
     *
     * <ul>
     *   <li>The computation is performed on the game's client thread to ensure thread safety unless the result is unavailable.</li>
     * </ul>
     *
     * @return The distance between the NPC's location and the local player's location in the game world,
     *         or {@code Integer.MAX_VALUE} if the distance cannot be determined.
     */
    public int getDistanceFromPlayer() {
        NPC raw = raw();
        return ctx.runOnClientThreadOptional(() -> raw.getLocalLocation().distanceTo(
                ctx.getClient().getLocalPlayer().getLocalLocation())).orElse(Integer.MAX_VALUE);
    }

    /**
     * Checks if the NPC's current location is within the game area.
     * @param area The {@link GameArea} to check.
     * @return True if the NPC location is within the game area and false otherwise.
     */
    public boolean isInArea(GameArea area) {
        if (area == null) return false;
        return area.contains(raw().getWorldLocation());
    }


    @Override
    public boolean interact(String action) {
        NPC raw = raw();
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    /**
     * Attacks an NPC. This is a shallow wrapper around the interact() method.
     * @return True if the attack interaction was successful and false otherwise
     */
    public boolean attack() {
        return interact("attack");
    }

    /**
     * Uses a specified widget on the NPC (i.e. Casting Crumble Undead Spell on the Vorkaths Spawn)
     * @param widget The widget to use on the NPC
     * @return True if the interaction was successful and false otherwise
     */
    public boolean useWidget(Widget widget) {
        NPC raw = raw();
        if (raw == null) return false;
        ctx.getInteractionManager().interact(widget, raw);
        return true;
    }
}