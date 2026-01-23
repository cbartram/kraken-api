package com.kraken.api.service.pathfinding;

import com.kraken.api.Context;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.OverlayUtil;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.*;
import java.util.List;


/**
 * <p>
 * The {@code LocalPathfinder} class is responsible for pathfinding within a local 104x104 tile 3D game scene.
 * It provides methods to compute paths using Breadth First Search (BFS),
 * determine sparse paths for waypoints where directional changes occur, render paths visually, and validate
 * the reachability of points within the currently loaded scene. This class is useful for
 * AI, navigation, and player movement scenarios.
 * </p>
 *
 * <p>
 * The class supports the following functionalities:
 * </p>
 * <ul>
 *   <li>Compute the shortest path between points in a game scene, including computing sparse waypoints with {@link #findSparsePath(WorldPoint, WorldPoint)}.</li>
 *   <li>Verify whether target points are within the loaded scene or reachable from a specific point using {@link #isInScene(WorldPoint)}.</li>
 *   <li>Render computed paths on a graphical interface using methods like {@link #renderMinimapPath(List, Graphics2D, Color)} and {@link #renderPath(List, Graphics2D, Color)}.</li>
 *   <li>Handle approximate pathfinding if exact target points are not reachable.</li>
 * </ul>
 */
@Slf4j
@Singleton
public class LocalPathfinder {
    private static final int SCENE_SIZE = 104;
    private static final int[] DIR_X = {-1, 1,  0, 0, -1, 1, -1, 1};
    private static final int[] DIR_Y = { 0, 0, -1, 1, -1, -1, 1, 1};


    @Inject
    private Context ctx;

    @Inject
    private Client client;

    /**
     * Finds a sparse path between a starting point and a target point by filtering
     * out unnecessary intermediate points from a previously computed dense path.
     *
     * <p>The method calculates directional changes in the dense path and retains
     * only the waypoints where the direction changes, along with the final destination.
     * This ensures a simplified path that accurately represents the required turns or
     * path changes while omitting redundant points.</p>
     *
     * @param start {@literal @}WorldPoint representing the starting location of the path.
     * @param target {@literal @}WorldPoint representing the destination point of the path.
     * @return A {@literal @}List of {@literal @}WorldPoint objects representing the sparse path.
     *         Returns an empty list if no path can be computed.
     */
    public List<WorldPoint> findSparsePath(WorldPoint start, WorldPoint target) {
        List<WorldPoint> densePath = findPath(start, target);

        if (densePath.isEmpty()) {
            return Collections.emptyList();
        }

        List<WorldPoint> sparsePath = new ArrayList<>();
        WorldPoint prev = start;

        // Iterate through the path up to the second-to-last point
        for (int i = 0; i < densePath.size() - 1; i++) {
            WorldPoint current = densePath.get(i);
            WorldPoint next = densePath.get(i + 1);

            // Calculate incoming direction (Previous -> Current)
            int d1x = current.getX() - prev.getX();
            int d1y = current.getY() - prev.getY();

            // outgoing direction (Current -> Next)
            int d2x = next.getX() - current.getX();
            int d2y = next.getY() - current.getY();

            // If the direction changes (vectors don't match), 'current' is a waypoint
            if (d1x != d2x || d1y != d2y) {
                sparsePath.add(current);
            }

            // Move our tracking point forward
            prev = current;
        }

        // Always add the final target destination
        sparsePath.add(densePath.get(densePath.size() - 1));

        return sparsePath;
    }

    /**
     * Calculates and returns a path from a starting point to a target point within the game world.
     * If the target point is outside the scene, the method attempts to determine the edge of the
     * scene closest to the target and calculates a path to that edge instead.
     *
     * <p>If the target point is within the loaded scene, the method directly computes the path
     * to the target using the {@code findScenePath} method. If the target is outside the scene,
     * it finds the nearest edge point to the target and calculates a path to that point.</p>
     *
     * @param start {@literal @}WorldPoint representing the starting location of the path.
     * @param target {@literal @}WorldPoint representing the destination point of the path.
     * @return A {@literal @}List of {@literal @}WorldPoint objects representing the calculated path
     *         from the start to the target (or closest reachable edge point). If no path can be calculated,
     *         an empty list is returned.
     */
    public List<WorldPoint> findPath(WorldPoint start, WorldPoint target) {
        return ctx.runOnClientThread(() -> {
            List<WorldPoint> waypoints = findWaypointsTo(start, target);
            if (waypoints == null || waypoints.isEmpty()) {
                return null;
            }

            if (!waypoints.get(waypoints.size() - 1).equals(target)) {
                return null;
            }

            waypoints.add(0, start);
            List<WorldPoint> fullPath = new ArrayList<>();
            for(int i = 0; i < waypoints.size() - 1; i++) {
                WorldPoint s = waypoints.get(i);
                WorldPoint end = waypoints.get(i + 1);
                int dx = Integer.signum(end.getX() - s.getX());
                int dy = Integer.signum(end.getY() - s.getY());
                WorldPoint current = s;
                if (i == 0) {
                    fullPath.add(current);
                }

                while (!current.equals(end)) {
                    current = current.dx(dx).dy(dy);
                    fullPath.add(current);
                }
            }
            return fullPath;
        });
    }

    /**
     * Attempts to find a path to the target. If the target is unreachable, it attempts to find
     * a path to a tile closer to the start point by "backing off" from the target in an
     * exponential/incremental fashion.
     *
     * <p>The backoff strategy works by calculating points along the line between the target
     * and the start. It steps back by 1 tile, then 3, then 6, then 10, etc., until a
     * reachable path is found or the search backs up all the way to the start.</p>
     *
     * @param start  The starting WorldPoint.
     * @param target The desired target WorldPoint.
     * @return A List of WorldPoints representing the path to the target or the best approximate
     * location found. Returns an empty list if no path can be found.
     */
    public List<WorldPoint> findPathWithBackoff(WorldPoint start, WorldPoint target) {
        List<WorldPoint> path = findPath(start, target);
        if (path != null && !path.isEmpty()) {
            return path;
        }

        // Calculate the straight-line distance to determine our bounds
        int totalDistance = start.distanceTo(target);

        if (totalDistance <= 1) {
            return Collections.emptyList();
        }

        int currentBackoff = 0;
        int step = 1;

        double dx = target.getX() - start.getX();
        double dy = target.getY() - start.getY();

        // We continue as long as we haven't backed off past the start point
        while (currentBackoff < totalDistance) {
            // Increase backoff distance by the current step (1, then 2, then 3...)
            // This creates the sequence: 1, 3, 6, 10, 15...
            currentBackoff += step;
            step++;

            if (currentBackoff >= totalDistance) {
                break; // We have backed off all the way to (or past) the start
            }

            // Calculate the percentage of the distance we want to travel (from start)
            // If backoff is 1, and total is 10, we want to go 90% of the way (0.9)
            double ratio = (double) (totalDistance - currentBackoff) / totalDistance;

            // Linear Interpolation (Lerp) to find the new candidate coordinates
            int newX = (int) Math.round(start.getX() + (dx * ratio));
            int newY = (int) Math.round(start.getY() + (dy * ratio));

            WorldPoint candidate = new WorldPoint(newX, newY, target.getPlane());

            // Check if this new candidate point is reachable
            path = findPath(start, candidate);
            if (path != null && !path.isEmpty()) {
                log.info("Found backoff path to {} (Original target: {}, Backoff tiles: {})",
                        candidate, target, currentBackoff);
                return path;
            }
        }

        // No path found even after full backoff
        return Collections.emptyList();
    }


    /**
     * Finds an approximate path to a random reachable tile within a default radius of 5 tiles
     * around the target location.
     *
     * @param start The starting WorldPoint.
     * @param target The target WorldPoint.
     * @return A list of WorldPoints representing the path to the approximate target.
     */
    public List<WorldPoint> findApproximatePath(WorldPoint start, WorldPoint target) {
        return findApproximatePath(start, target, 5);
    }

    /**
     * Finds an approximate path to a random reachable tile within a specified radius
     * around the target location.
     *
     * <p>This method first calculates all reachable tiles from the start point using BFS.
     * It then filters these tiles to find ones that lie within the specified square radius
     * (Chebyshev distance) of the target point. Finally, it selects one of these candidates
     * at random and computes a path to it.</p>
     *
     * @param start The starting WorldPoint.
     * @param target The target WorldPoint.
     * @param radius The radius (in tiles) around the target to search for reachable tiles.
     * @return A list of WorldPoints representing the path to the approximate target.
     */
    public List<WorldPoint> findApproximatePath(WorldPoint start, WorldPoint target, int radius) {
        List<WorldPoint> reachable = reachableTiles(start);

        if (reachable == null || reachable.isEmpty()) {
            return Collections.emptyList();
        }

        List<WorldPoint> candidates = new ArrayList<>();
        int tX = target.getX();
        int tY = target.getY();
        int plane = target.getPlane();

        for (WorldPoint p : reachable) {
            if (p.getPlane() != plane) {
                continue;
            }

            int dx = Math.abs(p.getX() - tX);
            int dy = Math.abs(p.getY() - tY);

            if (dx <= radius && dy <= radius) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        WorldPoint destination = candidates.get(new Random().nextInt(candidates.size()));
        return findPath(start, destination);
    }

    /**
     * Finds an approximate path to a random reachable tile within a specified WorldArea.
     *
     * @param start The starting WorldPoint.
     * @param area The WorldArea to search for reachable tiles within.
     * @return A list of WorldPoints representing the path to a random point within the area.
     */
    public List<WorldPoint> findApproximatePath(WorldPoint start, WorldArea area) {
        List<WorldPoint> reachable = reachableTiles(start);

        if (reachable == null || reachable.isEmpty()) {
            return Collections.emptyList();
        }

        List<WorldPoint> candidates = new ArrayList<>();

        for (WorldPoint p : reachable) {
            if (area.contains(p)) {
                candidates.add(p);
            }
        }

        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        WorldPoint destination = candidates.get(new Random().nextInt(candidates.size()));
        return findPath(start, destination);
    }

    /**
     * Returns a list of all reachable tiles from the origins position using a breadth-first search algorithm.
     * This method considers the collision data to determine which tiles can be reached.
     *
     * @param origin The point to query from
     * @return A list of WorldPoint objects representing all reachable tiles from the origin.
     */
    public List<WorldPoint> reachableTiles(WorldPoint origin) {
        return ctx.runOnClientThread(() -> {
            Client client = ctx.getClient();
            boolean[][] visited = new boolean[104][104];
            CollisionData[] collisionData = client.getTopLevelWorldView().getCollisionMaps();
            if (collisionData == null) {
                return new ArrayList<>();
            }
            WorldView worldView = client.getTopLevelWorldView();
            int[][] flags = collisionData[worldView.getPlane()].getFlags();
            int firstPoint = (origin.getX()-worldView.getBaseX() << 16) | origin.getY()-worldView.getBaseY();
            ArrayDeque<Integer> queue = new ArrayDeque<>();
            queue.add(firstPoint);
            while (!queue.isEmpty()) {
                int point = queue.poll();
                short x =(short)(point >> 16);
                short y = (short)point;
                if (y < 0 || x < 0 || y > 104 || x > 104) {
                    continue;
                }
                if ((flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0 && (flags[x][y - 1] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && !visited[x][y - 1]) {
                    queue.add((x << 16) | (y - 1));
                    visited[x][y - 1] = true;
                }
                if ((flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0 && (flags[x][y + 1] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && !visited[x][y + 1]) {
                    queue.add((x << 16) | (y + 1));
                    visited[x][y + 1] = true;
                }
                if ((flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0 && (flags[x - 1][y] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && !visited[x - 1][y]) {
                    queue.add(((x - 1) << 16) | y);
                    visited[x - 1][y] = true;
                }
                if ((flags[x][y] & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0 && (flags[x + 1][y] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && !visited[x + 1][y]) {
                    queue.add(((x + 1) << 16) | y);
                    visited[x + 1][y] = true;
                }
            }
            int baseX = worldView.getBaseX();
            int baseY = worldView.getBaseY();
            int plane = worldView.getPlane();
            List<WorldPoint> finalPoints = new ArrayList<>();
            for (int x = 0; x < 104; ++x) {
                for (int y = 0; y < 104; ++y) {
                    if (visited[x][y]) {
                        finalPoints.add(new WorldPoint(baseX + x, baseY + y, plane));
                    }
                }
            }
            return finalPoints;
        });
    }
    
    public WorldPoint findEdgeOfScene(WorldPoint target) {
        return ctx.runOnClientThread(() -> {
            WorldView wv = client.getTopLevelWorldView();
            WorldPoint base = new WorldPoint(wv.getBaseX(), wv.getBaseY(), wv.getPlane());
            
            int sceneMinX = base.getX();
            int sceneMinY = base.getY();
            int sceneMaxX = sceneMinX + SCENE_SIZE - 1;
            int sceneMaxY = sceneMinY + SCENE_SIZE - 1;

            int targetX = target.getX();
            int targetY = target.getY();

            int clampedX = Math.max(sceneMinX, Math.min(targetX, sceneMaxX));
            int clampedY = Math.max(sceneMinY, Math.min(targetY, sceneMaxY));

            return new WorldPoint(clampedX, clampedY, wv.getPlane());
        });
    }

    /**
     * Finds the waypoints between two {@literal @}WorldPoint locations on the same plane.
     * The method calculates the intermediate checkpoint waypoints required to navigate
     * from the starting point to the destination point.
     * <p>
     * It performs computations by translating the world points into
     * local points within the scene and traversing the path using available
     * tiles.
     * </p>
     *
     * Credit to Vitalite and TonicBox for this methods implementation. It was taken from their scene API:
     * <a href="https://github.com/Tonic-Box/VitaLite/blob/main/api/src/main/java/com/tonic/api/game/SceneAPI.java">Link</a>
     *
     * @param from the starting {@literal @}WorldPoint.
     *             Must not be {@literal null}. The {@literal @}WorldPoint
     *             must exist on the same plane as the {@code to} point.
     * @param to   the destination {@literal @}WorldPoint.
     *             Must not be {@literal null}. The {@literal @}WorldPoint
     *             must exist on the same plane as the {@code from} point.
     *
     * @return a {@literal @}List of {@literal @}WorldPoint objects
     *         representing checkpoint waypoints to navigate from {@code from}
     *         to {@code to}. If a direct path cannot be calculated, the list
     *         may be empty. Returns {@literal null} if the points
     *         are on different planes or if any required data is unavailable.
     */
    private List<WorldPoint> findWaypointsTo(WorldPoint from, WorldPoint to) {
        return ctx.runOnClientThread(() -> {
            if (from.getPlane() != to.getPlane()) {
                return null;
            }

            Client client = ctx.getClient();
            WorldView worldView = client.getTopLevelWorldView();
            int x = from.getX();
            int y = from.getY();
            int plane = from.getPlane();

            LocalPoint sourceLp = LocalPoint.fromWorld(worldView, x, y);
            LocalPoint targetLp = LocalPoint.fromWorld(worldView, to.getX(), to.getY());
            if (sourceLp == null || targetLp == null) {
                return null;
            }

            int thisX = sourceLp.getSceneX();
            int thisY = sourceLp.getSceneY();
            int otherX = targetLp.getSceneX();
            int otherY = targetLp.getSceneY();

            Tile[][][] tiles = worldView.getScene().getTiles();
            Tile sourceTile = tiles[plane][thisX][thisY];
            Tile targetTile = tiles[plane][otherX][otherY];

            if(sourceTile == null || targetTile == null)
                return new ArrayList<>();

            List<Tile> checkpointTiles = findWaypointsTo(sourceTile, targetTile);
            if (checkpointTiles == null) {
                return null;
            }
            List<WorldPoint> checkpointWPs = new ArrayList<>();
            for (Tile checkpointTile : checkpointTiles) {
                if (checkpointTile == null) {
                    break;
                }
                checkpointWPs.add(checkpointTile.getWorldLocation());
            }
            return checkpointWPs;
        });
    }


    /**
     * Finds the waypoints needed to navigate from the starting {@code Tile} to the destination {@code Tile}.
     * This method calculates a path using directional and distance matrices, while considering collision data
     * within the game world. If a direct path is not possible, it searches for the closest accessible tile
     * around the destination.
     *
     * <p>Note that both the starting and destination tiles must reside on the same plane (z-coordinate).
     * If they are not, this method will return {@code null}.</p>
     *
     * Credit to Vitalite and TonicBox for this methods implementation. It was taken from their scene API:
     * <a href="https://github.com/Tonic-Box/VitaLite/blob/main/api/src/main/java/com/tonic/api/game/SceneAPI.java">Link</a>
     *
     * @param from the starting {@code Tile} from which the path needs to be calculated.
     * @param to the destination {@code Tile} to which the path needs to lead.
     * @return a {@code List} of {@code Tile} objects representing the calculated waypoints to the destination,
     *         or {@code null} if the path cannot be calculated (e.g., due to inaccessible areas or mismatched planes).
     */
    public List<Tile> findWaypointsTo(Tile from, Tile to) {
        return ctx.runOnClientThread(() -> {
            int z = from.getPlane();
            if (z != to.getPlane()) {
                return null;
            }

            Client client = ctx.getClient();
            WorldView worldView = client.getTopLevelWorldView();
            CollisionData[] collisionData = worldView.getCollisionMaps();
            if (collisionData == null) {
                return null;
            }

            int[][] directions = new int[128][128];
            int[][] distances = new int[128][128];
            int[] bufferX = new int[4096];
            int[] bufferY = new int[4096];

            // Initialise directions and distances
            for (int i = 0; i < 128; ++i) {
                for (int j = 0; j < 128; ++j) {
                    directions[i][j] = 0;
                    distances[i][j] = Integer.MAX_VALUE;
                }
            }

            Point p1 = from.getSceneLocation();
            Point p2 = to.getSceneLocation();

            int middleX = p1.getX();
            int middleY = p1.getY();
            int currentX = middleX;
            int currentY = middleY;
            int offsetX = 64;
            int offsetY = 64;

            // Initialize directions and distances for the starting tile
            directions[offsetX][offsetY] = 99;
            distances[offsetX][offsetY] = 0;
            int index1 = 0;
            bufferX[0] = currentX;
            int index2 = 1;
            bufferY[0] = currentY;
            int[][] collisionDataFlags = collisionData[z].getFlags();

            boolean isReachable = false;

            while (index1 != index2) {
                currentX = bufferX[index1];
                currentY = bufferY[index1];
                index1 = index1 + 1 & 4095;
                // currentX is for the local coordinate while currentMapX is for the index in the directions and distances arrays
                int currentMapX = currentX - middleX + offsetX;
                int currentMapY = currentY - middleY + offsetY;
                if ((currentX == p2.getX()) && (currentY == p2.getY()))
               {
                    isReachable = true;
                    break;
                }

                int currentDistance = distances[currentMapX][currentMapY] + 1;
                if (currentMapX > 0 && directions[currentMapX - 1][currentMapY] == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0) {
                    // Able to move 1 tile west
                    bufferX[index2] = currentX - 1;
                    bufferY[index2] = currentY;
                    index2 = index2 + 1 & 4095;
                    directions[currentMapX - 1][currentMapY] = 2;
                    distances[currentMapX - 1][currentMapY] = currentDistance;
                }

                if (currentMapX < 127 && directions[currentMapX + 1][currentMapY] == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0) {
                    // Able to move 1 tile east
                    bufferX[index2] = currentX + 1;
                    bufferY[index2] = currentY;
                    index2 = index2 + 1 & 4095;
                    directions[currentMapX + 1][currentMapY] = 8;
                    distances[currentMapX + 1][currentMapY] = currentDistance;
                }

                if (currentMapY > 0 && directions[currentMapX][currentMapY - 1] == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0) {
                    // Able to move 1 tile south
                    bufferX[index2] = currentX;
                    bufferY[index2] = currentY - 1;
                    index2 = index2 + 1 & 4095;
                    directions[currentMapX][currentMapY - 1] = 1;
                    distances[currentMapX][currentMapY - 1] = currentDistance;
                }

                if (currentMapY < 127 && directions[currentMapX][currentMapY + 1] == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0) {
                    // Able to move 1 tile north
                    bufferX[index2] = currentX;
                    bufferY[index2] = currentY + 1;
                    index2 = index2 + 1 & 4095;
                    directions[currentMapX][currentMapY + 1] = 4;
                    distances[currentMapX][currentMapY + 1] = currentDistance;
                }

                if (currentMapX > 0 && currentMapY > 0 && directions[currentMapX - 1][currentMapY - 1] == 0 && (collisionDataFlags[currentX - 1][currentY - 1] & 19136782) == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0) {
                    // Able to move 1 tile south-west
                    bufferX[index2] = currentX - 1;
                    bufferY[index2] = currentY - 1;
                    index2 = index2 + 1 & 4095;
                    directions[currentMapX - 1][currentMapY - 1] = 3;
                    distances[currentMapX - 1][currentMapY - 1] = currentDistance;
                }

                if (currentMapX < 127 && currentMapY > 0 && directions[currentMapX + 1][currentMapY - 1] == 0 && (collisionDataFlags[currentX + 1][currentY - 1] & 19136899) == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0 && (collisionDataFlags[currentX][currentY - 1] & 19136770) == 0) {
                    // Able to move 1 tile north-west
                    bufferX[index2] = currentX + 1;
                    bufferY[index2] = currentY - 1;
                    index2 = index2 + 1 & 4095;
                    directions[currentMapX + 1][currentMapY - 1] = 9;
                    distances[currentMapX + 1][currentMapY - 1] = currentDistance;
                }
                if (currentMapX > 0 && currentMapY < 127 && directions[currentMapX - 1][currentMapY + 1] == 0 && (collisionDataFlags[currentX - 1][currentY + 1] & 19136824) == 0 && (collisionDataFlags[currentX - 1][currentY] & 19136776) == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0) {
                    // Able to move 1 tile south-east
                    bufferX[index2] = currentX - 1;
                    bufferY[index2] = currentY + 1;
                    index2 = index2 + 1 & 4095;
                    directions[currentMapX - 1][currentMapY + 1] = 6;
                    distances[currentMapX - 1][currentMapY + 1] = currentDistance;
                }

                if (currentMapX < 127 && currentMapY < 127 && directions[currentMapX + 1][currentMapY + 1] == 0 && (collisionDataFlags[currentX + 1][currentY + 1] & 19136992) == 0 && (collisionDataFlags[currentX + 1][currentY] & 19136896) == 0 && (collisionDataFlags[currentX][currentY + 1] & 19136800) == 0) {
                    // Able to move 1 tile north-east
                    bufferX[index2] = currentX + 1;
                    bufferY[index2] = currentY + 1;
                    index2 = index2 + 1 & 4095;
                    directions[currentMapX + 1][currentMapY + 1] = 12;
                    distances[currentMapX + 1][currentMapY + 1] = currentDistance;
                }
            }

            if (!isReachable) {
                // Try find a different reachable tile in the 21x21 area around the target tile, as close as possible to the target tile
                int upperboundDistance = Integer.MAX_VALUE;
                int pathLength = Integer.MAX_VALUE;
                int checkRange = 10;
                int approxDestinationX = p2.getX();
                int approxDestinationY = p2.getY();
                for (int i = approxDestinationX - checkRange; i <= checkRange + approxDestinationX; ++i) {
                    for (int j = approxDestinationY - checkRange; j <= checkRange + approxDestinationY; ++j) {
                        int currentMapX = i - middleX + offsetX;
                        int currentMapY = j - middleY + offsetY;
                        if (currentMapX >= 0 && currentMapY >= 0 && currentMapX < 128 && currentMapY < 128 && distances[currentMapX][currentMapY] < 100) {
                            int deltaX = 0;
                            if (i < approxDestinationX) {
                                deltaX = approxDestinationX - i;
                            } else if (i > approxDestinationX) {
                                deltaX = i - (approxDestinationX);
                            }

                            int deltaY = 0;
                            if (j < approxDestinationY) {
                                deltaY = approxDestinationY - j;
                            } else if (j > approxDestinationY) {
                                deltaY = j - (approxDestinationY);
                            }

                            int distanceSquared = deltaX * deltaX + deltaY * deltaY;
                            if (distanceSquared < upperboundDistance || distanceSquared == upperboundDistance && distances[currentMapX][currentMapY] < pathLength) {
                                upperboundDistance = distanceSquared;
                                pathLength = distances[currentMapX][currentMapY];
                                currentX = i;
                                currentY = j;
                            }
                        }
                    }
                }

                if (upperboundDistance == Integer.MAX_VALUE) {
                    log.error("No path found between: {} and {}, check that the tile is within the local scene.", from.getWorldLocation(), to.getWorldLocation());
                    return null;
                }
            }

            // Getting path from directions and distances
            bufferX[0] = currentX;
            bufferY[0] = currentY;
            int index = 1;
            int directionNew;
            int directionOld;
            for (directionNew = directionOld = directions[currentX - middleX + offsetX][currentY - middleY + offsetY]; p1.getX() != currentX || p1.getY() != currentY; directionNew = directions[currentX - middleX + offsetX][currentY - middleY + offsetY]) {
                if (directionNew != directionOld) {
                    // "Corner" of the path --> new checkpoint tile
                    directionOld = directionNew;
                    bufferX[index] = currentX;
                    bufferY[index++] = currentY;
                }

                if ((directionNew & 2) != 0) {
                    ++currentX;
                } else if ((directionNew & 8) != 0) {
                    --currentX;
                }

                if ((directionNew & 1) != 0) {
                    ++currentY;
                } else if ((directionNew & 4) != 0) {
                    --currentY;
                }
            }

            int checkpointTileNumber = 1;
            Tile[][][] tiles = worldView.getScene().getTiles();
            List<Tile> checkpointTiles = new ArrayList<>();

            while (index-- > 0) {
                checkpointTiles.add(tiles[from.getPlane()][bufferX[index]][bufferY[index]]);
                if (checkpointTileNumber == 25) {
                    break;
                }
                checkpointTileNumber++;
            }
            return checkpointTiles;
        });
    }

    /**
     * Returns true if the source point is walkable to the target point given the constraints
     * of collision flags.
     * @param flags Game Collision flags
     * @param source Source World Point
     * @param target Target World Point
     * @return True if there is a walkable path between the two points
     */
    private boolean isWalkable(int[][] flags, WorldPoint source, WorldPoint target) {
        int sX = source.getX() - client.getTopLevelWorldView().getBaseX();
        int sY = source.getY() - client.getTopLevelWorldView().getBaseY();
        int tX = target.getX() - client.getTopLevelWorldView().getBaseX();
        int tY = target.getY() - client.getTopLevelWorldView().getBaseY();

        int dx = tX - sX;
        int dy = tY - sY;

        int sourceFlags = flags[sX][sY];
        int targetFlags = flags[tX][tY];

        if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0) return false;

        if (dx == 0 && dy == 0) return true;

        // Cardinal
        if (dx == 0 || dy == 0) {
            if (dx > 0) return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0 && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0;
            if (dx < 0) return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) == 0 && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) == 0;
            if (dy > 0) return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0 && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0;
            return (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) == 0 && (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
        }

        // Diagonal
        // dx != 0 && dy != 0 is implicitly true here
        int xFlags = flags[sX + dx][sY];
        int yFlags = flags[sX][sY + dy];

        if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0) return false;

        if (dx > 0 && dy > 0) { // NE
            if ((sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0 || (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) return false;
            if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0 || (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) return false;
            return (xFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && (yFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
        }
        if (dx < 0 && dy > 0) { // NW
            if ((sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0 || (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) return false;
            if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0 || (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) return false;
            return (xFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && (yFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
        }
        if (dx > 0 && dy < 0) { // SE
            if ((sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0 || (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) return false;
            if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0 || (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) return false;
            return (xFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && (yFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
        }
        if (dx < 0 && dy < 0) { // SW
            if ((sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0 || (sourceFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) return false;
            if ((targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0 || (targetFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) return false;
            return (xFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0 && (yFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) == 0;
        }

        return false;
    }

    /**
     * Renders a path on the minimap.
     *
     * @param path     The list of WorldPoints representing the path.
     * @param graphics The graphics context to draw on.
     * @param color    The color to use for drawing the path.
     */
    public void renderMinimapPath(List<WorldPoint> path, Graphics2D graphics, Color color) {
        if(graphics == null || path == null || path.isEmpty()) {
            return;
        }

        for (WorldPoint point : path) {
            renderMinimapPoint(graphics, point, color);
        }
    }

    /**
     * Renders a single point on the minimap.
     *
     * @param graphics The graphics context to draw on.
     * @param point    The WorldPoint to render.
     * @param color    The color to use for rendering the node.
     */
    private void renderMinimapPoint(Graphics2D graphics, WorldPoint point, Color color) {
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        if (point.distanceTo(playerLocation) >= 16) {
            return;
        }

        LocalPoint lp = LocalPoint.fromWorld(client, point);
        if (lp == null) {
            return;
        }

        Point miniMapPosition = Perspective.localToMinimap(client, lp);
        if (miniMapPosition == null) {
            return;
        }

        OverlayUtil.renderMinimapLocation(graphics, miniMapPosition, color);
    }


    /**
     * Renders a series of tiles representing a path on the game canvas.
     * This includes drawing connected lines between the tiles and optionally
     * highlighting the last tile in the path.
     *
     * <p> The method uses the provided {@literal Graphics2D} instance to draw
     * on the screen and a {@literal Color} to style the tiles.
     *
     * @param path       The list of {@literal WorldPoint} objects representing the path.
     *                   Each point is rendered on the game canvas.
     * @param graphics   The {@literal Graphics2D} instance used to render the path on the screen.
     * @param pathColor  The {@literal Color} used to draw the tiles on the path. The last tile
     *                   is highlighted in red with partial transparency.
     */
    public void renderPath(List<WorldPoint> path, Graphics2D graphics, Color pathColor) {
        if (path == null || path.isEmpty()) {
            return;
        }

        Color endColor = new Color(255, 0, 0, 150);

        int i = 0;
        for (WorldPoint point : path) {
            if (point.getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }

            LocalPoint lp = LocalPoint.fromWorld(client, point);
            if (lp == null) {
                continue;
            }

            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly != null) {
                if (i == path.size() - 1) {
                    graphics.setColor(endColor);
                    graphics.fill(poly);
                } else {
                    graphics.setColor(pathColor);
                    graphics.setStroke(new BasicStroke(2));
                    graphics.draw(poly);
                }
            }
            i++;
        }

        // Draw lines connecting the tiles for a "route" look
        if(path.size() < 2) {
            return;
        }

        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(1));

        Point prevCenter = null;

        // Add player location as the start of the line
        LocalPoint playerLp = client.getLocalPlayer().getLocalLocation();
        if (playerLp != null) {
            prevCenter = Perspective.localToCanvas(client, playerLp, client.getTopLevelWorldView().getPlane());
        }

        for (WorldPoint point : path) {
            LocalPoint lp = LocalPoint.fromWorld(client, point);
            if (lp == null) continue;

            Point center = Perspective.localToCanvas(client, lp, client.getTopLevelWorldView().getPlane());
            if (center != null && prevCenter != null) {
                graphics.drawLine(prevCenter.getX(), prevCenter.getY(), center.getX(), center.getY());
            }
            prevCenter = center;
        }
    }
}