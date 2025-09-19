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
import net.runelite.api.CollisionDataFlag;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main visualizer for OSRS tile collision data and movement simulation
 */
@Slf4j
@Singleton
public class SimulationVisualizer extends JFrame {
    public static final int TILE_SIZE = 20;
    public static final int DEFAULT_MAP_WIDTH = 50;
    public static final int DEFAULT_MAP_HEIGHT = 50;

    private JButton simulateButton;
    private JButton clearPathButton;
    private JSpinner tickDelaySpinner;

    // Zoom control buttons
    private JButton zoomInButton;
    private JButton zoomOutButton;
    private JButton resetZoomButton;
    private JButton centerViewButton;

    @Getter
    private JCheckBox showGridCheckbox;

    @Getter
    private JCheckBox showFlagsCheckbox;

    @Getter
    private JLabel infoLabel;

    @Getter
    private int[][] collisionData;

    @Getter
    @Setter
    private Point playerPosition;

    @Getter
    private List<SimNpc> npcs;

    @Inject
    private SimulationEngine simulationEngine;

    @Inject
    private TilePanel tilePanel;

    public void init() {
        initializeData();
        initializeUI();
        loadCollisionDataFromJson("collision_dump.json");
    }

    private void initializeData() {
        collisionData = new int[DEFAULT_MAP_HEIGHT][DEFAULT_MAP_WIDTH];
        playerPosition = new Point(25, 25);
        npcs = new CopyOnWriteArrayList<>();

        // Add some sample NPCs
//        npcs.add(new SimNpc(new Point(20, 20), Color.RED, "Guard"));
//        npcs.add(new SimNpc(new Point(30, 30), Color.BLUE, "Merchant"));
//        npcs.add(new SimNpc(new Point(15, 35), Color.GREEN, "Goblin"));
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
        panel.add(new JLabel("Zoom Controls:"));
        JPanel zoomPanel = createZoomControlPanel();
        panel.add(zoomPanel);

        panel.add(Box.createVerticalStrut(10));

        // Display options
        panel.add(new JLabel("Display Options:"));
        showGridCheckbox = new JCheckBox("Show Grid", true);
        showGridCheckbox.addActionListener(e -> tilePanel.repaint());
        panel.add(showGridCheckbox);

        showFlagsCheckbox = new JCheckBox("Show Collision Flags", true);
        showFlagsCheckbox.addActionListener(e -> tilePanel.repaint());
        panel.add(showFlagsCheckbox);

        panel.add(Box.createVerticalStrut(20));

        // Simulation controls
        panel.add(new JLabel("Simulation:"));

        JPanel tickPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        tickPanel.add(new JLabel("Tick Delay (ms):"));
        tickDelaySpinner = new JSpinner(new SpinnerNumberModel(600, 100, 2000, 100));
        tickPanel.add(tickDelaySpinner);
        panel.add(tickPanel);

        simulateButton = new JButton("Start Simulation");
        simulateButton.addActionListener(e -> toggleSimulation(simulationEngine));
        panel.add(simulateButton);

        clearPathButton = new JButton("Clear Paths");
        clearPathButton.addActionListener(e -> {
            tilePanel.clearPaths();
            tilePanel.repaint();
        });
        panel.add(clearPathButton);

        panel.add(Box.createVerticalStrut(20));

        // Collision flag legend
        panel.add(new JLabel("Collision Types:"));
        panel.add(createLegend());

        panel.add(Box.createVerticalStrut(20));

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
        instructions.setBackground(panel.getBackground());
        panel.add(instructions);

        return panel;
    }

    private JPanel createZoomControlPanel() {
        JPanel zoomPanel = new JPanel();
        zoomPanel.setLayout(new GridLayout(2, 2, 5, 5));

        zoomInButton = new JButton("Zoom In");
        zoomInButton.addActionListener(e -> tilePanel.zoomIn());
        zoomPanel.add(zoomInButton);

        zoomOutButton = new JButton("Zoom Out");
        zoomOutButton.addActionListener(e -> tilePanel.zoomOut());
        zoomPanel.add(zoomOutButton);

        resetZoomButton = new JButton("Reset Zoom");
        resetZoomButton.addActionListener(e -> tilePanel.resetZoom());
        zoomPanel.add(resetZoomButton);

        centerViewButton = new JButton("Center View");
        centerViewButton.addActionListener(e -> tilePanel.centerView());
        zoomPanel.add(centerViewButton);

        // Add keyboard shortcuts
        addKeyboardShortcuts();

        return zoomPanel;
    }

    private void addKeyboardShortcuts() {
        // Add keyboard shortcuts for zoom controls
        InputMap inputMap = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actionMap = getRootPane().getActionMap();

        // Zoom in: Ctrl + Plus or Ctrl + Equals
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "zoomIn");
        actionMap.put("zoomIn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tilePanel.zoomIn();
            }
        });

        // Zoom out: Ctrl + Minus
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "zoomOut");
        actionMap.put("zoomOut", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tilePanel.zoomOut();
            }
        });

        // Reset zoom: Ctrl + 0
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_0, InputEvent.CTRL_DOWN_MASK), "resetZoom");
        actionMap.put("resetZoom", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tilePanel.resetZoom();
            }
        });

        // Center view: Ctrl + Shift + C
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK), "centerView");
        actionMap.put("centerView", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                tilePanel.centerView();
            }
        });
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

            // Determine bounds (you could also just use DEFAULT_MAP_WIDTH/HEIGHT)
            int minX = tiles.stream().mapToInt(TileCollisionDump::getSceneX).min().orElse(0);
            int minY = tiles.stream().mapToInt(TileCollisionDump::getSceneY).min().orElse(0);
            int maxX = tiles.stream().mapToInt(TileCollisionDump::getSceneX).max().orElse(DEFAULT_MAP_WIDTH);
            int maxY = tiles.stream().mapToInt(TileCollisionDump::getSceneY).max().orElse(DEFAULT_MAP_HEIGHT);

            int width = maxX - minX + 1;
            int height = maxY - minY + 1;

            collisionData = new int[height][width];
            playerPosition.setLocation(tiles.get(0).getSceneX(), tiles.get(0).getSceneY());

            for (TileCollisionDump tile : tiles) {
                int localX = tile.getSceneX();
                int localY = tile.getSceneY();
                if (localX >= 0 && localX < width && localY >= 0 && localY < height) {
                    collisionData[localY][localX] = tile.getRawFlags();
                }
            }

            simulationEngine.setCollisionData(collisionData);
            log.info("Loaded collision data from JSON (" + tiles.size() + " tiles)");
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
            int delay = (int) tickDelaySpinner.getValue();
            engine.start(delay);
            simulateButton.setText("Stop Simulation");
        }
    }
}