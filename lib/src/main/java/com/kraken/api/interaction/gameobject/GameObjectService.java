package com.kraken.api.interaction.gameobject;


import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.camera.CameraService;
import com.kraken.api.interaction.ui.UIService;
import com.kraken.api.model.NewMenuEntry;
import com.kraken.api.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class GameObjectService extends AbstractService {

    /**
     * Extracts all {@link GameObject}s located on a given {@link Tile}.
     *
     * @see Tile#getGameObjects()
     * @param tile the tile from which to extract game objects
     * @return a {@link List} of {@link GameObject} instances on the tile (never null)
     */
    private static final Function<Tile, Collection<? extends GameObject>> GAMEOBJECT_EXTRACTOR =
            tile -> Arrays.asList(tile.getGameObjects());

    /**
     * Extracts all types of {@link TileObject} (decorative, ground, wall) from a given {@link Tile}.
     *
     * @param tile the tile from which to extract all tile objects
     * @return a {@link List} containing the {@link DecorativeObject}, {@link GroundObject},
     *         and {@link WallObject} (some entries may be null if that object is not present)
     */
    private static final Function<Tile, Collection<? extends TileObject>> TILEOBJECT_EXTRACTOR =
            tile -> Arrays.asList(
                    tile.getDecorativeObject(),
                    tile.getGroundObject(),
                    tile.getWallObject()
            );

    @Inject
    private UIService uiService;

    @Inject
    private CameraService cameraService;

    public boolean interact(WorldPoint worldPoint) {
        return interact(worldPoint, "");
    }

    public boolean interact(WorldPoint worldPoint, String action) {
        TileObject gameObject = getAll(o -> o.getWorldLocation().equals(worldPoint)).stream().findFirst().orElse(null);
        return clickObject(gameObject, action);
    }

    public boolean interact(GameObject gameObject) {
        return clickObject(gameObject);
    }

    public boolean interact(TileObject tileObject) {
        return clickObject(tileObject, "");
    }

    public boolean interact(TileObject tileObject, String action) {
        return clickObject(tileObject, action);
    }

    public boolean interact(GameObject gameObject, String action) {
        return clickObject(gameObject, action);
    }

    public boolean interact(int id) {
        TileObject object = getAll(o -> o.getId() == id).stream().findFirst().orElse(null);
        return clickObject(object);
    }

    private boolean clickObject(TileObject object) {
        return clickObject(object, "");
    }


    public List<TileObject> getAll() {
        return getAll(o -> true);
    }

    public <T extends TileObject> List<TileObject> getAll(Predicate<? super T> predicate) {
        return getAll(predicate, Constants.SCENE_SIZE);
    }

    public <T extends TileObject> List<TileObject> getAll(Predicate<? super T> predicate, int distance) {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return Collections.emptyList();
        }
        return getAll(predicate, player.getWorldLocation(), distance);
    }

    private static boolean isWithinTiles(LocalPoint anchor, LocalPoint objLoc, int distance) {
        int dx = Math.abs(anchor.getX() - objLoc.getX());
        int dy = Math.abs(anchor.getY() - objLoc.getY());

        if (distance == 0) {
            return (dx == Perspective.LOCAL_TILE_SIZE && dy == 0) || (dy == Perspective.LOCAL_TILE_SIZE && dx == 0);
        } else {
            return objLoc.distanceTo(anchor) <= distance;
        }
    }

    private <T extends TileObject> Predicate<T> withinTilesPredicate(int distance, LocalPoint anchor) {
        return to -> isWithinTiles(anchor, to.getLocalLocation(), distance);
    }

    public <T extends TileObject> List<TileObject> getAll(Predicate<? super T> predicate, WorldPoint anchor, int distance) {
        List<TileObject> all = new ArrayList<>();
        all.addAll(fetchGameObjects(predicate, anchor, distance));
        all.addAll(fetchTileObjects(predicate, anchor, distance));
        return all;
    }

    @SuppressWarnings("unchecked")
    private <T extends TileObject> List<T> fetchTileObjects(Predicate<? super T> predicate, WorldPoint anchor, int distance) {
        return (List<T>) getTileObjects((Predicate<TileObject>) predicate, anchor, distance);
    }


    @SuppressWarnings("unchecked")
    private <T extends TileObject> List<T> fetchGameObjects(Predicate<? super T> predicate, WorldPoint anchor, int distance) {
        return (List<T>) getGameObjects((Predicate<GameObject>) predicate, anchor, distance);
    }

    public List<TileObject> getTileObjects(Predicate<TileObject> predicate, WorldPoint anchor, int distance) {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return Collections.emptyList();
        }
        LocalPoint anchorLocal = LocalPoint.fromWorld(player.getWorldView(), anchor);
        if (anchorLocal == null) {
            return Collections.emptyList();
        }
        return getTileObjects(predicate, anchorLocal, distance * Perspective.LOCAL_TILE_SIZE);
    }

    public List<TileObject> getTileObjects(Predicate<TileObject> predicate, LocalPoint anchorLocal, int distance) {
        return getSceneObjects(TILEOBJECT_EXTRACTOR, predicate, anchorLocal, distance);
    }

    public List<GameObject> getGameObjects(Predicate<GameObject> predicate, LocalPoint anchorLocal, int distance) {
        return getSceneObjects(GAMEOBJECT_EXTRACTOR, predicate, anchorLocal, distance);
    }

    public List<GameObject> getGameObjects(Predicate<GameObject> predicate, WorldPoint anchor, int distance) {
        Player player = client.getLocalPlayer();
        if (player == null) {
            return Collections.emptyList();
        }
        LocalPoint anchorLocal = LocalPoint.fromWorld(player.getWorldView(), anchor);
        if (anchorLocal == null) {
            return Collections.emptyList();
        }
        return getGameObjects(predicate, anchorLocal, distance * Perspective.LOCAL_TILE_SIZE);
    }

    private <T extends TileObject> List<T> getSceneObjects(Function<Tile, Collection<? extends T>> extractor, Predicate<T> predicate, LocalPoint anchorLocal, int distance) {
        if (distance > Constants.SCENE_SIZE * Perspective.LOCAL_TILE_SIZE) {
            distance = Constants.SCENE_SIZE * Perspective.LOCAL_TILE_SIZE;
        }

        return getSceneObjects(extractor)
                .filter(withinTilesPredicate(distance, anchorLocal))
                .filter(predicate)
                .sorted(Comparator.comparingInt(o -> o.getLocalLocation().distanceTo(anchorLocal)))
                .collect(Collectors.toList());
    }

    private <T extends TileObject> Stream<T> getSceneObjects(Function<Tile, Collection<? extends T>> extractor) {
        Player player = client.getLocalPlayer();
        if (player == null) return Stream.empty();

        Scene scene = player.getWorldView().getScene();
        Tile[][][] tiles = scene.getTiles();
        if (tiles == null) return Stream.empty();

        List<T> result = new ArrayList<>();
        int z = player.getWorldView().getPlane();

        for (int x = 0; x < Constants.SCENE_SIZE; x++) {
            for (int y = 0; y < Constants.SCENE_SIZE; y++) {
                Tile tile = tiles[z][x][y];
                if (tile == null) continue;

                Collection<? extends T> objs = extractor.apply(tile);
                if (objs != null) {
                    for (T obj : objs) {
                        if (obj == null) continue;

                        if (obj instanceof GameObject) {
                            GameObject gameObject = (GameObject) obj;
                            if (gameObject.getSceneMinLocation().equals(tile.getSceneLocation())) {
                                result.add(obj);
                            }
                        } else {
                            if (obj.getLocalLocation().equals(tile.getLocalLocation())) {
                                result.add(obj);
                            }
                        }
                    }
                }
            }
        }

        return result.stream();
    }


    @Nullable
    public <T extends TileObject> ObjectComposition convertToObjectComposition(T object) {
        return convertToObjectComposition(object.getId(), false);
    }

    @Nullable
    public ObjectComposition convertToObjectComposition(int id, boolean ignoreImpostor) {
        return convertToObjectCompositionInternal(id, ignoreImpostor);
    }

    @Nullable
    private ObjectComposition convertToObjectCompositionInternal(int objectId, boolean ignoreImpostor) {
        return context.runOnClientThreadOptional(() -> {
            ObjectComposition comp = client.getObjectDefinition(objectId);
            if (comp == null) return null;
            return (ignoreImpostor || comp.getImpostorIds() == null) ? comp : comp.getImpostor();
        }).orElse(null);
    }

    private boolean clickObject(TileObject object, String action) {
        if (object == null) return false;
        if (client.getLocalPlayer().getWorldLocation().distanceTo(object.getWorldLocation()) > 51) {
            log.info("Object to far from player. Cannot interact");
            return false;
        }

        try {
            int param0;
            int param1;
            MenuAction menuAction = MenuAction.WALK;

            ObjectComposition objComp = convertToObjectComposition(object);
            if (objComp == null) return false;

            if (object instanceof GameObject) {
                GameObject obj = (GameObject) object;
                if (obj.sizeX() > 1) {
                    param0 = obj.getLocalLocation().getSceneX() - obj.sizeX() / 2;
                } else {
                    param0 = obj.getLocalLocation().getSceneX();
                }

                if (obj.sizeY() > 1) {
                    param1 = obj.getLocalLocation().getSceneY() - obj.sizeY() / 2;
                } else {
                    param1 = obj.getLocalLocation().getSceneY();
                }
            } else {
                // Default objects like walls, groundobjects, decorationobjects etc...
                param0 = object.getLocalLocation().getSceneX();
                param1 = object.getLocalLocation().getSceneY();
            }

            int index = 0;
            if (action != null) {
                String[] actions;
                if (objComp.getImpostorIds() != null && objComp.getImpostor() != null) {
                    actions = objComp.getImpostor().getActions();
                } else {
                    actions = objComp.getActions();
                }

                for (int i = 0; i < actions.length; i++) {
                    if (actions[i] == null) continue;
                    if (action.equalsIgnoreCase(StringUtils.stripColTags(actions[i]))) {
                        index = i;
                        break;
                    }
                }

                if (index == actions.length)
                    index = 0;
            }

            if (index == -1) {
                log.error("Failed to interact with object: {}, action: {}", object.getId(), action);
            }


            if (client.isWidgetSelected()) {
                menuAction = MenuAction.WIDGET_TARGET_ON_GAME_OBJECT;
            } else if (index == 0) {
                menuAction = MenuAction.GAME_OBJECT_FIRST_OPTION;
            } else if (index == 1) {
                menuAction = MenuAction.GAME_OBJECT_SECOND_OPTION;
            } else if (index == 2) {
                menuAction = MenuAction.GAME_OBJECT_THIRD_OPTION;
            } else if (index == 3) {
                menuAction = MenuAction.GAME_OBJECT_FOURTH_OPTION;
            } else if (index == 4) {
                menuAction = MenuAction.GAME_OBJECT_FIFTH_OPTION;
            }

            if (!cameraService.isTileOnScreen(object.getLocalLocation())) {
                cameraService.turnTo(object);
            }

            // TODO Reflection object interaction
            context.doInvoke(new NewMenuEntry(param0, param1, menuAction.getId(), object.getId(), -1, action, objComp.getName(), object), uiService.getObjectClickbox(object));
        } catch (Exception ex) {
            log.error("Failed to interact with object: {}", ex.getMessage());
        }

        return true;
    }
}
