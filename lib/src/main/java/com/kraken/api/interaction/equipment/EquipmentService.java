package com.kraken.api.interaction.equipment;

import com.example.EthanApiPlugin.Collections.Equipment;
import com.example.EthanApiPlugin.Collections.EquipmentItemWidget;
import com.example.EthanApiPlugin.Collections.query.EquipmentItemQuery;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.InventoryInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.inventory.InventoryItem;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;

import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Singleton
public class EquipmentService extends AbstractService {

    @Inject
    private ReflectionService reflectionService;

    @Getter
    private final List<InventoryItem> inventory = new ArrayList<>();

    @Getter
    private final List<InventoryItem> equipment = new ArrayList<>();

    static HashMap<Integer, Integer> equipmentSlotWidgetMapping = new HashMap<>();

    static {
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

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        var containerItems = event.getItemContainer().getItems();
        List<InventoryItem> list = new ArrayList<>();

        for (int index = 0; index < containerItems.length; index++) {
            var item = containerItems[index];

            if (item.getId() == -1 || item.getQuantity() < 1) {
                continue;
            }

            list.add(new InventoryItem(item, context.getClient().getItemDefinition(item.getId()), index, context));
        }

        if(event.getContainerId() == 93) {
            inventory.clear();
            inventory.addAll(list);
        } else if(event.getContainerId() == 94) {
            equipment.clear();
            equipment.addAll(list);
        }
    }

    /**
     * Interacts with an item with the specified ID in the inventory using either the wield or wear action.
     *
     * @param id The ID of the item to wield/wear.
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean wield(int id) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThread(() -> {
            List<String> actions = Arrays.stream(client.getItemDefinition(id).getInventoryActions())
                    .filter(action -> action.equalsIgnoreCase("wield") || action.equalsIgnoreCase("wear"))
                    .collect(Collectors.toList());
            if(actions.isEmpty()) {
                log.info("No actions found for id: {}", id);
                return false;
            }

            return InventoryInteraction.useItem(id, actions.get(0));
        });
    }

    /**
     * Wields gear from the players inventory by name.
     * @param name The name of the item to equip.
     * @return True when the wield operation was successful and false otherwise
     */
    public boolean wield(String name) {
        List<InventoryItem> items = inventory.stream()
                .filter(item -> name.equalsIgnoreCase(item.getName()))
                .collect(Collectors.toList());
        if(items.isEmpty()) {
            log.info("No item in inventory found: {}", name);
            return false;
        }
        return wield(items.get(0).getId());
    }

    /**
     * Wields gear from the players inventory by InventoryItem.
     * @param item The InventoryItem to equip.
     * @return True when the wield operation was successful and false otherwise
     */
    public boolean wield(InventoryItem item) {
        return wield(item.getId());
    }

    /**
     * Wields gear from the players inventory using reflection to make the menu invocations.
     * @param item The InventoryItem to equip.
     * @return True when the wield operation was successful and false otherwise
     */
    public boolean wieldReflect(InventoryItem item) {
        return wieldReflect(item.getId());
    }

    /**
     * Wields gear from the players inventory using reflection to make the menu invocations.
     * @param name The name of the item to equip.
     * @return True when the wield operation was successful and false otherwise
     */
    public boolean wieldReflect(String name) {
        List<Integer> ids = new ArrayList<>();
        for (InventoryItem item : inventory) {
            if (name.equalsIgnoreCase(item.getName())) {
                ids.add(item.getId());
            }
        }

        if (ids.isEmpty()) {
            return true;
        }

        int[] idsArray = ids.stream().mapToInt(i -> i).toArray();
        return wieldReflect(idsArray);
    }

    /**
     * Wields gear from the players inventory using reflection to make the menu invocations.
     * @param id The id of the item to equip.
     * @return True when the wield operation was successful and false otherwise
     */
    public boolean wieldReflect(int id) {
        int[] ids = new int[]{id};
        return wieldReflect(ids);
    }

    /**
     * Wields gear from the players inventory using reflection to make the menu invocations.
     * @param itemIds int[] item ids to equip.
     * @return True when the wield operation was successful and false otherwise
     */
    public boolean wieldReflect(int[] itemIds) {
        if (itemIds == null) {
            return true;
        }

        Widget inventoryWidget = context.getClient().getWidget(InterfaceID.Inventory.ITEMS);
        if (inventoryWidget == null) {
            return true;
        }

        Widget[] itemWidgets = inventoryWidget.getChildren();
        if (itemWidgets == null) {
            return true;
        }

        for (Widget itemWidget : itemWidgets) {
            int slot = itemWidget.getIndex();
            String[] menuActions = itemWidget.getActions();
            if (menuActions == null) {
                continue;
            }

            List<String> menuEntries = Arrays.asList(menuActions);
            boolean canWield = menuEntries.contains("Wield");
            boolean canWear = menuEntries.contains("Wear");
            boolean canEquip = menuEntries.contains("Equip");

            // Dynamically gets the index of the Wear or Wield action for the invoke actions.
            // You add 1 to the index of the actions because the returned index is always 1 less than the required action.
            int index = 1;

            if (canWield) {
                index += menuEntries.lastIndexOf("Wield");
            } else if (canWear) {
                index += menuEntries.lastIndexOf("Wear");
            } else if (canEquip) {
                index += menuEntries.lastIndexOf("Equip");
            } else {
                continue;
            }

            for (int itemId : itemIds) {
                if (itemWidget.getItemId() == itemId) {
                    reflectionService.invokeMenuAction(slot, InterfaceID.Inventory.ITEMS, MenuAction.CC_OP.getId(), index, itemId);
                }
            }
        }

        return true;
    }

    /**
     * Removes an item in an inventory slot.
     * @param slot The inventory slot with the item to remove.
     * @return True if the item was un-equipped successfully and false otherwise
     */
    public boolean remove(EquipmentInventorySlot slot) {
        return context.runOnClientThreadOptional(() -> {
            Widget widget = client.getWidget(WidgetInfo.EQUIPMENT.getGroupId(), equipmentSlotWidgetMapping.get(slot.getSlotIdx()));
            if(widget == null) {
                log.warn("Could not find item in slot: {}, index: {}", slot.name(), slot.getSlotIdx());
                return false;
            }

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(widget, "Remove");
            return true;
        }).orElse(false);
    }

    /**
     * Removes an item from your equipment given the item id.
     * @param itemId The id of the item to remove.
     * @return True if the item was un-equipped successfully and false otherwise
     */
    public boolean remove(int itemId) {
        return context.runOnClientThreadOptional(() -> {
            EquipmentItemWidget widget = Equipment.search()
                    .withId(itemId)
                    .first()
                    .orElse(null);

            if(widget == null) {
                log.warn("Could not find item with id: {}", itemId);
                return false;
            }

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(client.getWidget(widget.getId()), "Remove");
            return true;
        }).orElse(false);
    }

    /**
     * Removes the first item with a given name from your equipment.
     * @param name The name of the item to remove.
     * @return True if the item was un-equipped successfully and false otherwise
     */
    public boolean remove(String name) {
        return context.runOnClientThreadOptional(() -> {
            EquipmentItemWidget widget = Equipment.search()
                    .withName(name)
                    .first()
                    .orElse(null);

            if(widget == null) {
                log.warn("Could not find item with name: {}", name);
                return false;
            }

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(client.getWidget(widget.getId()), "Remove");
            return true;
        }).orElse(false);
    }

    /**
     * Removes an inventory item from your equipment.
     * @param item The item to remove.
     * @return True if the item was un-equipped successfully and false otherwise
     */
    public boolean remove(InventoryItem item) {
        return remove(item.getId());
    }

    /**
     * Checks if the player is wearing an item by id.
     * @param id The id of the item to check.
     * @return True if the player is wearing the item, false otherwise.
     */
    public boolean isWearing(int id) {
        return this.equipment
                .stream()
                .filter(i -> id == i.getId())
                .max(Comparator.comparing(InventoryItem::getName))
                .isPresent();
    }

    /**
     * Checks if the player is wearing an item by name.
     * @param name The name of the item to check.
     * @return True if the player is wearing the item, false otherwise.
     */
    public boolean isWearing(String name) {
        return this.equipment.stream()
                .filter(i -> name.equalsIgnoreCase(i.getName()))
                .max(Comparator.comparing(InventoryItem::getName))
                .isPresent();
    }

    /**
     * Gets the item equipped in the specified equipment slot.
     * @param slot The equipment slot to check.
     * @return The Item equipped in the specified slot, or null if the slot is empty or the equipment container is not available.
     */
    public Item getEquipmentInSlot(EquipmentInventorySlot slot) {
        ItemContainer equipment = client.getItemContainer(94);
        if (equipment == null) {
            return null;
        }

        return equipment.getItem(slot.getSlotIdx());
    }
}
