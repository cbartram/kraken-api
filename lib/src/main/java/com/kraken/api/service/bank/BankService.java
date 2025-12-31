package com.kraken.api.service.bank;

import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.ui.UIService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;


/**
 * A service class for interacting with global bank operations. This class handles operations on the bank interface
 * like: opening, closing, depositing all items, and depositing all equipment.
 * <p>
 * For depositing or withdrawing specific items use {@code BankInventoryQuery} and {@code BankQuery} respectively.
 */
@Slf4j
@Singleton
public class BankService {
    private static final int WITHDRAW_AS_VARBIT = 3958;
    private static final int WITHDRAW_ITEM_MODE_WIDGET = 786456;
    private static final int WITHDRAW_NOTE_MODE_WIDGET = 786458;

    private final Map<Integer, Widget> pinWidgets = new ConcurrentHashMap<>();

    @Inject
    private Context ctx;

    /**
     * Checks whether the bank interface is open.
     *
     * @return {@code true} if the bank interface is open, {@code false} otherwise.
     */
    public boolean isOpen() {
        WidgetEntity bank = ctx.widgets().withText("Rearrange mode").first();
        if(bank == null) return false;
        return ctx.runOnClientThread(() -> !bank.raw().isHidden());
    }

    /**
     * Returns true when the bank PIN interface is open and false otherwise.
     * @return True for an open bank pin interface
     */
    public boolean isPinOpen() {
        Widget w = ctx.getClient().getWidget(InterfaceID.BankpinKeypad.UNIVERSE);
        if(w == null) return false;

        return !w.isSelfHidden();
    }

    @Subscribe
    private void onGameTick(GameTick e) {
        log.info("Game tick, bank service, subs working");
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent event) {
        if (event.getEventName().equals("bankpinButtonSetup")) {
            int[] intStack = ctx.getClient().getIntStack();
            int intStackSize = ctx.getClient().getIntStackSize();

            // The value on the stack representing the number (0-9)
            final int buttonId = intStack[intStackSize - 1];

            // The widget component ID
            final int compId = intStack[intStackSize - 2];

            Widget button = ctx.getClient().getWidget(compId);

            // Usually the clickable part is child 1 (the rect), but check your debugs.
            // Based on your previous code:
            Widget buttonRect = button.getChild(0);

            if (buttonRect != null) {
                log.info("Adding button: {} and rect: {}", buttonId, buttonRect);
                pinWidgets.put(buttonId, buttonRect);
            }
        }
    }

    /**
     * Enters the bank pin using the provided 4 digits.
     * Note: This method attempts to queue interactions. Depending on your script loop speed,
     * you may need to add sleeps between clicks if this is running on a fast loop.
     * @param pin An integer array of size 4 which contains the bank pin to enter.
     * @return boolean true if the pin was entered and false otherwise. This will return true if the pin was entered
     * at all. This doesn't necessarily mean the pin was correct.
     */
    public boolean enterPin(int[] pin) {
        if (!isPinOpen()) {
            return false;
        }

        // Validate range
        for (int digit : pin) {
            if (digit > 9 || digit < 0) {
                log.error("Bank pin digits must be between 0-9.");
                return false;
            }
        }

        MousePackets mousePackets = ctx.getService(MousePackets.class);
        WidgetPackets widgetPackets = ctx.getService(WidgetPackets.class);

        for (int digit : pin) {
//            InterfaceID.BankpinKeypad.A - J += 2 each widget id starting with: 13959184 so 86, 88, 90, 92, 94, 96, 98, 200
            Widget targetWidget = pinWidgets.get(digit);
            ctx.runOnClientThread(() -> {
                if (targetWidget != null && !targetWidget.isHidden()) {
                    Point pt = UIService.getClickbox(targetWidget);
                    mousePackets.queueClickPacket(pt.getX(), pt.getY());
                    widgetPackets.queueWidgetAction(targetWidget, "Select");
                    SleepService.sleep(600, 900);
                } else {
                    log.warn("Widget for digit {} not found. Interface might not be fully loaded.", digit);
                }
            });
        }

        pinWidgets.clear();
        return true;
    }

    /**
     * Sets the withdrawal mode as either a note or item. If the withdrawal mode already matches the provided parameter no action will
     * be taken.
     * @param noted The boolean representing which withdraw mode to set. When set to false items will be withdrawn while true will withdraw
     *                     items in a noted format.
     * @return True if the withdrawal mode was set correctly and false otherwise.
     */
    public boolean setWithdrawMode(boolean noted) {
        MousePackets mousePackets = ctx.getService(MousePackets.class);
        WidgetPackets widgetPackets = ctx.getService(WidgetPackets.class);

        int targetMode = noted ? 1 : 0;
        int currentMode = ctx.getVarbitValue(WITHDRAW_AS_VARBIT);

        if (currentMode == targetMode) return true;

        Widget toggleWidget = ctx.getClient().getWidget(noted ? WITHDRAW_NOTE_MODE_WIDGET : WITHDRAW_ITEM_MODE_WIDGET);

        if (toggleWidget != null) {
            String action = noted ? "Note" : "Item";
            boolean hasAction = Arrays.asList(Objects.requireNonNull(toggleWidget.getActions()))
                    .contains(action);

            if (!hasAction) return false;

            Point pt = UIService.getClickbox(toggleWidget);
            if (pt != null) {
                mousePackets.queueClickPacket(pt.getX(), pt.getY());
                widgetPackets.queueWidgetAction(toggleWidget, action);
                return true;
            }
        }
        return false;
    }


    /**
     * Closes the bank interface if it is open.
     * @return True if the bank interface was closed successfully and false otherwise
     */
    public boolean close() {
        if (isOpen()) {
            ctx.runOnClientThread(() -> ctx.getClient().runScript(29));
            return true;
        }
        return false;
    }

    /**
     * Deposit all items in the players inventory into the bank.
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositAll() {
        return depositAllInternal(786476, "Deposit inventory");
    }

    /**
     * Deposits all worn items from the players equipment tab into the bank.
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositAllEquipment() {
        return depositAllInternal(786478, "Deposit worn items");
    }

    /**
     * An internal method to wrap deposit all widgets.
     * @param widgetId Widget id of the deposit button to interact with
     * @return
     */
    private boolean depositAllInternal(int widgetId, String action) {
        return ctx.runOnClientThread(() -> {
            if(ctx.inventory().isEmpty()) return true;
            if (!isOpen()) return false;

            Widget widget = ctx.getClient().getWidget(widgetId); // Deposit All
            if (widget == null) return false;

            ctx.widgets().withId(widgetId).first().interact(action);
            return true;
        });
    }
}

