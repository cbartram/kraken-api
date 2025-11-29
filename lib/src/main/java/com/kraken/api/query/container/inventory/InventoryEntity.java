package com.kraken.api.query.container.inventory;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;

public class InventoryEntity extends AbstractEntity<ContainerItem> {
    public InventoryEntity(Context ctx, ContainerItem raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        return raw.getName();
    }

    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, false, action);
        return true;
    }
}
