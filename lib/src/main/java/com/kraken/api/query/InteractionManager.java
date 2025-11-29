package com.kraken.api.query;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.*;
import com.kraken.api.query.bank.BankItemWidget;
import com.kraken.api.query.inventory.ContainerItem;
import com.kraken.api.query.groundobject.GroundItem;
import com.kraken.api.service.ui.UIService;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;

import java.util.Arrays;
import java.util.Objects;

/**
 * Manages interactions across various game entities like NPC's, Players, Widgets, GameObjects, TileObjects and more.
 */
@Singleton
public class InteractionManager {

    @Inject
    private NPCPackets npcPackets;

    @Inject
    private PlayerPackets playerPackets;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private WidgetPackets widgetPackets;

    @Inject
    private GameObjectPackets gameObjectPackets;

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
     * Interacts with a Player using the specified action i.e. "Attack", "Trade", or "Follow"
     *
     * @param player the Player to interact with
     * @param action The action to take, "Attack", "Trade", or "Follow"
     */
    public void interact(Player player, String action) {
        if(!ctx.isPacketsLoaded()) return;
        Point point = uiService.getClickbox(player);
        if (point != null) {
            mousePackets.queueClickPacket(point.getX(), point.getY());
            playerPackets.queuePlayerAction(player, action);
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

    /**
     * Interacts with a widget using the specific action.
     * @param item The widget to interact with
     * @param action The action to take i.e. Wield, Use or Examine
     */
    public void interact(Widget item, String action) {
        if(!ctx.isPacketsLoaded()) return;
        Point pt = uiService.getClickbox(item);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(item, action);
        }
    }

    /**
     * Uses a source widget on a destination widget (i.e. High Alchemy)
     * @param src The source widget to use on the destination widget
     * @param dest The destination widget
     */
    public void interact(Widget src, Widget dest) {
        if(!ctx.isPacketsLoaded()) return;

        Point pt = uiService.getClickbox(src);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetOnWidget(src, dest);
        }
    }

    /**
     * Interacts with a GameObject ({@code TileObject}) using the specified action i.e. "Chop", "Mine", or "Examine".
     * GameObject's are objects that exist on a tile like walls, trees, ore, or fishing spots.
     *
     * @param object the {@code TileObject} to interact with
     * @param action The action to take on the game object, i.e. "Chop", "Mine", or "Examine".
     */
    public void interact(TileObject object, String action) {
        if(!ctx.isPacketsLoaded()) return;
        Point pt = uiService.getClickbox(object);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            gameObjectPackets.queueObjectAction(object, false, action);
        }
    }

    /**
     * Interacts with a ground item ({@code GroundItem}) using the specified action i.e. "Take" or "Examine". A
     * Ground item is an actual item that is on the ground like coins dropped from a boss or logs a player has
     * dropped on a tile. This differs from GameObjects like trees, ore, or fish which exist on a tile but are not
     * "takeable" into the players inventory.
     *
     * @param item the {@code GroundItem} to interact with
     * @param action The action to take on the game object, i.e. "Take" or "Examine"
     */
    public void interact(GroundItem item, String action) {
        if(!ctx.isPacketsLoaded()) return;
        Point pt = uiService.getClickbox(item.getTileObject());

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            gameObjectPackets.queueObjectAction(item.getTileObject(), false, action);
        }
    }

}