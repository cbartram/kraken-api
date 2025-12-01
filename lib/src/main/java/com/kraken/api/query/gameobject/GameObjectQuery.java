package com.kraken.api.query.gameobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GameObjectQuery extends AbstractQuery<GameObjectEntity, GameObjectQuery, GameObject> {

    public GameObjectQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<GameObjectEntity>> source() {
        return () -> {
            List<GameObject> tileObjects = new ArrayList<>();
            WorldPoint playerLoc = ctx.getClient().getLocalPlayer().getWorldLocation();

            Scene scene = ctx.getClient().getTopLevelWorldView().getScene();
            Tile[][][] tiles = scene.getTiles();
            int plane = ctx.getClient().getTopLevelWorldView().getPlane();

            for (int x = 0; x < Constants.SCENE_SIZE; x++) {
                for (int y = 0; y < Constants.SCENE_SIZE; y++) {
                    Tile tile = tiles[plane][x][y];
                    if (tile == null) continue;

                    // Ignore game objects that are on the player tile
                    if(tile.getWorldLocation().getX() == playerLoc.getX() && tile.getWorldLocation().getY() == playerLoc.getY()) {
                        continue;
                    }

                    GameObject[] gameObjects = tile.getGameObjects();
                    if (gameObjects != null) {
                        for (GameObject gameObject : gameObjects) {
                            if (gameObject != null && gameObject.getId() != -1) {
                                tileObjects.add(gameObject);
                            }
                        }
                    }
                }
            }

            return tileObjects.stream().map(t -> new GameObjectEntity(ctx, t));
        };
    }

    /**
     * Filters game objects to only contain objects which can be interacted with (non Examine) actions. i.e.
     * Trees, Ore, Doors, Stairs etc...
     * @return GameObjectQuery
     */
    public GameObjectQuery interactable() {
        return filter(gameObject ->  {
            String[] rawActions = gameObject.getObjectComposition().getActions();
            if(rawActions == null || rawActions.length == 0) return false;
            return Arrays.stream(rawActions)
                    .filter(Objects::nonNull)
                    .filter(s -> !s.isEmpty())
                    .anyMatch(s -> !s.equalsIgnoreCase("examine"));
        });
    }

    /**
     * Filters for objects that have a specific action available.
     * Usage: ctx.objects().withAction("Mine").nearest().first();
     * @param action The action to check for i.e "Mine", "Chop", "Examine".
     * @return GameObjectQuery
     */
    public GameObjectQuery withAction(String action) {
        return filter(obj -> {
            if (obj.getObjectComposition() == null) return false;
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
        return filter(gameObject -> gameObject.raw() != null && ctx.getTileService().isObjectReachable(gameObject.raw()));
    }

    /**
     * Sorts the stream of game objects to order them by manhattan distance to the local player.
     * @return GameObjectQuery
     */
    public GameObjectQuery nearest() {
        final WorldPoint playerLoc = ctx.players().local().raw().getWorldLocation();
        return sorted(Comparator.comparingInt(obj -> obj.raw().getWorldLocation().distanceTo(playerLoc)));
    }

    /**
     * Filters for only objects whose location is within the specified distance from the anchor point.
     * @param anchor The anchor local point.
     * @param distance The maximum distance from the anchor point (in local units).
     * @return True if the object is within the specified distance from the anchor point, false otherwise.
     */
    public GameObjectQuery within(LocalPoint anchor, int distance) {
        int range = distance * Perspective.LOCAL_TILE_SIZE;
        return filter(obj -> obj.raw().getLocalLocation().distanceTo(anchor) <= range);
    }

    /**
     * Filters for only objects whose location is within the specified distance from the players current local point.
     * @param distance The maximum distance from the anchor point (in local units).
     * @return True if the object is within the specified distance from the anchor point, false otherwise.
     */
    public GameObjectQuery within(int distance) {
        LocalPoint anchor = ctx.players().local().raw().getLocalLocation();
        int range = distance * Perspective.LOCAL_TILE_SIZE;

        return filter(obj -> obj.raw().getLocalLocation().distanceTo(anchor) <= range);
    }

    /**
     * Filters by exact WorldPoint.
     * @param point The world point to filter for entities on
     * @return GameObjectQuery
     */
    public GameObjectQuery at(WorldPoint point) {
        return filter(obj -> obj.raw().getWorldLocation().equals(point));
    }
}
