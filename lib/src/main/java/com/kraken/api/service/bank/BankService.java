package com.kraken.api.service.bank;

import com.google.inject.Provider;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.input.KeyboardService;
import com.kraken.api.service.ui.UIService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.InventoryID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.JavaScriptCallback;
import net.runelite.api.widgets.Widget;
import net.runelite.client.eventbus.Subscribe;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Objects;

import static net.runelite.api.gameval.VarbitID.BANK_WITHDRAWNOTES;


/**
 * A service class for interacting with global bank operations. This class handles operations on the bank interface
 * like: opening, closing, depositing all items, and depositing all equipment.
 * <p>
 * For depositing or withdrawing specific items use {@code BankInventoryQuery} and {@code BankQuery} respectively.
 */
@Slf4j
@Singleton
public class BankService {
    private static final int WITHDRAW_ITEM_NOT_MODE_WIDGET = 786451;

    @Inject
    private Provider<Context> ctxProvider;

    @Inject
    private Client client;

    @Inject
    private KeyboardService keyboard;

    /**
     * Checks whether the bank interface is open.
     *
     * @return {@code true} if the bank interface is open, {@code false} otherwise.
     */
    public boolean isOpen() {
        Client client = ctxProvider.get().getClient();
        return ctxProvider.get().runOnClientThread(() -> client.getItemContainer(InventoryID.BANK) != null);
    }

    /**
     * Determines whether the bank interface is closed.
     * <p>
     * This method provides the inverse of the {@code isOpen} method by checking
     * whether the bank interface is not open.
     *
     * @return {@code true} if the bank interface is closed, {@code false} otherwise.
     */
    public boolean isClosed() {
        return !isOpen();
    }

    /**
     * Returns true when the bank PIN interface is open and false otherwise.
     * @return True for an open bank pin interface
     */
    public boolean isPinOpen() {
        Widget w = ctxProvider.get().getClient().getWidget(InterfaceID.BankpinKeypad.UNIVERSE);
        if(w == null) return false;

        return !w.isSelfHidden();
    }

    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent event) {
        if (event.getEventName().equals("bankpinButtonSetup")) {
            int[] intStack = client.getIntStack();
            int intStackSize = client.getIntStackSize();

            // The value on the stack representing the number (0-9)
            final int pinNumber = intStack[intStackSize - 1];

            // The widget component ID
            final int compId = intStack[intStackSize - 2];

            Widget button = client.getWidget(compId);
            Widget pinButtonWidget = button.getChild(0);
            final Object[] onOpListener = pinButtonWidget.getOnOpListener();
            pinButtonWidget.setOnKeyListener((JavaScriptCallback) e -> {
                int typedChar = e.getTypedKeyChar() - '0';
                if (typedChar != pinNumber) {
                    return;
                }

                log.info("Bank pin keypress: {}", typedChar);
                client.runScript(onOpListener);
                client.setVarcIntValue(VarClientID.KEYBOARD_TIMEOUT, client.getGameCycle() + 1);
            });
        }
    }

    /**
     * Enters the bank pin using the provided 4 digits.
     * @param pin An integer array of size 4 which contains the bank pin to enter.
     * @return boolean true if the pin was entered and false otherwise. This will return true if the pin was entered
     * at all. This doesn't necessarily mean the pin was correct.
     */
    public boolean enterPin(int[] pin) {
        if (!isPinOpen()) {
            return false;
        }

        for (int digit : pin) {
            if (digit < 0 || digit > 9) {
                log.error("Invalid pin digit: {}, must be between 0-9", digit);
                return false;
            }

            keyboard.typeChar(Character.forDigit(digit, 10));
            SleepService.sleep(30, 70);
        }

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
        MousePackets mousePackets = ctxProvider.get().getService(MousePackets.class);
        WidgetPackets widgetPackets = ctxProvider.get().getService(WidgetPackets.class);

        int targetMode = noted ? 1 : 0;
        int currentMode = ctxProvider.get().getVarbitValue(BANK_WITHDRAWNOTES);

        if (currentMode == targetMode) return true;

        Widget toggleWidget = client.getWidget(WITHDRAW_ITEM_NOT_MODE_WIDGET);

        if (toggleWidget != null) {
            String action = noted ? "Enable <col=ff9040>Notes" : "Disable <col=ff9040>Notes";
            String cleanedAction = noted ? "enable notes" : "disable notes";
            boolean hasAction = Arrays.asList(Objects.requireNonNull(toggleWidget.getActions()))
                    .contains(action);

            if (!hasAction) return false;

            Point pt = UIService.getClickbox(toggleWidget);
            if (pt != null) {
                mousePackets.queueClickPacket(pt.getX(), pt.getY());
                widgetPackets.queueWidgetAction(toggleWidget, cleanedAction);
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
            ctxProvider.get().runOnClientThread(() -> client.runScript(29));
            return true;
        }
        return false;
    }

    /**
     * Deposit all items in the players inventory into the bank.
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositAll() {
        return ctxProvider.get().runOnClientThread(() -> {
            if(ctxProvider.get().inventory().isEmpty()) return true;
            if (!isOpen()) return false;

            Widget widget = client.getWidget(786473);
            if (widget == null) return false;

            ctxProvider.get().widgets().withId(786473).first().interact("Deposit inventory");
            return true;
        });
    }

    /**
     * Deposits all worn items from the players equipment tab into the bank.
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositAllEquipment() {
        return ctxProvider.get().runOnClientThread(() -> {
            if (!isOpen()) return false;

            Widget widget = client.getWidget(786475);
            if (widget == null) return false;

            ctxProvider.get().widgets().withId(786475).first().interact("Deposit worn items");
            return true;
        });
    }

    /**
     * Deposits items in stored containers (like wilderness loot bags, rune pouches, etc...)
     * into the bank.
     * @return True if the deposit was successful and false otherwise
     */
    public boolean depositContainers() {
        return ctxProvider.get().runOnClientThread(() -> {
            if (!isOpen()) return false;

            Widget widget = client.getWidget(786471);
            if (widget == null) return false;

            ctxProvider.get().widgets().withId(786471).first().interact("Empty containers");
            return true;
        });
    }
}

