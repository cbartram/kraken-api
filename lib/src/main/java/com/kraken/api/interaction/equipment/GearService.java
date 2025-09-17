package com.kraken.api.interaction.equipment;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.inventory.InventoryItem;
import com.kraken.api.model.NewMenuEntry;
import com.kraken.api.util.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.EquipmentInventorySlot;
import net.runelite.api.Item;
import net.runelite.api.ItemContainer;
import net.runelite.api.MenuAction;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;


@Slf4j
@Singleton
public class GearService extends AbstractService {

    @Inject
    private ReflectionService reflectionService;

    @Getter
    private final List<InventoryItem> inventory = new ArrayList<>();

    @Getter
    private final List<InventoryItem> equipment = new ArrayList<>();

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

    public boolean isWearing(Integer id) {
        return this.equipment
                .stream()
                .filter(i -> id == i.getId())
                .max(Comparator.comparing(InventoryItem::getName))
                .isPresent();
    }

    public boolean isWearing(String name) {
        return this.equipment.stream()
                .filter(i -> name.equalsIgnoreCase(i.getName()))
                .max(Comparator.comparing(InventoryItem::getName))
                .isPresent();
    }

    public Item getEquipmentInSlot(EquipmentInventorySlot slot) {
        ItemContainer equipment = client.getItemContainer(94);
        if (equipment == null) {
            return null;
        }

        return equipment.getItem(slot.getSlotIdx());
    }

    public boolean hasItem(int id) {
        return this.inventory.stream().anyMatch(i -> i.getId() == id);
    }

    public boolean hasItem(String itemName) {
        return this.inventory.stream().anyMatch(i -> i.getName().equalsIgnoreCase(itemName));
    }

    public InventoryItem getItem(int id) {
        return this.inventory.stream()
                .filter(i -> i.getId() == id)
                .findFirst()
                .orElse(null);
    }

    public InventoryItem getItem(String itemName) {
        return this.inventory.stream()
                .filter(i -> i.getName().equalsIgnoreCase(itemName))
                .findFirst()
                .orElse(null);
    }
}
