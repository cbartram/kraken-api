package com.kraken.api.sim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.interaction.tile.CollisionDumper;
import com.kraken.api.interaction.tile.CollisionMap;
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

/**
 * Simulation engine for NPC and player movement.
 */
@Slf4j
@Singleton
public class SimulationEngine {
    private static final int MAX_HISTORY_SIZE = 50; // Limits the amount of times a user can go back in time

    @Getter
    private boolean running = false;

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

        // tilePanel.repaint();
        notifyObservers();
    }

    /**
     * Progresses the game state by 1 tick.
     */
    public void tick() {
        saveCurrentState();
        for (SimNpc npc : npcs) {
            Point npcPos = npc.getPosition();
            Point playerPos = getPlayerPosition();

            // Find next move towards player
            Point nextMove = calculateNextMove(npcPos, playerPos);
            if (nextMove != null && isValidMove(npcPos, nextMove)) {
                addNPCPathPoint(npc, new Point(npcPos));
                npc.setPosition(nextMove);
            }
        }

        if (tick > 0 && !playerCurrentPath.isEmpty()) {
            if (playerPathIndex < playerCurrentPath.size() - 1) {
                // Remove the current point (the one we're leaving)
                playerCurrentPath.remove(0);
                Point nextStep = playerCurrentPath.get(0);
                setPlayerPosition(nextStep);
            } else {
                // Path completed, clear it
                playerCurrentPath.clear();
                playerPathIndex = 0;
                stop();
            }
        }

        // tilePanel.repaint();
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
        playerCurrentPath = findPath(start, target);
        targetPosition = target;
        playerPathIndex = 0;
        log.info("Calculated path of size: {}", playerCurrentPath.size());
    }

    /**
     * Computes the next viable movement position for an NPC given 2 points.
     * @param from The point that the NPC is currently at
     * @param to The point the NPC wants to move towards.
     * @return Point a viable point or null if no point is viable in any direction
     */
    private Point calculateNextMove(Point from, Point to) {
        int dx = Integer.compare(to.x, from.x);
        int dy = Integer.compare(to.y, from.y);

        // Try diagonal movement first
        if (dx != 0 && dy != 0) {
            Point diagonal = new Point(from.x + dx, from.y + dy);
            if (isValidMove(from, diagonal)) {
                return diagonal;
            }
        }

        // Try horizontal/vertical movement
        if (dx != 0) {
            Point horizontal = new Point(from.x + dx, from.y);
            if (isValidMove(from, horizontal)) {
                return horizontal;
            }
        }

        if (dy != 0) {
            Point vertical = new Point(from.x, from.y + dy);
            if (isValidMove(from, vertical)) {
                return vertical;
            }
        }

        return null;
    }

    /**
     * Runs a BFS (Breadth-First-Search) algorithm to compute the shortest player path from a starting
     * point to a goal point taking into account collision data and obstacles.
     * @param start Point the starting point of the player
     * @param goal Point the ending point.
     * @return A list of shortest path points to the destination
     */
    private List<Point> findPath(Point start, Point goal) {
        boolean[][] visited = new boolean[collisionData.length][collisionData[0].length];
        Point[][] parent = new Point[collisionData.length][collisionData[0].length];

        Queue<Point> queue = new ArrayDeque<>();
        queue.add(start);
        visited[start.y][start.x] = true;

        int[] dx = {1, -1, 0, 0, 1, 1, -1, -1};
        int[] dy = {0, 0, 1, -1, 1, -1, 1, -1}; // SE, NE, SW, NW

        while (!queue.isEmpty()) {
            Point cur = queue.poll();

            if (cur.equals(goal)) {
                // reconstruct path
                LinkedList<Point> path = new LinkedList<>();
                for (Point at = goal; at != null; at = parent[at.y][at.x]) {
                    path.addFirst(at);
                }
                return path;
            }

            for (int i = 0; i < 8; i++) {
                Point next = new Point(cur.x + dx[i], cur.y + dy[i]);
                if (!inBounds(next) || visited[next.y][next.x]) {
                    continue;
                }

                // Skip diagonals if one of the cardinal neighbors is blocked
                if (Math.abs(dx[i]) == 1 && Math.abs(dy[i]) == 1) {
                    Point horiz = new Point(cur.x + dx[i], cur.y);
                    Point vert  = new Point(cur.x, cur.y + dy[i]);
                    if (!isValidMove(cur, horiz) || !isValidMove(cur, vert)) {
                        continue; // blocked corner
                    }
                }

                if (isValidMove(cur, next)) {
                    visited[next.y][next.x] = true;
                    parent[next.y][next.x] = cur;
                    queue.add(next);
                }
            }
        }

        return Collections.emptyList();
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
     * Resets the state of the simulation
     */
    public void reset() {
        npcPaths.clear();
        stop();
        targetPosition = null;
        playerCurrentPath.clear();
    }
}