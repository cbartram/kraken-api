package com.kraken.api.interaction;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.NPCPackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.interaction.container.bank.BankItemWidget;
import com.kraken.api.interaction.container.inventory.ContainerItem;
import com.kraken.api.interaction.ui.UIService;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.util.Arrays;
import java.util.Objects;

/**
 * Manages interactions across various game entities like NPC's, Widgets, GameObjects, TileObjects and more.
 */
@Singleton
public class InteractionManager {

    @Inject
    private NPCPackets npcPackets;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private WidgetPackets widgetPackets;

    @Inject
    private UIService uiService;

    @Inject
    private Context ctx;

    /**
     * Interacts with an NPC using the specified action i.e. "Attack", "Talk-To", or "Examine".
     *
     * @param npc the NPC to interact with
     * @param action The action to take, "Attack", "Talk-To", or "Examine".
     */
    public void interact(NPC npc, String action) {
        if(!ctx.isPacketsLoaded()) return;
        Point point = uiService.getClickbox(npc);
        if (point != null) {
            mousePackets.queueClickPacket(point.getX(), point.getY());
            npcPackets.queueNPCAction(npc, action);
        }
    }

    /**
     * Interacts with an item with the specified ID in the inventory using the specified actions. If no action is specified
     * the first available action is used.
     *
     * @param item The Inventory Item to interact with.
     * @param bankInventory True if the bank interface is open and the function should use the Bank Inventory widget to search for items to interact
     *                      with instead of the normal players inventory.
     * @param action The action to take. i.e. "Eat" or "Use"
     */
    public void interact(ContainerItem item, boolean bankInventory, String action) {
        if(!ctx.isPacketsLoaded()) return;

        // Get first action is no specific action is passed
        String parsedAction = (action == null || action.trim().isEmpty())
                ? Arrays.stream(item.getInventoryActions())
                .findFirst().orElse(null)
                : action;

        ctx.runOnClientThread(() -> {
            if(item == null) return;
            Widget w;

            if(bankInventory) {
                w = item.getBankInventoryWidget();
            } else {
                w = item.getWidget();
            }

            // This can happen if the user hasn't changed something in their inventory since logging in, since widgets
            // weren't loaded when refresh() was called.
            if(w == null) {
                Widget inven;

                if(bankInventory) {
                    inven = ctx.getClient().getWidget(WidgetInfo.BANK_INVENTORY_ITEMS_CONTAINER);
                } else {
                    inven = ctx.getClient().getWidget(149, 0);
                }

                if(inven == null) return;
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
        });
    }

    /**
     * Interacts with a widget in the players bank using the specific action.
     * @param item The bank item widget to interact with
     * @param action The action to take i.e. Withdraw-1, Withdraw-X, Examine
     */
    public void interact(BankItemWidget item, String action) {
        if(!ctx.isPacketsLoaded()) return;
        Point pt = uiService.getClickbox(item);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(item, action);
        }
    }

}