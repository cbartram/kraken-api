package com.kraken.api.sim.ui;

import com.google.inject.Inject;
import com.kraken.api.interaction.tile.MovementFlag;
import com.kraken.api.sim.SimNpc;
import com.kraken.api.sim.SimulationEngine;
import lombok.Getter;
import lombok.Setter;
import net.runelite.api.CollisionDataFlag;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Main visualizer for OSRS tile collision data and movement simulation
 */
public class SimulationVisualizer extends JFrame {
    public static final int TILE_SIZE = 20;
    public static final int DEFAULT_MAP_WIDTH = 50;
    public static final int DEFAULT_MAP_HEIGHT = 50;

    private JButton simulateButton;
    private JButton clearPathButton;
    private JSpinner tickDelaySpinner;

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
        generateSampleCollisionData();
    }

    private void initializeData() {
        collisionData = new int[DEFAULT_MAP_HEIGHT][DEFAULT_MAP_WIDTH];
        playerPosition = new Point(25, 25);
        npcs = new CopyOnWriteArrayList<>();

        // Add some sample NPCs
        npcs.add(new SimNpc(new Point(20, 20), Color.RED, "Guard"));
        npcs.add(new SimNpc(new Point(30, 30), Color.BLUE, "Merchant"));
        npcs.add(new SimNpc(new Point(15, 35), Color.GREEN, "Goblin"));
    }

    private void initializeUI() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Main tile panel
        JScrollPane scrollPane = new JScrollPane(tilePanel);
        scrollPane.setPreferredSize(new Dimension(800, 600));
        add(scrollPane, BorderLayout.CENTER);

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
                        "• Middle Click: Add NPC\n" +
                        "• Hover: View tile info"
        );
        instructions.setEditable(false);
        instructions.setBackground(panel.getBackground());
        panel.add(instructions);

        return panel;
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

    private void generateSampleCollisionData() {
        // Create some walls
        for (int x = 10; x < 40; x++) {
            collisionData[10][x] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
            collisionData[40][x] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        }

        for (int y = 10; y < 40; y++) {
            collisionData[y][10] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
            collisionData[y][40] = CollisionDataFlag.BLOCK_MOVEMENT_FULL;
        }

        // Add some objects
        collisionData[25][20] = CollisionDataFlag.BLOCK_MOVEMENT_OBJECT;
        collisionData[25][30] = CollisionDataFlag.BLOCK_MOVEMENT_OBJECT;

        // Add directional blocks
        collisionData[20][25] = CollisionDataFlag.BLOCK_MOVEMENT_NORTH;
        collisionData[30][25] = CollisionDataFlag.BLOCK_MOVEMENT_SOUTH;

        simulationEngine.setCollisionData(collisionData);
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