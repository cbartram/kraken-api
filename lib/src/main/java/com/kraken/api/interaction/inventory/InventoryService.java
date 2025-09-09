package com.kraken.api.interaction.inventory;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.kraken.api.core.AbstractService;
import com.kraken.api.core.SleepService;
import com.kraken.api.interaction.bank.BankService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.ui.InterfaceTab;
import com.kraken.api.interaction.ui.TabService;
import com.kraken.api.interaction.widget.WidgetService;
import com.kraken.api.model.InventoryItem;
import com.kraken.api.model.NewMenuEntry;
import com.kraken.api.util.RandomUtils;
import com.kraken.api.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.ItemContainerChanged;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
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
    private final List<InventoryItem> inventoryItems = new ArrayList<>();
    private final List<InventoryItem> prevInventoryItems = new ArrayList<>();

    // Store the raw container for diff comparisons
    private ItemContainer lastProcessedContainer = null;

    @Inject
    private SleepService sleepService;

    @Inject
    private WidgetService widgetService;
    
    @Inject
    private TabService tabService;
    
    @Inject
    private BankService bankService;

    @Inject
    private ReflectionService reflectionService;

    @Subscribe
    private void onGameStateChanged(GameStateChanged event) {
        // This ensures that the inventory is populated with the latest data as soon as the user logs in. Users can stop/start
        // their plugins as often as they want and the Context (accessible through super class) will continually register/unregister
        // the necessary classes from the eventbus to keep the inventory in sync.
        if(event.getGameState() == GameState.LOGGED_IN) {
            Optional<ItemContainer> container = context.runOnClientThreadOptional(() -> client.getItemContainer(InventoryID.INV));
            container.ifPresent(this::refresh);
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
     * Refreshes the inventory with a new item container.
     * @param itemContainer The item container to refresh with.
     */
    private void refresh(ItemContainer itemContainer) {
        if(!prevInventoryItems.isEmpty()) {
            prevInventoryItems.clear();
        }

        prevInventoryItems.addAll(inventoryItems);
        inventoryItems.clear();

        for (int i = 0; i < itemContainer.getItems().length; i++) {
            final Item item = itemContainer.getItems()[i];
            if (item.getId() == -1) continue;
            final ItemComposition itemComposition = client.getItemDefinition(item.getId());
            inventoryItems.add(new InventoryItem(item, itemComposition, i, context));
        }

        lastProcessedContainer = itemContainer;
    }

    private List<InventoryItem> toInventoryItemModel(Item[] items) {
        List<InventoryItem> newItems = new ArrayList<>();
        for (int i = 0; i < items.length; i++) {
            final Item item = items[i];
            if (item.getId() == -1) continue;
            final ItemComposition itemComposition = client.getItemDefinition(item.getId());
            newItems.add(new InventoryItem(item, itemComposition, i, context));
        }
        return newItems;
    }

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

    public InventoryChanged diff(ItemContainer itemContainer) {
        if(itemContainer.getId() != InventoryID.INV) {
            return new InventoryChanged(null, -1, InventoryUpdateType.NONE);
        }

        // If this is the same container we just processed, use our stored prev items
        List<InventoryItem> comparisonPrevItems;
        if (lastProcessedContainer == itemContainer) {
            comparisonPrevItems = new ArrayList<>(prevInventoryItems);
        } else {
            // This is a different container, so current items become previous
            comparisonPrevItems = new ArrayList<>(inventoryItems);
        }

        List<InventoryItem> currentItems = toInventoryItemModel(itemContainer.getItems());

        // Find changes
        for(int i = 0; i < Math.max(currentItems.size(), comparisonPrevItems.size()); i++) {
            InventoryItem currentItem = i < currentItems.size() ? currentItems.get(i) : null;
            InventoryItem previousItem = i < comparisonPrevItems.size() ? comparisonPrevItems.get(i) : null;

            if(!inventoryItemsEqual(currentItem, previousItem)) {
                return handleItemChange(i, previousItem, currentItem);
            }
        }

        return new InventoryChanged(null, -1, InventoryUpdateType.NONE);
    }

    private boolean inventoryItemsEqual(InventoryItem item1, InventoryItem item2) {
        if(item1 == null && item2 == null) {
            return true;
        }
        if(item1 == null || item2 == null) {
            return false;
        }
        return item1.getId() == item2.getId() && item1.getQuantity() == item2.getQuantity();
    }

    private InventoryChanged handleItemChange(int slot, InventoryItem previousItem, InventoryItem currentItem) {
        if(previousItem == null && currentItem != null) {
            // Item was added
            return new InventoryChanged(currentItem, slot, InventoryUpdateType.ADDED);
        } else if(previousItem != null && currentItem == null) {
            // Item was removed
            return new InventoryChanged(previousItem, slot, InventoryUpdateType.REMOVED);
        } else if(previousItem != null && currentItem != null) {
            if(previousItem.getId() != currentItem.getId()) {
                // Different item in same slot
                return new InventoryChanged(currentItem, slot, InventoryUpdateType.MOVED);
            } else if(previousItem.getQuantity() != currentItem.getQuantity()) {
                // Same item, different quantity
                return new InventoryChanged(currentItem, slot, InventoryUpdateType.QUANTITY);
            }
        }

        return new InventoryChanged(null, -1, InventoryUpdateType.NONE);
    }

    /**
     * Gets all the items in the inventory.
     *
     * @return A list of all items in the inventory.
     */
    public List<InventoryItem> all() {
        return inventoryItems;
    }

    /**
     * A list of all the items that meet a specified filter criteria.
     *
     * @param filter The filter to apply when selecting items.
     *
     * @return A list of items that match the filter.
     */
    public List<InventoryItem> all(Predicate<InventoryItem> filter) {
        return inventoryItems.stream().filter(filter).collect(Collectors.toList());
    }


    /**
     * Returns true if the inventory contains an item with the specified ID.
     * @param id The ID of the item to check for.
     */
    public boolean hasItem(int id) {
        return inventoryItems.stream().anyMatch(item -> item.getId() == id);
    }

    /**
     * Returns true if the inventory contains an item with the specified ID.
     * @param item The InventoryItem to check for.
     */
    public boolean hasItem(InventoryItem item) {
        return inventoryItems.stream().anyMatch(i -> i.getId() == item.getId());
    }

    /**
     * Returns true if the inventory is empty and false otherwise.
     * @return
     */
    public boolean isEmpty() {
        return inventoryItems.isEmpty();
    }

    /**
     * Returns true if the inventory is full and false otherwise.
     * @return
     */
    public boolean isFull() {
        return inventoryItems.size() >= 28;
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
     * Combines two items in the inventory using InventoryItem objects, ensuring distinct items are used.
     *
     * @param primary   The primary item.
     * @param secondary The secondary item.
     *
     * @return True if the combine operation was successful, false otherwise.
     */
    public boolean combine(InventoryItem primary, InventoryItem secondary, boolean shortSleep) {
        // Get the primary item
        InventoryItem primaryItem = get(item -> item.getId() == primary.getId());
        if (primaryItem == null) {
            log.error("Primary item not found in the inventory.");
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
        InventoryItem secondaryItem = get(item -> item.getId() == secondary.getId() && item.getSlot() != primaryItem.getSlot());
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
     * @param rs2Item The item to use.
     *
     * @return True if the item is successfully used, false otherwise.
     */
    public boolean use(InventoryItem rs2Item) {
        if (rs2Item == null) return false;
        return interact(rs2Item, "Use");
    }

    /**
     * Interacts with a given item in the inventory using the specified action.
     * If the item has an invalid slot value, it will find the slot based on the item ID.
     *
     * @param item   The item to interact with.
     * @param action The action to perform on the item.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(InventoryItem item, String action) {
        if (item == null) return false;
        invokeMenu(item, action);
        return true;
    }

    /**
     * Interacts with an item in the inventory using reflection. If the item has an invalid slot value, it will find the slot based on the item ID.
     * @param item   The item to interact with.
     * @param action The action to perform on the item.
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interactReflect(InventoryItem item, String action) {
        return interactReflect(item, action, -1);
    }

    /**
     * Interacts with an item in the inventory using reflection. If the item has an invalid slot value, it will find the slot based on the item ID.
     * @param item   The item to interact with.
     * @param action The action to perform on the item.
     * @param providedIdentifier An optional identifier to provide for the interaction. Defaults to -1
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interactReflect(InventoryItem item, String action, int providedIdentifier) {
        int identifier = -1;
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
     * Interacts with an item with the specified ID in the inventory using the specified action.
     *
     * @param id     The ID of the item to interact with.
     * @param action The action to perform on the item.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(int id, String action) {
        return interact(get(id), action);
    }
    /**
     * Interacts with an item with the specified ID in the inventory using the specified action.
     *
     * @param id     The ID of the item to interact with.
     * @param action The action to perform on the item.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(int id, String action, int identifier) {
        final InventoryItem item = get(id);
        if (item == null) return false;
        invokeMenu(item, action, identifier);
        return true;
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
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(String name, String action, boolean exact) {
        return interact(get(name, exact),action);
    }

    /**
     * Interacts with an item in the inventory using the first available action based on the specified filter.
     *
     * @param filter The filter to apply.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(Predicate<InventoryItem> filter) {
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
    public boolean interact(Predicate<InventoryItem> filter, String action) {
        return interact(get(filter),action);
    }

    /**
     * Interacts with a given item in the inventory using the first available action.
     * If the item has an invalid slot value, it will find the slot based on the item ID.
     *
     * @param item The item to interact with.
     *
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(InventoryItem item) {
        return interact(item, "");
    }

    /**
     * Gets a random item in the inventory which matches the specified filter criteria.
     * @param predicate The filter to apply
     * @return The item that matches the filter criteria or null if not found.
     */
    public InventoryItem getRandom(Predicate<InventoryItem> predicate) {
        List<InventoryItem> items = inventoryItems.stream()
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
    public InventoryItem getRandom(int... ids) {
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
    public InventoryItem getFirst(Predicate<InventoryItem> predicate) {
        return get(predicate);
    }

    /**
     * Gets the item in the inventory that matches the specified filter criteria.
     *
     * @param predicate The filter to apply.
     *
     * @return The item that matches the filter criteria, or null if not found.
     */
    public InventoryItem get(Predicate<InventoryItem> predicate) {
        return inventoryItems.stream().filter(predicate).findFirst().orElse(null);
    }

    /**
     * Gets the first item in the inventory that matches one of the given IDs.
     *
     * @param ids The IDs to match.
     *
     * @return The first item that matches one of the IDs, or null if not found.
     */
    public InventoryItem get(int... ids) {
        return get(item -> Arrays.stream(ids).anyMatch(id -> id == item.getId()));
    }

    /**
     * Gets the item in the inventory with one of the specified names.
     *
     * @param names to match.
     *
     * @return The item with one of the specified names, or null if not found.
     */
    public InventoryItem get(String... names) {
        return get(names, false);
    }

    /**
     * Gets the item in the inventory with the specified name.
     * this method ignores casing
     *
     * @param name The name to match.
     *
     * @return The item with the specified name, or null if not found.
     */
    public InventoryItem get(String name, boolean exact) {
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
    public InventoryItem get(String[] names, boolean exact) {
        return get(names, false, exact);
    }

    /**
     * Gets the item in the inventory with the specified name.
     * this method ignores casing
     *
     * @param name The name to match.
     *
     * @return The item with the specified name, or null if not found.
     */
    public InventoryItem get(String name, boolean stackable, boolean exact) {
        return get(new String[] {name}, stackable, exact);
    }

    /**
     * Gets the item in the inventory with the specified name.
     * this method ignores casing
     *
     * @param names to match.
     *
     * @return The item with the specified name, or null if not found.
     */
    public InventoryItem get(String[] names, boolean stackable, boolean exact) {
        Predicate<InventoryItem> filter = InventoryItem.matches(exact, names);
        if (stackable) filter = filter.and(InventoryItem::isStackable);
        return exact ? inventoryItems.stream().filter(filter).findFirst().orElse(null) :
                inventoryItems.stream().filter(filter).min(Comparator.comparingInt(item -> item.getName().length())).orElse(null);
    }


    private boolean drop(InventoryItem item) {
        if (item == null) return false;

        invokeMenu(item, "Drop");
        return true;
    }

    /**
     * Drops the item from the inventory that matches the specified filter.
     *
     * @param predicate The filter to identify the item to drop.
     *
     * @return True if the item was successfully dropped, false otherwise.
     */
    public boolean drop(Predicate<InventoryItem> predicate) {
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
     *
     * @param name
     * @return
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
    public boolean dropAll(Predicate<InventoryItem> predicate) {
        inventoryItems.stream().filter(predicate).forEachOrdered(item -> {
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
    public Rectangle itemBounds(InventoryItem item) {
        Widget inventory = getInventory();
        if (inventory == null) return null;

        return Arrays.stream(inventory.getDynamicChildren())
                .filter(widget -> widget.getIndex() == item.getSlot())
                .findFirst().map(Widget::getBounds).orElse(null);
    }


    /**
     * Method executes menu actions
     *
     * @param item Current item to interact with
     * @param action  Action used on the item
     */
    private void invokeMenu(InventoryItem item, String action) {
        invokeMenu(item, action, -1);
    }

    /**
     * Opens the inventory.
     *
     * @return True if the inventory is successfully opened, false otherwise.
     */
    public boolean open() {
        return tabService.switchTo(InterfaceTab.INVENTORY);
    }

    /**
     * Executes menu actions with a provided identifier.
     * If the provided identifier is -1, the old logic is used to determine the identifier.
     *
     * @param item            The current item to interact with.
     * @param action             The action to be used on the item.
     * @param providedIdentifier The identifier to use; if -1, compute using the old logic.
     */
    private void invokeMenu(InventoryItem item, String action, int providedIdentifier) {
        if (item == null) return;
        open();
        int param0;
        int param1;
        int identifier = -1;
        MenuAction menuAction = MenuAction.CC_OP;
        Widget[] inventoryWidgets;
        param0 = item.getSlot();
        boolean isDepositBoxOpen = !context.runOnClientThreadOptional(() -> widgetService.getWidget(12582935) == null
                || widgetService.getWidget(12582935).isHidden()).orElse(false);

        Widget widget;

        if (bankService.isOpen()) {
            param1 = ComponentID.BANK_INVENTORY_ITEM_CONTAINER;
            widget = widgetService.getWidget(param1);
        } else if (isDepositBoxOpen) {
            param1 = ComponentID.DEPOSIT_BOX_INVENTORY_ITEM_CONTAINER;
            widget = widgetService.getWidget(param1);
        } else {
            param1 = ComponentID.INVENTORY_CONTAINER;
            widget = widgetService.getWidget(param1);
        }

        if (widget != null && widget.getChildren() != null) {
            inventoryWidgets = widget.getChildren();
        } else {
            inventoryWidgets = null;
        }

        if (inventoryWidgets == null) return;

        if (!action.isEmpty()) {
            var itemWidget = Arrays.stream(inventoryWidgets).filter(x -> x != null && x.getIndex() == item.getSlot()).findFirst().orElseGet(null);

            String[] actions = itemWidget != null && itemWidget.getActions() != null ?
                    itemWidget.getActions() :
                    item.getInventoryActions();

            identifier = providedIdentifier == -1 ? indexOfIgnoreCase(stripColTags(actions), action) + 1 : providedIdentifier;
        }


        if (client.isWidgetSelected()) {
            menuAction = MenuAction.WIDGET_TARGET_ON_WIDGET;
        } else if (action.equalsIgnoreCase("use")) {
            menuAction = MenuAction.WIDGET_TARGET;
        } else if (action.equalsIgnoreCase("cast")) {
            menuAction = MenuAction.WIDGET_TARGET_ON_WIDGET;
        }

        context.doInvoke(new NewMenuEntry(action, param0, param1, menuAction.getId(), identifier, item.getId(), item.getName()), (itemBounds(item) == null) ? new Rectangle(1, 1) : itemBounds(item));

        if (action.equalsIgnoreCase("destroy")) {
            sleepService.sleepUntil(() -> widgetService.isWidgetVisible(584, 0));
            widgetService.clickWidget(widgetService.getWidget(584, 1).getId());
        }
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
