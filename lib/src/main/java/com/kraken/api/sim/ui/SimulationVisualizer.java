package com.kraken.api.sim.ui;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.interaction.tile.TileCollisionDump;
import com.kraken.api.sim.SimNpc;
import com.kraken.api.sim.SimulationEngine;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main visualizer for OSRS tile collision data and movement simulation. This class is responsible
 * for the UI and frame for rendering the visualization.
 */
@Slf4j
@Singleton
public class SimulationVisualizer extends JFrame {
    public static final int TILE_SIZE = 20;
    public static final int DEFAULT_MAP_WIDTH = 50;
    public static final int DEFAULT_MAP_HEIGHT = 50;

    private JButton simulateButton;

    @Getter
    private JCheckBox showGridCheckbox;

    @Getter
    private JCheckBox showFlagsCheckbox;

    @Getter
    private JLabel infoLabel;

    @Getter
    private int[][] collisionData;

    @Getter
    private List<SimNpc> npcs;

    @Inject
    private SimulationEngine simulationEngine;

    @Inject
    private TilePanel tilePanel;

    public void init() {
        collisionData = new int[DEFAULT_MAP_HEIGHT][DEFAULT_MAP_WIDTH];
        npcs = new CopyOnWriteArrayList<>();
        initializeUI();
        loadCollisionDataFromJson("collision_dump.json");
    }
    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Main tile panel - removed scroll pane, using direct panel
        tilePanel.setPreferredSize(new Dimension(800, 600));
        add(tilePanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.EAST);

        // Info panel
        JPanel infoPanel = new JPanel();
        infoLabel = new JLabel("Hover over tiles for information");
        infoPanel.add(infoLabel);
        add(infoPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Zoom controls
        JLabel zoomLabel = new JLabel("Zoom Controls:");
        zoomLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(zoomLabel);

        JPanel zoomPanel = createZoomControlPanel();
        zoomPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(zoomPanel);

        panel.add(Box.createVerticalStrut(15));

        // Display options
        JLabel displayLabel = new JLabel("Display Options:");
        displayLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(displayLabel);

        showGridCheckbox = new JCheckBox("Show Grid", true);
        showGridCheckbox.addActionListener(e -> tilePanel.repaint());
        showGridCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(showGridCheckbox);

        showFlagsCheckbox = new JCheckBox("Show Collision Flags", true);
        showFlagsCheckbox.addActionListener(e -> tilePanel.repaint());
        showFlagsCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(showFlagsCheckbox);

        panel.add(Box.createVerticalStrut(15));

        // Simulation controls
        JLabel simLabel = new JLabel("Simulation:");
        simLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(simLabel);

        simulateButton = new JButton("Start Simulation");
        simulateButton.addActionListener(e -> toggleSimulation(simulationEngine));
        simulateButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(simulateButton);

        JButton clearPathButton = new JButton("Clear Paths");
        clearPathButton.addActionListener(e -> {
            tilePanel.clearPaths();
            tilePanel.repaint();
        });
        clearPathButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(clearPathButton);

        panel.add(Box.createVerticalStrut(15));

        // Collision legend
        JLabel legendLabel = new JLabel("Collision Types:");
        legendLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(legendLabel);

        JPanel legend = createLegend();
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(legend);

        panel.add(Box.createVerticalStrut(15));

        // Instructions
        JTextArea instructions = new JTextArea(
                "Instructions:\n" +
                        "• Left Click: Move player\n" +
                        "• Right Click: Add/Remove wall\n" +
                        "• Middle Click + Drag: Pan view\n" +
                        "• Mouse Wheel: Zoom in/out\n" +
                        "• Hover: View tile info"
        );
        instructions.setEditable(false);
        instructions.setOpaque(false); // transparent background
        instructions.setFont(new Font("SansSerif", Font.PLAIN, 12));
        instructions.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(instructions);

        return panel;
    }

    private JPanel createZoomControlPanel() {
        JPanel zoomPanel = new JPanel(new GridLayout(2, 2, 5, 5));

        // Zoom control buttons
        JButton zoomInButton = new JButton("Zoom In");
        zoomInButton.addActionListener(e -> tilePanel.zoomIn());
        zoomPanel.add(zoomInButton);

        JButton zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.addActionListener(e -> tilePanel.zoomOut());
        zoomPanel.add(zoomOutButton);

        JButton resetZoomButton = new JButton("Reset Zoom");
        resetZoomButton.addActionListener(e -> tilePanel.resetZoom());
        zoomPanel.add(resetZoomButton);

        JButton centerViewButton = new JButton("Center View");
        centerViewButton.addActionListener(e -> tilePanel.centerView());
        zoomPanel.add(centerViewButton);

        return zoomPanel;
    }

    private JPanel createLegend() {
        JPanel legend = new JPanel();
        legend.setLayout(new GridLayout(0, 1));

        legend.add(createLegendItem(Color.DARK_GRAY, "Full Block"));
        legend.add(createLegendItem(Color.GRAY, "Object Block"));
        legend.add(createLegendItem(new Color(139, 69, 19), "Floor Decoration"));
        legend.add(createLegendItem(Color.ORANGE, "Directional Block"));

        return legend;
    }

    private JPanel createLegendItem(Color color, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel colorBox = new JLabel("■");
        colorBox.setForeground(color);
        item.add(colorBox);
        item.add(new JLabel(text));
        return item;
    }

    private void loadCollisionDataFromJson(final String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Gson gson = new Gson();
            java.lang.reflect.Type listType = new TypeToken<List<TileCollisionDump>>() {}.getType();
            List<TileCollisionDump> tiles = gson.fromJson(reader, listType);

            // Find bounds of world coordinates
            int minX = tiles.stream().mapToInt(TileCollisionDump::getWorldPointX).min().orElse(0);
            int minY = tiles.stream().mapToInt(TileCollisionDump::getWorldPointY).min().orElse(0);
            int maxX = tiles.stream().mapToInt(TileCollisionDump::getWorldPointX).max().orElse(DEFAULT_MAP_WIDTH);
            int maxY = tiles.stream().mapToInt(TileCollisionDump::getWorldPointY).max().orElse(DEFAULT_MAP_HEIGHT);

            int width = maxX - minX + 1;
            int height = maxY - minY + 1;

            collisionData = new int[height][width];

            // Place player at first tile relative to minX/minY (this is the tile collision data was generated from)
            int playerLocalX = tiles.get(0).getWorldPointX() - minX;
            int playerLocalY = tiles.get(0).getWorldPointY() - minY;
            int playerFlippedY = height - 1 - playerLocalY;
            simulationEngine.setPlayerPosition(new Point(playerLocalX, playerFlippedY));

            for (TileCollisionDump tile : tiles) {
                int localX = tile.getWorldPointX() - minX;
                int localY = tile.getWorldPointY() - minY;

                // Flip the Y coordinate to correct the reflection
                int flippedY = height - 1 - localY;

                if (localX >= 0 && localX < width && flippedY >= 0 && flippedY < height) {
                    collisionData[flippedY][localX] = tile.getRawFlags();
                }
            }

            simulationEngine.setCollisionData(collisionData);
            log.info("Loaded collision data from JSON (" + tiles.size() + " tiles). " +
                    "Bounds: X[" + minX + "," + maxX + "] Y[" + minY + "," + maxY + "]");
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void toggleSimulation(SimulationEngine engine) {
        if (engine.isRunning()) {
            engine.stop();
            simulateButton.setText("Start Simulation");
        } else {
            engine.start();
            simulateButton.setText("Stop Simulation");
        }
    }
}