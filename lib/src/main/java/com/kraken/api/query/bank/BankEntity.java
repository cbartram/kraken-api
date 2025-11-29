package com.kraken.api.query.bank;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;

public class BankEntity extends AbstractEntity<BankItemWidget> {
    public BankEntity(Context ctx, BankItemWidget raw) {
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
