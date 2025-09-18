package com.kraken.api.sim;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.sim.ui.SimulationVisualizer;
import com.kraken.api.sim.ui.TilePanel;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.CollisionDataFlag;

import javax.swing.*;
import java.awt.*;

import static com.kraken.api.sim.ui.SimulationVisualizer.DEFAULT_MAP_HEIGHT;
import static com.kraken.api.sim.ui.SimulationVisualizer.DEFAULT_MAP_WIDTH;

/**
 * Simulation engine for NPC movement
 */
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

    public void start(int tickDelay) {
        if (timer != null) {
            timer.stop();
        }

        running = true;
        timer = new Timer(tickDelay, e -> tick());
        timer.start();
    }

    public void stop() {
        if (timer != null) {
            timer.stop();
            running = false;
        }
    }

    private void tick() {
        // Simple NPC AI: move towards player if within range
        for (SimNpc npc : visualizer.getNpcs()) {
            Point npcPos = npc.getPosition();
            Point playerPos = visualizer.getPlayerPosition();

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

        tilePanel.repaint();
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

    private boolean isValidMove(Point from, Point to) {
        // Check bounds
        if (to.x < 0 || to.x >= DEFAULT_MAP_WIDTH ||
                to.y < 0 || to.y >= DEFAULT_MAP_HEIGHT) {
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
