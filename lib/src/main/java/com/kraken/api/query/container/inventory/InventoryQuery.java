package com.kraken.api.query.container.inventory;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.query.container.ContainerItem;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
     * Returns true when the inventory contains a specific item, found by its item id.
     * @param id The id of the item to search for
     * @return True if the inventory has the item and false otherwise
     */
    public boolean hasItem(int id) {
        return filter(i -> i.getId() == id).count() > 0;
    }

    /**
     * Returns true when the inventory contains a specific item, found by its name.
     * This is case-insensitive but does require the entire item name.
     * @param name The name of the item to search for
     * @return True if the inventory has the item and false otherwise
     */
    public boolean hasItem(String name) {
        return filter(i -> i.getName().equalsIgnoreCase(name)).count() > 0;
    }

    /**
     * Returns true ONLY if the inventory contains ALL of the specified item IDs.
     * @param ids Variable argument of item IDs to search for.
     * @return True if every single ID in the arguments exists in the inventory.
     */
    public boolean hasItems(int... ids) {
        if (ids == null || ids.length == 0) return true;

        // Collect all valid IDs currently in the inventory into a Set
        // We use a Set for O(1) lookups and to handle duplicates automatically
        Set<Integer> inventoryIds = stream()
                .map(InventoryEntity::getId)
                .collect(Collectors.toSet());

        for (int id : ids) {
            if (!inventoryIds.contains(id)) {
                return false; // Return false immediately if ANY required item is missing
            }
        }

        return true;
    }

    /**
     * Determines whether the inventory contains all items specified by the given list of IDs.
     * <p>
     * If the provided list of IDs is {@code null} or empty, the method returns {@code true}.
     * Otherwise, it internally converts the list to an array and delegates the check to the
     * {@code hasItems(int... ids)} method.
     *
     * @param ids A {@code List} of {@code Integer} IDs representing the items to search for.
     *            Each ID corresponds to a specific inventory item.
     *            <ul>
     *                <li>If the list is {@code null} or empty, the method will return {@code true}.</li>
     *                <li>All IDs in the list must exist in the inventory for the method to return {@code true}.</li>
     *            </ul>
     * @return {@code true} if the inventory contains all items specified in the list, or if the list is {@code null} or empty.
     *         Otherwise, {@code false} is returned.
     */
    public boolean hasItems(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return true;
        return hasItems(ids.stream().mapToInt(Integer::intValue).toArray());
    }

    /**
     * Returns true ONLY if the inventory contains ALL of the specified item names.
     * This is case-insensitive.
     * @param names Variable argument of item names to search for.
     * @return True if every single name in the arguments exists in the inventory.
     */
    public boolean hasItems(String... names) {
        if (names == null || names.length == 0) return true;

        Set<String> inventoryNames = stream()
                .map(InventoryEntity::getName)
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        for (String name : names) {
            if (name == null) continue;

            // If the inventory set does not contain the required name, fail immediately
            if (!inventoryNames.contains(name.toLowerCase())) {
                System.out.println("Inventory names does not contain: " + name + " names: " + inventoryNames);
                return false;
            }
        }

        return true;
    }

    /**
     * Sorts the inventory query results based on the specified {@code InventoryOrder}.
     * <p>
     * This method applies the given {@code InventoryOrder}'s comparator to
     * sort inventory items based on the desired order or pattern.
     * </p>
     *
     * @param order The {@code InventoryOrder} specifying the sorting strategy.
     *              <ul>
     *                  <li>{@literal @}TOP_LEFT_BOTTOM_RIGHT - Standard reading order: Row 1 (Left{@literal ->}Right),
     *                      Row 2 (Left{@literal ->}Right), etc.</li>
     *                  <li>{@literal @}BOTTOM_RIGHT_TOP_LEFT - Reverse reading order: Last Item {@literal ->} First Item.</li>
     *                  <li>{@literal @}ZIG_ZAG - Snake/Zig-Zag pattern: Row 1 (Left{@literal ->}Right),
     *                      Row 2 (Right{@literal ->}Left), Row 3 (Left{@literal ->}Right), etc.</li>
     *                  <li>{@literal @}ZIG_ZAG_REVERSE - Reverse Snake/Zig-Zag pattern starting from the bottom right.</li>
     *                  <li>{@literal @}TOP_DOWN_LEFT_RIGHT - Vertical columns:
     *                      Column 1 (Top{@literal ->}Bottom), Column 2 (Top{@literal ->}Bottom), etc.</li>
     *              </ul>
     *
     * @return An {@code InventoryQuery} object containing the inventory items
     *         sorted based on the given order.
     */
    public InventoryQuery orderBy(InventoryOrder order) {
         return sorted(order.getComparator());
    }

    /**
     * Filters for inventory items with a specific action like: "Drop", "Eat", or "Examine".
     * @param action The action to filter for
     * @return InventoryQuery
     */
    public InventoryQuery withAction(String action) {
        return filter(i -> i.hasAction(action.toLowerCase()));
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
     * Filters for items that are noted (cert).
     * @return InventoryQuery
     */
    public InventoryQuery noted() {
        return filter(item -> item.raw().isNoted());
    }

    /**
     * Filters for un-noted items.
     * @return InventoryQuery
     */
    public InventoryQuery unnoted() {
        return filter(item -> !item.raw().isNoted());
    }

    /**
     * Filters for items that stack (runes, arrows, noted items).
     * @return InventoryQuery
     */
    public InventoryQuery stackable() {
        return filter(item -> item.raw().isStackable());
    }

    /**
     * Filters by item quantity. This filter is strictly greater than i.e {@code ctx.inventory().nameContains("karambwanji").quantityGreaterThan(500);}
     * will only return a {@code ContainerItem} when 501 Karambwanji's are present.
     * @param amount The amount of the stack to filter for.
     * @return InventoryQuery
     */
    public InventoryQuery quantityGreaterThan(int amount) {
        return filter(item -> item.raw().getQuantity() > amount);
    }
}