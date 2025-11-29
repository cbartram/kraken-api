package com.kraken.api.query.inventory;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;

import java.util.Arrays;

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

    /**
     * Returns true if the inventory item has the specified action. i.e "Swordfish" will have the action "Eat", "Drop", and "Examine" but not "Drink"
     * @param action The action to check for
     * @return True if the item has the action and false otherwise
     */
    public boolean hasAction(String action) {
        return Arrays.stream(raw.getInventoryActions()).anyMatch(a -> a != null && a.equalsIgnoreCase(action));
    }

}
