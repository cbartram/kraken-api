package com.kraken.api.interaction.groundobject;

import com.example.EthanApiPlugin.Collections.ETileItem;
import com.example.EthanApiPlugin.Collections.TileItems;
import com.example.EthanApiPlugin.Collections.TileObjects;
import com.example.EthanApiPlugin.Collections.query.TileObjectQuery;
import com.example.Packets.MousePackets;
import com.example.Packets.ObjectPackets;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.inventory.InventoryService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.tile.TileService;
import com.kraken.api.model.NewMenuEntry;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.GroundObjectDespawned;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.ui.overlay.OverlayUtil;
import net.runelite.client.util.RSTimeUnit;

import java.awt.*;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// TODO This class has potential for more helpful methods which can loot entire stacks of items, loot by item value, etc.
@Slf4j
@Singleton
public class GroundObjectService extends AbstractService {

    @Inject
    private ReflectionService reflectionService;

    @Inject
    private ItemManager itemManager;

    private final Table<WorldPoint, Integer, GroundItem> groundItems = HashBasedTable.create();
    private static final int COINS = 617;

    @Subscribe
    private void onItemSpawned(ItemSpawned itemSpawned) {
        TileItem item = itemSpawned.getItem();
        Tile tile = itemSpawned.getTile();

        GroundItem groundItem = buildGroundItem(tile, item);
        GroundItem existing = groundItems.get(tile.getWorldLocation(), item.getId());
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + groundItem.getQuantity());
        } else {
            groundItems.put(tile.getWorldLocation(), item.getId(), groundItem);
        }
    }

    @Subscribe
    private void onItemDespawned(ItemDespawned event) {
        TileItem item = event.getItem();
        Tile tile = event.getTile();
        groundItems.remove(tile.getWorldLocation(), item.getId());
    }

    /**
     * Builds a GroundItem object from a Tile and TileItem. The tile item is the actual object that is on the ground
     * however, data from the tile is also necessary to build a full representation
     * @param tile The tile the item is on
     * @param item The tile item which contains information about the item on the ground
     * @return A GroundItem object which represents the item on the ground
     */
    private GroundItem buildGroundItem(final Tile tile, final TileItem item) {
        int tickCount = context.runOnClientThreadOptional(() -> client.getTickCount()).orElse(0);
        final int itemId = item.getId();
        final ItemComposition itemComposition = itemManager.getItemComposition(itemId);
        final int realItemId = itemComposition.getNote() != -1 ? itemComposition.getLinkedNoteId() : itemId;
        final int alchPrice = itemComposition.getHaPrice();
        final int despawnTime = item.getDespawnTime() - tickCount;
        final int visibleTime = item.getVisibleTime() - tickCount;
        final String key = String.format("%d-%d-%d-%d-%d",
                item.getId(),
                item.getQuantity(),
                tile.getWorldLocation().getX(),
                tile.getWorldLocation().getY(),
                tile.getPlane());

        final GroundItem groundItem = GroundItem.builder()
                .id(itemId)
                .key(key)
                .itemComposition(itemComposition)
                .tileObject(tile.getItemLayer())
                .location(tile.getWorldLocation())
                .itemId(realItemId)
                .quantity(item.getQuantity())
                .name(itemComposition.getName())
                .haPrice(alchPrice)
                .height(tile.getItemLayer().getHeight())
                .tradeable(itemComposition.isTradeable())
                .ownership(item.getOwnership())
                .isPrivate(item.isPrivate())
                .spawnTime(Instant.now())
                .stackable(itemComposition.isStackable())
                .despawnTime(Duration.of(despawnTime, RSTimeUnit.GAME_TICKS))
                .visibleTime(Duration.of(visibleTime, RSTimeUnit.GAME_TICKS))
                .build();

        if (realItemId == COINS) {
            groundItem.setHaPrice(1);
            groundItem.setGePrice(1);
        } else {
            groundItem.setGePrice(itemManager.getItemPrice(realItemId));
        }

        return groundItem;
    }

    /**
     * Finds all tile objects
     * @return returns all tile objects on the ground
     */
    public List<GroundItem> all() {
        return context.runOnClientThreadOptional(() -> new ArrayList<>(groundItems.values())).orElse(new ArrayList<>());
    }

    /**
     * Finds all tile objects which match a given predicate.
     * @param filter A predicate to filter which tile objects are returned
     * @return Tile objects which pass the filter
     */
    public List<GroundItem> all(Predicate<GroundItem> filter) {
        return context.runOnClientThreadOptional(() -> groundItems.values().stream().filter(filter).collect(Collectors.toList())).orElse(new ArrayList<>());
    }

    /**
     * Finds all Tile objects with a given id.
     * @param id The id of the ground item to find.
     * @return Ground items which match the passed id parameter.
     */
    public GroundItem get(int id) {
        return context.runOnClientThreadOptional(() -> groundItems.values().stream().filter(g -> g != null && g.getId() == id)
                .findFirst()
                .orElse(null))
                .orElse(null);
    }

    /**
     * Finds all Tile objects with a given name which matches exactly. This is still case-insensitive.
     * @param name The name of a tile object to find
     * @return Tile objects which match the passed name parameter.
     */
    public GroundItem get(String name) {
        return get(name, true);
    }

    /**
     * Finds all Tile objects with a given name
     * @param name The name of a tile object to find
     * @param exact True if the name needs to match exactly, false if a partial match is sufficient, case-insensitive.
     * @return Tile objects which match the passed name parameter.
     */
    public GroundItem get(String name, boolean exact) {
        return context.runOnClientThreadOptional(() -> groundItems.values().stream().filter(g -> {
            if(g == null) {
                return false;
            }

            if(exact) {
                return g.getName().equalsIgnoreCase(name);
            } else {
                return g.getName().toLowerCase().contains(name.toLowerCase());
            }
        }).findFirst().orElse(null)).orElse(null);
    }

    /**
     * Performs the "Take" action on a ground item using reflection
     * @param item The ground item to interact with
     * @return true if the interaction was successful, false otherwise.
     */
    public boolean interactReflect(GroundItem item) {
        return interactReflect(item, "Take");
    }

    /**
     * Interacts with a ground item by performing a specified action using reflection
     *
     * @param groundItem The ground item to interact with.
     * @param action     The action to perform on the ground item. i.e "Take", "Examine", etc...
     *
     * @return true if the interaction was successful, false otherwise.
     */
    public boolean interactReflect(GroundItem groundItem, String action) {
        if(!context.isHooksLoaded()) return false;
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

            String[] groundActions = GroundItem.getGroundItemActions(groundItem.getItemComposition());

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

            Polygon canvas = Perspective.getCanvasTilePoly(client, localPoint);
            if (canvas != null) {
                reflectionService.invokeMenuAction(param0, param1, menuAction.getId(), identifier, -1, action, target);
            }
        } catch (Exception ex) {
            log.error("failed to interact with ground item: {}", groundItem.getName(), ex);
        }
        return true;
    }


    /**
     * Interacts with an item on the ground given the items name using packets
     * @param name the item name to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(String name, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThreadOptional(() -> get(name)).flatMap(g -> {
            MousePackets.queueClickPacket();
            ObjectPackets.queueObjectAction(g.getTileObject(), false, actions);
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
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThreadOptional(() -> get(id)).flatMap(g -> {
            MousePackets.queueClickPacket();
            ObjectPackets.queueObjectAction(g.getTileObject(), false, actions);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Interacts with an item on the ground given the item id using Packets
     * @param item The ground item to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(GroundItem item, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThreadOptional(() -> groundItems.get(item.getLocation(), item.getId())).flatMap(g -> {
            MousePackets.queueClickPacket();
            ObjectPackets.queueObjectAction(g.getTileObject(), false, actions);
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
        if(!context.isPacketsLoaded()) return false;
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
