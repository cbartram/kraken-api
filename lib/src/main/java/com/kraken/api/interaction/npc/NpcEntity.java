package com.kraken.api.interaction.npc;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import net.runelite.api.NPC;

public class NpcEntity extends AbstractEntity<NPC> {
    public NpcEntity(Context ctx, NPC raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        return raw.getName();
    }

    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }
}