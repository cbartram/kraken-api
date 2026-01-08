package com.kraken.api.query;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.*;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.container.bank.BankItemWidget;
import com.kraken.api.query.groundobject.GroundItem;
import com.kraken.api.service.ui.UIService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.widgets.Widget;

/**
 * Manages interactions across various game entities like NPC's, Players, Widgets, GameObjects, TileObjects and more.
 */
@Slf4j
@Getter
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
    private GroundItemPackets groundItemPackets;

    @Inject
    private Provider<Context> ctxProvider;

    /**
     * Interacts with an NPC using the specified action i.e. "Attack", "Talk-To", or "Examine".
     *
     * @param npc the NPC to interact with
     * @param action The action to take, "Attack", "Talk-To", or "Examine".
     */
    public void interact(NPC npc, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point point = UIService.getClickbox(npc);
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
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point point = UIService.getClickbox(player);
        if (point != null) {
            mousePackets.queueClickPacket(point.getX(), point.getY());
            playerPackets.queuePlayerAction(player, action);
        }
    }

    /**
     * Interacts with an item with the specified ID in an item container (inventory, inventory while banking, equipment, etc...)
     * using the specified action.
     * <p>
     * @param item The Container Item to interact with. A container item is an item stored in a container like an inventory, a inventory while banking
     *             or the equipment interface.
     * @param action The action to take. i.e. "Eat", "Remove", "Wield", "Wear", or "Use"
     */
    public void interact(ContainerItem item, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        ctxProvider.get().runOnClientThread(() -> {
            if(item == null) return;

            Widget w = item.getWidget();
            if (w == null) {
                log.error("Failed to resolve widget for item interaction: {}", item.getName());
                return;
            }

            Point pt = UIService.getClickbox(item);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(w, action);
        });
    }

    /**
     * Interacts with a widget in the players bank using the specific action.
     * @param item The bank item widget to interact with
     * @param action The action to take i.e. Withdraw-1, Withdraw-X, Examine
     */
    public void interact(BankItemWidget item, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(item);

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
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(item);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(item, action);
        }
    }

    /**
     * Interacts with a widget using the specific sub action.
     * @param item The widget to interact with
     * @param menu The menu to select
     * @param action The action to take i.e. Wield, Use or Examine
     */
    public void interact(Widget item, String menu, String action) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(item);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetSubAction(item, menu, action);
        }
    }

    /**
     * Uses a source widget on a destination widget (i.e. High Alchemy)
     * @param src The source widget to use on the destination widget
     * @param dest The destination widget
     */
    public void interact(Widget src, Widget dest) {
        if(!ctxProvider.get().isPacketsLoaded()) return;

        Point pt = UIService.getClickbox(src);
        Point destPoint = UIService.getClickbox(dest);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            mousePackets.queueClickPacket(destPoint.getX(), destPoint.getY());
            widgetPackets.queueWidgetOnWidget(src, dest);
        }
    }

    /**
     * Interacts with a widget using the specific action index
     * @param action The action index to take
     * @param packedWidgetId The packed widget id
     * @param childId The child id of the widget to interact with
     * @param itemId The item id of the widget to interact with
     */
    public void interact(int action, int packedWidgetId, int childId, int itemId) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(ctxProvider.get().widgets().get(packedWidgetId).raw());
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetActionPacket(packedWidgetId, childId, itemId, action);
    }

    /**
     * Uses a source widget on a destination NPC (i.e. Crumble Undead spell on Vorkath Spawn)
     * @param src The source widget to use on the destination widget
     * @param npc The NPC to use the widget on
     */
    public void interact(Widget src, NPC npc) {
        if(!ctxProvider.get().isPacketsLoaded()) return;

        Point pt = UIService.getClickbox(src);
        Point npcPoint = UIService.getClickbox(npc);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            mousePackets.queueClickPacket(npcPoint.getX(), npcPoint.getY());
            npcPackets.queueWidgetOnNPC(npc, src);
        }
    }

    /**
     * Uses a source widget on a destination Game Object (i.e. "Bones" on the "Chaos Altar")
     * @param src The source widget to use on the destination widget
     * @param gameObject The Game Object to use the widget on
     */
    public void interact(Widget src, GameObject gameObject) {
        if(!ctxProvider.get().isPacketsLoaded()) return;

        Point pt = UIService.getClickbox(src);
        Point gameObjectPoint = UIService.getClickbox(gameObject);

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            mousePackets.queueClickPacket(gameObjectPoint.getX(), gameObjectPoint.getY());
            gameObjectPackets.queueWidgetOnTileObject(src, gameObject);
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
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(object);

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
     */
    public void interact(GroundItem item) {
        if(!ctxProvider.get().isPacketsLoaded()) return;
        Point pt = UIService.getClickbox(item.getTileObject());

        if(pt != null) {
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            groundItemPackets.queueGroundItemAction(item.getTileItem(), item.getLocation(), false);
        }
    }

}