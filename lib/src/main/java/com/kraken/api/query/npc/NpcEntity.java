package com.kraken.api.query.npc;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import lombok.SneakyThrows;
import net.runelite.api.HeadIcon;
import net.runelite.api.NPC;

public class NpcEntity extends AbstractEntity<NPC> {
    public NpcEntity(Context ctx, NPC raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        return raw.getName();
    }

    /**
     * Gets the health percentage of the NPC.
     * @return Health percentage (0-100), or -1 if unknown
     */
    public double getHealthPercentage() {
        int ratio = raw.getHealthRatio();
        int scale = raw.getHealthScale();

        if (scale == 0) return -1;
        return (double) ratio / (double) scale * 100.0;
    }

    /**
     * Returns the head icon for an NPC if it exists. A head icon would be a prayer the NPC is prayer:
     * i.e. Hunlleff's prayers or Nex's deflect melee.
     * @return HeadIcon for the NPC
     */
    @SneakyThrows
    public HeadIcon getHeadIcon() {
        if (raw.getOverheadSpriteIds() == null) return null;

        for (int i = 0; i < raw.getOverheadSpriteIds().length; i++) {
            int overheadSpriteId = raw.getOverheadSpriteIds()[i];
            if (overheadSpriteId == -1) continue;
            return HeadIcon.values()[overheadSpriteId];
        }

        return null;
    }

    /**
     * Gets the distance from this NPC to the player.
     * Uses client thread for safe access to player location.
     *
     * @return Distance in tiles
     */
    public int getDistanceFromPlayer() {
        return ctx.runOnClientThreadOptional(() -> raw.getLocalLocation().distanceTo(
                ctx.getClient().getLocalPlayer().getLocalLocation())).orElse(Integer.MAX_VALUE);
    }

    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }
}