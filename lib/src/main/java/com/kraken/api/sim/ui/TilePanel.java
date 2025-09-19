package com.kraken.api.sim.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.interaction.tile.MovementFlag;
import com.kraken.api.sim.SimNpc;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.CollisionDataFlag;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kraken.api.sim.ui.SimulationVisualizer.*;

@Slf4j
@Singleton
public class TilePanel extends JPanel {
    private final List<Point> playerPath = new ArrayList<>();
    private final Map<SimNpc, List<Point>> npcPaths = new HashMap<>();
    private Point hoveredTile = null;
    private final SimulationVisualizer visualizer;

    // Zoom and pan variables
    private double zoomLevel = 1.0;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 4.0;
    private static final double ZOOM_STEP = 0.1;

    private int panX = 0;
    private int panY = 0;
    private Point lastMousePoint = null;
    private boolean isPanning = false;

    @Inject
    public TilePanel(SimulationVisualizer visualizer) {
        this.visualizer = visualizer;
        setPreferredSize(new Dimension(
                (int)(DEFAULT_MAP_WIDTH * TILE_SIZE * zoomLevel),
                (int)(DEFAULT_MAP_HEIGHT * TILE_SIZE * zoomLevel)
        ));
        setBackground(Color.BLACK);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (!isPanning) {
                    handleMouseClick(e);
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    isPanning = true;
                    lastMousePoint = e.getPoint();
                    setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isMiddleMouseButton(e)) {
                    isPanning = false;
                    setCursor(Cursor.getDefaultCursor());
                }
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (!isPanning) {
                    handleMouseMove(e);
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (isPanning && lastMousePoint != null) {
                    int dx = e.getX() - lastMousePoint.x;
                    int dy = e.getY() - lastMousePoint.y;
                    panX += dx;
                    panY += dy;
                    lastMousePoint = e.getPoint();
                    repaint();
                }
            }
        });

        // Add mouse wheel listener for zooming
        addMouseWheelListener(e -> {
            double oldZoom = zoomLevel;
            Point mousePoint = e.getPoint();

            // Calculate zoom
            if (e.getWheelRotation() < 0) {
                zoomIn();
            } else {
                zoomOut();
            }

            // Adjust pan to keep zoom centered on mouse position
            if (oldZoom != zoomLevel) {
                double zoomRatio = zoomLevel / oldZoom;
                panX = (int)((panX - mousePoint.x) * zoomRatio + mousePoint.x);
                panY = (int)((panY - mousePoint.y) * zoomRatio + mousePoint.y);
            }
        });
    }

    private void handleMouseClick(MouseEvent e) {
        Point tileCoords = screenToTile(e.getX(), e.getY());
        int tileX = tileCoords.x;
        int tileY = tileCoords.y;

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
            }
            // Note: Removed middle mouse button NPC creation since it's now used for panning

            repaint();
        }
    }

    private void handleMouseMove(MouseEvent e) {
        Point tileCoords = screenToTile(e.getX(), e.getY());
        int tileX = tileCoords.x;
        int tileY = tileCoords.y;

        if (tileX >= 0 && tileX < DEFAULT_MAP_WIDTH &&
                tileY >= 0 && tileY < DEFAULT_MAP_HEIGHT) {
            hoveredTile = new Point(tileX, tileY);
            updateInfoLabel(tileX, tileY);
        } else {
            hoveredTile = null;
        }
        repaint();
    }

    // Convert screen coordinates to tile coordinates
    private Point screenToTile(int screenX, int screenY) {
        int worldX = (int)((screenX - panX) / (TILE_SIZE * zoomLevel));
        int worldY = (int)((screenY - panY) / (TILE_SIZE * zoomLevel));
        return new Point(worldX, worldY);
    }

    // Convert tile coordinates to screen coordinates
    private Point tileToScreen(int tileX, int tileY) {
        int screenX = (int)(tileX * TILE_SIZE * zoomLevel + panX);
        int screenY = (int)(tileY * TILE_SIZE * zoomLevel + panY);
        return new Point(screenX, screenY);
    }

    private void updateInfoLabel(int x, int y) {
        int flags = visualizer.getCollisionData()[y][x];
        Set<MovementFlag> setFlags = MovementFlag.getSetFlags(flags);
        String flagsStr = setFlags.isEmpty() ? "None" : setFlags.toString();
        visualizer.getInfoLabel().setText(String.format("Tile [%d, %d] - Flags: %s (0x%04X) - Zoom: %.1fx",
                x, y, flagsStr, flags, zoomLevel));
    }

    public void zoomIn() {
        if (zoomLevel < MAX_ZOOM) {
            zoomLevel = Math.min(MAX_ZOOM, zoomLevel + ZOOM_STEP);
            updatePreferredSize();
            repaint();
        }
    }

    public void zoomOut() {
        if (zoomLevel > MIN_ZOOM) {
            zoomLevel = Math.max(MIN_ZOOM, zoomLevel - ZOOM_STEP);
            updatePreferredSize();
            repaint();
        }
    }

    public void resetZoom() {
        zoomLevel = 1.0;
        panX = 0;
        panY = 0;
        updatePreferredSize();
        repaint();
    }

    public void centerView() {
        panX = (getWidth() - (int)(DEFAULT_MAP_WIDTH * TILE_SIZE * zoomLevel)) / 2;
        panY = (getHeight() - (int)(DEFAULT_MAP_HEIGHT * TILE_SIZE * zoomLevel)) / 2;
        repaint();
    }

    private void updatePreferredSize() {
        setPreferredSize(new Dimension(
                (int)(DEFAULT_MAP_WIDTH * TILE_SIZE * zoomLevel),
                (int)(DEFAULT_MAP_HEIGHT * TILE_SIZE * zoomLevel)
        ));
        revalidate();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply zoom and pan transformations
        g2d.translate(panX, panY);
        g2d.scale(zoomLevel, zoomLevel);

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

        int px = x * TILE_SIZE;
        int py = y * TILE_SIZE;

        // Base tile background
        g.setColor(new Color(50, 50, 50));
        g.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        if (flags != 0 && visualizer.getShowFlagsCheckbox().isSelected()) {
            // Full block = fill whole tile
            if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0) {
                g.setColor(Color.DARK_GRAY);
                g.fillRect(px, py, TILE_SIZE, TILE_SIZE);
            }

            // Object block = smaller rectangle inside
            if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_OBJECT) != 0) {
                g.setColor(Color.GRAY);
                g.fillRect(px + 2, py + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            }

            // Floor decoration = brown circle
            if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION) != 0) {
                g.setColor(new Color(139, 69, 19));
                g.fillOval(px + 4, py + 4, TILE_SIZE - 8, TILE_SIZE - 8);
            }

            // Directional blocks (orange lines)
            drawDirectionalBlocks(g, px, py, flags);
        }
    }

    private void drawDirectionalBlocks(Graphics2D g, int px, int py, int flags) {
        g.setColor(Color.ORANGE);

        // Draw lines along the edges where movement is blocked
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0) {
            g.drawLine(px, py, px + TILE_SIZE, py);
        }
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) {
            g.drawLine(px, py + TILE_SIZE, px + TILE_SIZE, py + TILE_SIZE);
        }
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0) {
            g.drawLine(px + TILE_SIZE, py, px + TILE_SIZE, py + TILE_SIZE);
        }
        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0) {
            g.drawLine(px, py, px, py + TILE_SIZE);
        }

        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST) != 0) {
            g.drawLine(px, py, px + TILE_SIZE, py);
            g.drawLine(px + TILE_SIZE, py, px + TILE_SIZE, py + TILE_SIZE);
        }

        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST) != 0) {
            g.drawLine(px, py, px + TILE_SIZE, py);
            g.drawLine(px, py, px, py + TILE_SIZE);
        }

        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST) != 0) {
            g.drawLine(px, py + TILE_SIZE, px + TILE_SIZE, py + TILE_SIZE);
            g.drawLine(px + TILE_SIZE, py, px + TILE_SIZE, py + TILE_SIZE);
        }

        if ((flags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST) != 0) {
            g.drawLine(px, py + TILE_SIZE, px + TILE_SIZE, py + TILE_SIZE);
            g.drawLine(px, py, px, py + TILE_SIZE);
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