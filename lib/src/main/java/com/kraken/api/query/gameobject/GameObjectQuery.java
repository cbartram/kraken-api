package com.kraken.api.query.gameobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.GameObject;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GameObjectQuery extends AbstractQuery<GameObjectEntity, GameObjectQuery, TileObject> {

    public GameObjectQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<GameObjectEntity>> source() {
        return () -> {
            HashSet<TileObject> tileObjectHashSet = new HashSet<>();
            for (Tile[] tiles : ctx.getClient().getTopLevelWorldView().getScene().getTiles()[ctx.getClient().getTopLevelWorldView().getPlane()]) {
                if (tiles == null) {
                    continue;
                }
                for (Tile tile : tiles) {
                    if (tile == null) {
                        continue;
                    }
                    for (GameObject gameObject : tile.getGameObjects()) {
                        if (gameObject == null) {
                            continue;
                        }
                        if (gameObject.getId() == -1) {
                            continue;
                        }
                        tileObjectHashSet.add(gameObject);
                    }
                    if (tile.getGroundObject() != null) {
                        if (tile.getGroundObject().getId() == -1) {
                            continue;
                        }
                        tileObjectHashSet.add(tile.getGroundObject());
                    }
                    if (tile.getWallObject() != null) {
                        if (tile.getWallObject().getId() == -1) {
                            continue;
                        }
                        tileObjectHashSet.add(tile.getWallObject());
                    }
                    if (tile.getDecorativeObject() != null) {
                        if (tile.getDecorativeObject().getId() == -1) {
                            continue;
                        }
                        tileObjectHashSet.add(tile.getDecorativeObject());
                    }
                }
            }

            return tileObjectHashSet.stream().map(t -> new GameObjectEntity(ctx, t));
        };
    }

    /**
     * Filters for objects that have a specific action available.
     * Usage: ctx.objects().withAction("Mine").nearest().first();
     */
    public GameObjectQuery withAction(String action) {
        return filter(obj -> {
            String[] actions = obj.getObjectComposition().getActions();
            if (actions == null) return false;
            return Arrays.stream(actions).anyMatch(a -> a != null && a.equalsIgnoreCase(action));
        });
    }

    /**
     * Filters for only game objects which are reachable from the players current tile.
     * @return GroundObjectQuery
     */
    public GameObjectQuery reachable() {
        return filter(gameObject ->
                ctx.runOnClientThread(() -> ctx.getTileService().isTileReachable(gameObject.raw().getWorldLocation())));
    }

    /**
     * Filters for only objects whose location is within the specified distance from the anchor point.
     * @param anchor The anchor local point.
     * @param distance The maximum distance from the anchor point (in local units).
     * @return True if the object is within the specified distance from the anchor point, false otherwise.
     */
    public GameObjectQuery within(LocalPoint anchor, int distance) {
        return filter(obj -> {
            int dx = Math.abs(anchor.getX() - obj.raw().getLocalLocation().getX());
            int dy = Math.abs(anchor.getY() - obj.raw().getLocalLocation().getY());

            if (distance == 0) {
                return (dx == Perspective.LOCAL_TILE_SIZE && dy == 0) || (dy == Perspective.LOCAL_TILE_SIZE && dx == 0);
            } else {
                return obj.raw().getLocalLocation().distanceTo(anchor) <= distance;
            }
        });
    }

    /**
     * Filters for only objects whose location is within the specified distance from the players current local point.
     * @param distance The maximum distance from the anchor point (in local units).
     * @return True if the object is within the specified distance from the anchor point, false otherwise.
     */
    public GameObjectQuery within(int distance) {
        LocalPoint anchor = ctx.players().local().raw().getLocalLocation();
        return filter(obj -> {
            int dx = Math.abs(anchor.getX() - obj.raw().getLocalLocation().getX());
            int dy = Math.abs(anchor.getY() - obj.raw().getLocalLocation().getY());

            if (distance == 0) {
                return (dx == Perspective.LOCAL_TILE_SIZE && dy == 0) || (dy == Perspective.LOCAL_TILE_SIZE && dx == 0);
            } else {
                return obj.raw().getLocalLocation().distanceTo(anchor) <= distance;
            }
        });
    }

    /**
     * Filters by exact WorldPoint.
     */
    public GameObjectQuery at(WorldPoint point) {
        return filter(obj -> obj.raw().getWorldLocation().equals(point));
    }
}
