package com.kraken.api.query.equipment;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import net.runelite.api.widgets.Widget;

public class EquipmentEntity extends AbstractEntity<Widget> {
    private final String name;

    public EquipmentEntity(Context ctx, String name, Widget raw) {
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