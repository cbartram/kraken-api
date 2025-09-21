package com.kraken.api.sim.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.interaction.tile.MovementFlag;
import com.kraken.api.sim.SimulationObserver;
import com.kraken.api.sim.model.SimNpc;
import com.kraken.api.sim.engine.SimulationEngine;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.CollisionDataFlag;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.kraken.api.sim.ui.SimulationVisualizer.*;

/**
 * This class is responsible for drawing on the canvas presented by the Simulation Visualizer. It handles
 * updating and drawing player and NPC paths based on updates made by the simulation engine. It also handles
 * user mouse and keyboard input on UI elements.
 */
@Slf4j
@Singleton
public class TilePanel extends JPanel implements SimulationObserver {
    private Point hoveredTile = null;
    private String hoverTooltipText = null;
    private Point hoverTooltipPosition = null;
    private final SimulationEngine engine;
    private final SimulationUIState state;
    private boolean npcPlacementMode = false;
    private SimNpc hoveredNpc = null;
    private String temporaryMessage = null;

    // Zoom and pan variables
    private double zoomLevel = 1.0;
    private static final double MIN_ZOOM = 0.25;
    private static final double MAX_ZOOM = 4.0;
    private static final double ZOOM_STEP = 0.1;

    private int panX = 0;
    private int panY = 0;
    private Point lastMousePoint = null;
    private boolean isPanning = false;

    // TODO Don't love this means of transferring data between the classes
    @Setter
    private SimulationVisualizer visualizer;

    @Inject
    public TilePanel(final SimulationUIState state, final SimulationEngine engine) {
        this.state = state;
        this.engine = engine;
        this.engine.addObserver(this);

        setFocusable(true);
        requestFocusInWindow();
        setPreferredSize(new Dimension(
                (int)(DEFAULT_MAP_WIDTH * TILE_SIZE * zoomLevel),
                (int)(DEFAULT_MAP_HEIGHT * TILE_SIZE * zoomLevel)
        ));
        setBackground(Color.BLACK);

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("P"), "updatePlayerPosition");
        getActionMap().put("updatePlayerPosition", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if(hoveredTile != null) {
                    engine.reset();
                    engine.setPlayerPosition(hoveredTile);
                    repaint();
                }
            }
        });

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

            @Override
            public void mouseExited(MouseEvent e) {
                // Clear hover info when mouse leaves the panel
                hoveredTile = null;
                hoverTooltipText = null;
                hoverTooltipPosition = null;
                repaint();
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
                } else if (!isPanning) {
                    // Update hover info even while dragging (if not panning)
                    handleMouseMove(e);
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

    @Override
    public void onSimulationUpdated() {
        SwingUtilities.invokeLater(this::repaint);
    }

    /**
     * Converts screen coordinates to tile coordinates
     * @param screenX Screen x coordinate
     * @param screenY Screen y coordinate
     * @return A point representing the tile.
     */
    private Point screenToTile(int screenX, int screenY) {
        int worldX = (int)((screenX - panX) / (TILE_SIZE * zoomLevel));
        int worldY = (int)((screenY - panY) / (TILE_SIZE * zoomLevel));
        return new Point(worldX, worldY);
    }

    /**
     * Converts tile coordinates to screen coordinates
     * @param tileX The tile coordinate to convert (X)
     * @param tileY The tile coordinate to convert (Y)
     * @return A point representing the screen point for a given tile.
     */
    private Point tileToScreen(int tileX, int tileY) {
        int screenX = (int)(tileX * TILE_SIZE * zoomLevel + panX);
        int screenY = (int)(tileY * TILE_SIZE * zoomLevel + panY);
        return new Point(screenX, screenY);
    }

    /**
     * Updates the information label based on which tile is currently being hovered.
     * @param x X coordinate
     * @param y Y coordinate
     */
    private void updateInfoLabel(int x, int y) {
        if (state.getInfoLabel() != null) {
            int flags = engine.getCollisionData()[y][x];
            Set<MovementFlag> setFlags = MovementFlag.getSetFlags(flags);
            String flagsStr = setFlags.isEmpty() ? "None" : setFlags.toString();
            state.getInfoLabel().setText(String.format("Tile [%d, %d] - Flags: %s (0x%04X) - Zoom: %.1fx",
                    x, y, flagsStr, flags, zoomLevel));
        }
    }
    /**
     * Zooms the map in
     */
    public void zoomIn() {
        if (zoomLevel < MAX_ZOOM) {
            zoomLevel = Math.min(MAX_ZOOM, zoomLevel + ZOOM_STEP);
            updatePreferredSize();
            repaint();
        }
    }

    /**
     * Zooms the map out
     */
    public void zoomOut() {
        if (zoomLevel > MIN_ZOOM) {
            zoomLevel = Math.max(MIN_ZOOM, zoomLevel - ZOOM_STEP);
            updatePreferredSize();
            repaint();
        }
    }

    /**
     * Resets the zoom to 1.0
     */
    public void resetZoom() {
        zoomLevel = 1.0;
        panX = 0;
        panY = 0;
        updatePreferredSize();
        repaint();
    }

    /**
     * Centers the grid on the current view
     */
    public void centerView() {
        panX = (getWidth() - (int)(engine.getCollisionData()[0].length * TILE_SIZE * zoomLevel)) / 2;
        panY = (getHeight() - (int)(engine.getCollisionData().length * TILE_SIZE * zoomLevel)) / 2;
        repaint();
    }

    /**
     * Updates the preferred size of the grid
     */
    private void updatePreferredSize() {
        int[][] data = engine.getCollisionData();
        int width = data[0].length;
        int height = data.length;

        setPreferredSize(new Dimension(
                width * TILE_SIZE,
                height * TILE_SIZE
        ));
        revalidate();
    }

    // Add visual feedback for NPC placement mode in paintComponent
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2d.translate(panX, panY);
        g2d.scale(zoomLevel, zoomLevel);

        int[][] data = engine.getCollisionData();
        for (int y = 0; y < data.length; y++) {
            for (int x = 0; x < data[y].length; x++) {
                drawTile(g2d, x, y);
            }
        }

        drawPaths(g2d);
        drawPlayer(g2d);
        drawNPCs(g2d);

        if (state.isShowGrid()) {
            drawGrid(g2d);
        }

        // Highlight hovered tile
        if (hoveredTile != null) {
            Color highlightColor = new Color(161, 0, 255, 100);

            // Different highlight color for NPC placement mode
            if (npcPlacementMode) {
                highlightColor = new Color(0, 255, 0, 100);
            }

            // Different color if NPC is present
            if (hoveredNpc != null) {
                highlightColor = new Color(255, 100, 100, 150);
            }

            g2d.setColor(highlightColor);
            g2d.setStroke(new BasicStroke(2));
            g2d.fillRect(hoveredTile.x * TILE_SIZE, hoveredTile.y * TILE_SIZE,
                    TILE_SIZE, TILE_SIZE);

            // Draw border for NPC placement mode
            if (npcPlacementMode) {
                g2d.setColor(Color.GREEN);
                g2d.setStroke(new BasicStroke(2));
                g2d.drawRect(hoveredTile.x * TILE_SIZE, hoveredTile.y * TILE_SIZE,
                        TILE_SIZE, TILE_SIZE);
            }
        }

        // Reset transform for tooltip and UI overlay drawing
        g2d.setTransform(new java.awt.geom.AffineTransform());
        drawHoverTooltip(g2d);
        drawNpcPlacementOverlay(g2d);
    }

    /**
     * Draws the hover tooltip near the mouse cursor
     * @param g2d Graphics2D object
     */
    private void drawHoverTooltip(Graphics2D g2d) {
        if (hoverTooltipText != null && hoverTooltipPosition != null) {
            // Create a temporary JLabel to measure the text
            JLabel tempLabel = new JLabel(hoverTooltipText);
            tempLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            tempLabel.setSize(tempLabel.getPreferredSize());

            int tooltipWidth = tempLabel.getPreferredSize().width + 10;
            int tooltipHeight = tempLabel.getPreferredSize().height + 8;

            // Draw semi-transparent background
            g2d.setColor(new Color(0, 0, 0, 200));
            g2d.fillRoundRect(hoverTooltipPosition.x, hoverTooltipPosition.y,
                    tooltipWidth, tooltipHeight, 8, 8);

            // Draw border
            g2d.setColor(new Color(255, 255, 255, 150));
            g2d.setStroke(new BasicStroke(1));
            g2d.drawRoundRect(hoverTooltipPosition.x, hoverTooltipPosition.y,
                    tooltipWidth, tooltipHeight, 8, 8);

            // Draw text
            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));

            // Parse HTML and draw line by line
            String[] lines = hoverTooltipText.replace("<html>", "").replace("</html>", "").split("<br/>");
            for (int i = 0; i < lines.length; i++) {
                g2d.drawString(lines[i],
                        hoverTooltipPosition.x + 5,
                        hoverTooltipPosition.y + 15 + (i * 14));
            }
        }
    }

    /**
     * Draws the collision data onto the grid of tiles. This method checks the collision
     * data and will draw the correct graphic representing if the tile is blocked, impassable, or walkable.
     * @param g Graphics object
     * @param x X coordinate to draw
     * @param y Y coordinate to draw
     */
    private void drawTile(Graphics2D g, int x, int y) {
        int flags = engine.getCollisionData()[y][x];

        int px = x * TILE_SIZE;
        int py = y * TILE_SIZE;

        // Base tile background
        g.setColor(new Color(50, 50, 50));
        g.fillRect(px, py, TILE_SIZE, TILE_SIZE);

        if (flags != 0 && state.isShowFlags()) {
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

    /**
     * Draws the directional blocks representing walls which cannot be passed through but can be walked around.
     * @param g Graphics object
     * @param px X coordinate
     * @param py Y coordinate
     * @param flags Collision flags
     */
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
    }

    /**
     * Draws the grid of tiles
     * @param g Graphics object
     */
    private void drawGrid(Graphics2D g) {
        int[][] data = engine.getCollisionData();
        int width = data[0].length;
        int height = data.length;

        g.setColor(new Color(100, 100, 100, 50));
        for (int x = 0; x <= width; x++) {
            g.drawLine(x * TILE_SIZE, 0, x * TILE_SIZE, height * TILE_SIZE);
        }
        for (int y = 0; y <= height; y++) {
            g.drawLine(0, y * TILE_SIZE, width * TILE_SIZE, y * TILE_SIZE);
        }
    }

    /**
     * Draws the players location on the grid of tiles.
     * @param g Graphics object
     */
    private void drawPlayer(Graphics2D g) {
        g.setColor(new Color(6, 239, 79, 255));
        g.setStroke(new BasicStroke(2));
        g.fillRect(engine.getPlayerPosition().x * TILE_SIZE + 2, engine.getPlayerPosition().y * TILE_SIZE + 2,
                TILE_SIZE - 4, TILE_SIZE - 4);
        g.setColor(Color.BLACK);
        g.drawString("P", engine.getPlayerPosition().x * TILE_SIZE + 6,
                engine.getPlayerPosition().y * TILE_SIZE + 14);
    }

    /**
     * Draws the NPCs onto the grid of tiles
     * @param g Graphics object
     */
    private void drawNPCs(Graphics2D g) {
        for (SimNpc npc : engine.getNpcs()) {
            int npcSize = npc.getSize();
            int x = npc.getPosition().x * TILE_SIZE;
            int y = npc.getPosition().y * TILE_SIZE;

            if (npcSize == 1) {
                g.setColor(npc.getColor());
                g.setStroke(new BasicStroke(2));
                g.fillRect(x + 2, y + 2, TILE_SIZE - 4, TILE_SIZE - 4);
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(npc.getName().charAt(0)), x + 6, y + 14);
            } else {
                int totalWidth = npcSize * TILE_SIZE;
                int totalHeight = npcSize * TILE_SIZE;

                Color npcColor = npc.getColor();
                Color transparentColor = new Color(
                        npcColor.getRed(),
                        npcColor.getGreen(),
                        npcColor.getBlue(),
                        180
                );

                g.setColor(transparentColor);
                g.fillRect(x + 1, y + 1, totalWidth - 2, totalHeight - 2);

                g.setColor(npcColor); // Use full opacity for border
                g.setStroke(new BasicStroke(3));
                g.drawRect(x + 1, y + 1, totalWidth - 2, totalHeight - 2);

                g.setColor(new Color(npcColor.getRed(), npcColor.getGreen(), npcColor.getBlue(), 100));
                g.setStroke(new BasicStroke(1));

                for (int i = 1; i < npcSize; i++) {
                    int lineX = x + i * TILE_SIZE;
                    g.drawLine(lineX, y + 1, lineX, y + totalHeight - 1);
                }

                for (int i = 1; i < npcSize; i++) {
                    int lineY = y + i * TILE_SIZE;
                    g.drawLine(x + 1, lineY, x + totalWidth - 1, lineY);
                }

                int nameX = x + 6;
                int nameY = y + (npcSize - 1) * TILE_SIZE + 14;

                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(3));
                g.drawString(String.valueOf(npc.getName().charAt(0)), nameX - 1, nameY);
                g.drawString(String.valueOf(npc.getName().charAt(0)), nameX + 1, nameY);
                g.drawString(String.valueOf(npc.getName().charAt(0)), nameX, nameY - 1);
                g.drawString(String.valueOf(npc.getName().charAt(0)), nameX, nameY + 1);

                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(npc.getName().charAt(0)), nameX, nameY);
            }
        }
    }

    /**
     * Draws the player and NPC paths
     * @param g Graphics object
     */
    private void drawPaths(Graphics2D g) {
        // Draw player path
        if (engine.getPlayerCurrentPath().size() > 1) {
            g.setColor(new Color(10, 236, 55, 200));
            g.setStroke(new BasicStroke(2));

            for (int i = 0; i < engine.getPlayerCurrentPath().size() - 1; i++) {
                Point p1 = engine.getPlayerCurrentPath().get(i);
                Point p2 = engine.getPlayerCurrentPath().get(i + 1);
                g.drawLine(p1.x * TILE_SIZE + TILE_SIZE/2, p1.y * TILE_SIZE + TILE_SIZE/2,
                        p2.x * TILE_SIZE + TILE_SIZE/2, p2.y * TILE_SIZE + TILE_SIZE/2);
            }

            // Draw small ovals at each path point
            g.setColor(new Color(10, 236, 55, 255)); // Solid color for ovals
            for (Point p : engine.getPlayerCurrentPath()) {
                int centerX = p.x * TILE_SIZE + TILE_SIZE/2;
                int centerY = p.y * TILE_SIZE + TILE_SIZE/2;
                g.fillOval(centerX - 3, centerY - 3, 6, 6); // 6x6 pixel oval
            }
        }

        if(engine.getTargetPosition() != null) {
            g.setColor(new Color(239, 122, 6, 255));
            g.setStroke(new BasicStroke(2));
            g.fillRect(engine.getTargetPosition().x * TILE_SIZE + 2, engine.getTargetPosition().y * TILE_SIZE + 2, TILE_SIZE - 4, TILE_SIZE - 4);
            g.setColor(Color.BLACK);
            g.drawString("T", engine.getTargetPosition().x * TILE_SIZE + 6,
                    engine.getTargetPosition().y * TILE_SIZE + 14);
        }

        // Draw NPC paths
        for (Map.Entry<SimNpc, java.util.List<Point>> entry : engine.getNpcPaths().entrySet()) {
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


    public void setNpcPlacementMode(boolean enabled) {
        this.npcPlacementMode = enabled;
        repaint();
    }

    /**
     * Enhanced mouse click handler with NPC placement support
     */
    private void handleMouseClick(MouseEvent e) {
        Point tileCoords = screenToTile(e.getX(), e.getY());
        int tileX = tileCoords.x;
        int tileY = tileCoords.y;

        int[][] data = engine.getCollisionData();
        if (tileY >= 0 && tileY < data.length && tileX >= 0 && tileX < data[0].length) {

            // Handle NPC operations first (with modifier keys)
            if (e.isShiftDown() && SwingUtilities.isLeftMouseButton(e)) {
                // Shift + Left Click: Place new NPC
                handleNpcPlacement(tileX, tileY);
                return;
            }

            if (e.isControlDown() && SwingUtilities.isRightMouseButton(e)) {
                // Ctrl + Right Click: Remove NPC
                handleNpcRemoval(tileX, tileY);
                return;
            }

            if (e.isAltDown() && SwingUtilities.isLeftMouseButton(e)) {
                // Alt + Left Click: Select/edit NPC
                handleNpcSelection(tileX, tileY);
                return;
            }

            // Handle double-click on NPC for detailed editing
            if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                SimNpc npcAtTile = getNpcAtTile(tileX, tileY);
                if (npcAtTile != null) {
                    // Open detailed edit dialog (implement in SimulationVisualizer)
                    state.setNpcEditDialogOpen(true);
                    return;
                }
            }

            // Default behavior for regular clicks
            if (SwingUtilities.isLeftMouseButton(e)) {
                if (npcPlacementMode) {
                    handleNpcPlacement(tileX, tileY);
                } else {
                    engine.setPlayerTarget(new Point(tileX, tileY));
                }
            } else if (SwingUtilities.isRightMouseButton(e)) {
                // Toggle wall (existing functionality)
                if (engine.getCollisionData()[tileY][tileX] == 0) {
                    engine.getCollisionData()[tileY][tileX] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
                } else {
                    engine.getCollisionData()[tileY][tileX] = 0;
                }
            }
            repaint();
        }
    }

    /**
     * Handles placing a new NPC at the specified tile
     */
    private void handleNpcPlacement(int tileX, int tileY) {
        // Check if tile is walkable
        if (engine.getCollisionData()[tileY][tileX] != 0) {
            // Show tooltip or status message
            showTemporaryMessage("Cannot place NPC on blocked tile!");
            return;
        }

        // Check if there's already an NPC at this location
        if (getNpcAtTile(tileX, tileY) != null) {
            showTemporaryMessage("NPC already exists at this location!");
            return;
        }

        Point position = new Point(tileX, tileY);
        SimNpc newNpc = visualizer.createNpcFromCurrentSettings(position);
        visualizer.addNpcToSimulation(newNpc);
        showTemporaryMessage("NPC '" + newNpc.getName() + "' placed!");
    }

    /**
     * Handles removing an NPC from the specified tile
     */
    private void handleNpcRemoval(int tileX, int tileY) {
        SimNpc npcToRemove = getNpcAtTile(tileX, tileY);
        if (npcToRemove != null) {
            engine.removeNpc(npcToRemove);
            if (visualizer != null) {
                visualizer.removeNpcFromList(npcToRemove);
            }
            showTemporaryMessage("NPC '" + npcToRemove.getName() + "' removed!");
            repaint();
        }
    }

    /**
     * Handles selecting an NPC for editing
     */
    private void handleNpcSelection(int tileX, int tileY) {
        SimNpc npcAtTile = getNpcAtTile(tileX, tileY);
        if (npcAtTile != null && visualizer != null) {
            visualizer.selectNpcInList(npcAtTile);
            showTemporaryMessage("Selected NPC: " + npcAtTile.getName());
        }
    }

    /**
     * Gets the NPC at the specified tile coordinates
     */
    private SimNpc getNpcAtTile(int tileX, int tileY) {
        for (SimNpc npc : engine.getNpcs()) {
            if (npc.getPosition().x == tileX && npc.getPosition().y == tileY) {
                return npc;
            }
        }
        return null;
    }

    /**
     * Enhanced mouse move handler with NPC hover detection
     */
    private void handleMouseMove(MouseEvent e) {
        Point tileCoords = screenToTile(e.getX(), e.getY());
        int tileX = tileCoords.x;
        int tileY = tileCoords.y;

        int[][] data = engine.getCollisionData();
        if (tileY >= 0 && tileY < data.length &&
                tileX >= 0 && tileX < data[0].length) {
            hoveredTile = new Point(tileX, tileY);
            hoveredNpc = getNpcAtTile(tileX, tileY);

            if(state.isShowTooltip()) {
                updateHoverTooltip(tileX, tileY, e.getX(), e.getY());
            }
            updateInfoLabel(tileX, tileY);
        } else {
            hoveredTile = null;
            hoveredNpc = null;
            hoverTooltipText = null;
            hoverTooltipPosition = null;
        }
        repaint();
    }

    /**
     * Enhanced tooltip with NPC information
     */
    private void updateHoverTooltip(int x, int y, int mouseX, int mouseY) {
        StringBuilder tooltipContent = new StringBuilder();
        tooltipContent.append(String.format("<html>Tile [%d, %d]<br/>", x, y));

        // Add collision flag info
        int flags = engine.getCollisionData()[y][x];
        Set<MovementFlag> setFlags = MovementFlag.getSetFlags(flags);
        String flagsStr = setFlags.isEmpty() ? "None" : setFlags.toString();
        tooltipContent.append(String.format("Flags: %s<br/>(0x%04X)<br/>", flagsStr, flags));

        // Add NPC info if present
        if (hoveredNpc != null) {
            tooltipContent.append(String.format("<br/>Name: %s<br/>", hoveredNpc.getName()));
            tooltipContent.append(String.format("Type: %s<br/>", hoveredNpc.getAttackStyle().name()));
            tooltipContent.append(String.format("Attack Range: %d<br/>", hoveredNpc.getAttackRange()));
        }

        tooltipContent.append(String.format("Zoom: %.1fx</html>", zoomLevel));

        hoverTooltipText = tooltipContent.toString();

        // Position tooltip slightly offset from mouse cursor
        hoverTooltipPosition = new Point(mouseX + 15, mouseY - 10);

        // Ensure tooltip stays within panel bounds
        if (hoverTooltipPosition.x + 250 > getWidth()) { // Increased width for NPC info
            hoverTooltipPosition.x = mouseX - 265;
        }
        if (hoverTooltipPosition.y < 0) {
            hoverTooltipPosition.y = mouseY + 20;
        }
    }

    /**
     * Draws overlay information for NPC placement mode
     */
    private void drawNpcPlacementOverlay(Graphics2D g2d) {
        if (npcPlacementMode) {
            g2d.setColor(new Color(0, 255, 0, 150));
            g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2d.drawString("NPC Placement Mode Active", 10, 25);

            g2d.setColor(new Color(255, 255, 255, 200));
            g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
            g2d.drawString("Shift+Click: Place NPC | Ctrl+RClick: Remove NPC | Alt+Click: Select NPC", 10, 45);
        }
    }

    private void showTemporaryMessage(String message) {
        temporaryMessage = message;
        long messageTimestamp = System.currentTimeMillis();

        // Auto-clear after 3 seconds
        Timer timer = new Timer(3000, e -> {
            temporaryMessage = null;
            repaint();
        });
        timer.setRepeats(false);
        timer.start();

        repaint();
    }
}