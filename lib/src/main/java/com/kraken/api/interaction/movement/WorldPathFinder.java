package com.kraken.api.interaction.movement;

import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;
import javax.inject.Singleton;
import java.util.*;

/**
 * WorldPathfinder handles long-distance pathfinding across the RuneScape world.
 * It generates paths that can span multiple loaded scenes and handles various
 * transportation methods and obstacles.
 */
@Slf4j
@Singleton
public class WorldPathFinder {

    private static final int MAX_PATH_LENGTH = 200;
    private static final int SCENE_SIZE = 104; // RuneScape scene size in tiles

    // Transportation methods and their associated data
    private final Map<String, TransportRoute> transportRoutes = new HashMap<>();
    private final Set<WorldPoint> blockedTiles = new HashSet<>();

    public WorldPathFinder() {
        initializeTransportRoutes();
        initializeBlockedAreas();
    }

    /**
     * Finds a path from start to end using A* pathfinding with transportation support
     */
    public List<WorldPoint> findPath(WorldPoint start, WorldPoint end) {
        if (start == null || end == null) {
            return Collections.emptyList();
        }

        // For short distances, use simple pathfinding
        if (start.distanceTo(end) < 30) {
            return findSimplePath(start, end);
        }

        // For long distances, use A* with transportation
        return findComplexPath(start, end);
    }

    /**
     * Simple pathfinding for short distances
     */
    private List<WorldPoint> findSimplePath(WorldPoint start, WorldPoint end) {
        List<WorldPoint> path = new ArrayList<>();

        // Use Bresenham-like algorithm for straight-line path with obstacle avoidance
        int dx = Math.abs(end.getX() - start.getX());
        int dy = Math.abs(end.getY() - start.getY());
        int x = start.getX();
        int y = start.getY();
        int xInc = start.getX() < end.getX() ? 1 : -1;
        int yInc = start.getY() < end.getY() ? 1 : -1;
        int error = dx - dy;

        dx *= 2;
        dy *= 2;

        while (x != end.getX() || y != end.getY()) {
            WorldPoint current = new WorldPoint(x, y, start.getPlane());

            if (!isBlocked(current)) {
                path.add(current);
            }

            if (error > 0) {
                x += xInc;
                error -= dy;
            } else {
                y += yInc;
                error += dx;
            }

            // Safety check to prevent infinite loops
            if (path.size() > MAX_PATH_LENGTH) {
                break;
            }
        }

        path.add(end);
        return path;
    }

    /**
     * Complex pathfinding using A* algorithm with transportation options
     */
    private List<WorldPoint> findComplexPath(WorldPoint start, WorldPoint end) {
        PriorityQueue<PathNode> openSet = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        Map<WorldPoint, PathNode> allNodes = new HashMap<>();
        Set<WorldPoint> closedSet = new HashSet<>();

        PathNode startNode = new PathNode(start, null, 0, heuristic(start, end));
        openSet.add(startNode);
        allNodes.put(start, startNode);

        while (!openSet.isEmpty()) {
            PathNode current = openSet.poll();

            if (current.point.equals(end) || current.point.distanceTo(end) <= 3) {
                return reconstructPath(current);
            }

            closedSet.add(current.point);

            // Get neighbors including transportation options
            List<PathNode> neighbors = getNeighbors(current, end);

            for (PathNode neighbor : neighbors) {
                if (closedSet.contains(neighbor.point)) {
                    continue;
                }

                double tentativeGScore = current.gScore + neighbor.movementCost;

                PathNode existingNeighbor = allNodes.get(neighbor.point);
                if (existingNeighbor == null) {
                    neighbor.gScore = tentativeGScore;
                    neighbor.fScore = neighbor.gScore + heuristic(neighbor.point, end);
                    neighbor.parent = current;

                    openSet.add(neighbor);
                    allNodes.put(neighbor.point, neighbor);
                } else if (tentativeGScore < existingNeighbor.gScore) {
                    existingNeighbor.gScore = tentativeGScore;
                    existingNeighbor.fScore = existingNeighbor.gScore + heuristic(existingNeighbor.point, end);
                    existingNeighbor.parent = current;
                }
            }

            // Safety check
            if (allNodes.size() > MAX_PATH_LENGTH * 2) {
                log.warn("Pathfinding exceeded maximum node limit, falling back to simple path");
                return findSimplePath(start, end);
            }
        }

        // No path found, try simple pathfinding as fallback
        log.warn("No complex path found from {} to {}, using simple path", start, end);
        return findSimplePath(start, end);
    }

    /**
     * Gets neighboring nodes for pathfinding, including transportation options
     */
    private List<PathNode> getNeighbors(PathNode current, WorldPoint destination) {
        List<PathNode> neighbors = new ArrayList<>();

        // Standard movement in 8 directions
        int[] dx = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dy = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int i = 0; i < 8; i++) {
            int newX = current.point.getX() + dx[i];
            int newY = current.point.getY() + dy[i];
            WorldPoint newPoint = new WorldPoint(newX, newY, current.point.getPlane());

            if (!isBlocked(newPoint)) {
                double cost = (i % 2 == 0) ? 1.414 : 1.0; // Diagonal vs cardinal movement
                neighbors.add(new PathNode(newPoint, current, 0, 0, cost));
            }
        }

        // Check for transportation options
        for (TransportRoute route : transportRoutes.values()) {
            if (current.point.distanceTo(route.startPoint) <= route.activationRange) {
                // Add transportation destination as neighbor with appropriate cost
                double transportCost = route.cost + current.point.distanceTo(route.startPoint);
                neighbors.add(new PathNode(route.endPoint, current, 0, 0, transportCost));
            }
        }

        return neighbors;
    }

    /**
     * Reconstructs the path from the goal node back to the start
     */
    private List<WorldPoint> reconstructPath(PathNode goalNode) {
        List<WorldPoint> path = new ArrayList<>();
        PathNode current = goalNode;

        while (current != null) {
            path.add(current.point);
            current = current.parent;
        }

        Collections.reverse(path);
        return path;
    }

    /**
     * Heuristic function for A* (Manhattan distance)
     */
    private double heuristic(WorldPoint a, WorldPoint b) {
        return Math.abs(a.getX() - b.getX()) + Math.abs(a.getY() - b.getY());
    }

    /**
     * Checks if a tile is blocked or inaccessible
     */
    private boolean isBlocked(WorldPoint point) {
        // Check if point is in blocked tiles set
        if (blockedTiles.contains(point)) {
            return true;
        }

        // Add additional blocking logic here (water, walls, etc.)
        return false;
    }

    /**
     * Initialize transportation routes (teleports, boats, etc.)
     */
    private void initializeTransportRoutes() {
        // Example transportation routes - customize based on your needs

        // Varrock teleport
        transportRoutes.put("varrock_teleport",
                new TransportRoute(
                        new WorldPoint(3213, 3424, 0), // Varrock center
                        new WorldPoint(3213, 3424, 0), // Same location (teleport to)
                        5, // activation range
                        25 // cost (represents time/effort)
                ));

        // Lumbridge teleport
        transportRoutes.put("lumbridge_teleport",
                new TransportRoute(
                        new WorldPoint(3225, 3218, 0), // Lumbridge center
                        new WorldPoint(3225, 3218, 0),
                        5,
                        25
                ));

        // Add more transportation routes as needed
        // Ships, magic carpets, fairy rings, etc.
    }

    /**
     * Initialize blocked areas (water, walls, etc.)
     */
    private void initializeBlockedAreas() {
        // Add known blocked areas
        // This could be loaded from a data file or calculated dynamically

        // Example: Block some water tiles
        for (int x = 3000; x < 3010; x++) {
            for (int y = 3000; y < 3010; y++) {
                blockedTiles.add(new WorldPoint(x, y, 0));
            }
        }
    }

    /**
     * Node class for A* pathfinding
     */
    private static class PathNode {
        final WorldPoint point;
        PathNode parent;
        double gScore; // Cost from start
        double fScore; // gScore + heuristic
        double movementCost; // Cost to move to this node

        PathNode(WorldPoint point, PathNode parent, double gScore, double fScore) {
            this(point, parent, gScore, fScore, 1.0);
        }

        PathNode(WorldPoint point, PathNode parent, double gScore, double fScore, double movementCost) {
            this.point = point;
            this.parent = parent;
            this.gScore = gScore;
            this.fScore = fScore;
            this.movementCost = movementCost;
        }
    }

    /**
     * Transportation route data class
     */
    private static class TransportRoute {
        final WorldPoint startPoint;
        final WorldPoint endPoint;
        final int activationRange;
        final double cost;

        TransportRoute(WorldPoint startPoint, WorldPoint endPoint, int activationRange, double cost) {
            this.startPoint = startPoint;
            this.endPoint = endPoint;
            this.activationRange = activationRange;
            this.cost = cost;
        }
    }
}