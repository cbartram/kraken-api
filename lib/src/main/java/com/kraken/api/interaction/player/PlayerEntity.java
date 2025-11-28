package com.kraken.api.interaction.player;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import net.runelite.api.Player;

public class PlayerEntity extends AbstractEntity<Player> {
    public PlayerEntity(Context ctx, Player raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        return raw.getName();
    }

    // TODO Filters for local player and then ability to call toggle special and toggle run, venom info etc...
    // i.e ctx.players().local().toggleRun() same with .toggleSpecial()

    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }
}