package com.kraken.api.sim.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.sim.SimulationEngine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

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

    @Inject
    private SimulationEngine engine;

    @Inject
    private TilePanel tilePanel;

    /**
     * Initializes the class. There are some weird Guice injection cyclic dependency issues
     * right now where only field level injection works thus this method is needed.
     */
    public void init() {
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

    /**
     * Creates the control panel to hold the various grid, tile, simulation, and tick controls.
     * @return JPanel panel to hold the control UI elements.
     */
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


        JPanel simPanel = new JPanel(new GridLayout(2, 2, 5, 5));

        simulateButton = new JButton("Start Simulation");
        simulateButton.addActionListener(e -> toggleSimulation());
        simulateButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        simPanel.add(simulateButton);
        JButton clearPathButton = new JButton("Clear Paths");
        clearPathButton.addActionListener(e -> {
            tilePanel.clearPaths();
            tilePanel.repaint();
        });
        clearPathButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        simPanel.add(clearPathButton);

        JButton forwardTick = new JButton("Next Tick >>");
        forwardTick.addActionListener(e -> engine.tick());
        forwardTick.setAlignmentX(Component.LEFT_ALIGNMENT);
        simPanel.add(forwardTick);

        JButton backwardTick = new JButton("<< Prev Tick");
        backwardTick.addActionListener(e -> engine.prevTick());
        backwardTick.setAlignmentX(Component.LEFT_ALIGNMENT);
        simPanel.add(backwardTick);

        simPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(simPanel);

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
                        "• Left Click: Set Player Target\n" +
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

    /**
     * Creates a subpanel for controlling the zoom on the tiles.
     * @return JPanel A subpanel for controlling the zoom
     */
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

    /**
     * Creates the legend for understanding which UI elements represent which in-game structures
     * @return JPanel a subpanel for the legend.
     */
    private JPanel createLegend() {
        JPanel legend = new JPanel();
        legend.setLayout(new GridLayout(0, 1));

        legend.add(createLegendItem(Color.DARK_GRAY, "Full Block"));
        legend.add(createLegendItem(Color.GRAY, "Object Block"));
        legend.add(createLegendItem(new Color(139, 69, 19), "Floor Decoration"));
        legend.add(createLegendItem(Color.ORANGE, "Directional Block"));

        return legend;
    }

    /**
     * Helper method to create a legend item.
     * @param color Color for the item
     * @param text String the text for the item
     * @return Subpanel for the legend item.
     */
    private JPanel createLegendItem(Color color, String text) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JLabel colorBox = new JLabel("■");
        colorBox.setForeground(color);
        item.add(colorBox);
        item.add(new JLabel(text));
        return item;
    }

    /**
     * Toggles the state of the simulation. When the simulation is running this will
     * turn the simulation off (pause it). When it is off it will start the simulation.
     */
    private void toggleSimulation() {
        if (engine.isRunning()) {
            engine.stop();
            simulateButton.setText("Start Simulation");
        } else {
            engine.start();
            simulateButton.setText("Stop Simulation");
        }
    }
}