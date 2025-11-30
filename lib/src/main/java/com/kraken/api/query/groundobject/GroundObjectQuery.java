package com.kraken.api.query.groundobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.ItemComposition;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.TileItem;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.util.RSTimeUnit;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GroundObjectQuery extends AbstractQuery<GroundObjectEntity, GroundObjectQuery, GroundItem> {
    private static final int COINS = 617;

    public GroundObjectQuery(Context ctx) {
        super(ctx);
    }

    /**
     * Builds a GroundItem object from a Tile and TileItem. The tile item is the actual object that is on the ground
     * however, data from the tile is also necessary to build a full representation
     *
     * @param tile The tile the item is on
     * @param item The tile item which contains information about the item on the ground
     * @return A GroundItem object which represents the item on the ground
     */
    private GroundItem buildGroundItem(final Tile tile, final TileItem item) {
        int tickCount = ctx.runOnClientThreadOptional(() -> ctx.getClient().getTickCount()).orElse(0);
        final int itemId = item.getId();
        final ItemComposition itemComposition = ctx.getItemManager().getItemComposition(itemId);
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
            groundItem.setGePrice(ctx.getItemManager().getItemPrice(realItemId));
        }

        return groundItem;
    }


    @Override
    protected Supplier<Stream<GroundObjectEntity>> source() {
        return () -> {
            HashSet<GroundItem> groundItems = new HashSet<>();
            for (Tile[] tiles : ctx.getClient().getTopLevelWorldView().getScene().getTiles()[ctx.getClient().getTopLevelWorldView().getPlane()]) {
                if (tiles == null) {
                    continue;
                }

                for (Tile tile : tiles) {
                    if (tile == null) {
                        continue;
                    }

                    if (tile.getGroundItems() != null) {
                        for (TileItem groundItem : tile.getGroundItems()) {
                            if (groundItem == null) {
                                continue;
                            }
                            groundItems.add(buildGroundItem(tile, groundItem));
                        }
                    }
                }
            }

            return groundItems.stream().map(groundItem -> new GroundObjectEntity(ctx, groundItem));
        };
    }

    /**
     * Filters for only objects whose location is within the specified distance from the anchor point.
     * @param anchor The anchor local point.
     * @param distance The maximum distance from the anchor point (in local units).
     * @return True if the object is within the specified distance from the anchor point, false otherwise.
     */
    public GroundObjectQuery within(LocalPoint anchor, int distance) {
        return filter(obj -> {
            LocalPoint pt = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), obj.raw().getLocation());
            if(pt == null) return false;

            int dx = Math.abs(anchor.getX() - pt.getX());
            int dy = Math.abs(anchor.getY() - pt.getY());

            if (distance == 0) {
                return (dx == Perspective.LOCAL_TILE_SIZE && dy == 0) || (dy == Perspective.LOCAL_TILE_SIZE && dx == 0);
            } else {
                return pt.distanceTo(anchor) <= distance;
            }
        });
    }

    /**
     * Filters for only objects whose location is within the specified distance from the players current local point.
     * @param distance The maximum distance from the anchor point (in local units).
     * @return True if the object is within the specified distance from the anchor point, false otherwise.
     */
    public GroundObjectQuery within(int distance) {
        LocalPoint anchor = ctx.players().local().raw().getLocalLocation();
        return filter(obj -> {
            LocalPoint pt = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), obj.raw().getLocation());
            if(pt == null) return false;

            int dx = Math.abs(anchor.getX() - pt.getX());
            int dy = Math.abs(anchor.getY() - pt.getY());

            if (distance == 0) {
                return (dx == Perspective.LOCAL_TILE_SIZE && dy == 0) || (dy == Perspective.LOCAL_TILE_SIZE && dx == 0);
            } else {
                return pt.distanceTo(anchor) <= distance;
            }
        });
    }

    /**
     * Filters by exact WorldPoint.
     * @param point The world point at which to find ground objects on
     * @return GroundObjectQuery
     */
    public GroundObjectQuery at(WorldPoint point) {
        return filter(obj -> obj.raw().getLocation().equals(point));
    }

    /**
     * Filters for only ground items which are reachable from the players current tile.
     * @return GroundObjectQuery
     */
    public GroundObjectQuery reachable() {
        return filter(groundItem ->
                ctx.runOnClientThread(() -> ctx.getTileService().isTileReachable(groundItem.raw().getLocation())));
    }
}

