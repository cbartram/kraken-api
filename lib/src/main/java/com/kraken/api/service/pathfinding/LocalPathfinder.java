package com.kraken.api.service.pathfinding;

import com.kraken.api.Context;
import com.kraken.api.service.util.RandomService;
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
import java.util.Queue;


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
        if (isInScene(target)) {
            return findScenePath(start, target);
        }

        WorldPoint edgePoint = findEdgeOfScene(target);
        if (edgePoint == null) {
            return Collections.emptyList();
        }

        return findScenePath(start, edgePoint);
    }

    /**
     * Finds an approximate path from a starting point to a target point within a given radius.
     * <p>
     * This method searches for paths using a breadth-first search (BFS) approach. If the target point is not within the game
     * scene, a full path is computed to the target. Otherwise, the method will look for an approximate path to a point within
     * the specified radius of the target that is traversable in the scene.
     *
     * @param start  the {@link WorldPoint} representing the starting point of the path.
     * @param target the {@link WorldPoint} representing the target point of the path.
     *               If it lies outside the game scene, a full path is calculated for this target.
     * @return a {@link List} of {@link WorldPoint} instances representing the path from the start to the selected point
     *         within the specified radius of the target. If no valid candidates are found, an approximate path to the target
     *         is returned. The list is in order of movement from start to destination.
     *
     * <p>Steps performed in the method:</p>
     * <ul>
     *   <li>If the target is not in the scene, calculate and return the full path to the target using {@code findPath}.</li>
     *   <li>Run a breadth-first search (BFS) to determine distances from the start point and record parent nodes.</li>
     *   <li>Search for all points within the specified radius of the target that are in the scene and traversable.</li>
     *   <li>If valid points are found and randomly select one of them, return the path to that point.</li>
     *   <li>If no valid points are found, calculate and return an approximate path to the target.</li>
     * </ul>
     */
    public List<WorldPoint> findApproximatePath(WorldPoint start, WorldPoint target) {
        return findApproximatePath(start, target, 5);
    }

    /**
     * Finds an approximate path from a starting point to a target point within a given radius.
     * <p>
     * This method searches for paths using a breadth-first search (BFS) approach. If the target point is not within the game
     * scene, a full path is computed to the target. Otherwise, the method will look for an approximate path to a point within
     * the specified radius of the target that is traversable in the scene.
     *
     * @param start  the {@link WorldPoint} representing the starting point of the path.
     * @param target the {@link WorldPoint} representing the target point of the path.
     *               If it lies outside the game scene, a full path is calculated for this target.
     * @param radius the radius, in tiles, within which the method will search for a traversable point near the target.
     * @return a {@link List} of {@link WorldPoint} instances representing the path from the start to the selected point
     *         within the specified radius of the target. If no valid candidates are found, an approximate path to the target
     *         is returned. The list is in order of movement from start to destination.
     *
     * <p>Steps performed in the method:</p>
     * <ul>
     *   <li>If the target is not in the scene, calculate and return the full path to the target using {@code findPath}.</li>
     *   <li>Run a breadth-first search (BFS) to determine distances from the start point and record parent nodes.</li>
     *   <li>Search for all points within the specified radius of the target that are in the scene and traversable.</li>
     *   <li>If valid points are found and randomly select one of them, return the path to that point.</li>
     *   <li>If no valid points are found, calculate and return an approximate path to the target.</li>
     * </ul>
     */
    public List<WorldPoint> findApproximatePath(WorldPoint start, WorldPoint target, int radius) {
        if (!isInScene(target)) {
            return findPath(start, target);
        }

        return ctx.runOnClientThread(() -> {
            BFSState bfsState = bfs(start, null);

            List<WorldPoint> candidates = new ArrayList<>();
            int targetX = target.getX() - client.getTopLevelWorldView().getBaseX();
            int targetY = target.getY() - client.getTopLevelWorldView().getBaseY();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    int x = targetX + dx;
                    int y = targetY + dy;

                    if (isInScene(x, y) && bfsState.dist[x][y] != -1) {
                        candidates.add(new WorldPoint(
                                client.getTopLevelWorldView().getBaseX() + x,
                                client.getTopLevelWorldView().getBaseY() + y,
                                client.getTopLevelWorldView().getPlane()
                        ));
                    }
                }
            }

            if (candidates.isEmpty()) {
                return findBestApproximatePath(target, bfsState.dist, bfsState.parent);
            }

            WorldPoint selected = candidates.get(new Random().nextInt(candidates.size()));
            return buildPath(bfsState.parent, selected);
        });
    }

    /**
     * Computes an approximate path from a starting point to a destination within a specified area.
     * This method identifies candidates within the area that are reachable and constructs a path
     * to one of the candidates. It uses breadth-first search (BFS) to calculate distances and possible paths.
     *
     * <p>The method performs pathfinding only if the specified area and starting point are on the same plane.</p>
     *
     * @param start the starting {@link WorldPoint} from which the pathfinding begins.
     * @param area the {@link WorldArea} that defines the bounds of the target area where the path should lead.
     *             The method will consider reachable points only within this area.
     *
     * @return a {@link List} of {@link WorldPoint}s representing the calculated path to a candidate
     *         within the defined area, or an empty list if no valid path could be determined.
     *
     * <ul>
     *     <li>Returns an empty list if the {@code area} is outside the current viewport or on a different plane.</li>
     *     <li>Returns an empty list if no reachable candidate points exist within the {@link WorldArea}.</li>
     * </ul>
     */
    public List<WorldPoint> findApproximatePath(WorldPoint start, WorldArea area) {
        return ctx.runOnClientThread(() -> {
            if (area.getPlane() != client.getTopLevelWorldView().getPlane()) {
                return Collections.emptyList();
            }

            BFSState bfsState = bfs(start, null);
            List<WorldPoint> candidates = new ArrayList<>();

            int sceneBaseX = client.getTopLevelWorldView().getBaseX();
            int sceneBaseY = client.getTopLevelWorldView().getBaseY();

            int minX = Math.max(area.getX(), sceneBaseX);
            int maxX = Math.min(area.getX() + area.getWidth() - 1, sceneBaseX + SCENE_SIZE - 1);
            int minY = Math.max(area.getY(), sceneBaseY);
            int maxY = Math.min(area.getY() + area.getHeight() - 1, sceneBaseY + SCENE_SIZE - 1);

            if (minX > maxX || minY > maxY) {
                return Collections.emptyList();
            }

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    int sx = x - sceneBaseX;
                    int sy = y - sceneBaseY;

                    if (bfsState.dist[sx][sy] != -1) {
                        candidates.add(new WorldPoint(x, y, area.getPlane()));
                    }
                }
            }

            if (candidates.isEmpty()) {
                return Collections.emptyList();
            }

            WorldPoint selected = candidates.get(RandomService.between(0, candidates.size() - 1));
            return buildPath(bfsState.parent, selected);
        });
    }
    
    private WorldPoint findEdgeOfScene(WorldPoint target) {
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

            return new WorldPoint(clampedX, clampedY, client.getTopLevelWorldView().getPlane());
        });
    }

    /**
     * Finds the shortest path between two points within the currently loaded
     * scene using a breadth-first search (BFS) algorithm. If the target point
     * is unreachable, the method returns an approximate path toward the target
     * based on available scene data.
     *
     * <p>The method ensures paths avoid collision flags and checks the accessibility
     * of neighboring tiles. It reconstructs the path upon completion of the BFS
     * using the parent array and returns an ordered list of path nodes. If the
     * target point is unreachable using valid scene tiles, this method falls back
     * to an approximation strategy to generate a path.
     *
     * @param start  {@literal @}WorldPoint representing the starting location.
     * @param target {@literal @}WorldPoint representing the target location.
     * @return A {@literal @}List of {@literal @}WorldPoint objects representing the
     *         path from the starting point to the target point. Returns an empty
     *         list if the target is unreachable.
     */
    private List<WorldPoint> findScenePath(WorldPoint start, WorldPoint target) {
        return ctx.runOnClientThread(() -> {
            if (start.equals(target)) return Collections.emptyList();

            BFSState bfsState = bfs(start, target);

            if (bfsState.targetReached) {
                return buildPath(bfsState.parent, target);
            } else {
                // Target not reached (unreachable or blocked).
                // Run the "Approximation" search
                return findBestApproximatePath(target, bfsState.dist, bfsState.parent);
            }
        });
    }

    private static class BFSState {
        final int[][] dist;
        final WorldPoint[][] parent;
        final boolean targetReached;

        BFSState(int[][] dist, WorldPoint[][] parent, boolean targetReached) {
            this.dist = dist;
            this.parent = parent;
            this.targetReached = targetReached;
        }
    }

    private BFSState bfs(WorldPoint start, WorldPoint target) {
        CollisionData[] collisionData = client.getTopLevelWorldView().getCollisionMaps();
        if (collisionData == null) return new BFSState(new int[SCENE_SIZE][SCENE_SIZE], new WorldPoint[SCENE_SIZE][SCENE_SIZE], false);

        int plane = client.getTopLevelWorldView().getPlane();
        int[][] flags = collisionData[plane].getFlags();

        int[][] dist = new int[SCENE_SIZE][SCENE_SIZE];
        for (int[] row : dist) Arrays.fill(row, -1);

        WorldPoint[][] parent = new WorldPoint[SCENE_SIZE][SCENE_SIZE];
        Queue<WorldPoint> queue = new ArrayDeque<>();

        int startX = start.getX() - client.getTopLevelWorldView().getBaseX();
        int startY = start.getY() - client.getTopLevelWorldView().getBaseY();

        if (!isInScene(startX, startY)) return new BFSState(dist, parent, false);

        dist[startX][startY] = 0;
        queue.add(start);

        boolean targetReached = false;

        while (!queue.isEmpty()) {
            WorldPoint current = queue.poll();

            if (target != null && current.equals(target)) {
                targetReached = true;
                break;
            }

            int cx = current.getX() - client.getTopLevelWorldView().getBaseX();
            int cy = current.getY() - client.getTopLevelWorldView().getBaseY();
            int currentDist = dist[cx][cy];

            for (int i = 0; i < 8; i++) {
                int nx = cx + DIR_X[i];
                int ny = cy + DIR_Y[i];

                if (!isInScene(nx, ny)) continue;

                if (dist[nx][ny] == -1) {
                    WorldPoint neighbor = new WorldPoint(
                            client.getTopLevelWorldView().getBaseX() + nx,
                            client.getTopLevelWorldView().getBaseY() + ny,
                            plane
                    );

                    if (isWalkable(flags, current, neighbor)) {
                        dist[nx][ny] = currentDist + 1;
                        parent[nx][ny] = current;
                        queue.add(neighbor);
                    }
                }
            }
        }

        return new BFSState(dist, parent, targetReached);
    }

    /**
     * Returns true if the World point X and Y coordinates are in the scene.
     * @param x X WorldPoint coordinate
     * @param y Y WorldPoint coordinate
     * @return True if the coordinates are in the 104x104 scene and false otherwise
     */
    private boolean isInScene(int x, int y) {
        return x >= 0 && y >= 0 && x < SCENE_SIZE && y < SCENE_SIZE;
    }

    /**
     * Returns true if the provided WorldPoint is in the currently loaded scene data.
     * @param worldPoint The point to check.
     * @return True if the point is in the scene, false otherwise.
     */
    public boolean isInScene(WorldPoint worldPoint) {
        return ctx.runOnClientThread(() -> {
            WorldPoint start = new WorldPoint(client.getTopLevelWorldView().getBaseX(), client.getTopLevelWorldView().getBaseY(), client.getTopLevelWorldView().getPlane());
            WorldPoint end = new WorldPoint(start.getX() + SCENE_SIZE - 1, start.getY() + SCENE_SIZE - 1, start.getPlane());
            return worldPoint.getX() >= start.getX() && worldPoint.getX() <= end.getX()
                    && worldPoint.getY() >= start.getY() && worldPoint.getY() <= end.getY()
                    && worldPoint.getPlane() == start.getPlane();
        });
    }

    /**
     * Finds the best approximate path to a target point based on both shortest path distance
     * and Euclidean proximity within the given constraints.
     *
     * <p>The method evaluates all tiles within a 21x21 grid centered on the target tile.
     * It prioritizes tiles based on shortest path distance and uses Euclidean proximity to
     * the actual target as a secondary sorting criterion. If a valid approximate point is
     * found, a path is built leading to that point. Otherwise, an empty path is returned.</p>
     *
     * @param target The target {@literal @}WorldPoint to which the method should compute an approximate path.
     * @param dist A 2D array of shortest path distances from the starting point to each tile in the scene.
     *             Distances of -1 indicate that no path exists to the given tile.
     * @param parent A 2D array representing the parent relationship of each tile in the scene,
     *               allowing the reconstruction of paths.
     * @return A {@literal @}List of {@literal @}WorldPoint objects representing the path to the best approximate target point.
     *         Returns an empty list if no valid path can be found.
     */
    private List<WorldPoint> findBestApproximatePath(WorldPoint target, int[][] dist, WorldPoint[][] parent) {
        int targetX = target.getX() - client.getTopLevelWorldView().getBaseX();
        int targetY = target.getY() - client.getTopLevelWorldView().getBaseY();

        int bestX = -1;
        int bestY = -1;
        int bestDist = Integer.MAX_VALUE;      // Path length from player
        double bestEuclidean = Double.MAX_VALUE; // Distance from approximation to actual target

        // "All tiles are considered within a 21x21 grid... centred at the clicked tile"
        // "Western priority then southern priority" implies loop order.
        // We scan from West (-10) to East (+10) and South (-10) to North (+10)
        for (int dx = -10; dx <= 10; dx++) {
            for (int dy = -10; dy <= 10; dy++) {
                int x = targetX + dx;
                int y = targetY + dy;

                if (!isInScene(x, y)) continue;

                // "Exists a previously found path" -> dist must not be -1
                if (dist[x][y] != -1) {

                    if (dist[x][y] >= 100) continue;

                    double euclidean = Math.sqrt((dx * dx) + (dy * dy));

                    // "Shortest path distance" is the primary sort
                    // "Closest in Euclidean distance to nearest requested tile" is the secondary sort
                    if (dist[x][y] < bestDist || (dist[x][y] == bestDist && euclidean < bestEuclidean)) {
                        bestDist = dist[x][y];
                        bestEuclidean = euclidean;
                        bestX = x;
                        bestY = y;
                    }
                }
            }
        }

        if (bestX != -1) {
            WorldPoint approxTarget = new WorldPoint(client.getTopLevelWorldView().getBaseX() + bestX, client.getTopLevelWorldView().getBaseY() + bestY, client.getTopLevelWorldView().getPlane());
            return buildPath(parent, approxTarget);
        }

        return Collections.emptyList();
    }


    /**
     * Builds a path given a parent path and a target point.
     * @param parent Parent 2D array of WorldPoints
     * @param target Target point
     * @return List of Worldpoints comprising the path
     */
    private List<WorldPoint> buildPath(WorldPoint[][] parent, WorldPoint target) {
        LinkedList<WorldPoint> path = new LinkedList<>();
        WorldPoint curr = target;

        while (curr != null) {
            path.addFirst(curr);
            int cx = curr.getX() - client.getTopLevelWorldView().getBaseX();
            int cy = curr.getY() - client.getTopLevelWorldView().getBaseY();

            WorldPoint prev = parent[cx][cy];
            if (prev == null) break; // Reached start
            curr = prev;
        }
        // The path list currently includes the Start point.
        // Usually we want to remove the start point for rendering/walking.
        if (!path.isEmpty()) {
            path.removeFirst();
        }
        return path;
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