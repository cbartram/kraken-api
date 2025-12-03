package com.kraken.api.query.equipment;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import com.kraken.api.query.container.ContainerItem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Slf4j
public class EquipmentQuery extends AbstractQuery<EquipmentEntity, EquipmentQuery, ContainerItem> {

    private final HashMap<Integer, Integer> equipmentSlotWidgetMapping = new HashMap<>();
    private final HashMap<Integer, Integer> intMapping = new HashMap<>();

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
        intMapping.put(0, 0);
        intMapping.put(1, 1);
        intMapping.put(2, 2);
        intMapping.put(3, 3);
        intMapping.put(4, 4);
        intMapping.put(5, 5);
        intMapping.put(6, 7);
        intMapping.put(7, 9);
        intMapping.put(8, 10);
        intMapping.put(9, 12);
        intMapping.put(10, 13);
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
     * @return EquipmentQuery
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

    /**
     * Sources inventory items which are wieldable or wearable from the players inventory
     * @return List of equipment entities
     */
    private List<EquipmentEntity> collectInventoryItems() {
        ctx.getClient().runScript(6009, 9764864, 28, 1, -1);

        ItemContainer container = ctx.getClient().getItemContainer(InventoryID.INV);
        if (container == null) return Collections.emptyList();

        Widget inventory = ctx.getClient().getWidget(149, 0);
        if (inventory == null) return Collections.emptyList();

        Widget[] inventoryWidgets = inventory.getDynamicChildren();
        List<EquipmentEntity> entities = new ArrayList<>();

        Item[] items = container.getItems();
        for (int i = 0; i < items.length; i++) {
            final Item item = items[i];
            if (item.getId() == -1 || item.getId() == 6512) continue;

            final ItemComposition def = ctx.getClient().getItemDefinition(item.getId());
            if (def == null || def.getInventoryActions() == null) continue;

            List<String> actions = Arrays.stream(def.getInventoryActions())
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());

            if(actions.contains("wield") || actions.contains("wear")) {
                Widget w = (i < inventoryWidgets.length) ? inventoryWidgets[i] : null;
                entities.add(new EquipmentEntity(ctx, def.getName(), new ContainerItem(item, def, i, ctx, w, ContainerItem.ItemOrigin.INVENTORY)));
            }
        }
        return entities;
    }

    /**
     * Sources equipped items from the players equipment interface
     * @return List of equipped item objects
     */
    private List<EquipmentEntity> collectEquippedItems() {
        for (int i = 0; i < 11; i++) {
            ctx.getClient().runScript(545, (25362447 + i), intMapping.get(i), 1, 1, 2);
        }

        List<EquipmentEntity> entities = new ArrayList<>();
        ItemContainer equipment = ctx.getClient().getItemContainer(94); // 94 = Equipment
        if (equipment == null) return Collections.emptyList();

        int i = -1;
        for (Item item : equipment.getItems()) {
            i++;
            if (item == null) continue;
            if (item.getId() == 6512 || item.getId() == -1) continue;
            
            // 387 = WidgetInfo.EQUIPMENT.getGroupId(), child id
            Widget w = ctx.getClient().getWidget(387, equipmentSlotWidgetMapping.get(i));
           
            if (w == null || w.getActions() == null) {
                continue;
            }

            final ItemComposition def = ctx.getClient().getItemDefinition(w.getItemId());

            // Create ContainerItem with EQUIPMENT origin
            // Pass slotIndex so we know which equipment slot this is
            entities.add(new EquipmentEntity(ctx, def.getName(), new ContainerItem(item, def, i, ctx, w, ContainerItem.ItemOrigin.EQUIPMENT)));
        }
        return entities;
    }


    /**
     * Filters for equipment with a specific item id.
     * @param id Item id to filter for
     * @return EquipmentQuery
     */
    public EquipmentQuery withId(int id) {
        return filter(i -> i.raw().getId() == id);
    }

    /**
     * Checks if the player is wearing an item by id.
     * Note: This strictly checks the equipment slots, ignoring the "inInventory" setting.
     * @param id The item id for the equipment to check
     * @return True if the player is wearing the equipment and false otherwise
     */
    public boolean isWearing(int id) {
        return ctx.runOnClientThread(() ->
                collectEquippedItems().stream().anyMatch(i -> i.raw().getId() == id)
        );
    }

    /**
     * Checks if the player is wearing an item by name.
     * Note: This strictly checks the equipment slots, ignoring the "inInventory" setting.
     * @param name The name of the equipment to check
     * @return True if the player is wearing the equipment and false otherwise
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
        return ctx.runOnClientThread(() -> collectEquippedItems()
                .stream()
                .filter(i -> i.raw().getSlot() == slot.getSlotIdx())
                .findFirst()
                .orElse(new EquipmentEntity(ctx, "", null)));
    }
}
