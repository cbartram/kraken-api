package com.kraken.api.interaction.bank;

import com.kraken.api.core.AbstractService;
import com.kraken.api.core.RandomService;
import com.kraken.api.core.SleepService;
import com.kraken.api.interaction.inventory.InventoryService;
import com.kraken.api.interaction.widget.WidgetService;
import com.kraken.api.model.InventoryItem;
import com.kraken.api.model.NewMenuEntry;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class BankService extends AbstractService {

    @Inject
    private WidgetService widgetService;

    @Inject
    private SleepService sleepService;

    @Inject
    private InventoryService inventoryService;

    private static final int BANK_INVENTORY_ITEM_CONTAINER = 983043;

    /**
     * Container describes from what interface the action happens
     * eg: withdraw means the container will be the bank container
     * eg: deposit means that the container will be the inventory container
     * and so on...
     */
    private static int container = -1;

    /**
     * Checks whether the bank interface is open.
     *
     * @return {@code true} if the bank interface is open, {@code false} otherwise.
     */
    public boolean isOpen() {
        return widgetService.hasWidgetText("Rearrange mode", 12, 18, false);
    }

    /**
     * Executes menu swapping for a specific item and entry index.
     *
     * @param identifier The index of the entry to swap.
     * @param item    The ItemWidget associated with the menu swap.
     */
    public void invokeMenu(final int identifier, InventoryItem item) {
        Rectangle itemBoundingBox = null;

        if (container == BANK_INVENTORY_ITEM_CONTAINER) {
            itemBoundingBox = inventoryService.itemBounds(item);
        }
        // Handles withdrawing at item from the bank
//        if (container == BANK_ITEM_CONTAINER) {
//            int itemTab = getItemTabForBankItem(item.getSlot());
//            if (!isTabOpen(itemTab))
//                openTab(itemTab);
//            scrollBankToSlot(item.getSlot());
//            itemBoundingBox = itemBounds(item);
//        }

        context.doInvoke(new NewMenuEntry(item.getSlot(), container, MenuAction.CC_OP.getId(), identifier, item.getId(), item.getName()), (itemBoundingBox == null) ? new Rectangle(1, 1) : itemBoundingBox);
    }

    /**
     * Closes the bank interface if it is open.
     *
     * @return true if the bank interface was open and successfully closed, true if already closed.
     */
    public boolean closeBank() {
        if (!isOpen()) return true;
        widgetService.clickWidget(786434, 11);
        return sleepService.sleepUntil(() -> !isOpen(), 5000);
    }

    /**
     * Deposits one item quickly into the bank by its ItemWidget.
     *
     * @param item The ItemWidget representing the item to deposit.
     */
    private boolean depositOne(InventoryItem item) {
        if (item == null) return false;
        if (!isOpen()) return false;
        if (!inventoryService.hasItem(item.getId())) return false;
        container = BANK_INVENTORY_ITEM_CONTAINER;

        if (context.getVarbitValue(VarbitID.BANK_QUANTITY_TYPE) == 0) {
            invokeMenu(2, item);
        } else {
            invokeMenu(3, item);
        }
        return true;
    }

    /**
     * Deposits one item quickly by its ID.
     *
     * @param id The ID of the item to deposit.
     */
    public boolean depositOne(int id) {
        return depositOne(inventoryService.get(id));
    }

    /**
     * Deposits one item quickly by its name with a partial name match.
     *
     * @param name The name of the item to deposit.
     */
    public boolean depositOne(String name, boolean exact) {
        return depositOne(inventoryService.get(name, exact));
    }

    /**
     * Deposits one item quickly by its name with a partial name match.
     *
     * @param name The name of the item to deposit.
     */
    public boolean depositOne(String name) {
        return depositOne(name, false);
    }

    /**
     * Deposit all items identified by its ItemWidget
     *
     * @param item item to deposit
     *
     * @returns did deposit anything
     */
    private boolean depositAll(InventoryItem item) {
        if (item == null) return false;
        if (!isOpen()) return false;
        if (!inventoryService.hasItem(item)) return false;
        container = BANK_INVENTORY_ITEM_CONTAINER;

        if (context.getVarbitValue(VarbitID.BANK_QUANTITY_TYPE) == 4) {
            invokeMenu(2, item);
        } else {
            invokeMenu(8, item);
        }
        return true;
    }

    /**
     * deposit all items identified by its id
     *
     * @param id searches based on the id
     *
     * @return true if anything deposited
     */
    public boolean depositAll(int id) {
        InventoryItem item = inventoryService.get(id);
        if (item == null) return false;
        return depositAll(item);
    }

    public boolean depositAll(Predicate<InventoryItem> predicate) {
        boolean result = false;
        List<InventoryItem> items = inventoryService.all().stream().filter(predicate).distinct().collect(Collectors.toList());
        for (InventoryItem item : items) {
            if (item == null) continue;
            depositAll(item);
            sleepService.sleep(RandomService.randomGaussian(400,200));
            result = true;
        }
        return result;
    }

    /**
     * deposit all items identified by its name
     * set exact to true if you want to be identified by its exact name
     *
     * @param name  name to search
     * @param exact does an exact search equalsIgnoreCase
     */
    public boolean depositAll(String name, boolean exact) {
        return depositAll(inventoryService.get(name, exact));
    }

    /**
     * deposit all items identified by its name
     *
     * @param name item name to search
     */
    public boolean depositAll(String name) {
        return depositAll(name, false);
    }

    /**
     * deposit all items
     */
    public boolean depositAll() {
        if (inventoryService.isEmpty()) return true;
        if (!isOpen()) return false;

        Widget widget = widgetService.findWidget(1041, null);
        if (widget == null) return false;

        widgetService.clickWidget(widget);
        sleepService.sleep(500, 1500);
        return true;
    }

    public boolean depositAllExcept(Predicate<InventoryItem> predicate) {
        return depositAll(predicate.negate());
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified IDs.
     * This method uses a lambda function to filter out the items with the specified IDs from the deposit operation.
     *
     * @param ids The IDs of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public boolean depositAllExcept(Integer... ids) {
        return depositAll(x -> Arrays.stream(ids).noneMatch(id -> id == x.getId()));
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified names.
     * This method uses a lambda function to filter out the items with the specified names from the deposit operation.
     *
     * @param names The names of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public boolean depositAllExcept(String... names) {
        return depositAllExcept(InventoryItem.matches(false, names));
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items with the specified names.
     * This method uses a lambda function to filter out the items with the specified names from the deposit operation.
     *
     * @param names The names of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public boolean depositAllExcept(Collection<String> names) {
        return depositAllExcept(names.toArray(String[]::new));
    }
}
