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
import com.kraken.api.interaction.inventory.InventoryItem;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
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

    /**
     * Withdraws a specified amount of an item from the bank.
     * @param item the Widget representing the item to withdraw
     * @param amount the amount of the item to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdrawX(Widget item, int amount) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThread(() -> {
            setWithdrawMode(EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_AS_VARBIT));

            if (EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_QUANTITY) == amount) {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetActionPacket(5, item.getId(), item.getItemId(), item.getIndex());
                return Optional.of(true);
            }
            BankInteraction.useItem(item, "Withdraw-X");
            EthanApiPlugin.getClient().setVarcStrValue(359, Integer.toString(amount));
            EthanApiPlugin.getClient().setVarcIntValue(5, 7);
            EthanApiPlugin.getClient().runScript(681);
            EthanApiPlugin.getClient().setVarbit(WITHDRAW_QUANTITY, amount);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Withdraws a specified amount of an item from the bank, either as notes or as regular items.
     * @param item the Widget representing the item to withdraw
     * @param amount the amount of the item to withdraw
     * @param noted if true, withdraws the item as notes; if false, withdraws it as a regular item
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdrawX(Widget item, int amount, boolean noted) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThread(() -> {
            setWithdrawMode(noted? WITHDRAW_NOTES_MODE : WITHDRAW_ITEM_MODE);
            if (EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_QUANTITY) == amount) {
                MousePackets.queueClickPacket();
                WidgetPackets.queueWidgetActionPacket(5, item.getId(), item.getItemId(), item.getIndex());
                return Optional.of(true);
            }
            BankInteraction.useItem(item, noted, "Withdraw-X");
            EthanApiPlugin.getClient().setVarcStrValue(359, Integer.toString(amount));
            EthanApiPlugin.getClient().setVarcIntValue(5, 7);
            EthanApiPlugin.getClient().runScript(681);
            EthanApiPlugin.getClient().setVarbit(WITHDRAW_QUANTITY, amount);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Withdraws a single item from the bank using its name.
     * @param name the name of the item to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdrawOne(String name) {
        if(!context.isPacketsLoaded()) return false;
        return withdraw(name, "Withdraw-1");
    }

    /**
     * Withdraws a single item from the bank using its name.
     * @param name the name of the item to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdrawFive(String name) {
        if(!context.isPacketsLoaded()) return false;
        return withdraw(name, "Withdraw-5");
    }

    /**
     * Withdraws a single item from the bank using its name.
     * @param name the name of the item to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdrawTen(String name) {
        if(!context.isPacketsLoaded()) return false;
        return withdraw(name, "Withdraw-10");
    }

    /**
     * Withdraws an item from the bank using its name and specified actions.
     * @param name the name of the item to withdraw
     * @param actions the actions to perform on the item widget (e.g., "Withdraw", "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X")
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdraw(String name, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return Bank.search().withName(name).first().flatMap(item -> {
            setWithdrawMode(EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_AS_VARBIT));

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Withdraws an item from the bank using its ID and specified actions.
     * @param id the ID of the item to withdraw
     * @param actions the actions to perform on the item widget (e.g., "Withdraw", "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X")
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdraw(int id, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return Bank.search().withId(id).first().flatMap(item -> {
            setWithdrawMode(EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_AS_VARBIT));

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Withdraws an item from the bank using its index and specified actions.
     * @param index the index of the item to withdraw
     * @param actions the actions to perform on the item widget (e.g., "Withdraw", "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X")
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdrawIndex(int index, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThread(() -> Bank.search().indexIs(index).first().flatMap(item -> {
            setWithdrawMode(EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_AS_VARBIT));

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        })).orElse(false);
    }

    /**
     * Withdraws an item from the bank using the provided Widget and actions.
     * @param item the Widget representing the item to withdraw
     * @param actions the actions to perform on the item widget (e.g., "Withdraw", "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X")
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdraw(Widget item, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThread(() -> {
            if (item == null) {
                return Optional.of(false);
            }

            setWithdrawMode(EthanApiPlugin.getClient().getVarbitValue(WITHDRAW_AS_VARBIT));
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Withdraws an item from the bank by its name, with an option to withdraw it as notes or as a regular item.
     * @param name the name of the item to withdraw
     * @param noted if true, withdraws the item as notes; if false, withdraws it as a regular item
     * @param actions the actions to perform on the item widget (e.g., "Withdraw", "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X")
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdraw(String name, boolean noted, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThread(() -> Bank.search().withName(name).first().flatMap(item -> {
            setWithdrawMode(noted ? WITHDRAW_NOTES_MODE : WITHDRAW_ITEM_MODE);

            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        })).orElse(false);
    }

    /**
     * Withdraws an item from the bank by its ID, with an option to withdraw it as notes or as a regular item.
     * @param id the ID of the item to withdraw
     * @param noted if true, withdraws the item as notes; if false, withdraws it as a regular item
     * @param actions the actions to perform on the item widget (e.g., "Withdraw", "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X")
     * @return True if the withdraw was successful and false otherwise
     */
    public boolean withdraw(int id, boolean noted, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThread(() -> Bank.search().withId(id).first().flatMap(item -> {
            setWithdrawMode(noted ? WITHDRAW_NOTES_MODE : WITHDRAW_ITEM_MODE);
            MousePackets.queueClickPacket();
            WidgetPackets.queueWidgetAction(item, actions);
            return Optional.of(true);
        })).orElse(false);
    }

    /**
     * Closes the bank interface if it is open.
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
            reflectionService.invokeMenuAction(item.getSlot(), container, MenuAction.CC_OP_LOW_PRIORITY.getId(), 2, item.getId(), item.getName(), "");
        } else {
            reflectionService.invokeMenuAction(item.getSlot(), container, MenuAction.CC_OP_LOW_PRIORITY.getId(), 3, item.getId(), item.getName(), "");
        }
        return true;
    }

    /**
     * Deposits one item quickly by its ID.
     *
     * @param id The ID of the item to deposit.
     * @return boolean true if the deposit was successful and false otherwise
     */
    public boolean depositOne(int id) {
        return depositOne(inventoryService.get(id));
    }

    /**
     * Deposits one item quickly by its name with a partial name match.
     *
     * @param name The name of the item to deposit.
     * @param exact When true only a name which exactly matches will be deposited. Partial matches are accepted when this value is false.
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositOne(String name, boolean exact) {
        return depositOne(inventoryService.get(name, exact));
    }

    /**
     * Deposits one item quickly by its name with a partial name match.
     *
     * @param name The name of the item to deposit.
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositOne(String name) {
        return depositOne(name, false);
    }

    /**
     * Deposit all items identified by its ItemWidget
     *
     * @param item item to deposit
     * @return True if the deposit was successful and false otherwise
     */
    private boolean depositAll(InventoryItem item) {
        if (item == null) return false;
        if (!isOpen()) return false;
        if (!inventoryService.hasItem(item)) return false;
        container = BANK_INVENTORY_ITEM_CONTAINER;

        if (context.getVarbitValue(VarbitID.BANK_QUANTITY_TYPE) == 4) {
            reflectionService.invokeMenuAction(item.getSlot(), container, MenuAction.CC_OP_LOW_PRIORITY.getId(), 2, item.getId(), item.getName(), "");
        } else {
            reflectionService.invokeMenuAction(item.getSlot(), container, MenuAction.CC_OP_LOW_PRIORITY.getId(), 3, item.getId(), item.getName(), "");
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
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositAll(String name, boolean exact) {
        return depositAll(inventoryService.get(name, exact));
    }

    /**
     * Deposit all items identified by its name
     *
     * @param name item name to search
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositAll(String name) {
        return depositAll(name, false);
    }

    /**
     * Deposit all items in the players inventory
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositAll() {
        if (inventoryService.isEmpty()) return true;
        if (!isOpen()) return false;

        Widget widget = widgetService.getWidget(786476);
        if (widget == null) return false;

        widgetService.interact(widget, "Deposit inventory");
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
