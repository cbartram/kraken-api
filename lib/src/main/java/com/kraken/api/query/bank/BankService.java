package com.kraken.api.query.bank;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.query.inventory.ContainerItem;
import com.kraken.api.query.inventory.InventoryService;
import com.kraken.api.service.RandomService;
import com.kraken.api.service.SleepService;
import com.kraken.api.service.ui.UIService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Item;
import net.runelite.api.ItemComposition;
import net.runelite.api.ItemContainer;
import net.runelite.api.Point;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;


@Slf4j
@Singleton
@Deprecated
public class BankService {
    private static final int WITHDRAW_QUANTITY = 3960;
    private static final int WITHDRAW_AS_VARBIT = 3958;
    private static final int WITHDRAW_ITEM_MODE = 0;
    private static final int WITHDRAW_NOTES_MODE = 1;
    private static final int WITHDRAW_ITEM_MODE_WIDGET = 786456;
    private static final int WITHDRAW_NOTE_MODE_WIDGET = 786458;
    private static final String ITEM_MODE_ACTION = "Item";
    private static final String NOTE_MODE_ACTION = "Note";


    @Inject
    private SleepService sleepService;

    @Inject
    private InventoryService inventoryService;

    @Inject
    private UIService uiService;
    
    @Inject
    private MousePackets mousePackets;
    
    @Inject
    private WidgetPackets widgetPackets;

    @Inject
    private ItemManager itemManager;
    
    @Inject
    private Context ctx;

    private static final int BANK_INVENTORY_ITEM_CONTAINER = 983043;
    private int lastUpdateTick = 0;

    private LoadingCache<Integer, ItemComposition> itemDefs = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(20, TimeUnit.MINUTES)
            .build(new CacheLoader<>() {
                    @Override
                    public ItemComposition load(Integer itemId) {
                        return ctx.runOnClientThread(() -> itemManager.getItemComposition(itemId));
                    }
                }
            );


    /**
     * Returns a list of {@code BankItemWidget} objects stored in a players bank. This can only be called when
     * the bank interface is open.
     * @return List of Inventory Items in the players bank
     */
    public List<BankItemWidget> getItems() {
        List<BankItemWidget> bankItems = new ArrayList<>();
        return ctx.runOnClientThread(() -> {
            if (lastUpdateTick < ctx.getClient().getTickCount()) {
                int i = 0;
                ItemContainer container = ctx.getClient().getItemContainer(InventoryID.BANK);
                if(container == null) {
                    return Collections.emptyList();
                }

                for (Item item : container.getItems()) {
                    try {
                        if (item == null) {
                            i++;
                            continue;
                        }

                        if (itemDefs.get(item.getId()).getPlaceholderTemplateId() == 14401) {
                            i++;
                            continue;
                        }

                        ItemComposition comp = ctx.getItemManager().getItemComposition(item.getId());
                        if(comp.getName().equalsIgnoreCase("Bank filler")) {
                            i++;
                            continue;
                        }

                        itemDefs.put(item.getId(), comp);
                        bankItems.add(new BankItemWidget(itemDefs.get(item.getId()).getName(), item.getId(), item.getQuantity(), i, ctx));
                    } catch (NullPointerException | ExecutionException ex) {
                        log.error("exception thrown while attempting to get items from bank:", ex);
                    }
                    i++;
                }
                lastUpdateTick = ctx.getClient().getTickCount();
            }
            return bankItems;
        });
    }

    /**
     * Checks whether the bank interface is open.
     *
     * @return {@code true} if the bank interface is open, {@code false} otherwise.
     */
    public boolean isOpen() {
        Widget bank = ctx.widgets().withText("Rearrange mode").first().raw();
        return bank != null && !bank.isHidden();
    }

    /**
     * Sets the withdrawal mode as either a note or item.
     * @param withdrawMode The integer representing which withdraw mode to set. When set to 0 items will be withdrawn while 1 will withdraw
     *                     items in a noted format.
     * @return True if the withdrawal mode was set correctly and false otherwise.
     */
    public boolean setWithdrawMode(int withdrawMode) {
        int withdrawAsVarbitValue = ctx.getVarbitValue(WITHDRAW_AS_VARBIT);
        Widget itemWidget = ctx.getClient().getWidget(WITHDRAW_ITEM_MODE_WIDGET);
        Widget noteWidget = ctx.getClient().getWidget(WITHDRAW_NOTE_MODE_WIDGET);
        if (Arrays.stream(itemWidget.getActions()).noneMatch((s) -> Objects.equals(s, ITEM_MODE_ACTION)) || Arrays.stream(noteWidget.getActions()).noneMatch((s) -> Objects.equals(s, NOTE_MODE_ACTION))) {
            return false;
        }

        if (withdrawMode == WITHDRAW_ITEM_MODE && withdrawAsVarbitValue != WITHDRAW_ITEM_MODE) {
            Point pt = uiService.getClickbox(itemWidget);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(itemWidget, "Item");

            return true;
        }

        if (withdrawMode == WITHDRAW_NOTES_MODE && withdrawAsVarbitValue != WITHDRAW_NOTES_MODE) {
            Point pt = uiService.getClickbox(noteWidget);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(noteWidget, "Note");

            return true;
        }

        return false;
    }

    /**
     * Withdraws a specified amount of an item from the bank.
     * @param item the Widget representing the item to withdraw
     * @param amount the amount of the item to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdrawX(Widget item, int amount) {
        if(!ctx.isPacketsLoaded()) return false;
        return ctx.runOnClientThread(() -> {
            setWithdrawMode(ctx.getVarbitValue(WITHDRAW_AS_VARBIT));
            if (ctx.getVarbitValue(WITHDRAW_QUANTITY) == amount) {
                Point pt = uiService.getClickbox(item);
                mousePackets.queueClickPacket(pt.getX(), pt.getY());
                widgetPackets.queueWidgetActionPacket(5, item.getId(), item.getItemId(), item.getIndex());
                return Optional.of(true);
            }

            setWithdrawMode(ctx.getVarbitValue(WITHDRAW_AS_VARBIT));
            Point pt = uiService.getClickbox(item);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(item, "Withdraw-X");

            ctx.getClient().setVarcStrValue(359, Integer.toString(amount));
            ctx.getClient().setVarcIntValue(5, 7);
            ctx.getClient().runScript(681);
            ctx.getClient().setVarbit(WITHDRAW_QUANTITY, amount);
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
        if(!ctx.isPacketsLoaded()) return false;
        return ctx.runOnClientThread(() -> {
            setWithdrawMode(noted ? WITHDRAW_NOTES_MODE : WITHDRAW_ITEM_MODE);
            if (ctx.getVarbitValue(WITHDRAW_QUANTITY) == amount) {
                Point pt = uiService.getClickbox(item);
                mousePackets.queueClickPacket(pt.getX(), pt.getY());
                widgetPackets.queueWidgetActionPacket(5, item.getId(), item.getItemId(), item.getIndex());
                return Optional.of(true);
            }

            setWithdrawMode(noted ? WITHDRAW_NOTES_MODE : WITHDRAW_ITEM_MODE);
            Point pt = uiService.getClickbox(item);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(item, "Withdraw-X");

            ctx.getClient().setVarcStrValue(359, Integer.toString(amount));
            ctx.getClient().setVarcIntValue(5, 7);
            ctx.getClient().runScript(681);
            ctx.getClient().setVarbit(WITHDRAW_QUANTITY, amount);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Withdraws a single item from the bank using its name.
     * @param name the name of the item to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdrawOne(String name) {
        if(!ctx.isPacketsLoaded()) return false;
        return withdraw(name, "Withdraw-1");
    }

    /**
     * Withdraws a single item from the bank using its name.
     * @param name the name of the item to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdrawFive(String name) {
        if(!ctx.isPacketsLoaded()) return false;
        return withdraw(name, "Withdraw-5");
    }

    /**
     * Withdraws a single item from the bank using its name.
     * @param name the name of the item to withdraw
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdrawTen(String name) {
        if(!ctx.isPacketsLoaded()) return false;
        return withdraw(name, "Withdraw-10");
    }

    /**
     * Withdraws an item from the bank using its name and specified actions.
     * @param name the name of the item to withdraw
     * @param actions the actions to perform on the item widget (e.g., "Withdraw", "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X")
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdraw(String name, String... actions) {
        if(!ctx.isPacketsLoaded() || !isOpen()) return false;
        BankItemWidget item = getItems().stream().filter(i -> i.getName().equals(name)).findFirst().orElse(null);
        setWithdrawMode(ctx.getVarbitValue(WITHDRAW_AS_VARBIT));

        Point pt = uiService.getClickbox(item);
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetAction(item, actions);
        return true;
    }

    /**
     * Withdraws an item from the bank using its ID and specified actions.
     * @param id the ID of the item to withdraw
     * @param actions the actions to perform on the item widget (e.g., "Withdraw", "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X")
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdraw(int id, String... actions) {
        if(!ctx.isPacketsLoaded() || !isOpen()) return false;
        BankItemWidget item = getItems().stream().filter(i -> i.getId() == id).findFirst().orElse(null);
        setWithdrawMode(ctx.getVarbitValue(WITHDRAW_AS_VARBIT));

        Point pt = uiService.getClickbox(item);
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetAction(item, actions);
        return true;
    }

    /**
     * Withdraws an item from the bank using the provided Widget and actions.
     * @param item the Widget representing the item to withdraw
     * @param actions the actions to perform on the item widget (e.g., "Withdraw", "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X")
     * @return true if the withdrawal was successful, false otherwise
     */
    public boolean withdraw(Widget item, String... actions) {
        if(!ctx.isPacketsLoaded()) return false;
        return ctx.runOnClientThread(() -> {
            if (item == null) {
                return Optional.of(false);
            }

            setWithdrawMode(ctx.getVarbitValue(WITHDRAW_AS_VARBIT));
            Point pt = uiService.getClickbox(item);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(item, actions);
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
        if(!ctx.isPacketsLoaded() || !isOpen()) return false;
        setWithdrawMode(noted ? WITHDRAW_NOTES_MODE : WITHDRAW_ITEM_MODE);
        BankItemWidget item = getItems().stream().filter(i -> i.getName().equalsIgnoreCase(name)).findFirst().orElse(null);
        setWithdrawMode(ctx.getVarbitValue(WITHDRAW_AS_VARBIT));

        Point pt = uiService.getClickbox(item);
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetAction(item, actions);
        return true;
    }

    /**
     * Withdraws an item from the bank by its ID, with an option to withdraw it as notes or as a regular item.
     * @param id the ID of the item to withdraw
     * @param noted if true, withdraws the item as notes; if false, withdraws it as a regular item
     * @param actions the actions to perform on the item widget (e.g., "Withdraw", "Withdraw-1", "Withdraw-5", "Withdraw-10", "Withdraw-All", "Withdraw-X")
     * @return True if the withdraw was successful and false otherwise
     */
    public boolean withdraw(int id, boolean noted, String... actions) {
        if(!ctx.isPacketsLoaded()) return false;
        setWithdrawMode(noted ? WITHDRAW_NOTES_MODE : WITHDRAW_ITEM_MODE);
        BankItemWidget item = getItems().stream().filter(i -> i.getId() == id).findFirst().orElse(null);
        setWithdrawMode(ctx.getVarbitValue(WITHDRAW_AS_VARBIT));

        Point pt = uiService.getClickbox(item);
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetAction(item, actions);
        return true;
    }

    /**
     * Closes the bank interface if it is open.
     */
    public void close() {
        if (isOpen()) {
            ctx.getClient().runScript(29);
        }
    }

    /**
     * Deposits one item quickly into the bank by its ItemWidget.
     *
     * @param item The ItemWidget representing the item to deposit.
     */
    private boolean depositOne(ContainerItem item) {
        if (item == null) return false;
        if (!isOpen()) return false;
        if (!inventoryService.hasItem(item.getId())) return false;

        Point pt = uiService.getClickbox(item);
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetAction(item.getBankInventoryWidget(), "Deposit-1");
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
    private boolean depositAll(ContainerItem item) {
        if (item == null) return false;
        if (!isOpen()) return false;
        if (!inventoryService.hasItem(item)) return false;

        if (ctx.getVarbitValue(VarbitID.BANK_QUANTITY_TYPE) == 4) {
            Point pt = uiService.getClickbox(item);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(item.getBankInventoryWidget(), "Deposit-All");
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
        ContainerItem item = inventoryService.getRandom(id);
        if (item == null) return false;
        return depositAll(item);
    }

    /**
     * Deposits all items in the inventory which match a given predicate.
     * @param predicate Predicate to filter items in the inventory.
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositAll(Predicate<ContainerItem> predicate) {
        boolean result = false;
        List<ContainerItem> items = inventoryService.all().stream().filter(predicate).distinct().collect(Collectors.toList());
        for (ContainerItem item : items) {
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

        Widget widget = ctx.getClient().getWidget(786476);
        if (widget == null) return false;

        ctx.widgets().withId(786476).first().interact("Deposit Inventory");
        sleepService.sleep(500, 1500);
        return true;
    }

    /**
     * Deposits all items in the player's inventory into the bank, except for the items which match the predicate.
     * This method uses a lambda function to filter out the items with the specified IDs from the deposit operation.
     *
     * @param predicate The predicate filter of the items to be excluded from the deposit.
     *
     * @return true if any items were deposited, false otherwise.
     */
    public boolean depositAllExcept(Predicate<ContainerItem> predicate) {
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
        return depositAllExcept(ContainerItem.matches(false, names));
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
