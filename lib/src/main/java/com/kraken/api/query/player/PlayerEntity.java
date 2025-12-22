package com.kraken.api.query.player;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import net.runelite.api.Player;

public class PlayerEntity extends AbstractEntity<Player> {
    public PlayerEntity(Context ctx, Player raw) {
        super(ctx, raw);
    }

    @Override
    public int getId() {
        Player p = raw();
        return p != null ? p.getId() : -1;
    }

    @Override
    public String getName() {
        Player p = raw();
        return p != null ? p.getName() : null;
    }

    @Override
    public boolean interact(String action) {
        Player p = raw();
        if (p == null) return false;

        ctx.getInteractionManager().interact(p, action);
        return true;
    }

    /**
     * Returns true if the player has a Skull icon above their head and false otherwise.
     * @return boolean True if the player is skulled
     */
    public boolean isSkulled() {
        return raw().getSkullIcon() != -1;
    }
}