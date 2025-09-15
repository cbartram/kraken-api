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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO This class has potential for more helpful methods which can loot entire stacks of items, loot by item value, etc.
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
        return context.runOnClientThreadOptional(() -> TileObjects.search().result()).orElse(new ArrayList<>());
    }

    /**
     * Finds all tile objects which match a given predicate.
     * @param filter A predicate to filter which tile objects are returned
     * @return Tile objects which pass the filter
     */
    public List<TileObject> get(Predicate<TileObject> filter) {
        return context.runOnClientThreadOptional(() -> TileObjects.search().filter(filter).result()).orElse(new ArrayList<>());
    }

    /**
     * Finds all Tile objects with a given name
     * @param name The name of a tile object to find
     * @return Tile objects which match the passed name parameter.
     */
    public List<TileObject> get(String name) {
        return context.runOnClientThreadOptional(() -> TileObjects.search().withName(name).result()).orElse(new ArrayList<>());
    }

    /**
     * Returns a list of actions which can be performed on the Tile object. Generally this will be "Take" or "Examine".
     * @param object The tile object to get actions for
     * @return A list of actions which can be performed on the tile object
     */
    @SneakyThrows
    public List<String> getTileObjectActions(TileObject object) {
        List<Field> fields = Arrays.stream(object.getClass().getFields()).filter(x -> x.getType().isArray()).collect(Collectors.toList());
        for (Field field : fields) {
            if (field.getType().getComponentType().getName().equals("java.lang.String")) {
                String[] actions = (String[]) field.get(object);
                if (Arrays.stream(actions).anyMatch(x -> x != null && x.equalsIgnoreCase("take"))) {
                    field.setAccessible(true);
                    return List.of(actions);
                }
            }
        }
        return Collections.emptyList();
    }

    /**
     * Interacts with a tile object by performing a specified action using reflection
     *
     * @param tileObject The tile object to interact with.
     * @param name The name of the tile object: i.e. "Bones", "Coins", "Bronze arrow", etc...
     * @param action     The action to perform on the ground item. i.e "Take", "Examine", etc...
     *
     * @return true if the interaction was successful, false otherwise.
     */
    public boolean interactReflect(TileObject tileObject, String name, String action) {
        if (tileObject == null) return false;
        try {
            int param0;
            int param1;
            int identifier;
            String target;
            MenuAction menuAction = MenuAction.CANCEL;
            ItemComposition item;

            item = context.runOnClientThreadOptional(() -> client.getItemDefinition(tileObject.getId())).orElse(null);
            if (item == null) return false;
            identifier = tileObject.getId();

            LocalPoint localPoint = tileObject.getLocalLocation();

            param0 = localPoint.getSceneX();
            target = "<col=ff9040>" + name;
            param1 = localPoint.getSceneY();

            List<String> groundActions = getTileObjectActions(tileObject);

            int index = -1;
            for (int i = 0; i < groundActions.size(); i++) {
                String groundAction = groundActions.get(i);
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

            Polygon canvas = Perspective.getCanvasTilePoly(client, localPoint);
            if (canvas != null) {
                reflectionService.invokeMenuAction(param0, param1, menuAction.getId(), identifier, -1, action, target);
            }
        } catch (Exception ex) {
            log.error("failed to interact with ground item: {}", ex.getMessage(), ex);
        }
        return true;
    }

    /**
     * Interacts with an item on the ground with the "Take" action using Reflection
     * @param object Tile Object to interact with
     * @param name the item name to interact with i.e. "Bones", "Coins", "Bronze arrow", etc...
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interactReflect(TileObject object, String name) {
        return interactReflect(object, name, "Take");
    }

    /**
     * Interacts with an item on the ground given the items name using packets
     * @param name the item name to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(String name, String... actions) {
        return context.runOnClientThreadOptional(() -> TileObjects.search().withName(name).first().flatMap(tileObject -> {
            MousePackets.queueClickPacket();
            ObjectPackets.queueObjectAction(tileObject, false, actions);
            return Optional.of(true);
        }).orElse(false)).orElse(false);
    }

    /**
     * Interacts with an item on the ground given the item id using Packets
     * @param id the id name to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(int id, String... actions) {
        return context.runOnClientThreadOptional(() -> TileObjects.search().withId(id).first().flatMap(tileObject -> {
            MousePackets.queueClickPacket();
            ObjectPackets.queueObjectAction(tileObject, false, actions);
            return Optional.of(true);
        }).orElse(false)).orElse(false);
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
        ObjectComposition comp = context.runOnClientThreadOptional(() -> TileObjectQuery.getObjectComposition(tileObject)).orElse(null);
        if (comp == null) {
            return false;
        }

        context.runOnClientThread(() -> {
            MousePackets.queueClickPacket();
            ObjectPackets.queueObjectAction(tileObject, false, actions);
        });
        return true;
    }
}
