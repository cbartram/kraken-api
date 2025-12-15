package com.kraken.api.query.equipment;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.container.ContainerItem;

public class EquipmentEntity extends AbstractEntity<ContainerItem> {

    public EquipmentEntity(Context ctx, ContainerItem raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        ContainerItem item = raw();
        return item != null ? item.getName(): null;
    }

    @Override
    public boolean interact(String action) {
        ContainerItem raw = raw();
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    @Override
    public int getId() {
        ContainerItem item = raw();
        return item != null ? item.getId() : -1;
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