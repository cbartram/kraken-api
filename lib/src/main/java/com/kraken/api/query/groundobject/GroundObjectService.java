package com.kraken.api.query.groundobject;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.core.packet.entity.GameObjectPackets;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.ui.UIService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ItemDespawned;
import net.runelite.api.events.ItemSpawned;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;
import net.runelite.client.util.RSTimeUnit;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

// TODO This class has potential for more helpful methods which can loot entire stacks of items, loot by item value, etc.
@Slf4j
@Singleton
@Deprecated
public class GroundObjectService extends AbstractService {

    @Inject
    private ItemManager itemManager;

    @Inject
    private TileService tileService;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private GameObjectPackets objectPackets;

    @Inject
    private UIService uiService;

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
     * Checks if the ground item can be reached from the players position
     * @param item The ground item to check
     * @return True if the ground item can be reached from the players position and false otherwise.
     */
    public boolean canReach(GroundItem item) {
        return context.runOnClientThread(() -> tileService.isTileReachable(item.getLocation()));
    }

    /**
     * Interacts with an item on the ground given the items name using packets
     * @param name the item name to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(String name, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThread(() -> {
            GroundItem groundItem = get(name);
            if (groundItem == null) return false;

            Point clickingPoint = uiService.getClickbox(groundItem.getTileObject());
            mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
            objectPackets.queueObjectAction(groundItem.getTileObject(), false, actions);
            return true;
        });
    }

    /**
     * Interacts with an item on the ground given the item id using Packets
     * @param id the id name to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(int id, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThread(() -> {
            GroundItem item = get(id);
            if(item == null) return false;
            Point clickingPoint = uiService.getClickbox(item.getTileObject());
            mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
            objectPackets.queueObjectAction(item.getTileObject(), false, actions);
            return true;
        });
    }

    /**
     * Interacts with an item on the ground given the item id using Packets
     * @param item The ground item to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(GroundItem item, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        return context.runOnClientThread(() -> {
            GroundItem groundItem = groundItems.get(item.getLocation(), item.getId());
            if(groundItem == null) return false;

            Point clickingPoint = uiService.getClickbox(groundItem.getTileObject());
            mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
            objectPackets.queueObjectAction(groundItem.getTileObject(), false, actions);
            return true;
        });
    }

    /**
     * Interacts with an item on the ground given a TileObject for the item using Packets
     * @param tileObject the tile object to interact with
     * @param actions The actions to perform, usually "Take"
     * @return True when the interaction was successful and false otherwise
     */
    public boolean interact(TileObject tileObject, String... actions) {
        if(!context.isPacketsLoaded()) return false;
        if (tileObject == null) return false;

        return context.runOnClientThread(() -> {
            ObjectComposition comp = tileService.getObjectComposition(tileObject);
            if (comp == null) return false;

            Point clickingPoint = uiService.getClickbox(tileObject);
            mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
            objectPackets.queueObjectAction(tileObject, false, actions);
            return true;
        });
    }
}
