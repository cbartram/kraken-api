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
    private CameraService cameraService;

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

    /**
     * Interacts with a game object located at the specified world point using the default action. By default
     * the action used is: "Walk".
     * @param worldPoint The world point of the game object to interact with.
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(WorldPoint worldPoint) {
        return interact(worldPoint, "");
    }

    /**
     * Interacts with a game object located at the specified world point using the specified action.
     * If the action is an empty string, the default action "Walk" is used.
     * @param worldPoint The world point of the game object to interact with.
     * @param action The action to perform on the game object. If empty, the default action "Walk" is used.
     * @return
     */
    public boolean interact(WorldPoint worldPoint, String action) {
        TileObject gameObject = all(o -> o.getWorldLocation().equals(worldPoint)).stream().findFirst().orElse(null);
        return interact(gameObject, action);
    }

    /**
     * Interacts with the specified game object using the default action. By default the action used is: "Walk".
     * @param gameObject The game object to interact with.
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(GameObject gameObject) {
        return interact(gameObject, "");
    }

    /**
     * Interacts with the specified tile object using the default action. By default the action used is: "Walk".
     * @param tileObject The tile object to interact with.
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(TileObject tileObject) {
        return interact(tileObject, "");
    }

    /**
     * Interacts with the first game object found with the specified ID using the default action. By default the action used is: "Walk".
     * @param id The ID of the game object to interact with.
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(int id) {
        TileObject object = all(o -> o.getId() == id).stream().findFirst().orElse(null);
        return interact(object);
    }

    /**
     * Interacts with the first game object found with the specified ID using the specified action.
     * @param object The tile object to interact with.
     * @param action The action to perform on the game object. If empty, the default action "Walk" is used.
     * @return True if the interaction was successful, false otherwise.
     */
    public boolean interact(TileObject object, String action) {
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

            reflectionService.invokeMenuAction(param0, param1, menuAction.getId(), object.getId(), -1, action, objComp.getName());
        } catch (Exception ex) {
            log.error("Failed to interact with object: {}", ex.getMessage());
        }

        return true;
    }

    /**
     * Gets all tile objects in the scene.
     * @return A list of all tile objects in the scene.
     */
    public List<TileObject> all() {
        return all(o -> true);
    }


    /**
     * Gets all tile objects in the scene that match the given predicate.
     * @param predicate The predicate to filter the tile objects.
     * @return A list of all tile objects that match the predicate.
     * @param <T> The type of tile object to filter (e.g., GameObject, WallObject, etc.)
     */
    public <T extends TileObject> List<TileObject> all(Predicate<? super T> predicate) {
        return all(predicate, Constants.SCENE_SIZE);
    }

    /**
     * Gets all tile objects in the scene that match the given predicate and are within the specified distance from the player.
     * @param predicate The predicate to filter the tile objects.
     * @param distance The maximum distance from the player to include tile objects (in tiles).
     * @return A list of all tile objects that match the predicate and are within the specified distance from the player.
     * @param <T> The type of tile object to filter (e.g., GameObject, WallObject, etc.)
     */
    public <T extends TileObject> List<TileObject> all(Predicate<? super T> predicate, int distance) {
        Player player = context.runOnClientThreadOptional(() -> client.getLocalPlayer()).orElse(null);
        if (player == null) {
            return Collections.emptyList();
        }
        return all(predicate, player.getWorldLocation(), distance);
    }

    /**
     * Creates a predicate that checks if a TileObject is within a certain distance (in local units) from an anchor point.
     * @param distance The maximum distance from the anchor point (in local units).
     * @param anchor The anchor local point.
     * @return A predicate that returns true if the TileObject is within the specified distance from the anchor point.
     * @param <T> The type of TileObject to check (e.g., GameObject, WallObject, etc.)
     */
    private <T extends TileObject> Predicate<T> withinTilesPredicate(int distance, LocalPoint anchor) {
        return to -> isWithinTiles(anchor, to.getLocalLocation(), distance);
    }

    /**
     * Gets all tile objects in the scene that match the given predicate and are within the specified distance from the anchor point.
     * @param predicate The predicate to filter the tile objects.
     * @param anchor The anchor world point to measure distance from.
     * @param distance The maximum distance from the anchor point to include tile objects (in tiles).
     * @return A list of all tile objects that match the predicate and are within the specified distance from the anchor point.
     * @param <T> The type of tile object to filter (e.g., GameObject, WallObject, etc.)
     */
    public <T extends TileObject> List<TileObject> all(Predicate<? super T> predicate, WorldPoint anchor, int distance) {
        List<TileObject> all = new ArrayList<>();
        all.addAll(fetchGameObjects(predicate, anchor, distance));
        all.addAll(fetchTileObjects(predicate, anchor, distance));
        return all;
    }

    /**
     * Fetches tile objects of type T that match the given predicate and are within the specified distance from the anchor point.
     * @param predicate The predicate to filter the tile objects.
     * @param anchor The anchor world point to measure distance from.
     * @param distance The maximum distance from the anchor point to include tile objects (in tiles).
     * @return A list of tile objects of type T that match the predicate and are within the specified distance from the anchor point.
     * @param <T> The type of tile object to filter (e.g., GameObject, WallObject, etc.)
     */
    @SuppressWarnings("unchecked")
    private <T extends TileObject> List<T> fetchTileObjects(Predicate<? super T> predicate, WorldPoint anchor, int distance) {
        return (List<T>) getTileObjects((Predicate<TileObject>) predicate, anchor, distance);
    }

    /**
     * Fetches game objects of type T that match the given predicate and are within the specified distance from the anchor point.
     * @param predicate The predicate to filter the game objects.
     * @param anchor The anchor world point to measure distance from.
     * @param distance The maximum distance from the anchor point to include game objects (in tiles).
     * @return A list of game objects of type T that match the predicate and are within the specified distance from the anchor point.
     * @param <T> The type of game object to filter (e.g., GameObject, WallObject, etc.)
     */
    @SuppressWarnings("unchecked")
    private <T extends TileObject> List<T> fetchGameObjects(Predicate<? super T> predicate, WorldPoint anchor, int distance) {
        return (List<T>) getGameObjects((Predicate<GameObject>) predicate, anchor, distance);
    }

    /**
     * Gets all tile objects in the scene that match the given predicate and are within the specified distance from the anchor point.
     * @param predicate The predicate to filter the tile objects.
     * @param anchor The anchor world point to measure distance from.
     * @param distance The maximum distance from the anchor point to include tile objects (in tiles).
     * @return A list of all tile objects that match the predicate and are within the specified distance from the anchor point.
     */
    public List<TileObject> getTileObjects(Predicate<TileObject> predicate, WorldPoint anchor, int distance) {
        Player player = context.runOnClientThreadOptional(() -> client.getLocalPlayer()).orElse(null);
        if (player == null) {
            return Collections.emptyList();
        }
        LocalPoint anchorLocal = LocalPoint.fromWorld(player.getWorldView(), anchor);
        if (anchorLocal == null) {
            return Collections.emptyList();
        }
        return getTileObjects(predicate, anchorLocal, distance * Perspective.LOCAL_TILE_SIZE);
    }

    /**
     * Gets all tile objects in the scene that match the given predicate and are within the specified distance from the anchor local point.
     * @param predicate The predicate to filter the tile objects.
     * @param anchorLocal The anchor local point to measure distance from.
     * @param distance The maximum distance from the anchor point to include tile objects (in local units).
     * @return A list of all tile objects that match the predicate and are within the specified distance from the anchor local point.
     */
    public List<TileObject> getTileObjects(Predicate<TileObject> predicate, LocalPoint anchorLocal, int distance) {
        return getSceneObjects(TILEOBJECT_EXTRACTOR, predicate, anchorLocal, distance);
    }

    /**
     * Gets all game objects in the scene that match the given predicate and are within the specified distance from the anchor point.
     * @param predicate The predicate to filter the game objects.
     * @param anchorLocal The anchor local point to measure distance from.
     * @param distance The maximum distance from the anchor point to include game objects (in tiles).
     * @return A list of all game objects that match the predicate and are within the specified distance from the anchor point.
     */
    public List<GameObject> getGameObjects(Predicate<GameObject> predicate, LocalPoint anchorLocal, int distance) {
        return getSceneObjects(GAMEOBJECT_EXTRACTOR, predicate, anchorLocal, distance);
    }

    /**
     * Gets all game objects in the scene that match the given predicate and are within the specified distance from the anchor point.
     * @param predicate The predicate to filter the game objects.
     * @param anchor The anchor world point to measure distance from.
     * @param distance The maximum distance from the anchor point to include game objects (in tiles).
     * @return A list of all game objects that match the predicate and are within the specified distance from the anchor point.
     */
    public List<GameObject> getGameObjects(Predicate<GameObject> predicate, WorldPoint anchor, int distance) {
        Player player = context.runOnClientThreadOptional(() -> client.getLocalPlayer()).orElse(null);
        if (player == null) {
            return Collections.emptyList();
        }

        LocalPoint anchorLocal = LocalPoint.fromWorld(player.getWorldView(), anchor);
        if (anchorLocal == null) {
            return Collections.emptyList();
        }
        return getGameObjects(predicate, anchorLocal, distance * Perspective.LOCAL_TILE_SIZE);
    }

    /**
     * Gets all scene objects of type T that match the given predicate and are within the specified distance from the anchor local point.
     * @param extractor The function to extract objects of type T from a Tile.
     * @param predicate The predicate to filter the objects.
     * @param anchorLocal The anchor local point to measure distance from.
     * @param distance The maximum distance from the anchor point to include objects (in local units).
     * @return A list of all objects of type T that match the predicate and are within the specified distance from the anchor local point.
     * @param <T> The type of object to filter (e.g., GameObject, WallObject, etc.)
     */
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

    /**
     * Gets a stream of all scene objects of type T extracted using the given extractor function.
     * @param extractor The function to extract objects of type T from a Tile.
     * @return A stream of all objects of type T in the scene.
     * @param <T> The type of object to extract (e.g., GameObject, WallObject, etc.)
     */
    private <T extends TileObject> Stream<T> getSceneObjects(Function<Tile, Collection<? extends T>> extractor) {
        Player player = context.runOnClientThreadOptional(() -> client.getLocalPlayer()).orElse(null);
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
     * Determines if a TileObject should be included based on its location relative to the Tile.
     * @param obj The TileObject to check.
     * @param tile The Tile containing the TileObject.
     * @return True if the TileObject's location matches the Tile's location, false otherwise.
     * @param <T> The type of TileObject (e.g., GameObject, WallObject, etc.)
     */
    private <T extends TileObject> boolean shouldIncludeObject(T obj, Tile tile) {
        if (obj instanceof GameObject) {
            GameObject gameObject = (GameObject) obj;
            return gameObject.getSceneMinLocation().equals(tile.getSceneLocation());
        }
        return obj.getLocalLocation().equals(tile.getLocalLocation());
    }

    /**
     * Checks if the object location is within the specified distance from the anchor point.
     * @param anchor The anchor local point.
     * @param objLoc The object's local location.
     * @param distance The maximum distance from the anchor point (in local units).
     * @return True if the object is within the specified distance from the anchor point, false otherwise.
     */
    private boolean isWithinTiles(LocalPoint anchor, LocalPoint objLoc, int distance) {
        int dx = Math.abs(anchor.getX() - objLoc.getX());
        int dy = Math.abs(anchor.getY() - objLoc.getY());

        if (distance == 0) {
            return (dx == Perspective.LOCAL_TILE_SIZE && dy == 0) || (dy == Perspective.LOCAL_TILE_SIZE && dx == 0);
        } else {
            return objLoc.distanceTo(anchor) <= distance;
        }
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
                .min(Comparator.comparingInt(o -> context.runOnClientThreadOptional(() -> client.getLocalPlayer().getWorldLocation().distanceTo(o.getWorldLocation())).orElse(Integer.MAX_VALUE)))
                .orElse(null);
    }

    /**
     * Checks if the given ObjectComposition has the specified action.
     * @param objComp the ObjectComposition to check
     * @param action the action to look for
     * @return true if the action is found, false otherwise
     */
    public boolean hasAction(ObjectComposition objComp, String action) {
        return hasAction(objComp, action, true);
    }

    /**
     * Checks if the given ObjectComposition has the specified action, with an option for exact or partial match.
     * @param objComp the ObjectComposition to check
     * @param action the action to look for
     * @param exact if true, requires an exact match; if false, allows partial (contains) match
     * @return
     */
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
    private <T extends TileObject> ObjectComposition convertToObjectComposition(T object) {
        return context.runOnClientThreadOptional(() -> {
            ObjectComposition comp = client.getObjectDefinition(object.getId());
            if (comp == null) return null;
            return comp.getImpostorIds() == null ? comp : comp.getImpostor();
        }).orElse(null);
    }
}
