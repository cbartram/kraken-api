package com.kraken.api.query.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.inventory.ContainerItem;

public class BankInventoryEntity extends AbstractEntity<ContainerItem> {
    public BankInventoryEntity(Context ctx, ContainerItem raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        return raw.getName();
    }

    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, true, action);
        return true;
    }
}
