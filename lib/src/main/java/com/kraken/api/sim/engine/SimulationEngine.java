package com.kraken.api.sim.engine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.interaction.tile.CollisionDumper;
import com.kraken.api.interaction.tile.CollisionMap;
import com.kraken.api.sim.SimulationObserver;
import com.kraken.api.sim.model.GameState;
import com.kraken.api.sim.model.SimNpc;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.CollisionDataFlag;

import javax.swing.Timer;
import java.awt.*;
import java.util.*;
import java.util.List;

// TODO
// 1. NPC's need to be able to try to found a way out if player moves under them

/**
 * Simulation engine for NPC and player movement.
 */
@Slf4j
@Singleton
public class SimulationEngine {
    private static final int MAX_HISTORY_SIZE = 50; // Limits the amount of times a user can go back in time

    // Simulation running (different from player run enabled move 2 tiles instead of 1)
    @Getter
    private boolean running = false;

    @Getter
    @Setter
    private boolean playerRunning = false;

    @Getter
    @Setter
    private int[][] collisionData;

    @Getter
    @Setter
    private Point playerPosition;

    @Getter
    @Setter
    private Point targetPosition;

    @Getter
    private final List<SimNpc> npcs = new ArrayList<>();

    @Getter
    private final Map<SimNpc, List<Point>> npcPaths = new HashMap<>();

    @Getter
    private List<Point> playerCurrentPath = new ArrayList<>();


    private final List<SimulationObserver> observers = new ArrayList<>();
    private Timer timer;
    int tick = 0;
    private int playerPathIndex = 0;
    private Stack<GameState> stateHistory = new Stack<>();

    @Inject
    public SimulationEngine(final CollisionDumper collisionDumper) {
        // TODO Make this load directly from game with .collect() since this is intended to be used from an API context
        // within a RuneLite plugin
        CollisionMap collisionMap = collisionDumper.loadFromFile("collision_data.json");
        playerPosition = new Point(collisionMap.getPlayerX(), collisionMap.getPlayerY());
        collisionData = collisionMap.getData();
    }

    /**
     * Starts a timer which runs the simulation on 0.6s (1 game tick) intervals.
     */
    public void start() {
        if (timer != null) {
            timer.stop();
        }

        running = true;
        timer = new Timer(600, e -> tick());
        timer.start();
    }

    /**
     * Stops the primary simulation timer and resets history.
     */
    public void stop() {
        if (timer != null) {
            timer.stop();
            running = false;
            tick = 0;
            stateHistory.clear();
        }
    }

    /**
     * Add an observer to be notified of simulation updates
     */
    public void addObserver(SimulationObserver observer) {
        observers.add(observer);
    }

    /**
     * Remove an observer
     */
    public void removeObserver(SimulationObserver observer) {
        observers.remove(observer);
    }

    /**
     * Notify all observers of simulation updates
     */
    private void notifyObservers() {
        for (SimulationObserver observer : observers) {
            observer.onSimulationUpdated();
        }
    }

    /**
     * Saves the current game state including player positions, npc positions, path indexes,
     * and the current tick to a stack in memory.
     */
    private void saveCurrentState() {
        // Collect current NPC positions
        List<Point> currentNpcPositions = new ArrayList<>();
        for (SimNpc npc : npcs) {
            currentNpcPositions.add(new Point(npc.getPosition()));
        }

        // Create a deep copy of the player path to avoid reference issues
        List<Point> pathCopy = new ArrayList<>();
        for (Point point : playerCurrentPath) {
            pathCopy.add(new Point(point));
        }

        // Save current state to history
        GameState currentState = new GameState(tick,
                playerPosition != null ? new Point(playerPosition) : null,
                pathCopy,
                playerPathIndex,
                currentNpcPositions);
        stateHistory.push(currentState);

        // Limit history size to prevent memory issues
        if (stateHistory.size() > MAX_HISTORY_SIZE) {
            // Remove oldest states
            Stack<GameState> newHistory = new Stack<>();
            for (int i = stateHistory.size() - MAX_HISTORY_SIZE; i < stateHistory.size(); i++) {
                newHistory.push(stateHistory.get(i));
            }
            stateHistory = newHistory;
        }
    }

    /**
     * Re-winds the game state by 1 tick.
     */
    public void prevTick() {
        if (stateHistory.isEmpty()) {
            log.info("Cannot go back further - no previous state saved");
            return;
        }

        GameState previousState = stateHistory.pop();

        // Restore previous state
        tick = previousState.tick;

        if (previousState.playerPosition != null) {
            setPlayerPosition(previousState.playerPosition);
        }

        playerCurrentPath = new ArrayList<>(previousState.playerPath);
        playerPathIndex = previousState.playerPathIndex;

        // Restore previous NPC positions
        List<SimNpc> prevNpcs = npcs;
        for (int i = 0; i < Math.min(prevNpcs.size(), previousState.npcPositions.size()); i++) {
            prevNpcs.get(i).setPosition(previousState.npcPositions.get(i));
        }

        // Remove the last path point for each NPC (since we're going backward)
        for (SimNpc npc : npcs) {
            removeLastNPCPathPoint(npc);
        }

        notifyObservers();
    }

    public void tick() {
        saveCurrentState();

        if(tick > 0) {
            for (SimNpc npc : npcs) {
                Point npcPos = npc.getPosition();
                Point playerPos = getPlayerPosition();

                if(npc.isCanPathfind()) {
                    List<Point> route = findPath(npcPos, playerPos, npc);
                    if(!route.isEmpty()) {
                        Point nextMove = route.get(0);
                        // Check for collisions with other entities
                        if(!isOccupiedByNPC(nextMove, npc) && !isOccupiedByPlayer(nextMove, npc)) {
                            addNPCPathPoint(npc, new Point(npcPos));
                            npc.setPosition(nextMove);
                            log.debug("NPC {} moved from {} to {} using pathfinding",
                                    npc.getName(), npcPos, nextMove);
                        } else {
                            log.debug("NPC {} path blocked at {}", npc.getName(), nextMove);
                        }
                    } else {
                        log.debug("NPC {} could not find path from {} to {}",
                                npc.getName(), npcPos, playerPos);
                    }
                    continue;
                }

                Point nextMove = calculateNextMove(npcPos, playerPos, npc);
                if (nextMove != null && !isOccupiedByNPC(nextMove, npc) && !isOccupiedByPlayer(nextMove, npc)) {
                    addNPCPathPoint(npc, new Point(npcPos));
                    npc.setPosition(nextMove);
                }
            }
        }

        // Existing player movement logic
        if (tick > 0 && !playerCurrentPath.isEmpty()) {
            Point nextStep;
            if (playerRunning && playerCurrentPath.size() > 1) {
                playerCurrentPath.remove(0);
                nextStep = playerCurrentPath.get(0);
                playerCurrentPath.remove(0);
            } else {
                nextStep = playerCurrentPath.get(0);
                playerCurrentPath.remove(0);
            }

            if (nextStep != null) {
                setPlayerPosition(nextStep);
            } else {
                playerCurrentPath.clear();
                playerPathIndex = 0;
            }
        }

        notifyObservers();
        tick += 1;
    }

    /**
     * Sets the target point for a player to move towards and runs a BFS
     * calculation to determine the shortest path.
     * @param target Target point to move towards
     */
    public void setPlayerTarget(Point target) {
        Point start = getPlayerPosition();
        playerCurrentPath = findPath(start, target, null);
        targetPosition = target;
        playerPathIndex = 0;
        log.info("Calculated path of size: {}", playerCurrentPath.size());
    }

    /**
     * Returns true if the movement is valid given collision flags and false otherwise. This takes
     * into account diagonal movement as well.
     * @param from Point starting point
     * @param to Point ending point
     * @return True if the movement between the 2 points is valid and false otherwise.
     */
    private boolean isValidMove(Point from, Point to) {
        if (to.x < 0 || to.x >= collisionData[0].length || to.y < 0 || to.y >= collisionData.length) {
            return false;
        }

        // Check collision flags
        int toFlags = collisionData[to.y][to.x];

        // Check if destination is blocked
        if ((toFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0 ||
                (toFlags & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0) {
            return false;
        }

        // Check directional blocks
        int dx = to.x - from.x;
        int dy = to.y - from.y;

        if (dx > 0 && (toFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0) return false;
        if (dx < 0 && (toFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0) return false;
        if (dy > 0 && (toFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) return false;
        if (dy < 0 && (toFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) return false;

        // Check if leaving current tile is blocked
        int fromFlags = collisionData[from.y][from.x];
        if (dx > 0 && (fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0) return false;
        if (dx < 0 && (fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0) return false;
        if (dy > 0 && (fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) return false;
        return dy >= 0 || (fromFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) == 0;
    }

    /**
     * Returns true if the movement is valid for an NPC of a specific size.
     * Checks collision for all tiles that the NPC occupies.
     * @param from Point starting point (southwest tile)
     * @param to Point ending point (southwest tile)
     * @param npcSize Size of the NPC (1 for 1x1, 2 for 2x2, 3 for 3x3, etc.)
     * @return True if the movement is valid for all tiles the NPC occupies
     */
    private boolean isValidMoveForNPC(Point from, Point to, int npcSize) {
        // Check all tiles that the NPC will occupy at the destination
        for (int dx = 0; dx < npcSize; dx++) {
            for (int dy = 0; dy < npcSize; dy++) {
                Point currentTile = new Point(from.x + dx, from.y - dy);
                Point destinationTile = new Point(to.x + dx, to.y - dy);

                // Check if this specific tile movement is valid
                if (!isValidMove(currentTile, destinationTile)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Computes the next viable movement position for an NPC given 2 points.
     * Prioritizes diagonal movement, then horizontal, then vertical.
     * @param from The point that the NPC is currently at
     * @param to The point the NPC wants to move towards.
     * @param npc The NPC that is moving (for collision checking with other NPCs)
     * @return Point a viable point or null if no point is viable in any direction
     */
    private Point calculateNextMove(Point from, Point to, SimNpc npc) {
        int dx = Integer.compare(to.x, from.x);
        int dy = Integer.compare(to.y, from.y);

        int npcSize = npc.getSize();

        // Priority 1: Try diagonal movement first
        if (dx != 0 && dy != 0) {
            Point diagonal = new Point(from.x + dx, from.y + dy);
            if (isValidMoveForNPC(from, diagonal, npcSize) && !isOccupiedByNPC(diagonal, npc) && !isOccupiedByPlayer(diagonal, npc)) {
                return diagonal;
            }
        }


        // Priority 2: Try horizontal movement
        if (dx != 0) {
            Point horizontal = new Point(from.x + dx, from.y);
            if (isValidMoveForNPC(from, horizontal, npcSize) && !isOccupiedByNPC(horizontal, npc) && !isOccupiedByPlayer(horizontal, npc)) {
                return horizontal;
            }
        }

        // Priority 3: Try vertical movement
        if (dy != 0) {
            Point vertical = new Point(from.x, from.y + dy);
            if (isValidMoveForNPC(from, vertical, npcSize) && !isOccupiedByNPC(vertical, npc) && !isOccupiedByPlayer(vertical, npc)) {
                return vertical;
            }
        }

        return null;
    }

    /**
     * Checks if a position is occupied by another NPC, taking into account NPC sizes.
     * @param position The position to check
     * @param movingNpc The NPC that wants to move (to exclude from collision check)
     * @return True if the position would cause NPCs to overlap, false otherwise
     */
    private boolean isOccupiedByNPC(Point position, SimNpc movingNpc) {
        int movingSize = movingNpc.getSize(); // Assuming getSize() returns 1 for 1x1, 2 for 2x2, etc.

        for (SimNpc otherNpc : npcs) {
            if (otherNpc == movingNpc) continue; // Skip self

            Point otherPos = otherNpc.getPosition();
            int otherSize = otherNpc.getSize();

            // Check if the bounding boxes would overlap
            if (isOverlapping(position, movingSize, otherPos, otherSize)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if two NPCs with given positions and sizes would overlap.
     * @param pos1 Position of first NPC
     * @param size1 Size of first NPC
     * @param pos2 Position of second NPC
     * @param size2 Size of second NPC
     * @return True if they would overlap, false otherwise
     */
    private boolean isOverlapping(Point pos1, int size1, Point pos2, int size2) {
        int left1 = pos1.x, right1 = pos1.x + size1 - 1;
        int top1 = pos1.y, bottom1 = pos1.y + size1 - 1;

        int left2 = pos2.x, right2 = pos2.x + size2 - 1;
        int top2 = pos2.y, bottom2 = pos2.y + size2 - 1;
        return !(right1 < left2 || right2 < left1 || bottom1 < top2 || bottom2 < top1);
    }

    /**
     * Checks if a position would cause the NPC to overlap with the player.
     * NPCs should position their southwest tile to be adjacent to the player, not overlapping.
     * @param position The position the NPC wants to move to
     * @param npc The NPC that wants to move
     * @return True if the position would cause overlap with player, false otherwise
     */
    private boolean isOccupiedByPlayer(Point position, SimNpc npc) {
        Point playerPos = getPlayerPosition();
        int npcSize = npc.getSize();

        return isOverlapping(position, npcSize, playerPos, 1);
    }

    /**
     * Runs a BFS (Breadth-First-Search) algorithm to compute the shortest path from a starting
     * point to a goal point taking into account collision data and obstacles.
     * @param start Point the starting point
     * @param goal Point the ending point
     * @param npc SimNpc the NPC that is pathfinding (null for player)
     * @return A list of path points to the destination (excluding start position)
     */
    private List<Point> findPath(Point start, Point goal, SimNpc npc) {
        if (start.equals(goal)) {
            return Collections.emptyList();
        }

        boolean[][] visited = new boolean[collisionData.length][collisionData[0].length];
        Point[][] parent = new Point[collisionData.length][collisionData[0].length];
        Queue<Point> queue = new ArrayDeque<>();

        queue.add(start);
        visited[start.y][start.x] = true;

        // Movement directions: E, W, S, N, SE, NE, SW, NW
        int[] dx = {1, -1, 0, 0, 1, 1, -1, -1};
        int[] dy = {0, 0, 1, -1, 1, -1, 1, -1};

        while (!queue.isEmpty()) {
            Point cur = queue.poll();

            // For NPCs, check if we're adjacent to the player (goal)
            // For players, check if we've reached the exact goal
            boolean reachedGoal = false;
            if (npc != null) {
                // NPC pathfinding - check if adjacent to player
                reachedGoal = isAdjacentToGoal(cur, goal, npc.getSize());
            } else {
                // Player pathfinding - exact match
                reachedGoal = cur.equals(goal);
            }

            if (reachedGoal) {
                // Reconstruct path (excluding start position)
                LinkedList<Point> path = new LinkedList<>();
                Point at = cur;
                while (at != null && !at.equals(start)) {
                    path.addFirst(at);
                    at = parent[at.y][at.x];
                }
                return path;
            }

            // Explore neighbors
            for (int i = 0; i < 8; i++) {
                Point next = new Point(cur.x + dx[i], cur.y + dy[i]);

                if (!inBounds(next) || visited[next.y][next.x]) {
                    continue;
                }

                // For diagonal movement, check if we can move through the corner
                if (Math.abs(dx[i]) == 1 && Math.abs(dy[i]) == 1) {
                    Point horizontal = new Point(cur.x + dx[i], cur.y);
                    Point vertical = new Point(cur.x, cur.y + dy[i]);

                    boolean canMoveHorizontal, canMoveVertical;

                    if (npc != null) {
                        canMoveHorizontal = isValidMoveForNPC(cur, horizontal, npc.getSize());
                        canMoveVertical = isValidMoveForNPC(cur, vertical, npc.getSize());
                    } else {
                        canMoveHorizontal = isValidMove(cur, horizontal);
                        canMoveVertical = isValidMove(cur, vertical);
                    }

                    // Skip diagonal if either cardinal direction is blocked
                    if (!canMoveHorizontal || !canMoveVertical) {
                        continue;
                    }
                }

                // Check if the move to 'next' is valid
                boolean validMove;
                if (npc != null) {
                    validMove = isValidMoveForNPC(cur, next, npc.getSize());
                } else {
                    validMove = isValidMove(cur, next);
                }

                if (validMove) {
                    visited[next.y][next.x] = true;
                    parent[next.y][next.x] = cur;
                    queue.add(next);
                }
            }
        }

        return Collections.emptyList(); // No path found
    }


    /**
     * Checks if an NPC position is adjacent to the goal (player position)
     * @param npcPos Current NPC position (southwest corner)
     * @param playerPos Player position
     * @param npcSize Size of the NPC
     * @return True if the NPC is adjacent to the player
     */
    private boolean isAdjacentToGoal(Point npcPos, Point playerPos, int npcSize) {
        // Check all tiles around the player position
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                if (dx == 0 && dy == 0) continue; // Skip the player's own position

                Point adjacentPos = new Point(playerPos.x + dx, playerPos.y + dy);

                // Check if the NPC at this adjacent position would be touching the player
                // but not overlapping
                if (isTouchingButNotOverlapping(adjacentPos, npcSize, playerPos, 1)) {
                    // Check if NPC's southwest corner matches this adjacent position
                    if (npcPos.equals(adjacentPos)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks if two entities are touching (adjacent) but not overlapping
     * @param pos1 Position of first entity
     * @param size1 Size of first entity
     * @param pos2 Position of second entity
     * @param size2 Size of second entity
     * @return True if touching but not overlapping
     */
    private boolean isTouchingButNotOverlapping(Point pos1, int size1, Point pos2, int size2) {
        // Calculate bounding boxes
        int left1 = pos1.x, right1 = pos1.x + size1 - 1;
        int top1 = pos1.y, bottom1 = pos1.y + size1 - 1;
        int left2 = pos2.x, right2 = pos2.x + size2 - 1;
        int top2 = pos2.y, bottom2 = pos2.y + size2 - 1;

        // Check if they're overlapping
        boolean overlapping = !(right1 < left2 || right2 < left1 || bottom1 < top2 || bottom2 < top1);

        if (overlapping) {
            return false; // Overlapping, not just touching
        }

        // Check if they're adjacent (touching)
        boolean horizontallyAdjacent = (right1 + 1 == left2 || right2 + 1 == left1) &&
                !(bottom1 < top2 || bottom2 < top1);
        boolean verticallyAdjacent = (bottom1 + 1 == top2 || bottom2 + 1 == top1) &&
                !(right1 < left2 || right2 < left1);

        return horizontallyAdjacent || verticallyAdjacent;
    }

    /**
     * Returns true if the point is in the bounds of the collision data and false otherwise
     * @param p Point to check bounds for
     * @return True if the point is in bounds and false otherwise.
     */
    private boolean inBounds(Point p) {
        return p.x >= 0 && p.x < collisionData[0].length && p.y >= 0 && p.y < collisionData.length;
    }

    /**
     * Removes the last NPC pathing point
     * @param npc Simulated NPC to remove point from
     */
    public void removeLastNPCPathPoint(SimNpc npc) {
        List<Point> path = npcPaths.get(npc);
        if (path != null && !path.isEmpty()) {
            path.remove(path.size() - 1);
        }
    }

    /**
     * Adds an NPC path point
     * @param npc NpcSim The npc to add to
     * @param point Point the point to add
     */
    public void addNPCPathPoint(SimNpc npc, Point point) {
        npcPaths.computeIfAbsent(npc, k -> new ArrayList<>()).add(point);
    }


    /**
     * Adds an NPC to the simulation
     */
    public void addNpc(SimNpc npc) {
        npcs.add(npc);
        npcPaths.put(npc, new ArrayList<>());
        notifyObservers();
    }

    /**
     * Removes an NPC from the simulation
     */
    public void removeNpc(SimNpc npc) {
        npcs.remove(npc);
        npcPaths.remove(npc);
        notifyObservers();
    }

    /**
     * Resets the state of the simulation
     */
    public void reset() {
        npcPaths.clear();
        stop();
        targetPosition = null;
        playerCurrentPath.clear();
    }
}