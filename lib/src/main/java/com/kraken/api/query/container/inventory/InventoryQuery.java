package com.kraken.api.query.container.inventory;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.query.container.ContainerItem;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class InventoryQuery extends AbstractQuery<InventoryEntity, InventoryQuery, ContainerItem> {

    public InventoryQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<InventoryEntity>> source() {
        return () -> {
            List<InventoryEntity> inventoryEntities = ctx.runOnClientThread(() -> {
                ctx.getClient().runScript(6009, 9764864, 28, 1, -1);

                ItemContainer container = ctx.getClient().getItemContainer(InventoryID.INV);
                if(container == null) return Collections.emptyList();

                Widget inventory = ctx.getClient().getWidget(149, 0);
                if(inventory == null) return Collections.emptyList();

                Widget[] inventoryWidgets = inventory.getDynamicChildren();

                List<InventoryEntity> entities = new ArrayList<>();
                for (int i = 0; i < container.getItems().length; i++) {
                    final Item item = container.getItems()[i];
                    if (item.getId() == -1 || item.getId() == 6512) continue;

                    final ItemComposition itemComposition = ctx.getClient().getItemDefinition(item.getId());
                    if (itemComposition == null) continue;

                    Widget widget = null;
                    if (i < inventoryWidgets.length) {
                        widget = inventoryWidgets[i];
                    }

                    entities.add(new InventoryEntity(ctx, new ContainerItem(item, itemComposition, i, ctx, widget, null)));
                }

                return entities;
            });

            return inventoryEntities.stream();
        };
    }

    /**
     * Returns true if the inventory is empty and false otherwise.
     * @return true if the inventory is empty and false otherwise.
     */
    public boolean isEmpty() {
        return source().get().findAny().isEmpty();
    }

    /**
     * Returns true if the inventory is full and false otherwise.
     * @return True if the inventory is full and false otherwise.
     */
    public boolean isFull() {
        return source().get().count() >= 28;
    }

    /**
     * Returns the count of items in the inventory
     * @return The count of items in the players inventory
     */
    public long count() {
        return source().get().count();
    }

    /**
     * Returns the free space in a users inventory.
     * @return The amount of free space available in the players inventory
     */
    public int freeSpace() {
        return Math.toIntExact(28 - source().get().count());
    }

    /**
     * Returns a list of Inventory Items which can be consumed for health.
     * @return List of Inventory items which are food.
     */
    public InventoryQuery food() {
        return filter(i -> i.raw().isFood());
    }

    /**
     * Returns true when the player has edible hard food in their inventory and false otherwise.
     * @return boolean
     */
    public boolean hasFood() {
        return filter(i -> i.raw().isFood()).count() > 0;
    }

    /**
     * Filters for items that match a specific name pattern.
     * Usage: ctx.inventory().nameContains("potion").list();
     */
    public InventoryQuery nameContains(String partialName) {
        return filter(item -> item.getName() != null && item.getName().toLowerCase().contains(partialName.toLowerCase()));
    }

    /**
     * Filters for items that are noted (cert).
     */
    public InventoryQuery noted() {
        return filter(item -> item.raw().isNoted());
    }

    /**
     * Filters for un-noted items.
     */
    public InventoryQuery unnoted() {
        return filter(item -> !item.raw().isNoted());
    }

    /**
     * Filters for items that stack (runes, arrows, noted items).
     */
    public InventoryQuery stackable() {
        return filter(item -> item.raw().isStackable());
    }

    /**
     * Filters by item quantity.
     */
    public InventoryQuery quantityGreaterThan(int amount) {
        return filter(item -> item.raw().getQuantity() > amount);
    }
}