package com.kraken.api.interaction.gameobject;


import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.camera.CameraService;
import com.kraken.api.interaction.reflect.ReflectionService;
import com.kraken.api.interaction.ui.UIService;
import com.kraken.api.model.NewMenuEntry;
import com.kraken.api.util.StringUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class GameObjectService extends AbstractService {

    @Inject
    private ReflectionService reflectionService;

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
            log.warn("No game objects found, local player null");
            return Collections.emptyList();
        }
        LocalPoint anchorLocal = LocalPoint.fromWorld(player.getWorldView(), anchor);
        if (anchorLocal == null) {
            log.warn("No game objects found cannot determine anchor from local play position");
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

    /**
     * Finds a reachable game object by name within a specified distance from an anchor point.
     *
     * @param objectName  The name of the game object to find.
     * @param exact       Whether to match the name exactly or partially.
     * @param distance    The maximum distance from the anchor point to search for the game object.
     * @param anchorPoint The point from which to measure the distance.
     * @return The nearest reachable game object that matches the criteria, or null if none is found.
     */
    public GameObject findReachableObject(String objectName, boolean exact, int distance, WorldPoint anchorPoint) {
        return findReachableObject(objectName, exact, distance, anchorPoint, false, "");
    }

    /**
     * Finds a reachable game object by name within a specified distance from an anchor point, optionally checking for a specific action.
     *
     * @param objectName  The name of the game object to find.
     * @param exact       Whether to match the name exactly or partially.
     * @param distance    The maximum distance from the anchor point to search for the game object.
     * @param anchorPoint The point from which to measure the distance.
     * @param checkAction Whether to check for a specific action on the game object.
     * @param action      The action to check for if checkAction is true.
     * @return The nearest reachable game object that matches the criteria, or null if none is found.
     */
    public GameObject findReachableObject(String objectName, boolean exact, int distance, WorldPoint anchorPoint, boolean checkAction, String action) {
        Predicate<TileObject> namePred = nameMatches(objectName, exact);

        Predicate<GameObject> filter = o -> {
            if (!namePred.test(o)) {
                return false;
            }

            if (checkAction) {
                ObjectComposition comp = convertToObjectComposition(o);
                return hasAction(comp, action);
            }

            return true;
        };

        return getGameObjects(filter, anchorPoint, distance)
                .stream()
                .min(Comparator.comparingInt(o ->
                        client.getLocalPlayer().getWorldLocation().distanceTo(o.getWorldLocation())))
                .orElse(null);
    }

    public boolean hasAction(ObjectComposition objComp, String action) {
        return hasAction(objComp, action, true);
    }

    public boolean hasAction(ObjectComposition objComp, String action, boolean exact) {
        if (objComp == null) return false;

        return Arrays.stream(objComp.getActions())
                .filter(Objects::nonNull)
                .anyMatch(a -> exact ? a.equalsIgnoreCase(action) : a.toLowerCase().contains(action.toLowerCase()));
    }

    /**
     * Creates a predicate that matches TileObjects whose name matches the given name.
     * Optionally, it can require an exact match or allow partial (contains) match.
     *
     * @param objectName The name of the object to match.
     * @param exact      If true, the object name must exactly match (case-insensitive).
     *                   If false, the name must only contain the given string (case-insensitive).
     * @param <T>        A type that extends TileObject.
     * @return A predicate that returns true if the object's name matches the given name.
     */
    public <T extends TileObject> Predicate<T> nameMatches(String objectName, boolean exact) {
        String normalizedForIds = objectName.toLowerCase().replace(" ", "_");
        Set<Integer> ids = new HashSet<>(getObjectIdsByName(normalizedForIds));

        String lower = objectName.toLowerCase();

        return obj -> {
            if (!ids.isEmpty() && !ids.contains(obj.getId())) {
                return false;
            }

            return getCompositionName(obj)
                    .map(compName -> exact ? compName.equalsIgnoreCase(objectName) : compName.toLowerCase().contains(lower))
                    .orElse(false);
        };
    }

    public Optional<String> getCompositionName(TileObject obj) {
        ObjectComposition comp = convertToObjectComposition(obj);
        if (comp == null) {
            return Optional.empty();
        }
        String name = comp.getName();
        return (name == null || name.equals("null"))
                ? Optional.empty()
                : Optional.of(StringUtils.stripColTags(name));
    }

    @SneakyThrows
    public List<Integer> getObjectIdsByName(String name) {
        List<Integer> ids = new ArrayList<>();
        String lowerName = name.toLowerCase();

        Class<?>[] classesToScan = {
                net.runelite.api.ObjectID.class,
                net.runelite.api.gameval.ObjectID.class,
        };

        for (Class<?> clazz : classesToScan) {
            for (Field f : clazz.getFields()) {
                if (f.getType() != int.class) continue;

                if (f.getName().toLowerCase().contains(lowerName)) {
                    f.setAccessible(true);
                    ids.add(f.getInt(null));
                }
            }
        }

        return ids;
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



            // TODO Performs many yellow clicks
            //Option=Mine, Target=<col=ffff>Iron rocks, Param0=45, Param1=49, MenuAction=GAME_OBJECT_FIRST_OPTION, ItemId=-1, id=11364, itemOp=-1, str=MenuOptionClicked(getParam0=45, getParam1=49, getMenuOption=Mine, getMenuTarget=<col=ffff>Iron rocks, getMenuAction=GAME_OBJECT_FIRST_OPTION, getId=11364)
            reflectionService.invokeMenuAction(param0, param1, menuAction.getId(), object.getId(), -1, action, objComp.getName());
            // context.doInvoke(new NewMenuEntry(param0, param1, menuAction.getId(), object.getId(), -1, action, objComp.getName(), object), uiService.getObjectClickbox(object));
        } catch (Exception ex) {
            log.error("Failed to interact with object: {}", ex.getMessage());
        }

        return true;
    }
}
