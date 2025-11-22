package com.kraken.api.interaction.container.inventory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.core.SleepService;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.ui.InterfaceTab;
import com.kraken.api.interaction.ui.TabService;
import com.kraken.api.interaction.ui.UIService;
import com.kraken.api.util.RandomUtils;
import com.kraken.api.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.eventbus.Subscribe;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.kraken.api.util.StringUtils.stripColTags;

@Slf4j
@Singleton
public class InventoryService extends AbstractService {
    private final List<ContainerItem> containerItems = new ArrayList<>();

    @Inject
    private SleepService sleepService;
    
    @Inject
    private TabService tabService;

    @Inject
    private ReflectionService reflectionService;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private WidgetPackets widgetPackets;

    @Inject
    private UIService uiService;

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        // This ensures that the inventory is populated with the latest data as soon as the user logs in. Users can stop/start
        // their plugins as often as they want and the Context (accessible through super class) will continually register/unregister
        // the necessary classes from the eventbus to keep the inventory in sync.
        if(event.getGameState() == GameState.LOGGED_IN) {
            ItemContainer container = context.runOnClientThread(() -> client.getItemContainer(InventoryID.INV));
            if(container == null) return;
            refresh(container);
        }
    }

    @Subscribe
    public void onWidgetLoaded(WidgetLoaded event) {
        // When the bank interface loads, the "Bank Inventory" widgets become available.
        // We need to refresh the inventory so our ContainerItems can link to these new widgets.
        if (event.getGroupId() == InterfaceID.BANKMAIN) {
            context.runOnClientThread(() -> {
                ItemContainer container = client.getItemContainer(InventoryID.INV);
                if (container != null) {
                    refresh(container);
                }
            });
        }
    }

    /**
     * Refreshes the internal inventory list with new/removed items based on the RuneLite event. This is designed
     * to be called within the @Subscribed method for item container changed.
     * @param event Item container change event indicating what changed in the inventory.
     */
    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
      if (event.getContainerId() == InventoryID.INV) {
          assert client.isClientThread();
          final ItemContainer itemContainer = event.getItemContainer();
          refresh(itemContainer);
      }
    }

    /**
     * Refreshes the inventory with a new item container. This should never be called directly and it can cause
     * bad state if the server rejects a change for an item container. RuneLite will deliver the item container
     * change event to let us re-build the inventory correctly.
     * @param itemContainer The item container to refresh with.
     */
    private void refresh(ItemContainer itemContainer) {
        containerItems.clear();

        Widget inventory = client.getWidget(WidgetInfo.INVENTORY);
        // The inventory widget while the bank is open
        Widget bankInventory = client.getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);

        if (inventory == null) {
            log.info("Could not get inventory widget, refresh failed");
            return;
        }

        Widget[] inventoryWidgets = inventory.getDynamicChildren();
        Widget[] bankWidgets = (bankInventory != null) ? bankInventory.getDynamicChildren() : null;

        for (int i = 0; i < itemContainer.getItems().length; i++) {
            final Item item = itemContainer.getItems()[i];

            // Skip empty slots
            if (item.getId() == -1 || item.getId() == 6512) continue;

            final ItemComposition itemComposition = context.runOnClientThreadOptional(() ->
                    client.getItemDefinition(item.getId())
            ).orElse(null);

            if (itemComposition == null) continue;

            Widget widget = null;
            if (i < inventoryWidgets.length) {
                widget = inventoryWidgets[i];
            }

            Widget bankInventoryWidget = null;
            if (bankWidgets != null && i < bankWidgets.length) {
                bankInventoryWidget = bankWidgets[i];
            }
            containerItems.add(new ContainerItem(item, itemComposition, i, context, widget, bankInventoryWidget));
        }
    }

    /**
     * Returns the Widget for the players current inventory
     * @return Widget the current players inventory
     */
    public Widget getInventory() {
        final int BANK_PIN_INVENTORY_ITEM_CONTAINER = 17563648;
        final int SHOP_INVENTORY_ITEM_CONTAINER = 19726336;
        return context.runOnClientThreadOptional(() -> {
            for (int id : new int[] {9764864, 983043,
                    BANK_PIN_INVENTORY_ITEM_CONTAINER, SHOP_INVENTORY_ITEM_CONTAINER,
                    30605312,
                    12582935}) {
                final Widget widget = client.getWidget(id);
                if (widget != null && widget.getDynamicChildren() != null && !widget.isHidden()) return widget;
            }
            return null;
        }).orElse(null);
    }

    /**
     * Gets all the items in the inventory.
     *
     * @return A list of all items in the inventory.
     */
    public List<ContainerItem> all() {
        return containerItems;
    }

    /**
     * A list of all the items that meet a specified filter criteria.
     *
     * @param filter The filter to apply when selecting items.
     *
     * @return A list of items that match the filter.
     */
    public List<ContainerItem> all(Predicate<ContainerItem> filter) {
        return containerItems.stream().filter(filter).collect(Collectors.toList());
    }


    /**
     * Returns true if the inventory contains an item with the specified ID.
     * @param id The ID of the item to check for.
     * @return True when the users inventory contains the item and false otherwise
     */
    public boolean hasItem(int id) {
        return containerItems.stream().anyMatch((item) -> context.runOnClientThreadOptional(() -> item.getId() == id).orElse(false));
    }

    /**
     * Returns true if the inventory contains an item with the specified ID.
     * @param item The ContainerItem to check for.
     * @return True when the users inventory contains the item and false otherwise
     */
    public boolean hasItem(ContainerItem item) {
        return hasItem(item.getId());
    }

    /**
     * Returns true if the inventory contains an item with the specified name.
     * @param name String name of the item to check for.
     * @return True when the users inventory contains the item and false otherwise
     */
    public boolean hasItem(String name) {
        return containerItems.stream().anyMatch((item) -> context.runOnClientThreadOptional(() -> item.getName().equalsIgnoreCase(name)).orElse(false));
    }

    /**
     * Returns true if the inventory is empty and false otherwise.
     * @return true if the inventory is empty and false otherwise.
     */
    public boolean isEmpty() {
        return containerItems.isEmpty();
    }

    /**
     * Returns true if the inventory is full and false otherwise.
     * @return True if the inventory is full and false otherwise.
     */
    public boolean isFull() {
        return containerItems.size() >= 28;
    }

    /**
     * Returns the count of items in the inventory
     * @return The count of items in the players inventory
     */
    public int count() {
        return containerItems.size();
    }

    /**
     * Returns the free space in a users inventory.
     * @return The amount of free space available in the players inventory
     */
    public int freeSpace() {
        return 28 - containerItems.size();
    }

    /**
     * Combines two items in the inventory by their IDs, ensuring distinct items are used.
     *
     * @param primaryItemId   The ID of the primary item.
     * @param secondaryItemId The ID of the secondary item.
     *
     * @return True if the combine operation was successful, false otherwise.
     */
    public boolean combine(int primaryItemId, int secondaryItemId) {
        return combine(get(primaryItemId), get(secondaryItemId), false);
    }

    /**
     * Combines two items in the inventory by their IDs, ensuring distinct items are used.
     *
     * @param primaryItemId   The ID of the primary item.
     * @param secondaryItemId The ID of the secondary item.
     * @param shortSleep True if there should be a short sleep in between the combine operation
     *
     * @return True if the combine operation was successful, false otherwise.
     */
    public boolean combine(int primaryItemId, int secondaryItemId, boolean shortSleep) {
        return combine(get(primaryItemId), get(secondaryItemId), shortSleep);
    }

    /**
     * Combines two items in the inventory by their names, ensuring distinct items are used.
     *
     * @param primaryItemName   The name of the primary item.
     * @param secondaryItemName The name of the secondary item.
     *
     * @return True if the combine operation was successful, false otherwise.
     */
    public boolean combine(String primaryItemName, String secondaryItemName) {
        return combine(get(primaryItemName, false), get(secondaryItemName, false), false);
    }

    /**
     * Combines two items in the inventory using ContainerItem objects, ensuring distinct items are used.
     *
     * @param primary   The primary item.
     * @param secondary The secondary item.
     * @param shortSleep True if there should be a short sleep operation between the combination
     *
     * @return True if the combine operation was successful, false otherwise.
     */
    public boolean combine(ContainerItem primary, ContainerItem secondary, boolean shortSleep) {
        // Get the primary item
        ContainerItem primaryItem = get(item -> item.getId() == primary.getId());
        if (primaryItem == null) {
            log.error("Primary item not found in the inventory, can't combine");
            return false;
        }

        // Select the primary item
        if (!use(primaryItem)) return false;

        if(shortSleep) {
            sleepService.sleep(20, 50);
        } else {
            sleepService.sleep(100, 175);
        }
        // Get a secondary item that isn't the same as the primary
        ContainerItem secondaryItem = get(item -> item.getId() == secondary.getId() && item.getSlot() != primaryItem.getSlot());
        if (secondaryItem == null) {
            log.error("No valid secondary item found to combine with.");
            return false;
        }

        // Interact with the secondary item
        return use(secondaryItem);
    }

    /**
     * Uses the item with the specified ID in the inventory.
     *
     * @param id The ID of the item to use.
     *
     * @return True if the item is successfully used, false otherwise.
     */
    public boolean use(int id) {
        return interact(id, "Use");
    }

    /**
     * Uses the item with the specified name in the inventory.
     *
     * @param name The name of the item to use.
     *
     * @return True if the item is successfully used, false otherwise.
     */
    public boolean use(String name) {
        return interact(name, "Use");
    }

    /**
     * Uses the given item in the inventory.
     *
     * @param item The item to use.
     *
     * @return True if the item is successfully used, false otherwise.
     */
    public boolean use(ContainerItem item) {
        if (item == null) return false;
        return interact(item, "Use");
    }


    /**
     * Interacts with an item in the inventory using reflection. If the item has an invalid slot value, it will find the slot based on the item ID.
     * @param item   The item to interact with.
     * @param action The action to perform on the item.
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interactReflect(ContainerItem item, String action) {
        return interactReflect(item, action, -1);
    }

    /**
     * Interacts with an item in the inventory using reflection. If the item has an invalid slot value, it will find the slot based on the item ID.
     * @param item   The item to interact with.
     * @param action The action to perform on the item.
     * @param providedIdentifier An optional identifier to provide for the interaction. Defaults to -1
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interactReflect(ContainerItem item, String action, int providedIdentifier) {
        if(!context.isHooksLoaded()) return false;
        int identifier;
        if(item == null) return false;
        Widget inventoryWidget = context.getClient().getWidget(InterfaceID.Inventory.ITEMS);
        if (inventoryWidget == null) {
            return true;
        }

        // Children of the inventory are the actual items in each of the 28 slots
        Widget[] itemWidgets = inventoryWidget.getChildren();
        if (itemWidgets == null) {
            return true;
        }

        if (!action.isEmpty()) {
            // First find the inventory widget which matches the passed item.
            Widget itemWidget = Arrays.stream(itemWidgets).filter(i -> i != null && i.getIndex() == item.getSlot()).findFirst().orElseGet(null);

            if(itemWidget == null) {
                return false;
            }

            // Get the actions for that item i.e. "Drink", "Wield", "Wear", "Drop"
            String[] actions = itemWidget.getActions() != null ? itemWidget.getActions() : item.getInventoryActions();

            identifier = providedIdentifier == -1 ? indexOfIgnoreCase(stripColTags(actions), action) + 1 : providedIdentifier;


            reflectionService.invokeMenuAction(itemWidget.getIndex(), InterfaceID.Inventory.ITEMS, MenuAction.CC_OP.getId(), identifier, itemWidget.getItemId());
        }
        return true;
    }

    /**
     * Returns true if the item with the specified ID has the specified action. i.e "Swordfish" will have the action "Eat" but not "Drink"
     * @param itemId The id of the item to check
     * @param action The action to check for
     * @return True if the item has the action and false otherwise
     */
    public boolean itemHasAction(int itemId, String action) {
        return Arrays.stream(client.getItemDefinition(itemId).getInventoryActions()).anyMatch(a -> a != null && a.equalsIgnoreCase(action));
    }

    /**
     * Interacts with an item with the specified ID in the inventory using the first available action.
     *
     * @param id The ID of the item to interact with.
     * @param action The action to take. i.e. "Eat" or "Use"
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(int id, String action) {
        if(!context.isPacketsLoaded()) return false;
        String parsedAction = (action == null || action.trim().isEmpty())
                ? Arrays.stream(client.getItemDefinition(id).getInventoryActions())
                .findFirst().orElse(null)
                : action;

        return context.runOnClientThreadOptional(() -> {
            ContainerItem item = containerItems.stream().filter(i -> i.getId() == id).findFirst().orElse(null);
            if(item == null) return false;

            Widget w = item.getWidget();

            // This can happen if the user hasn't changed something in their inventory since logging in, since widgets
            // weren't loaded when refresh() was called.
            if(w == null) {
                Widget inven = client.getWidget(WidgetInfo.INVENTORY);
                Widget[] items = inven.getDynamicChildren();
                w = Arrays.stream(items)
                        .filter(Objects::nonNull)
                        .filter(wid -> wid.getItemId() != 6512 && wid.getItemId() != -1)
                        .filter(wid -> wid.getItemId() == item.getId())
                        .findFirst().orElse(null);
            }

            Point pt = uiService.getClickbox(item);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(w, parsedAction);
            return true;
        }).orElse(false);
    }

    /**
     * Interacts with an item with the specified ID in the inventory using the first available action.
     *
     * @param item The Inventory Item to interact with.
     * @param action The action to take. i.e. "Eat" or "Use"
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(ContainerItem item, String action) {
        if(!context.isPacketsLoaded()) return false;
        String parsedAction = (action == null || action.trim().isEmpty())
                ? Arrays.stream(item.getInventoryActions()).findFirst().orElse(action)
                : action;

        return context.runOnClientThreadOptional(() -> interact(item.getId(), parsedAction)).orElse(false);
    }

    /**
     * Interacts with an item with the specified ID in the inventory using the first available action.
     *
     * @param id The ID of the item to interact with.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(int id) {
        return interact(id, "");
    }

    /**
     * Interacts with an item with the specified name in the inventory using the first available action.
     *
     * @param name The name of the item to interact with.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(String name) {
        return interact(name, "", false);
    }

    /**
     * Interacts with an item with the specified name in the inventory using the specified action.
     *
     * @param name   The name of the item to interact with.
     * @param action The action to perform on the item.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(String name, String action) {
        return interact(name, action, false);
    }

    /**
     * Interacts with an item with the specified id(s) in the inventory using the specified action.
     *
     * @param ids  The ids of the item to interact with.
     * @param action The action to perform on the item.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(int[] ids, String action) {
        return Arrays.stream(ids).sequential().anyMatch(id -> interact(id, action));
    }

    /**
     * Interacts with an item with the specified name in the inventory using the specified action.
     *
     * @param names  The name of the item to interact with.
     * @param action The action to perform on the item.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(String[] names, String action) {
        return Arrays.stream(names).anyMatch(name -> interact(name, action));
    }

    /**
     * Interacts with an item with the specified name in the inventory using the specified action.
     *
     * @param name   The name of the item to interact with.
     * @param action The action to perform on the item.
     * @param exact when true
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(String name, String action, boolean exact) {
        return interact(get(name, exact), action);
    }

    /**
     * Interacts with an item in the inventory using the first available action based on the specified filter.
     *
     * @param filter The filter to apply.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(Predicate<ContainerItem> filter) {
        return interact(filter, "Use");
    }

    /**
     * Interacts with an item in the inventory using the specified action based on the specified filter.
     *
     * @param filter The filter to apply.
     * @param action The action to perform on the item.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(Predicate<ContainerItem> filter, String action) {
        return interact(get(filter), action);
    }

    /**
     * Interacts with a given item in the inventory using the first available action.
     * If the item has an invalid slot value, it will find the slot based on the item ID.
     *
     * @param item The item to interact with.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(ContainerItem item) {
        return interact(item, "");
    }

    /**
     * Gets a random item in the inventory which matches the specified filter criteria.
     * @param predicate The filter to apply
     * @return The item that matches the filter criteria or null if not found.
     */
    public ContainerItem getRandom(Predicate<ContainerItem> predicate) {
        List<ContainerItem> items = containerItems.stream()
                .filter(predicate)
                .collect(Collectors.toList());

        if (items.isEmpty()) {
            return null;
        }

        int randomIndex = RandomUtils.randomIntBetween(0, items.size() - 1);
        return items.get(randomIndex);
    }

    /**
     * Gets a random item in the inventory which matches the specified filter criteria.
     * @param ids The ids to search for
     * @return The item that matches the filter criteria or null if not found.
     */
    public ContainerItem getRandom(int... ids) {
        return getRandom(item -> Arrays.stream(ids).anyMatch(id -> id == item.getId()));
    }

    /**
     * Gets the item in the inventory that matches the specified filter criteria. This will return
     * the first item which matches. Use {findRandom} if you wish to find the item specified by the filter criteria
     * in a random slot.
     *
     * @param predicate The filter to apply.
     *
     * @return The item that matches the filter criteria, or null if not found.
     */
    public ContainerItem getFirst(Predicate<ContainerItem> predicate) {
        return get(predicate);
    }

    /**
     * Gets the item in the inventory that matches the specified filter criteria.
     *
     * @param predicate The filter to apply.
     *
     * @return The item that matches the filter criteria, or null if not found.
     */
    public ContainerItem get(Predicate<ContainerItem> predicate) {
        return containerItems.stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * Gets the first item in the inventory that matches one of the given IDs.
     *
     * @param ids The IDs to match.
     *
     * @return The first item that matches one of the IDs, or null if not found.
     */
    public ContainerItem get(int... ids) {
        return get(item -> Arrays.stream(ids).anyMatch(id -> id == context.runOnClientThreadOptional(item::getId).orElse(-1)));
    }

    /**
     * Gets the item in the inventory with one of the specified names.
     *
     * @param names to match.
     *
     * @return The item with one of the specified names, or null if not found.
     */
    public ContainerItem get(String... names) {
        return get(names, false);
    }

    /**
     * Gets the item in the inventory with the specified name.
     * this method ignores casing
     *
     * @param name The name to match.
     * @param exact When true only exact matches on the name will be included
     *
     * @return The item with the specified name, or null if not found.
     */
    public ContainerItem get(String name, boolean exact) {
        return get(name, false, exact);
    }

    /**
     * Gets the item in the inventory with one of the specified names.
     *
     * @param names The names to match.
     * @param exact true to match the exact name
     *
     * @return The item with one of the specified names, or null if not found.
     */
    public ContainerItem get(String[] names, boolean exact) {
        return get(names, false, exact);
    }

    /**
     * Gets the item in the inventory with the specified name.
     * this method ignores casing
     *
     * @param name The name to match.
     * @param stackable When true will return the item stack
     * @param exact when true only exact matches for the name will be accepted
     *
     * @return The item with the specified name, or null if not found.
     */
    public ContainerItem get(String name, boolean stackable, boolean exact) {
        return get(new String[] {name}, stackable, exact);
    }

    /**
     * Gets the item in the inventory with the specified name.
     * this method ignores casing
     *
     * @param names to match.
     * @param stackable True if the item is stackable
     * @param exact when true only exact matches for the name will be accepted
     *
     * @return The item with the specified name, or null if not found.
     */
    public ContainerItem get(String[] names, boolean stackable, boolean exact) {
        Predicate<ContainerItem> filter = ContainerItem.matches(exact, names);
        if (stackable) filter = filter.and(ContainerItem::isStackable);
        return exact ? containerItems.stream().filter(filter).findFirst().orElse(null) :
                containerItems.stream().filter(filter).min(Comparator.comparingInt(item -> item.getName().length())).orElse(null);
    }

    /**
     * Returns a list of Inventory Items which can be consumed for health.
     * @return List of Inventory items which are food.
     */
    public List<ContainerItem> getFood() {
        return new ArrayList<>(all(ContainerItem::isFood));
    }

    /**
     * Returns true when the player has edible hard food in their inventory and false otherwise.
     * @return boolean
     */
    public boolean hasFood() {
        return !new ArrayList<>(all(ContainerItem::isFood)).isEmpty();
    }

    /**
     * Drops the item from the inventory.
     *
     * @param item The item to drop
     * @return True if the item was successfully dropped, false otherwise.
     */
    private boolean drop(ContainerItem item) {
        if (item == null) return false;

        interact(item, "Drop");
        return true;
    }

    /**
     * Drops the item from the inventory that matches the specified filter.
     *
     * @param predicate The filter to identify the item to drop.
     *
     * @return True if the item was successfully dropped, false otherwise.
     */
    public boolean drop(Predicate<ContainerItem> predicate) {
        return drop(get(predicate));
    }

    /**
     * Drops the item with the specified ID from the inventory.
     *
     * @param id The ID of the item to drop.
     *
     * @return True if the item was successfully dropped, false otherwise.
     */
    public boolean drop(int id) {
        return drop(get(id));
    }

    /**
     * Drops the item with the specified name from the inventory.
     *
     * @param name The name of the item to drop.
     *
     * @return True if the item was successfully dropped, false otherwise.
     */
    public boolean drop(String name) {
        return drop(name, false);
    }

    /**
     * Drops an item given the items name
     * @param name The name of the item to drop
     * @param exact When true only the exact name will return an item to drop
     * @return True if the item was dropped successfully and false otherwise.
     */
    public boolean drop(String name, boolean exact) {
        return drop(get(name,exact));
    }

    /**
     * Drops all items in the inventory matching the specified filter.
     *
     * @param predicate The filter to apply.
     *
     * @return True if all matching items were successfully dropped, false otherwise.
     */
    public boolean dropAll(Predicate<ContainerItem> predicate) {
        containerItems.stream().filter(predicate).forEachOrdered(item -> {
            drop(item);
            sleepService.sleep(150, 300);
        });
        return true;
    }

    /**
     * Drops all items in the inventory that don't match the given IDs.
     *
     * @param ids The IDs to exclude.
     *
     * @return True if all non-matching items were successfully dropped, false otherwise.
     */
    public boolean dropAllExcept(int... ids) {
        return dropAll(x -> Arrays.stream(ids).noneMatch(id -> id == x.getId()));
    }

    /**
     * Gets the bounding rectangle for the slot of the specified item in the inventory.
     *
     * @param item The item to get the bounds for.
     *
     * @return The bounding rectangle for the item's slot, or null if the item is not found.
     */
    public Rectangle itemBounds(ContainerItem item) {
        Widget inventory = getInventory();
        if (inventory == null) return null;

        return Arrays.stream(inventory.getDynamicChildren())
                .filter(widget -> widget.getIndex() == item.getSlot())
                .findFirst().map(Widget::getBounds).orElse(null);
    }

    /**
     * Opens the inventory.
     *
     * @return True if the inventory is successfully opened, false otherwise.
     */
    public boolean open() {
        return tabService.switchTo(InterfaceTab.INVENTORY);
    }

    private static int indexOfIgnoreCase(String[] sourceList, String searchString) {
        if (sourceList == null || searchString == null) return -1;  // or throw an IllegalArgumentException

        int idx = StringUtils.getIndex(sourceList, searchString);
        if (idx != -1) return idx;

        if (searchString.equalsIgnoreCase("wield")) return StringUtils.getIndex(sourceList, "wear");
        else if (searchString.equalsIgnoreCase("wear")) return StringUtils.getIndex(sourceList, "wield");
        else return idx;  // return -1 if the string is not found
    }
}
