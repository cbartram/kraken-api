package com.kraken.api.query.container.inventory;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.container.ContainerItem;

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
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    @Override
    public int getId() {
        return raw.getId();
    }

    /**
     * Returns true if the inventory item has the specified action. i.e "Swordfish" will have the action "Eat", "Drop", and "Examine" but not "Drink"
     * @param action The action to check for
     * @return True if the item has the action and false otherwise
     */
    public boolean hasAction(String action) {
        return Arrays.stream(raw.getInventoryActions()).anyMatch(a -> a != null && a.equalsIgnoreCase(action));
    }

    /**
     * Uses one inventory item on another.
     * @param other The other inventory item to be used on.
     * @return True if the combination action was successful and false otherwise
     */
    public boolean combineWith(ContainerItem other) {
        if(raw.getWidget() != null && other.getWidget() != null){
            ctx.getInteractionManager().interact(raw.getWidget(), other.getWidget());
            return true;
        }
        return false;
    }

    /**
     * Uses one item in the inventory on the other. This is a shallow wrapper
     * around {@code combineWith()}
     * @param other The other inventory item to be used on.
     * @return True if the use on item was successful and false otherwise
     */
    public boolean useOn(ContainerItem other) {
        return combineWith(other);
    }

    /**
     * Drops the item from the inventory.
     * @return True if the item was successfully dropped, false otherwise.
     */
    public boolean drop() {
        return interact("Drop");
    }
}
