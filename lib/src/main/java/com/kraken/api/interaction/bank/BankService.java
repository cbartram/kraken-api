package com.kraken.api.interaction.bank;

import com.example.EthanApiPlugin.Collections.Bank;
import com.example.EthanApiPlugin.EthanApiPlugin;
import com.example.InteractionApi.BankInteraction;
import com.example.Packets.MousePackets;
import com.example.Packets.WidgetPackets;
import com.kraken.api.core.AbstractService;
import com.kraken.api.core.RandomService;
import com.kraken.api.core.SleepService;
import com.kraken.api.interaction.inventory.InventoryService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.widget.WidgetService;
import com.kraken.api.model.InventoryItem;
import com.kraken.api.model.NewMenuEntry;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.SpriteID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.example.InteractionApi.BankInteraction.setWithdrawMode;

@Slf4j
@Singleton
public class BankService extends AbstractService {
    private static final int WITHDRAW_QUANTITY = 3960;
    private static final int WITHDRAW_AS_VARBIT = 3958;
    private static final int WITHDRAW_ITEM_MODE = 0;
    private static final int WITHDRAW_NOTES_MODE = 1;
    private static final int WITHDRAW_ITEM_MODE_WIDGET = 786454;
    private static final int WITHDRAW_NOTE_MODE_WIDGET = 786456;

    private static final String ITEM_MODE_ACTION = "Item";
    private static final String NOTE_MODE_ACTION = "Note";


    @Inject
    private WidgetService widgetService;

    @Inject
    private SleepService sleepService;

    @Inject
    private InventoryService inventoryService;

    @Inject
    private ReflectionService reflectionService;

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

    public void withdrawX(Widget item, int amount) {
        setWithdrawMode(EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_AS_VARBIT));

        if (EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_QUANTITY) == amount) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(5, item.getId(), item.getItemId(), item.getIndex());
            return;
        }
        BankInteraction.useItem(item, "Withdraw-X");
        EthanApiPlugin.getClient().setVarcStrValue(359, Integer.toString(amount));
        EthanApiPlugin.getClient().setVarcIntValue(5, 7);
        EthanApiPlugin.getClient().runScript(681);
        EthanApiPlugin.getClient().setVarbit(WITHDRAW_QUANTITY, amount);
    }

    public void withdrawX(Widget item, int amount, boolean noted) {
        setWithdrawMode(noted? WITHDRAW_NOTES_MODE : WITHDRAW_ITEM_MODE);

        if (EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_QUANTITY) == amount) {
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetActionPacket(5, item.getId(), item.getItemId(), item.getIndex());
            return;
        }
        BankInteraction.useItem(item, noted, "Withdraw-X");
        EthanApiPlugin.getClient().setVarcStrValue(359, Integer.toString(amount));
        EthanApiPlugin.getClient().setVarcIntValue(5, 7);
        EthanApiPlugin.getClient().runScript(681);
        EthanApiPlugin.getClient().setVarbit(WITHDRAW_QUANTITY, amount);
    }

    public boolean withdraw(String name, String... actions) {
        return Bank.search().withName(name).first().flatMap(item -> {
            setWithdrawMode(EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_AS_VARBIT));

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    public boolean withdraw(int id, String... actions) {
        return Bank.search().withId(id).first().flatMap(item -> {
            setWithdrawMode(EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_AS_VARBIT));

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    public boolean withdrawIndex(int index, String... actions) {
        return Bank.search().indexIs(index).first().flatMap(item -> {
            setWithdrawMode(EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_AS_VARBIT));

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    public boolean withdraw(Widget item, String... actions) {
        if (item == null) {
            return false;
        }

        setWithdrawMode(EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_AS_VARBIT));

        MousePackets.queueClickPacket();
        WidgetPackets.queueWidgetAction(item, actions);
        return true;
    }

    public boolean withdraw(String name, boolean noted, String... actions) {
        return Bank.search().withName(name).first().flatMap(item -> {
            setWithdrawMode(noted ? WITHDRAW_NOTES_MODE : WITHDRAW_ITEM_MODE);

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    public boolean withdraw(int id, boolean noted, String... actions) {
        return Bank.search().withId(id).first().flatMap(item -> {
            setWithdrawMode(noted ? WITHDRAW_NOTES_MODE : WITHDRAW_ITEM_MODE);

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Executes menu swapping for a specific item and entry index.
     *
     * @param identifier The index of the entry to swap.
     * @param item    The ItemWidget associated with the menu swap.
     */
    public void invokeMenuReflect(final int identifier, InventoryItem item) {
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

        log.info("Invoking Bank menu: param0={}, param1={}, opcode={}, identifier={}, target={}, itemId={}", item.getSlot(), container, MenuAction.CC_OP_LOW_PRIORITY.getId(), identifier, item.getName(), item.getId());
        reflectionService.invokeMenuAction(item.getSlot(), container, MenuAction.CC_OP_LOW_PRIORITY.getId(), identifier, item.getId(), item.getName(), "");
    }

    /**
     * Closes the bank interface if it is open.
     *
     * @return true if the bank interface was open and successfully closed, true if already closed.
     */
    public void close() {
        if (isOpen()) {
            client.runScript(29);
        }
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
            invokeMenuReflect(2, item);
        } else {
            invokeMenuReflect(3, item);
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
            invokeMenuReflect(2, item);
        } else {
            invokeMenuReflect(8, item);
        }
        return true;
    }

    /**
     * Deposits all items identified with the given id.
     * @param id searches based on the id
     *
     * @return true if anything deposited
     */
    public boolean depositAll(int id) {
        InventoryItem item = inventoryService.getRandom(id);
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
