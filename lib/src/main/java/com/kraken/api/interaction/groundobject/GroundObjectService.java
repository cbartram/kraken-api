package com.kraken.api.interaction.groundobject;

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

@Slf4j
@Singleton
public class GroundObjectService extends AbstractService {
    
    @Inject
    private ReflectionService reflectionService;

    @Inject
    private InventoryService inventoryService;

    @Inject
    private TileService tileService;
    
    /**
     * Interacts with a ground item by performing a specified action.
     *
     * @param groundItem The ground item to interact with.
     * @param action     The action to perform on the ground item.
     *
     * @return true if the interaction was successful, false otherwise.
     */
    private boolean interact(GroundItem groundItem, String action) {
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
     * Interacts with an item on the ground with the "Take" action.
     * @param groundItem Ground item to interact with
     * @return
     */
    public boolean interact(GroundItem groundItem) {
        return interact(groundItem, "Take");
    }

    /**
     * Loots all ground items within a given range from the players local location.
     * @param itemId Item id to loot
     * @param range int range to search
     * @return
     */
    public boolean loot(int itemId, int range) {
        if (inventoryService.isFull()) return false;
        final GroundItem item = Arrays.stream(getAll(range))
                .filter(i -> i.getId() == itemId)
                .findFirst().orElse(null);
        return interact(item);
    }

    /**
     * Returns an array of ground items within a given range from the players local position.
     * @param range int search radius.
     * @return
     */
    public GroundItem[] getAll(int range) {
        return getAllFromWorldPoint(range, client.getLocalPlayer().getWorldLocation());
    }

    /**
     * Retrieves all GroundItem objects within a specified range of a WorldPoint, sorted by distance.
     *
     * @param range The radius in tiles to search around the given world point
     * @param worldPoint The center WorldPoint to search around
     * @return An array of GroundItem objects found within the specified range, sorted by proximity
     *         to the center point (closest first). Returns an empty array if no items are found.
     */
    public GroundItem[] getAllFromWorldPoint(int range, WorldPoint worldPoint) {
        if (worldPoint == null) return new GroundItem[0];

        return context.runOnClientThreadOptional(() -> {
            List<GroundItem> temp = new ArrayList<>();
            final int pX = worldPoint.getX();
            final int pY = worldPoint.getY();
            final int minX = pX - range, minY = pY - range;
            final int maxX = pX + range, maxY = pY + range;
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (GroundItem item : getAllAt(x, y)) {
                        if (item == null) continue;
                        temp.add(item);
                    }
                }
            }

            // Sort by closest item first
            return temp.stream().sorted(Comparator.comparingInt(value -> Objects.requireNonNull(LocalPoint.fromWorld(client.getTopLevelWorldView(), value.getLocation()))
                            .distanceTo(client.getLocalPlayer().getLocalLocation())))
                    .toArray(GroundItem[]::new);
        }).orElse(new GroundItem[0]);
    }

    /**
     * Returns all the ground items at a tile on the current plane.
     *
     * @param x The x position of the tile in the world.
     * @param y The y position of the tile in the world.
     *
     * @return An array of the ground items on the specified tile.
     */
    public GroundItem[] getAllAt(int x, int y) {
        return (GroundItem[]) context.runOnClientThreadOptional(() -> {
            final Tile tile = tileService.getTile(x, y);
            if (tile == null) return new GroundItem[0];

            List<TileItem> groundItems = tile.getGroundItems();
            if (groundItems == null) return new GroundItem[0];
            return groundItems.toArray(TileItem[]::new);
        }).orElse(new GroundItem[0]);
    }
}
