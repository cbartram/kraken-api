package com.kraken.api.sim.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.interaction.tile.MovementFlag;
import com.kraken.api.sim.SimNpc;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.CollisionDataFlag;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kraken.api.sim.ui.SimulationVisualizer.*;

/**
 * Main tile rendering panel
 */
@Slf4j
@Singleton
public class TilePanel extends JPanel {
    private final List<Point> playerPath = new ArrayList<>();
    private final Map<SimNpc, List<Point>> npcPaths = new HashMap<>();
    private Point hoveredTile = null;
    private final SimulationVisualizer visualizer;


    @Inject
    public TilePanel(SimulationVisualizer visualizer) {
        this.visualizer = visualizer;
        setPreferredSize(new Dimension(
                DEFAULT_MAP_WIDTH * TILE_SIZE,
                DEFAULT_MAP_HEIGHT * TILE_SIZE
        ));
        setBackground(Color.BLACK);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                handleMouseClick(e);
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                handleMouseMove(e);
            }
        });
    }

    private void handleMouseClick(MouseEvent e) {
        int tileX = e.getX() / TILE_SIZE;
        int tileY = e.getY() / TILE_SIZE;

        if (tileX >= 0 && tileX < DEFAULT_MAP_WIDTH &&
                tileY >= 0 && tileY < DEFAULT_MAP_HEIGHT) {

            if (SwingUtilities.isLeftMouseButton(e)) {
                // Move player
                Point oldPos = new Point(visualizer.getPlayerPosition());
                visualizer.setPlayerPosition(new Point(tileX, tileY));

                // Record path
                if (!playerPath.isEmpty() && !playerPath.get(playerPath.size() - 1).equals(oldPos)) {
                    playerPath.add(oldPos);
                } else if (playerPath.isEmpty()) {
                    playerPath.add(oldPos);
                }
                playerPath.add(visualizer.getPlayerPosition());

            } else if (SwingUtilities.isRightMouseButton(e)) {
                // Toggle wall
                if (visualizer.getCollisionData()[tileY][tileX] == 0) {
                    visualizer.getCollisionData()[tileY][tileX] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
                } else {
                    visualizer.getCollisionData()[tileY][tileX] = 0;
                }
            } else if (SwingUtilities.isMiddleMouseButton(e)) {
                // Add NPC
                visualizer.getNpcs().add(new SimNpc(new Point(tileX, tileY),
                        new Color((int)(Math.random() * 255),
                                (int)(Math.random() * 255),
                                (int)(Math.random() * 255)),
                        "NPC_" + visualizer.getNpcs().size()));
            }

            repaint();
        }
    }

    private void handleMouseMove(MouseEvent e) {
        int tileX = e.getX() / TILE_SIZE;
        int tileY = e.getY() / TILE_SIZE;

        if (tileX >= 0 && tileX < DEFAULT_MAP_WIDTH &&
                tileY >= 0 && tileY < DEFAULT_MAP_HEIGHT) {
            hoveredTile = new Point(tileX, tileY);
            updateInfoLabel(tileX, tileY);
        } else {
            hoveredTile = null;
        }
        repaint();
    }

    private void updateInfoLabel(int x, int y) {
        int flags = visualizer.getCollisionData()[y][x];
        Set<MovementFlag> setFlags = MovementFlag.getSetFlags(flags);
        String flagsStr = setFlags.isEmpty() ? "None" : setFlags.toString();
        visualizer.getInfoLabel().setText(String.format("Tile [%d, %d] - Flags: %s (0x%04X)",
                x, y, flagsStr, flags));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw tiles
        for (int y = 0; y < DEFAULT_MAP_HEIGHT; y++) {
            for (int x = 0; x < DEFAULT_MAP_WIDTH; x++) {
                drawTile(g2d, x, y);
            }
        }

        // Draw paths
        drawPaths(g2d);

        // Draw entities
        drawPlayer(g2d);
        drawNPCs(g2d);

        // Draw grid
        if (visualizer.getShowGridCheckbox().isSelected()) {
            drawGrid(g2d);
        }

        // Highlight hovered tile
        if (hoveredTile != null) {
            g2d.setColor(new Color(255, 255, 0, 100));
            g2d.fillRect(hoveredTile.x * TILE_SIZE, hoveredTile.y * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE);
        }
    }

    private void drawTile(Graphics2D g, int x, int y) {
        int flags = visualizer.getCollisionData()[y][x];

        // Base tile color
        g.setColor(new Color(50, 50, 50));
        g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);

        if (flags != 0 && visualizer.getShowFlagsCheckbox().isSelected()) {
            // Color based on collision type
            if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
            } else if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0) {
                g.setColor(Color.GRAY);
                g.fillRect(x * TILE_SIZE + 2, y * TILE_SIZE + 2,
                        TILE_SIZE - 4, TILE_SIZE - 4);
            } else if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION) != 0) {
                g.setColor(new Color(139, 69, 19));
                g.fillOval(x * TILE_SIZE + 4, y * TILE_SIZE + 4,
                        TILE_SIZE - 8, TILE_SIZE - 8);
            }

            // Draw directional blocks
            drawDirectionalBlocks(g, x, y, flags);
        }
    }

    private void drawDirectionalBlocks(Graphics2D g, int x, int y, int flags) {
        g.setColor(Color.ORANGE);
        int cx = x * TILE_SIZE + TILE_SIZE / 2;
        int cy = y * TILE_SIZE + TILE_SIZE / 2;

        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) {
            g.drawLine(cx, cy, cx, y * TILE_SIZE);
        }
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) {
            g.drawLine(cx, cy, cx, y * TILE_SIZE + TILE_SIZE);
        }
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0) {
            g.drawLine(cx, cy, x * TILE_SIZE + TILE_SIZE, cy);
        }
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0) {
            g.drawLine(cx, cy, x * TILE_SIZE, cy);
        }
    }

    private void drawGrid(Graphics2D g) {
        g.setColor(new Color(100, 100, 100, 50));
        for (int x = 0; x <= DEFAULT_MAP_WIDTH; x++) {
            g.drawLine(x * TILE_SIZE, 0, x * TILE_SIZE, DEFAULT_MAP_HEIGHT * TILE_SIZE);
        }
        for (int y = 0; y <= DEFAULT_MAP_HEIGHT; y++) {
            g.drawLine(0, y * TILE_SIZE, DEFAULT_MAP_WIDTH * TILE_SIZE, y * TILE_SIZE);
        }
    }

    private void drawPlayer(Graphics2D g) {
        g.setColor(Color.YELLOW);
        g.fillOval(visualizer.getPlayerPosition().x * TILE_SIZE + 2, visualizer.getPlayerPosition().y * TILE_SIZE + 2,
                TILE_SIZE - 4, TILE_SIZE - 4);
        g.setColor(Color.BLACK);
        g.drawString("P", visualizer.getPlayerPosition().x * TILE_SIZE + 6,
                visualizer.getPlayerPosition().y * TILE_SIZE + 14);
    }

    private void drawNPCs(Graphics2D g) {
        for (SimNpc npc : visualizer.getNpcs()) {
            g.setColor(npc.getColor());
            g.fillOval(npc.getPosition().x * TILE_SIZE + 4, npc.getPosition().y * TILE_SIZE + 4,
                    TILE_SIZE - 8, TILE_SIZE - 8);
        }
    }

    private void drawPaths(Graphics2D g) {
        // Draw player path
        if (playerPath.size() > 1) {
            g.setColor(new Color(255, 255, 0, 100));
            g.setStroke(new BasicStroke(2));
            for (int i = 0; i < playerPath.size() - 1; i++) {
                Point p1 = playerPath.get(i);
                Point p2 = playerPath.get(i + 1);
                g.drawLine(p1.x * TILE_SIZE + TILE_SIZE/2, p1.y * TILE_SIZE + TILE_SIZE/2,
                        p2.x * TILE_SIZE + TILE_SIZE/2, p2.y * TILE_SIZE + TILE_SIZE/2);
            }
        }

        // Draw NPC paths
        for (Map.Entry<SimNpc, java.util.List<Point>> entry : npcPaths.entrySet()) {
            SimNpc npc = entry.getKey();
            List<Point> path = entry.getValue();
            if (path.size() > 1) {
                Color pathColor = new Color(npc.getColor().getRed(), npc.getColor().getGreen(),
                        npc.getColor().getBlue(), 100);
                g.setColor(pathColor);
                g.setStroke(new BasicStroke(1));
                for (int i = 0; i < path.size() - 1; i++) {
                    Point p1 = path.get(i);
                    Point p2 = path.get(i + 1);
                    g.drawLine(p1.x * TILE_SIZE + TILE_SIZE/2, p1.y * TILE_SIZE + TILE_SIZE/2,
                            p2.x * TILE_SIZE + TILE_SIZE/2, p2.y * TILE_SIZE + TILE_SIZE/2);
                }
            }
        }
    }

    public void clearPaths() {
        playerPath.clear();
        npcPaths.clear();
    }

    public void addNPCPathPoint(SimNpc npc, Point point) {
        npcPaths.computeIfAbsent(npc, k -> new ArrayList<>()).add(point);
    }
}
