package com.kraken.api.interaction.groundobject;

import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.Packets.MousePackets;
import com.example.Packets.ObjectPackets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.inventory.InventoryService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.tile.TileService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class GroundObjectService extends AbstractService {
    
    @Inject
    private ReflectionService reflectionService;

    /**
     * Finds all tile objects
     * @return returns all tile objects on the ground
     */
    public List<TileObject> all() {
        return TileObjects.search().result();
    }

    /**
     * Finds all tile objects which match a given predicate.
     * @param filter A predicate to filter which tile objects are returned
     * @return Tile objects which pass the filter
     */
    public List<TileObject> get(Predicate<TileObject> filter) {
        return TileObjects.search().filter(filter).result();
    }

    /**
     * Finds all Tile objects with a given name
     * @param name The name of a tile object to find
     * @return Tile objects which match the passed name parameter.
     */
    public List<TileObject> findByName(String name) {
        return TileObjects.search().withName(name).result();
    }
    
    /**
     * Interacts with a ground item by performing a specified action using reflection
     *
     * @param groundItem The ground item to interact with.
     * @param action     The action to perform on the ground item.
     *
     * @return true if the interaction was successful, false otherwise.
     */
    private boolean interactReflect(GroundItem groundItem, String action) {
        if (groundItem == null) return false;
        try {
            int param0;
            int param1;
            int identifier;
            String target;
            MenuAction menuAction = MenuAction.CANCEL;
            ItemComposition item;

            item = context.runOnClientThreadOptional(() -> client.getItemDefinition(groundItem.getId())).orElse(null);
            if (item == null) return false;
            identifier = groundItem.getId();

            LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), groundItem.getLocation());
            if (localPoint == null) return false;

            param0 = localPoint.getSceneX();
            target = "<col=ff9040>" + groundItem.getName();
            param1 = localPoint.getSceneY();

            String[] groundActions = GroundItem.getGroundItemActions(item);

            int index = -1;
            for (int i = 0; i < groundActions.length; i++) {
                String groundAction = groundActions[i];
                if (groundAction == null || !groundAction.equalsIgnoreCase(action)) continue;
                index = i;
            }

            if (client.isWidgetSelected()) {
                menuAction = MenuAction.WIDGET_TARGET_ON_GROUND_ITEM;
            } else if (index == 0) {
                menuAction = MenuAction.GROUND_ITEM_FIRST_OPTION;
            } else if (index == 1) {
                menuAction = MenuAction.GROUND_ITEM_SECOND_OPTION;
            } else if (index == 2) {
                menuAction = MenuAction.GROUND_ITEM_THIRD_OPTION;
            } else if (index == 3) {
                menuAction = MenuAction.GROUND_ITEM_FOURTH_OPTION;
            } else if (index == 4) {
                menuAction = MenuAction.GROUND_ITEM_FIFTH_OPTION;
            }

            LocalPoint localPoint1 = LocalPoint.fromWorld(client.getTopLevelWorldView(), groundItem.getLocation());

            if (localPoint1 != null) {
                Polygon canvas = Perspective.getCanvasTilePoly(client, localPoint1);
                if (canvas != null) {
                    reflectionService.invokeMenuAction(param0, param1, menuAction.getId(), identifier, -1, action, target);
                }
            } else {
                reflectionService.invokeMenuAction(param0, param1, menuAction.getId(), identifier, -1, action, "");
            }
        } catch (Exception ex) {
            log.error("failed to interact with ground item: {}", ex.getMessage(), ex);
        }
        return true;
    }

    /**
     * Interacts with an item on the ground with the "Take" action using Reflection
     * @param groundItem Ground item to interact with
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interactReflect(GroundItem groundItem) {
        return interactReflect(groundItem, "Take");
    }

    /**
     * Interacts with an item on the ground given the items name using packets
     * @param name the item name to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(String name, String... actions) {
        return TileObjects.search().withName(name).first().flatMap(tileObject -> {
            MousePackets.queueClickPacket();
            ObjectPackets.queueObjectAction(tileObject, false, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Interacts with an item on the ground given the item id using Packets
     * @param id the id name to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(int id, String... actions) {
        return TileObjects.search().withId(id).first().flatMap(tileObject -> {
            MousePackets.queueClickPacket();
            ObjectPackets.queueObjectAction(tileObject, false, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Interacts with an item on the ground given a TileObject for the item using Packets
     * @param tileObject the tile object to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(TileObject tileObject, String... actions) {
        if (tileObject == null) {
            return false;
        }
        ObjectComposition comp = TileObjectQuery.getObjectComposition(tileObject);
        if (comp == null) {
            return false;
        }
        MousePackets.queueClickPacket();
        ObjectPackets.queueObjectAction(tileObject, false, actions);
        return true;
    }
}
