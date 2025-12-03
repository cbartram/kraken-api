package com.kraken.api.query.equipment;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.container.ContainerItem;

public class EquipmentEntity extends AbstractEntity<ContainerItem> {
    private final String name;

    public EquipmentEntity(Context ctx, String name, ContainerItem raw) {
        super(ctx, raw);
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    @Override
    public int getId() {
        return raw.getId();
    }

    /**
     * Wears an equippable item
     * @return true if the item was equipped and false otherwise
     */
    public boolean wear() {
        return interact("wear");
    }

    /**
     * Wields a weapon.
     * @return True if the weapon was equipped and false otherwise
     */
    public boolean wield() {
        return interact("wield");
    }

    /**
     * Removes an equipped weapon or piece of armour.
     * @return True if the removal was successful and false otherwise.
     */
    public boolean remove() {
        return interact("remove");
    }
}