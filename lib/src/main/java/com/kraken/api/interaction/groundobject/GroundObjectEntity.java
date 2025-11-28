package com.kraken.api.interaction.groundobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;

public class GroundObjectEntity extends AbstractEntity<GroundItem> {

    public GroundObjectEntity(Context ctx, GroundItem raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        return this.raw.getName();
    }

    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }
}