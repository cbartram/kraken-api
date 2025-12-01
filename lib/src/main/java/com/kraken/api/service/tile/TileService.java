package com.kraken.api.service.tile;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.player.LocalPlayerEntity;
import com.kraken.api.sim.MovementFlag;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.*;
import java.util.stream.Collectors;

import static net.runelite.api.Constants.CHUNK_SIZE;
import static net.runelite.api.Perspective.SCENE_SIZE;

@Slf4j
@Singleton
public class TileService {

    private static final int FLAG_DATA_SIZE = 104;

    @Inject
    private Provider<Context> ctxProvider;

    /**
     * Returns the object composition for a given TileObject.
     * @param tileObject The tile object to retrieve the composition for
     * @return The object composition for a given tile object
     */
    public ObjectComposition getObjectComposition(TileObject tileObject) {
        ObjectComposition def = ctxProvider.get().runOnClientThread(() -> ctxProvider.get().getClient().getObjectDefinition(tileObject.getId()));
        if(def.getImpostorIds() != null && def.getImpostor() != null) {
            return ctxProvider.get().runOnClientThread(def::getImpostor);
        }

        return def;
    }

    /**
     * This method calculates the distances to a specified tile in the game world
     * using a breadth-first search (BFS) algorithm, considering movement restrictions
     * and collision data. The distances are stored in a HashMap where the key is a
     * WorldPoint (representing a tile location), and the value is the distance
     * from the starting tile. The method accounts for movement flags that block
     * movement in specific directions (east, west, north, south) and removes
     * unreachable tiles based on collision data.
     * <p>
     * The method iterates over a range of distances, progressively updating
     * reachable tiles and adding them to the tileDistances map. It checks if a
     * tile can be reached by verifying its collision flags and whether it’s blocked
     * for movement in any direction.
     *
     * @param tile The starting tile for the distance calculation.
     * @param distance The maximum distance to calculate to neighboring tiles.
     * @param ignoreCollision If true, ignores collision data during the calculation.
     * @return A HashMap containing WorldPoints and their corresponding distances from the start tile.
     */
    public HashMap<WorldPoint, Integer> getReachableTilesFromTile(WorldPoint tile, int distance, boolean ignoreCollision) {
        final HashMap<WorldPoint, Integer> tileDistances = new HashMap<>();
        tileDistances.put(tile, 0);

        for (int i = 0; i < distance + 1; i++) {
            int dist = i;
            for (var kvp : tileDistances.entrySet().stream().filter(x -> x.getValue() == dist).collect(Collectors.toList())) {
                WorldPoint point = kvp.getKey();
                LocalPoint localPoint;
                if (ctxProvider.get().getClient().getTopLevelWorldView().isInstance()) {
                    WorldPoint worldPoint = WorldPoint.toLocalInstance(ctxProvider.get().getClient().getTopLevelWorldView(), point).stream().findFirst().orElse(null);
                    if (worldPoint == null) break;
                    localPoint = LocalPoint.fromWorld(ctxProvider.get().getClient().getTopLevelWorldView(), worldPoint);
                } else {
                    localPoint = LocalPoint.fromWorld(ctxProvider.get().getClient().getTopLevelWorldView(), point);
                }

                CollisionData[] collisionMap = ctxProvider.get().getClient().getTopLevelWorldView().getCollisionMaps();
                if (collisionMap != null && localPoint != null) {
                    CollisionData collisionData = collisionMap[ctxProvider.get().getClient().getTopLevelWorldView().getPlane()];
                    int[][] flags = collisionData.getFlags();
                    int data = flags[localPoint.getSceneX()][localPoint.getSceneY()];

                    Set<MovementFlag> movementFlags = MovementFlag.getSetFlags(data);

                    if (!ignoreCollision && !tile.equals(point)) {
                        if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_FULL) || movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_FLOOR)) {
                            tileDistances.remove(point);
                            continue;
                        }
                    }

                    if (kvp.getValue() >= distance)
                        continue;

                    if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_EAST))
                        tileDistances.putIfAbsent(point.dx(1), dist + 1);
                    if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_WEST))
                        tileDistances.putIfAbsent(point.dx(-1), dist + 1);
                    if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH))
                        tileDistances.putIfAbsent(point.dy(1), dist + 1);
                    if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH))
                        tileDistances.putIfAbsent(point.dy(-1), dist + 1);
                }
            }
        }

        return tileDistances;
    }

    /**
     * Checks if a GameObject is reachable.
     * This considers the object's size and checks if the player can reach
     * any tile touching the object's boundary (the "Interactable Halo").
     * @return true if the game object is reachable and false otherwise
     */
    public boolean isObjectReachable(GameObject obj) {
        if (obj == null) return false;

        // 1. Get the boundary of the object in Scene Coordinates
        // We use Scene Coordinates (0-103) because that matches the CollisionData flags.
        LocalPoint lp = obj.getLocalLocation(); // Center of object
        if (lp == null) return false;

        Client client = ctxProvider.get().getClient();
        int sceneX = lp.getSceneX();
        int sceneY = lp.getSceneY();

        // Object composition gives us width/height (for 1x1, 2x2 objects etc)
        ObjectComposition comp = getObjectComposition(obj);
        int sizeX = 1;
        int sizeY = 1;

        if (comp != null) {
            // Adjust for rotation if necessary (swaps width/height)
            if (obj.getOrientation() == 1 || obj.getOrientation() == 3) {
                sizeX = comp.getSizeY();
                sizeY = comp.getSizeX();
            } else {
                sizeX = comp.getSizeX();
                sizeY = comp.getSizeY();
            }
        }

        // Calculate the bottom-left corner of the object in Scene coords
        // LocalPoint is center, so we shift back to corner
        int minX = sceneX - (sizeX - 1) / 2;
        int minY = sceneY - (sizeY - 1) / 2;
        int maxX = minX + sizeX - 1;
        int maxY = minY + sizeY - 1;

        // 2. Run the BFS to find all reachable tiles from player
        boolean[][] visited = getReachableTilesMatrix();
        if (visited == null) return false;

        // 3. Check if any tile occupying the object OR adjacent to the object is reachable
        // We search from minX-1 to maxX+1 to cover the "halo" around the object.
        for (int x = minX - 1; x <= maxX + 1; x++) {
            for (int y = minY - 1; y <= maxY + 1; y++) {
                if (x >= 0 && y >= 0 && x < SCENE_SIZE && y < SCENE_SIZE) {
                    if (visited[x][y]) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * Standard BFS to map all reachable tiles from the current player position.
     * @return A 104x104 boolean array where true = walkable from player.
     */
    private boolean[][] getReachableTilesMatrix() {
        Client client = ctxProvider.get().getClient();
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) return null;

        WorldView wv = client.getTopLevelWorldView();
        if (wv == null) return null;

        LocalPoint playerLp = localPlayer.getLocalLocation();
        if (playerLp == null) return null;

        int startX = playerLp.getSceneX();
        int startY = playerLp.getSceneY();
        int plane = wv.getPlane();

        CollisionData[] collisionData = wv.getCollisionMaps();
        if (collisionData == null) return null;
        int[][] flags = collisionData[plane].getFlags();

        boolean[][] visited = new boolean[SCENE_SIZE][SCENE_SIZE];
        ArrayDeque<Integer> queue = new ArrayDeque<>();

        // Start BFS
        int startPoint = (startX << 16) | startY;
        queue.add(startPoint);
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int point = queue.poll();
            int x = point >> 16;
            int y = point & 0xFFFF;

            // Check 4 cardinal directions
            checkNeighbour(queue, visited, flags, x, y, -1, 0, CollisionDataFlag.BLOCK_MOVEMENT_WEST);
            checkNeighbour(queue, visited, flags, x, y, 1, 0, CollisionDataFlag.BLOCK_MOVEMENT_EAST);
            checkNeighbour(queue, visited, flags, x, y, 0, -1, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH);
            checkNeighbour(queue, visited, flags, x, y, 0, 1, CollisionDataFlag.BLOCK_MOVEMENT_NORTH);
        }

        return visited;
    }

    private void checkNeighbour(ArrayDeque<Integer> queue, boolean[][] visited, int[][] flags, int x, int y, int dx, int dy, int blockFlag) {
        int nx = x + dx;
        int ny = y + dy;

        if (nx >= 0 && nx < SCENE_SIZE && ny >= 0 && ny < SCENE_SIZE) {
            if (!visited[nx][ny]) {
                // Check if we can leave current tile (blockFlag) AND enter next tile (BLOCK_MOVEMENT_FULL)
                // Note: We check CollisionDataFlag.BLOCK_MOVEMENT_FULL on the *destination* to ensure we don't walk into walls
                if ((flags[x][y] & blockFlag) == 0 && (flags[nx][ny] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0) {
                    visited[nx][ny] = true;
                    queue.add((nx << 16) | ny);
                }
            }
        }
    }
    
    
    /**
     * This method checks if a given target tile (WorldPoint) is reachable from the
     * player's current location, considering collision data and the plane of the
     * world. The method uses a breadth-first search (BFS) algorithm to traverse
     * neighboring tiles while checking for movement blocks in the four cardinal
     * directions (north, south, east, west). It ensures the target tile is within
     * the same plane as the player and that movement between tiles is not blocked.
     * <p>
     * The method initializes a queue to explore the world grid, marking visited
     * tiles to avoid revisiting. It checks the flags for collision data to determine
     * whether movement is allowed in each direction, and only adds neighboring tiles
     * to the queue if they are not blocked. Finally, it verifies if the target point
     * has been visited during the traversal and returns true if reachable, false otherwise.
     *
     * @param targetPoint The WorldPoint representing the target tile to check for
     *                    reachability.
     * @return True if the target tile is reachable from the player's location,
     *         otherwise false.
     */
    public boolean isTileReachable(WorldPoint targetPoint) {
        if (targetPoint == null) return false;

        LocalPlayerEntity player = ctxProvider.get().players().local();
        final WorldPoint playerLoc = player.raw().getWorldLocation();
        if (playerLoc == null) return false;

        if (targetPoint.getPlane() != playerLoc.getPlane()) return false;

        final boolean[][] visited = new boolean[FLAG_DATA_SIZE][FLAG_DATA_SIZE];
        final int[][] flags = getFlags();
        if (flags == null) return false;

        final int startX;
        final int startY;
        if (ctxProvider.get().getClient().getTopLevelWorldView().getScene().isInstance()) {
            LocalPoint localPoint = player.raw().getLocalLocation();
            startX = localPoint.getSceneX();
            startY = localPoint.getSceneY();
        } else {
            startX = playerLoc.getX() - ctxProvider.get().getClient().getTopLevelWorldView().getBaseX();
            startY = playerLoc.getY() - ctxProvider.get().getClient().getTopLevelWorldView().getBaseY();
        }
        final int startPoint = (startX << 16) | startY;

        ArrayDeque<Integer> queue = new ArrayDeque<>();
        queue.add(startPoint);
        visited[startX][startY] = true;

        while (!queue.isEmpty()) {
            int point = queue.poll();
            int x = point >> 16;
            int y = point & 0xFFFF;

            if (isWithinBounds(x, y)) {
                checkAndAddNeighbour(queue, visited, flags, x, y, -1, 0, CollisionDataFlag.BLOCK_MOVEMENT_WEST);
                checkAndAddNeighbour(queue, visited, flags, x, y, 1, 0, CollisionDataFlag.BLOCK_MOVEMENT_EAST);
                checkAndAddNeighbour(queue, visited, flags, x, y, 0, -1, CollisionDataFlag.BLOCK_MOVEMENT_SOUTH);
                checkAndAddNeighbour(queue, visited, flags, x, y, 0, 1, CollisionDataFlag.BLOCK_MOVEMENT_NORTH);
            }
        }

        return isVisited(targetPoint, visited);
    }

    /**
     * This method checks whether a given WorldPoint has been visited during the
     * traversal of the game world. It calculates the tile’s local coordinates relative
     * to the base coordinates, considering whether the client is in an instanced region
     * or not. The method then checks if the calculated coordinates are within bounds
     * and if the tile has been marked as visited in the provided visited array.
     * <p>
     * The method ensures that the given WorldPoint corresponds to a valid tile on
     * the game map by verifying if its coordinates fall within the bounds of the
     * world grid, and if so, it checks whether that tile has already been visited
     * during the search or traversal process.
     *
     * @param worldPoint The WorldPoint representing the tile to check for visit status.
     * @param visited A 2D boolean array tracking visited tiles during world traversal.
     * @return True if the tile has been visited and is within bounds, otherwise false.
     */
    private boolean isVisited(WorldPoint worldPoint, boolean[][] visited) {
        int baseX, baseY, x, y;
        if (ctxProvider.get().getClient().getTopLevelWorldView().getScene().isInstance()) {
            LocalPlayerEntity player = ctxProvider.get().players().local();
            LocalPoint localPoint = player.raw().getLocalLocation();
            x = localPoint.getSceneX();
            y = localPoint.getSceneY();
        } else {
            baseX = ctxProvider.get().getClient().getTopLevelWorldView().getBaseX();
            baseY = ctxProvider.get().getClient().getTopLevelWorldView().getBaseY();
            x = worldPoint.getX() - baseX;
            y = worldPoint.getY() - baseY;
        }


        return isWithinBounds(x, y) && visited[x][y];
    }

    /**
     * This method checks a neighboring tile and adds it to the queue if it is valid
     * and not blocked for movement. It considers both the current tile's collision
     * data and the neighboring tile’s collision flags to determine whether movement
     * in the specified direction (dx, dy) is possible. The method ensures the neighboring
     * tile is within bounds, hasn't been visited, and doesn't have movement restrictions
     * (such as full-block movement or movement in the specified direction).
     * <p>
     * The method performs a bitwise check on the tile’s flags to determine if movement
     * in the given direction is allowed and ensures that the neighboring tile is not
     * already visited before adding it to the queue.
     *
     * @param queue The queue that stores the coordinates of tiles to be visited.
     * @param visited A 2D boolean array tracking which tiles have already been visited.
     * @param flags A 2D array containing the collision flags for each tile.
     * @param x The current tile’s x-coordinate.
     * @param y The current tile’s y-coordinate.
     * @param dx The change in x-coordinate for the neighboring tile.
     * @param dy The change in y-coordinate for the neighboring tile.
     * @param blockMovementFlag The collision flag that blocks movement in a given direction.
     */
    private void checkAndAddNeighbour(ArrayDeque<Integer> queue, boolean[][] visited, int[][] flags, int x, int y, int dx, int dy, int blockMovementFlag) {
        int nx = x + dx;
        int ny = y + dy;

        if (isWithinBounds(nx, ny) && !visited[nx][ny] && (flags[x][y] & blockMovementFlag) == 0 && (flags[nx][ny] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0) {
            queue.add((nx << 16) | ny);
            visited[nx][ny] = true;
        }
    }

    /**
     * Returns collision flags for the current plane
     * @return 2D array of collision flags
     */
    private int[][] getFlags() {
        final WorldView wv = ctxProvider.get().getClient().getTopLevelWorldView();
        if (wv == null) return null;

        final CollisionData[] collisionData = wv.getCollisionMaps();
        if (collisionData == null) return null;

        return collisionData[wv.getPlane()].getFlags();
    }

    /**
     * This method checks if the given coordinates (x, y) are within the valid bounds
     * of the game world grid. It ensures that the coordinates are non-negative and
     * within the range of the grid dimensions (0 to 103 for both x and y).
     * <p>
     * The method is used to prevent out-of-bounds errors when accessing world tiles
     * by ensuring that the coordinates provided for the tile are within the valid
     * range before performing further operations.
     *
     * @param x The x-coordinate of the tile to check.
     * @param y The y-coordinate of the tile to check.
     * @return True if the coordinates are within bounds (0 <= x, y < {@code FLAG_DATA_SIZE}), otherwise false.
     */
    private static boolean isWithinBounds(int x, int y) {
        return x >= 0 && y >= 0 && x < FLAG_DATA_SIZE && y < FLAG_DATA_SIZE;
    }


    /**
     * This method retrieves the tile at the specified coordinates (x, y) on the current plane.
     * It first creates a WorldPoint for the given coordinates and checks if the point is within
     * the scene using the `isInScene` method. If the WorldPoint is valid and within the scene,
     * it converts the WorldPoint to a LocalPoint, then retrieves and returns the corresponding
     * Tile from the game scene.
     * <p>
     * If the WorldPoint is out of bounds or the LocalPoint is null, the method returns null
     * to indicate that no valid tile is found at the given coordinates.
     *
     * @param x The x-coordinate of the tile.
     * @param y The y-coordinate of the tile.
     * @return The Tile at the specified coordinates, or null if the tile is invalid or not in the scene.
     */
    public Tile getTile(int x, int y) {
        WorldPoint worldPoint = new WorldPoint(x, y, ctxProvider.get().getClient().getTopLevelWorldView().getPlane());
        LocalPoint localPoint;

        if (ctxProvider.get().getClient().getTopLevelWorldView().getScene().isInstance()) {
            localPoint = fromWorldInstance(worldPoint);
        } else {
            localPoint = LocalPoint.fromWorld(ctxProvider.get().getClient().getTopLevelWorldView(), worldPoint);
        }

        if (localPoint == null) return null;
        return ctxProvider.get().getClient().getTopLevelWorldView().getScene().getTiles()[worldPoint.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
    }

    /**
     * Used to convert a WorldPoint in an instance to a LocalPoint
     * @param worldPoint The world point to convert
     * @return A local point representing the same global world point
     */
    public LocalPoint fromWorldInstance(WorldPoint worldPoint) {
        int[][][] instanceTemplateChunks = ctxProvider.get().getClient().getTopLevelWorldView().getInstanceTemplateChunks();
        // Extract the coordinates from the WorldPoint
        int worldX = worldPoint.getX();
        int worldY = worldPoint.getY();
        int worldPlane = ctxProvider.get().getClient().getTopLevelWorldView().getPlane();

        // Loop through all chunks to find which one contains the world point
        for (int chunkX = 0; chunkX < instanceTemplateChunks[worldPlane].length; chunkX++) {
            for (int chunkY = 0; chunkY < instanceTemplateChunks[worldPlane][chunkX].length; chunkY++) {
                // Get the template chunk at this chunk position
                int templateChunk = instanceTemplateChunks[worldPlane][chunkX][chunkY];

                // Extract rotation, template chunk coordinates, and plane
                int rotation = (templateChunk >> 1) & 0x3;
                int templateChunkY = (templateChunk >> 3 & 0x7FF) * CHUNK_SIZE;
                int templateChunkX = (templateChunk >> 14 & 0x3FF) * CHUNK_SIZE;
                int templateChunkPlane = (templateChunk >> 24) & 0x3;

                // Check if the WorldPoint matches this chunk (after reversing rotation)
                WorldPoint rotatedWorldPoint = rotate(new WorldPoint(worldX, worldY, templateChunkPlane), rotation);

                if (rotatedWorldPoint.getX() >= templateChunkX && rotatedWorldPoint.getX() < templateChunkX + CHUNK_SIZE
                        && rotatedWorldPoint.getY() >= templateChunkY && rotatedWorldPoint.getY() < templateChunkY + CHUNK_SIZE) {
                    // Calculate local coordinates within the scene
                    int localX = (rotatedWorldPoint.getX() - templateChunkX) + (chunkX * CHUNK_SIZE);
                    int localY = (rotatedWorldPoint.getY() - templateChunkY) + (chunkY * CHUNK_SIZE);

                    // Return the corresponding LocalPoint
                    return  LocalPoint.fromScene(localX, localY, ctxProvider.get().getClient().getTopLevelWorldView());
                }
            }
        }
        return null;
    }

    /**
     * Gets the coordinate of the tile that contains the passed world point,
     * accounting for instances.
     *
     * @param worldPoint the instance worldpoint
     * @return the tile coordinate containing the local point
     */
    public WorldPoint fromInstance(WorldPoint worldPoint) {

        //get local
        LocalPoint localPoint = LocalPoint.fromWorld(ctxProvider.get().getClient().getTopLevelWorldView(), worldPoint);

        // if local point is null or not in an instanced region, return the world point as is
        if(localPoint == null || !ctxProvider.get().getClient().getTopLevelWorldView().isInstance())
            return worldPoint;

        // get position in the scene
        int sceneX = localPoint.getSceneX();
        int sceneY = localPoint.getSceneY();

        // get chunk from scene
        int chunkX = sceneX / CHUNK_SIZE;
        int chunkY = sceneY / CHUNK_SIZE;

        // get the template chunk for the chunk
        int[][][] instanceTemplateChunks = ctxProvider.get().getClient().getTopLevelWorldView().getInstanceTemplateChunks();
        int templateChunk = instanceTemplateChunks[worldPoint.getPlane()][chunkX][chunkY];

        int rotation = templateChunk >> 1 & 0x3;
        int templateChunkY = (templateChunk >> 3 & 0x7FF) * CHUNK_SIZE;
        int templateChunkX = (templateChunk >> 14 & 0x3FF) * CHUNK_SIZE;
        int templateChunkPlane = templateChunk >> 24 & 0x3;

        // calculate world point of the template
        int x = templateChunkX + (sceneX & (CHUNK_SIZE - 1));
        int y = templateChunkY + (sceneY & (CHUNK_SIZE - 1));

        // create and rotate point back to 0, to match with template
        return rotate(new WorldPoint(x, y, templateChunkPlane), 4 - rotation);
    }

    /**
     * Converts a world point into a list of instanced world points
     * @param worldPoint World point to convert
     * @return List of instanced world points.
     */
    public ArrayList<WorldPoint> toInstance(WorldPoint worldPoint) {
        // if not in an instanced region, return the world point as is
        if (!ctxProvider.get().getClient().getTopLevelWorldView().isInstance()) {
            return new ArrayList<>(Collections.singletonList(worldPoint));
        }

        // find instance chunks using the template point. there might be more than one.
        ArrayList<WorldPoint> worldPoints = new ArrayList<>();
        int[][][] instanceTemplateChunks = ctxProvider.get().getClient().getTopLevelWorldView().getInstanceTemplateChunks();
        for (int z = 0; z < instanceTemplateChunks.length; z++) {
            for (int x = 0; x < instanceTemplateChunks[z].length; ++x) {
                for (int y = 0; y < instanceTemplateChunks[z][x].length; ++y) {
                    int chunkData = instanceTemplateChunks[z][x][y];
                    int rotation = chunkData >> 1 & 0x3;
                    int templateChunkY = (chunkData >> 3 & 0x7FF) * CHUNK_SIZE;
                    int templateChunkX = (chunkData >> 14 & 0x3FF) * CHUNK_SIZE;
                    int plane = chunkData >> 24 & 0x3;
                    if (worldPoint.getX() >= templateChunkX && worldPoint.getX() < templateChunkX + CHUNK_SIZE
                            && worldPoint.getY() >= templateChunkY && worldPoint.getY() < templateChunkY + CHUNK_SIZE
                            && plane == worldPoint.getPlane())
                    {
                        WorldPoint p = new WorldPoint(ctxProvider.get().getClient().getTopLevelWorldView().getBaseX() + x * CHUNK_SIZE + (worldPoint.getX() & (CHUNK_SIZE - 1)),
                                ctxProvider.get().getClient().getTopLevelWorldView().getBaseY() + y * CHUNK_SIZE + (worldPoint.getY() & (CHUNK_SIZE - 1)),
                                z);
                        p = rotate(p, rotation);
                        worldPoints.add(p);
                    }
                }
            }
        }
        if(worldPoints.isEmpty())
            worldPoints.add(worldPoint);
        return worldPoints;
    }

    /**
     * Returns a normal WorldPoint given a world point that originated in an instance.
     * @param worldPoint WorldPoint to convert
     * @return a normalized WorldPoint from an instance WorldPoint
     */
//    public WorldPoint fromInstance(WorldPoint worldPoint) {
//        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
//
//        // if local point is null or not in an instanced region, return the world point as is
//        if (localPoint == null || !client.getTopLevelWorldView().isInstance())
//            return worldPoint;
//
//        int sceneX = localPoint.getSceneX();
//        int sceneY = localPoint.getSceneY();
//
//        int chunkX = sceneX / CHUNK_SIZE;
//        int chunkY = sceneY / CHUNK_SIZE;
//
//        int[][][] instanceTemplateChunks = client.getTopLevelWorldView().getInstanceTemplateChunks();
//        int templateChunk = instanceTemplateChunks[worldPoint.getPlane()][chunkX][chunkY];
//
//        int rotation = templateChunk >> 1 & 0x3;
//        int templateChunkY = (templateChunk >> 3 & 0x7FF) * CHUNK_SIZE;
//        int templateChunkX = (templateChunk >> 14 & 0x3FF) * CHUNK_SIZE;
//        int templateChunkPlane = templateChunk >> 24 & 0x3;
//
//        int x = templateChunkX + (sceneX & (CHUNK_SIZE - 1));
//        int y = templateChunkY + (sceneY & (CHUNK_SIZE - 1));
//
//        return rotate(new WorldPoint(x, y, templateChunkPlane), 4 - rotation);
//    }

    /**
     * Converts a normal WorldPoint into an instanced version of the WorldPoint
     * @param worldPoint Normal WorldPoint to convert
     * @return The instanced WorldPoint
     */
//    public WorldPoint toInstance(WorldPoint worldPoint) {
//        if (!client.getTopLevelWorldView().isInstance()) {
//            return worldPoint;
//        }
//
//        ArrayList<WorldPoint> worldPoints = new ArrayList<>();
//        int[][][] instanceTemplateChunks = client.getTopLevelWorldView().getInstanceTemplateChunks();
//        for (int z = 0; z < instanceTemplateChunks.length; z++) {
//            for (int x = 0; x < instanceTemplateChunks[z].length; ++x) {
//                for (int y = 0; y < instanceTemplateChunks[z][x].length; ++y) {
//                    int chunkData = instanceTemplateChunks[z][x][y];
//                    int rotation = chunkData >> 1 & 0x3;
//                    int templateChunkY = (chunkData >> 3 & 0x7FF) * CHUNK_SIZE;
//                    int templateChunkX = (chunkData >> 14 & 0x3FF) * CHUNK_SIZE;
//                    int plane = chunkData >> 24 & 0x3;
//                    if (worldPoint.getX() >= templateChunkX && worldPoint.getX() < templateChunkX + CHUNK_SIZE
//                            && worldPoint.getY() >= templateChunkY && worldPoint.getY() < templateChunkY + CHUNK_SIZE
//                            && plane == worldPoint.getPlane()) {
//                        WorldPoint p = new WorldPoint(client.getTopLevelWorldView().getBaseX() + x * CHUNK_SIZE + (worldPoint.getX() & (CHUNK_SIZE - 1)),
//                                client.getTopLevelWorldView().getBaseY() + y * CHUNK_SIZE + (worldPoint.getY() & (CHUNK_SIZE - 1)),
//                                z);
//                        p = rotate(p, rotation);
//                        worldPoints.add(p);
//                    }
//                }
//            }
//        }
//        if (worldPoints.isEmpty())
//            worldPoints.add(worldPoint);
//        return worldPoints.get(0);
//    }

//    /**
//     * Returns a world point from a clicked point on the world map.
//     * @return WorldPoint calculated world point object
//     */
//    public WorldPoint fromMap() {
//        WorldMap worldMap = client.getWorldMap();
//        if(worldMap == null || !widgetService.isWidgetVisible(WidgetInfo.WORLD_MAP_VIEW.getId()))
//            return null;
//
//        Point p = worldMap.getWorldMapPosition();
//        return WorldPointUtil.translate(new WorldPoint(p.getX(), p.getY(), client.getTopLevelWorldView().getPlane()));
//    }

    /**
     * Rotate the coordinates in the chunk according to chunk rotation
     *
     * @param point    point
     * @param rotation rotation
     * @return world point
     */
    private WorldPoint rotate(WorldPoint point, int rotation) {
        int chunkX = point.getX() & -CHUNK_SIZE;
        int chunkY = point.getY() & -CHUNK_SIZE;
        int x = point.getX() & (CHUNK_SIZE - 1);
        int y = point.getY() & (CHUNK_SIZE - 1);
        switch (rotation)
        {
            case 1:
                return new WorldPoint(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), point.getPlane());
            case 2:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), point.getPlane());
            case 3:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, point.getPlane());
        }
        return point;
    }

    /**
     * Returns the distance from a world point to another world point in local point distance.
     * @param distance Distance to convert
     * @return The distance in local points between a world point and another world point
     */
    public static Integer worldToLocalDistance(int distance) {
        return distance * Perspective.LOCAL_TILE_SIZE;
    }

    /**
     * Returns the distance from a local point to another local point in world point distance.
     * @param distance Distance to convert
     * @return The distance in world points between a local point and another local point
     */
    public static Integer localToWorldDistance(int distance) {
        return distance / Perspective.LOCAL_TILE_SIZE;
    }
}
