package com.kraken.api.core.packet.entity;

import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketDefFactory;
import com.kraken.api.interaction.tile.TileService;
import lombok.SneakyThrows;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A high-level utility class for sending packets related to {@link TileObject} (Game Object)
 * interactions to the game server.
 * <p>
 * This class handles actions like clicking on doors, trees, or banks, as well as
 * "use-on" actions (e.g., using an item on an object). It abstracts the low-level
 * packet construction and world coordinate calculations.
 */
public class ObjectPackets {

    @Inject
    private Provider<PacketClient> packetClientProvider;

    @Inject
    private PacketDefFactory packetDefFactory;

    @Inject
    private TileService tileService;

    @Inject
    private Client client;

    /**
     * Queues the low-level packet to perform a generic action click on a tile object.
     * <p>
     * This method sends one of the {@code OPLOC} packets (e.g., {@code OPLOC1} through {@code OPLOC10}),
     * where the action is determined by the {@code actionFieldNo}.
     *
     * @param actionFieldNo The 1-based index of the action to execute (1-10).
     * @param objectId The ID of the target object.
     * @param worldPointX The X coordinate of the object's location in the world.
     * @param worldPointY The Y coordinate of the object's location in the world.
     * @param ctrlDown If true, indicates the control key was held down.
     */
    @SneakyThrows
    public void queueObjectAction(int actionFieldNo, int objectId, int worldPointX, int worldPointY, boolean ctrlDown) {
        int ctrl = ctrlDown ? 1 : 0;
        packetClientProvider.get().sendPacket(packetDefFactory.getOpLoc(actionFieldNo), objectId, worldPointX, worldPointY, ctrl);
    }

    /**
     * Queues a tile object action by matching a human-readable action string (e.g., "Chop", "Open", "Bank").
     * <p>
     * This is a high-level convenience method that determines the target object's
     * world coordinates, checks the object's available actions, finds a match
     * for {@code actionlist}, and sends the correct low-level {@code OPLOC} packet.
     *
     * @param object The target {@link TileObject} (e.g., GameObject, WallObject).
     * @param ctrlDown If true, indicates the control key was held down.
     * @param actionlist A varargs list of action strings to search for (case-insensitive).
     */
    @SneakyThrows
    public void queueObjectAction(TileObject object, boolean ctrlDown, String... actionlist) {
        if (object == null) {
            return;
        }
        ObjectComposition comp = tileService.getObjectComposition(object);
        if (comp == null || comp.getActions() == null) {
            return;
        }

        List<String> actions = Arrays.stream(comp.getActions()).collect(Collectors.toList());
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i) == null)
                continue;
            actions.set(i, actions.get(i).toLowerCase());
        }

        Point p;
        if (object instanceof GameObject) {
            GameObject gameObject = (GameObject) object;
            p = gameObject.getSceneMinLocation();
        } else {
            p = new Point(object.getLocalLocation().getSceneX(), object.getLocalLocation().getSceneY());
        }

        LocalPoint lp = new LocalPoint(p.getX(), p.getY(), client.getTopLevelWorldView());
        WorldPoint wp = WorldPoint.fromScene(client.getTopLevelWorldView(), lp.getX(), lp.getY(), object.getPlane());

        int num = -1;
        for (String action : actions) {
            for (String action2 : actionlist) {
                if (action != null && action.equalsIgnoreCase(action2.toLowerCase())) {
                    num = actions.indexOf(action) + 1;
                }
            }
        }

        if (num < 1 || num > 10) {
            return;
        }

        queueObjectAction(num, object.getId(), wp.getX(), wp.getY(), ctrlDown);
    }

    /**
     * Queues the raw packet for using a widget (typically an item) on a tile object.
     * <p>
     * This method sends the {@code OPLOCT} (Use Widget on Location/Object) packet,
     * which includes the coordinates of the target object and the details of the source item/widget.
     *
     * @param objectId The ID of the target object.
     * @param worldPointX The X coordinate of the object's location in the world.
     * @param worldPointY The Y coordinate of the object's location in the world.
     * @param sourceSlot The slot index of the item being used (e.g., inventory slot).
     * @param sourceItemId The ID of the item being used.
     * @param sourceWidgetId The ID of the parent widget containing the item (e.g., inventory widget ID).
     * @param ctrlDown If true, indicates the control key was held down.
     */
    public void queueWidgetOnTileObject(int objectId, int worldPointX, int worldPointY, int sourceSlot, int sourceItemId, int sourceWidgetId, boolean ctrlDown) {
        int ctrl = ctrlDown ? 1 : 0;
        packetClientProvider.get().sendPacket(packetDefFactory.getOpLocT(), objectId, worldPointX, worldPointY, sourceSlot, sourceItemId, sourceWidgetId, ctrl);
    }

    /**
     * Queues the packet for using a specific {@link Widget} (or item it represents) on a target {@link TileObject}.
     * <p>
     * This is a convenience method that first calculates the target object's world coordinates
     * and then calls the raw {@code queueWidgetOnTileObject} method with the source widget's details.
     *
     * @param widget The source {@link Widget} containing the item or action to be used on the object.
     * @param object The target {@link TileObject}.
     */
    public void queueWidgetOnTileObject(Widget widget, TileObject object) {
        Point p;
        if (object instanceof GameObject) {
            GameObject gameObject = (GameObject) object;
            p = gameObject.getSceneMinLocation();
        } else {
            p = new Point(object.getLocalLocation().getSceneX(), object.getLocalLocation().getSceneY());
        }

        LocalPoint lp = new LocalPoint(p.getX(), p.getY(), client.getTopLevelWorldView());
        WorldPoint wp = WorldPoint.fromScene(client.getTopLevelWorldView(), lp.getX(), lp.getY(), object.getPlane());

        queueWidgetOnTileObject(
                object.getId(),
                wp.getX(),
                wp.getY(),
                widget.getIndex(),
                widget.getItemId(),
                widget.getId(),
                false
        );
    }
}