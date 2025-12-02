package com.kraken.api.service.movement;

import com.kraken.api.Context;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.Queue;

@Slf4j
@Singleton
public class Pathfinder {
    private static final int SCENE_SIZE = 104;
    private static final int[] DIR_X = {-1, 1,  0, 0, -1, 1, -1, 1};
    private static final int[] DIR_Y = { 0, 0, -1, 1, -1, -1, 1, 1};


    @Inject
    private Context ctx;

    @Inject
    private Client client;

    /**
     * Generates a sparse path (Waypoints only) from start to target.
     * Reduces a dense tile-by-tile path into a list of "clicking points" where direction changes.
     *
     * @param start  WorldPoint the starting location
     * @param target WorldPoint the ending location
     * @return List of WorldPoints representing the turns/waypoints
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

            // Calculate outgoing direction (Current -> Next)
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
     * Finds the shortest path from a given start point to a target point. This uses player movement behavior and BFS
     * to find the shortest path.
     * @param start WorldPoint the starting location (generally the players current position)
     * @param target WorldPoint the ending or target location to find a path towards.
     * @return List of Points comprising the path
     */
    public List<WorldPoint> findPath(WorldPoint start, WorldPoint target) {
        return ctx.runOnClientThread(() -> {
            if (start.equals(target)) return Collections.emptyList();

            // 1. Setup State
            CollisionData[] collisionData = client.getTopLevelWorldView().getCollisionMaps();
            if (collisionData == null) return Collections.emptyList();

            int plane = client.getTopLevelWorldView().getPlane();
            int[][] flags = collisionData[plane].getFlags();

            // Distances array (initialized to -1)
            // This maps [sceneX][sceneY] -> distance from start
            int[][] dist = new int[SCENE_SIZE][SCENE_SIZE];
            for (int[] row : dist) Arrays.fill(row, -1);

            // Parent array to reconstruct path
            WorldPoint[][] parent = new WorldPoint[SCENE_SIZE][SCENE_SIZE];

            // BFS Queue
            Queue<WorldPoint> queue = new ArrayDeque<>();

            int startX = start.getX() - client.getTopLevelWorldView().getBaseX();
            int startY = start.getY() - client.getTopLevelWorldView().getBaseY();

            if (!isInScene(startX, startY)) return Collections.emptyList();

            dist[startX][startY] = 0;
            queue.add(start);

            boolean targetReached = false;

            // Run BFS
            while (!queue.isEmpty()) {
                WorldPoint current = queue.poll();

                // If we found the exact target, we can stop early
                if (current.equals(target)) {
                    targetReached = true;
                    break;
                }

                int cx = current.getX() - client.getTopLevelWorldView().getBaseX();
                int cy = current.getY() - client.getTopLevelWorldView().getBaseY();
                int currentDist = dist[cx][cy];

                // Check neighbors in specific order: W, E, S, N, SW, SE, NW, NE
                for (int i = 0; i < 8; i++) {
                    int nx = cx + DIR_X[i];
                    int ny = cy + DIR_Y[i];

                    // Bounds check
                    if (!isInScene(nx, ny)) continue;

                    // If not visited yet
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

            if (targetReached) {
                return buildPath(parent, target);
            } else {
                // Target not reached (unreachable or blocked).
                // Run the "Approximation" search
                return findBestApproximatePath(target, dist, parent);
            }
        });
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
     * If exact target is unreachable, searches a 21x21 area around target 
     * for the best reachable tile.
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
        if (dx != 0 && dy != 0) {
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
        }

        return false;
    }


    /**
     * A helper function to render a computed path onto the UI.
     * @param path List of WorldPoints (the path) to render.
     * @param graphics Graphics2D object to draw on the canvas.
     * @return Dimension rendering the path onto the canvas.
     */
    public Dimension renderPath(List<WorldPoint> path, Graphics2D graphics) {
        if (path == null || path.isEmpty()) {
            return null;
        }

        Color pathColor = new Color(0, 255, 0, 150);
        Color endColor = new Color(255, 0, 0, 150);

        int i = 0;
        for (WorldPoint point : path) {
            // 1. Validate Planes (don't draw if player is on z=0 and path is on z=1)
            if (point.getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }

            // 2. Convert WorldPoint to LocalPoint
            // This is necessary because drawing is done relative to the camera in the scene
            LocalPoint lp = LocalPoint.fromWorld(client, point);
            if (lp == null) {
                continue; // Point is outside the currently loaded scene
            }

            // 3. Get the Polygon for the tile
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly != null) {
                // Determine color based on position in path
                if (i == path.size() - 1) {
                    graphics.setColor(endColor);
                    graphics.fill(poly); // Fill the last tile
                } else {
                    graphics.setColor(pathColor);
                    graphics.setStroke(new BasicStroke(2));
                    graphics.draw(poly); // Outline the path tiles
                }
            }
            i++;
        }

        // Draw lines connecting the tiles for a "route" look
        if(path.size() < 2) {
            return null;
        }

        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(1));

        net.runelite.api.Point prevCenter = null;

        // Add player location as the start of the line
        LocalPoint playerLp = client.getLocalPlayer().getLocalLocation();
        if (playerLp != null) {
            prevCenter = Perspective.localToCanvas(client, playerLp, client.getTopLevelWorldView().getPlane());
        }

        for (WorldPoint point : path) {
            LocalPoint lp = LocalPoint.fromWorld(client, point);
            if (lp == null) continue;

            net.runelite.api.Point center = Perspective.localToCanvas(client, lp, client.getTopLevelWorldView().getPlane());
            if (center != null && prevCenter != null) {
                graphics.drawLine(prevCenter.getX(), prevCenter.getY(), center.getX(), center.getY());
            }
            prevCenter = center;
        }

        return null;
    }
}