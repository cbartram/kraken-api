package com.kraken.api.query.equipment;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import net.runelite.api.widgets.Widget;

public class EquipmentEntity extends AbstractEntity<Widget> {
    public EquipmentEntity(Context ctx, Widget raw) {
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