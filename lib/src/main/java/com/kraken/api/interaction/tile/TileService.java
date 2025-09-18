package com.kraken.api.interaction.tile;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.player.PlayerService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.devtools.MovementFlag;
import shortestpath.pathfinder.CollisionMap;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

import static net.runelite.api.Constants.CHUNK_SIZE;

@Slf4j
@Singleton
public class TileService extends AbstractService {

    private static final int FLAG_DATA_SIZE = 104;

    @Inject
    private PlayerService playerService;

    /**
     * This method calculates the distances to a specified tile in the game world
     * using a breadth-first search (BFS) algorithm, considering movement restrictions
     * and collision data. The distances are stored in a HashMap where the key is a
     * WorldPoint (representing a tile location), and the value is the distance
     * from the starting tile. The method accounts for movement flags that block
     * movement in specific directions (east, west, north, south) and removes
     * unreachable tiles based on collision data.
     *
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
                var point = kvp.getKey();
                LocalPoint localPoint;
                if (client.getTopLevelWorldView().isInstance()) {
                    WorldPoint worldPoint = WorldPoint.toLocalInstance(client.getTopLevelWorldView(), point).stream().findFirst().orElse(null);
                    if (worldPoint == null) break;
                    localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
                } else
                    localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), point);

                CollisionData[] collisionMap = client.getTopLevelWorldView().getCollisionMaps();
                if (collisionMap != null && localPoint != null) {
                    CollisionData collisionData = collisionMap[client.getTopLevelWorldView().getPlane()];
                    int[][] flags = collisionData.getFlags();
                    int data = flags[localPoint.getSceneX()][localPoint.getSceneY()];

                    Set<MovementFlag> movementFlags = MovementFlag.getSetFlags(data);

                    if (!ignoreCollision && !tile.equals(point)) {
                        if (movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_FULL)
                                || movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_FLOOR)) {
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
     * This method checks if a given target tile (WorldPoint) is reachable from the
     * player's current location, considering collision data and the plane of the
     * world. The method uses a breadth-first search (BFS) algorithm to traverse
     * neighboring tiles while checking for movement blocks in the four cardinal
     * directions (north, south, east, west). It ensures the target tile is within
     * the same plane as the player and that movement between tiles is not blocked.
     *
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

        final WorldPoint playerLoc = playerService.getLocation();
        if (playerLoc == null) return false;

        if (targetPoint.getPlane() != playerLoc.getPlane()) return false;

        final boolean[][] visited = new boolean[FLAG_DATA_SIZE][FLAG_DATA_SIZE];
        final int[][] flags = getFlags();
        if (flags == null) return false;

        final int startX;
        final int startY;
        if (client.getTopLevelWorldView().getScene().isInstance()) {
            LocalPoint localPoint = playerService.getLocalLocation();
            startX = localPoint.getSceneX();
            startY = localPoint.getSceneY();
        } else {
            startX = playerLoc.getX() - client.getTopLevelWorldView().getBaseX();
            startY = playerLoc.getY() - client.getTopLevelWorldView().getBaseY();
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
     *
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
        if (client.getTopLevelWorldView().getScene().isInstance()) {
            LocalPoint localPoint = playerService.getLocalLocation();
            x = localPoint.getSceneX();
            y = localPoint.getSceneY();
        } else {
            baseX = client.getTopLevelWorldView().getBaseX();
            baseY = client.getTopLevelWorldView().getBaseY();
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
     *
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
        final WorldView wv = client.getTopLevelWorldView();
        if (wv == null) return null;

        final CollisionData[] collisionData = wv.getCollisionMaps();
        if (collisionData == null) return null;

        return collisionData[wv.getPlane()].getFlags();
    }

    /**
     * This method checks if the given coordinates (x, y) are within the valid bounds
     * of the game world grid. It ensures that the coordinates are non-negative and
     * within the range of the grid dimensions (0 to 103 for both x and y).
     *
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
     *
     * If the WorldPoint is out of bounds or the LocalPoint is null, the method returns null
     * to indicate that no valid tile is found at the given coordinates.
     *
     * @param x The x-coordinate of the tile.
     * @param y The y-coordinate of the tile.
     * @return The Tile at the specified coordinates, or null if the tile is invalid or not in the scene.
     */
    public Tile getTile(int x, int y) {
        WorldPoint worldPoint = new WorldPoint(x, y, client.getTopLevelWorldView().getPlane());
        LocalPoint localPoint;

        if (client.getTopLevelWorldView().getScene().isInstance()) {
            localPoint = fromWorldInstance(worldPoint);
        } else {
            localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
        }

        if (localPoint == null) return null;
        return client.getTopLevelWorldView().getScene().getTiles()[worldPoint.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
    }

    /**
     * Used to convert a WorldPoint in an instance to a LocalPoint
     * @param worldPoint The world point to convert
     * @return A local point representing the same global world point
     */
    public LocalPoint fromWorldInstance(WorldPoint worldPoint) {
        int[][][] instanceTemplateChunks = client.getTopLevelWorldView().getInstanceTemplateChunks();
        // Extract the coordinates from the WorldPoint
        int worldX = worldPoint.getX();
        int worldY = worldPoint.getY();
        int worldPlane = client.getTopLevelWorldView().getPlane();

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
                    return  LocalPoint.fromScene(localX, localY, client.getTopLevelWorldView());
                }
            }
        }
        return null;
    }

    /**
     * Rotate the coordinates in the chunk according to chunk rotation
     *
     * @param point point
     * @param rotation rotation
     * @return world point
     */
    public static WorldPoint rotate(WorldPoint point, int rotation) {
        int chunkX = point.getX() & ~(CHUNK_SIZE - 1);
        int chunkY = point.getY() & ~(CHUNK_SIZE - 1);
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

    public static Integer worldToLocalDistance(int distance) {
        return distance * Perspective.LOCAL_TILE_SIZE;
    }
}
