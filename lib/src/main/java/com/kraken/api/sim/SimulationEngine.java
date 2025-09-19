package com.kraken.api.sim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.sim.ui.SimulationVisualizer;
import com.kraken.api.sim.ui.TilePanel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.CollisionDataFlag;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Simulation engine for NPC movement
 */
@Slf4j
@Singleton
public class SimulationEngine {
    private Timer timer;

    @Inject
    private SimulationVisualizer visualizer;

    @Inject
    private TilePanel tilePanel;

    @Getter
    private boolean running = false;

    @Getter
    @Setter
    private int[][] collisionData = new int[2][2];

    int tick = 0;

    @Getter
    @Setter
    private Point playerPosition;

    @Getter
    @Setter
    private Point targetPosition;

    @Getter
    private java.util.List<Point> playerCurrentPath = new ArrayList<>();


    private int playerPathIndex = 0;

    public void start() {
        if (timer != null) {
            timer.stop();
        }

        running = true;
        timer = new Timer(600, e -> tick());
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            running = false;
            tick = 0;
        }
    }

    private void tick() {
        if(tick == 0) {
            // On the first tick snap the players point to their starting position
            if(!tilePanel.getPlayerPath().isEmpty()) {
                setPlayerPosition(tilePanel.getPlayerPath().get(0));
            }
        }

        for (SimNpc npc : visualizer.getNpcs()) {
            Point npcPos = npc.getPosition();
            Point playerPos = getPlayerPosition();

            // Calculate distance
            double distance = npcPos.distance(playerPos);

            if (distance < 10 && distance > 1) {
                // Find next move towards player
                Point nextMove = calculateNextMove(npcPos, playerPos);
                if (nextMove != null && isValidMove(npcPos, nextMove)) {
                    tilePanel.addNPCPathPoint(npc, new Point(npcPos));
                    npc.setPosition(nextMove);
                }
            }
        }


        log.info("Tick: {}, Player Path Size: {}", tick, playerCurrentPath.size());
        if (tick > 0 && !playerCurrentPath.isEmpty()) {
            if (playerPathIndex < playerCurrentPath.size() - 1) {
                playerPathIndex++;
                Point nextStep = playerCurrentPath.get(playerPathIndex);
                setPlayerPosition(nextStep);
            }
        }

        tilePanel.repaint();
        tick += 1;
    }

    public void setPlayerTarget(Point target) {
        Point start = getPlayerPosition();
        playerCurrentPath = findPath(start, target);
        targetPosition = target;
        playerPathIndex = 0;
        log.info("Calculated path of size: {}", playerCurrentPath.size());
    }

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

    private java.util.List<Point> findPath(Point start, Point goal) {
        boolean[][] visited = new boolean[collisionData.length][collisionData[0].length];
        Point[][] parent = new Point[collisionData.length][collisionData[0].length];

        java.util.Queue<Point> queue = new ArrayDeque<>();
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

    private boolean inBounds(Point p) {
        return p.x >= 0 && p.x < collisionData[0].length && p.y >= 0 && p.y < collisionData.length;
    }

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
}
