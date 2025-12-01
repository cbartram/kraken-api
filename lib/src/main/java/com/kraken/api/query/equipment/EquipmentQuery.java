package com.kraken.api.query.equipment;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class EquipmentQuery extends AbstractQuery<EquipmentEntity, EquipmentQuery, Widget> {

    private final HashMap<Integer, Integer> equipmentSlotWidgetMapping = new HashMap<>();

    private enum EquipmentSource {
        INVENTORY_ONLY,
        INTERFACE_ONLY,
        BOTH
    }

    private EquipmentSource dataSource = EquipmentSource.BOTH;

    public EquipmentQuery(Context ctx) {
        super(ctx);
        equipmentSlotWidgetMapping.put(0, 15);
        equipmentSlotWidgetMapping.put(1, 16);
        equipmentSlotWidgetMapping.put(2, 17);
        equipmentSlotWidgetMapping.put(3, 18);
        equipmentSlotWidgetMapping.put(4, 19);
        equipmentSlotWidgetMapping.put(5, 20);
        equipmentSlotWidgetMapping.put(7, 21);
        equipmentSlotWidgetMapping.put(9, 22);
        equipmentSlotWidgetMapping.put(10, 23);
        equipmentSlotWidgetMapping.put(12, 24);
        equipmentSlotWidgetMapping.put(13, 25);
    }


    /**
     * Configures the query to only look for wearable/wieldable items currently
     * inside the player's inventory.
     * @return EquipmentQuery
     */
    public EquipmentQuery inInventory() {
        this.dataSource = EquipmentSource.INVENTORY_ONLY;
        return this;
    }

    /**
     * Configures the query to only look for items currently equipped
     * on the player (read from the Equipment interface widgets).
     * @return EquipmentQuery
     */
    public EquipmentQuery inInterface() {
        this.dataSource = EquipmentSource.INTERFACE_ONLY;
        return this;
    }

    /**
     * Configures the query to look at both equipped items and wearable items
     * in the inventory.
     */
    public EquipmentQuery all() {
        this.dataSource = EquipmentSource.BOTH;
        return this;
    }

    @Override
    protected Supplier<Stream<EquipmentEntity>> source() {
        return () -> {
            List<EquipmentEntity> entities = new ArrayList<>();
            if (dataSource == EquipmentSource.INVENTORY_ONLY || dataSource == EquipmentSource.BOTH) {
                entities.addAll(collectInventoryItems());
            }

            if (dataSource == EquipmentSource.INTERFACE_ONLY || dataSource == EquipmentSource.BOTH) {
                entities.addAll(collectEquippedItems());
            }

            return entities.stream();
        };
    }

    private List<EquipmentEntity> collectInventoryItems() {
        ctx.getClient().runScript(6009, 9764864, 28, 1, -1);

        ItemContainer container = ctx.getClient().getItemContainer(InventoryID.INV);
        if (container == null) return Collections.emptyList();

        Widget inventory = ctx.getClient().getWidget(149, 0);
        if (inventory == null) return Collections.emptyList();

        Widget[] inventoryWidgets = inventory.getDynamicChildren();
        List<EquipmentEntity> entities = new ArrayList<>();

        for (int i = 0; i < container.getItems().length; i++) {
            final Item item = container.getItems()[i];
            if (item.getId() == -1 || item.getId() == 6512) continue;

            final ItemComposition itemComposition = ctx.getClient().getItemDefinition(item.getId());

            List<String> actions = List.of(itemComposition.getInventoryActions());
            if (!actions.contains("wield") && !actions.contains("wear")) continue;

            Widget widget = null;
            if (i < inventoryWidgets.length) {
                widget = inventoryWidgets[i];
            }

            entities.add(new EquipmentEntity(ctx, widget));
        }
        return entities;
    }

    private List<EquipmentEntity> collectEquippedItems() {
        List<EquipmentEntity> entities = new ArrayList<>();
        for (int i = 0; i < 13; i++) {
            if (!equipmentSlotWidgetMapping.containsKey(i)) continue;

            int widgetChildId = equipmentSlotWidgetMapping.get(i);
            Widget widget = ctx.getClient().getWidget(WidgetInfo.EQUIPMENT.getGroupId(), widgetChildId);

            // 13 slots in your equipment to wear, if no widget found then no gear in that slot
            if (widget == null) continue;
            if (widget.getItemId() == -1) continue;

            entities.add(new EquipmentEntity(ctx, widget));
        }
        return entities;
    }


    /**
     * Filters for equipment with a specific item id.
     * @param id Item id to filter for
     * @return EquipmentQuery
     */
    public EquipmentQuery withId(int id) {
        return filter(i -> i.raw().getItemId() == id);
    }

    /**
     * Checks if the player is wearing an item by id.
     * Note: This strictly checks the equipment slots, ignoring the "inInventory" setting.
     */
    public boolean isWearing(int id) {
        // We use the helper directly here to avoid overhead of creating a new query
        return ctx.runOnClientThread(() ->
                collectEquippedItems().stream().anyMatch(i -> i.raw().getItemId() == id)
        );
    }

    /**
     * Checks if the player is wearing an item by name.
     * Note: This strictly checks the equipment slots, ignoring the "inInventory" setting.
     */
    public boolean isWearing(String name) {
        return ctx.runOnClientThread(() ->
                collectEquippedItems().stream().anyMatch(i -> name.equalsIgnoreCase(i.getName()))
        );
    }

    /**
     * Returns the interactable equipment entity for a given equipment slot.
     * @param slot The {@code EquipmentInventorySlot} to retrieve.
     * @return EquipmentEntity
     */
    public EquipmentEntity inSlot(EquipmentInventorySlot slot) {
        return ctx.runOnClientThread(() -> {
            if (!equipmentSlotWidgetMapping.containsKey(slot.getSlotIdx())) return null;

            Widget widget = ctx.getClient().getWidget(WidgetInfo.EQUIPMENT.getGroupId(), equipmentSlotWidgetMapping.get(slot.getSlotIdx()));
            if (widget == null) {
                return null;
            }

            return new EquipmentEntity(ctx, widget);
        });
    }
}
