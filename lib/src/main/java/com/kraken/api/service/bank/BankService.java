package com.kraken.api.service.bank;

import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.service.ui.UIService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Objects;


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

    @Inject
    private UIService uiService;
    
    @Inject
    private MousePackets mousePackets;
    
    @Inject
    private WidgetPackets widgetPackets;
    
    @Inject
    private Context ctx;

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
     * Sets the withdrawal mode as either a note or item. If the withdrawal mode already matches the provided parameter no action will
     * be taken.
     * @param noted The boolean representing which withdraw mode to set. When set to false items will be withdrawn while true will withdraw
     *                     items in a noted format.
     * @return True if the withdrawal mode was set correctly and false otherwise.
     */
    public boolean setWithdrawMode(boolean noted) {
        int targetMode = noted ? 1 : 0;
        int currentMode = ctx.getVarbitValue(WITHDRAW_AS_VARBIT);

        if (currentMode == targetMode) return true;

        Widget toggleWidget = ctx.getClient().getWidget(noted ? WITHDRAW_NOTE_MODE_WIDGET : WITHDRAW_ITEM_MODE_WIDGET);

        if (toggleWidget != null) {
            String action = noted ? "Note" : "Item";

            boolean hasAction = Arrays.stream(toggleWidget.getActions())
                    .anyMatch(s -> Objects.equals(s, action));

            if (!hasAction) return false;

            Point pt = uiService.getClickbox(toggleWidget);
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
     */
    public void close() {
        if (isOpen()) {
            ctx.getClient().runScript(29);
        }
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
        if(ctx.inventory().isEmpty()) return true;
        if (!isOpen()) return false;

        Widget widget = ctx.getClient().getWidget(widgetId); // Deposit All
        if (widget == null) return false;

        ctx.widgets().withId(widgetId).first().interact(action);
        return true;
    }
}

