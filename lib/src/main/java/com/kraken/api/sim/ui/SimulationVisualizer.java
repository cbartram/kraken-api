package com.kraken.api.sim.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.sim.SimulationEngine;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
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

    // Modern color scheme
    private static final Color BACKGROUND_COLOR = new Color(45, 45, 50);
    private static final Color PANEL_COLOR = new Color(60, 60, 65);
    private static final Color PRIMARY_BUTTON_COLOR = new Color(70, 130, 180);
    private static final Color SECONDARY_BUTTON_COLOR = new Color(105, 105, 115);
    private static final Color DANGER_BUTTON_COLOR = new Color(220, 53, 69);
    private static final Color SUCCESS_BUTTON_COLOR = new Color(40, 167, 69);
    private static final Color TEXT_COLOR = new Color(245, 245, 245);
    private static final Color ACCENT_COLOR = new Color(108, 117, 125);

    private JButton simulateButton;

    @Getter
    private JCheckBox showGridCheckbox;

    @Getter
    private JCheckBox showFlagsCheckbox;

    private final SimulationEngine engine;
    private final TilePanel tilePanel;
    private final SimulationUIState state;

    @Inject
    public SimulationVisualizer(SimulationEngine engine, TilePanel tilePanel, SimulationUIState state) {
        this.engine = engine;
        this.tilePanel = tilePanel;
        this.state = state;

        setupModernLookAndFeel();
        initializeWindow();
        setupLayout();
    }

    /**
     * Sets up modern look and feel for the application
     */
    private void setupModernLookAndFeel() {
        try {
            // Set system look and feel as base
            UIManager.setLookAndFeel(UIManager.getLookAndFeel());

            // Override specific UI components for modern appearance
            UIManager.put("Panel.background", PANEL_COLOR);
            UIManager.put("Button.background", SECONDARY_BUTTON_COLOR);
            UIManager.put("Button.foreground", TEXT_COLOR);
            UIManager.put("CheckBox.background", PANEL_COLOR);
            UIManager.put("CheckBox.foreground", TEXT_COLOR);
            UIManager.put("Label.foreground", TEXT_COLOR);
            UIManager.put("TextArea.background", PANEL_COLOR);
            UIManager.put("TextArea.foreground", TEXT_COLOR);
            UIManager.put("TextArea.border", BorderFactory.createEmptyBorder());
        } catch (Exception e) {
            log.warn("Failed to set look and feel: {}", e.getMessage());
        }
    }

    /**
     * Initializes the main window with title and icon
     */
    private void initializeWindow() {
        setTitle("OSRS Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set custom icon (you can replace this path with your own icon)
        setIconImage(loadWindowIcon());

        // Set dark background
        getContentPane().setBackground(BACKGROUND_COLOR);
    }

    /**
     * Loads the window icon. Override this method to use a different icon.
     * @return Image for the window icon, or null if not found
     */
    protected Image loadWindowIcon() {
        try {
            // Try to load a custom icon from resources
            URL iconUrl = getClass().getResource("/icons/kraken-icon.png");
            if (iconUrl != null) {
                return new ImageIcon(iconUrl).getImage();
            }

            // Fallback: create a simple programmatic icon
            return createDefaultIcon();
        } catch (Exception e) {
            log.warn("Failed to load window icon: {}", e.getMessage());
            return createDefaultIcon();
        }
    }

    /**
     * Creates a simple default icon programmatically
     * @return A simple default icon
     */
    private Image createDefaultIcon() {
        int size = 32;
        BufferedImage icon = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = icon.createGraphics();

        // Enable antialiasing
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Draw a simple geometric icon
        g2d.setColor(PRIMARY_BUTTON_COLOR);
        g2d.fillRoundRect(4, 4, size-8, size-8, 8, 8);

        g2d.setColor(Color.WHITE);
        g2d.fillRoundRect(8, 8, size-16, size-16, 4, 4);

        g2d.setColor(PRIMARY_BUTTON_COLOR);
        g2d.fillRoundRect(12, 12, size-24, size-24, 2, 2);

        g2d.dispose();
        return icon;
    }

    /**
     * Sets up the main layout of the application
     */
    private void setupLayout() {
        setLayout(new BorderLayout());

        // Main tile panel
        tilePanel.setPreferredSize(new Dimension(800, 600));
        add(tilePanel, BorderLayout.CENTER);

        // Control panel
        JPanel controlPanel = createControlPanel();
        add(controlPanel, BorderLayout.EAST);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Creates a modern styled button
     * @param text Button text
     * @param buttonType Type of button (primary, secondary, danger, success)
     * @return Styled JButton
     */
    private JButton createModernButton(String text, String buttonType) {
        JButton button = new JButton(text);

        // Set colors based on button type
        Color bgColor = SECONDARY_BUTTON_COLOR;
        switch (buttonType.toLowerCase()) {
            case "primary":
                bgColor = PRIMARY_BUTTON_COLOR;
                break;
            case "danger":
                bgColor = DANGER_BUTTON_COLOR;
                break;
            case "success":
                bgColor = SUCCESS_BUTTON_COLOR;
                break;
            default:
                bgColor = SECONDARY_BUTTON_COLOR;
        }

        button.setBackground(bgColor);
        button.setForeground(TEXT_COLOR);
        button.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setOpaque(true);

        // Rounded corners
        Border roundedBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        );
        button.setBorder(roundedBorder);

        // Hover effects
        Color finalBgColor = bgColor;
        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(finalBgColor.brighter());
                button.setCursor(new Cursor(Cursor.HAND_CURSOR));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(finalBgColor);
                button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                button.setBackground(finalBgColor.darker());
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                button.setBackground(finalBgColor.brighter());
            }
        });

        return button;
    }

    /**
     * Creates a modern styled checkbox
     * @param text Checkbox text
     * @param selected Initial selection state
     * @return Styled JCheckBox
     */
    private JCheckBox createModernCheckBox(String text, boolean selected) {
        JCheckBox checkbox = new JCheckBox(text, selected);
        checkbox.setBackground(PANEL_COLOR);
        checkbox.setForeground(TEXT_COLOR);
        checkbox.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        checkbox.setFocusPainted(false);
        return checkbox;
    }

    /**
     * Creates the control panel to hold the various grid, tile, simulation, and tick controls.
     * @return JPanel panel to hold the control UI elements.
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 1, 0, 0, ACCENT_COLOR),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)
        ));

        // Title
        JLabel titleLabel = new JLabel("Control Panel");
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        titleLabel.setForeground(TEXT_COLOR);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(titleLabel);
        panel.add(Box.createVerticalStrut(20));

        // Zoom controls
        JLabel zoomLabel = createSectionLabel("Zoom Controls");
        panel.add(zoomLabel);

        JPanel zoomPanel = createZoomControlPanel();
        zoomPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(zoomPanel);

        panel.add(Box.createVerticalStrut(20));

        // Display options
        JLabel displayLabel = createSectionLabel("Display Options");
        panel.add(displayLabel);

        showGridCheckbox = createModernCheckBox("Show Grid", true);
        showGridCheckbox.addActionListener(e -> {
            state.setShowGrid(showGridCheckbox.isSelected());
            tilePanel.repaint();
        });
        showGridCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(showGridCheckbox);

        showFlagsCheckbox = createModernCheckBox("Show Collision Flags", true);
        showFlagsCheckbox.addActionListener(e -> {
            state.setShowFlags(showFlagsCheckbox.isSelected());
            tilePanel.repaint();
        });
        showFlagsCheckbox.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(showFlagsCheckbox);

        panel.add(Box.createVerticalStrut(20));

        // Simulation controls
        JLabel simLabel = createSectionLabel("Simulation Controls");
        panel.add(simLabel);

        JPanel simPanel = createSimulationControlPanel();
        simPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(simPanel);

        panel.add(Box.createVerticalStrut(20));

        // Collision legend
        JLabel legendLabel = createSectionLabel("Collision Types");
        panel.add(legendLabel);

        JPanel legend = createLegend();
        legend.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(legend);

        panel.add(Box.createVerticalStrut(20));

        // Instructions
        JTextArea instructions = createInstructionsArea();
        panel.add(instructions);

        return panel;
    }

    /**
     * Creates a styled section label
     * @param text Label text
     * @return Styled JLabel
     */
    private JLabel createSectionLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));
        label.setForeground(TEXT_COLOR);
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        return label;
    }

    /**
     * Creates a subpanel for controlling the zoom on the tiles.
     * @return JPanel A subpanel for controlling the zoom
     */
    private JPanel createZoomControlPanel() {
        JPanel zoomPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        zoomPanel.setBackground(PANEL_COLOR);

        JButton zoomInButton = createModernButton("Zoom In", "primary");
        zoomInButton.addActionListener(e -> tilePanel.zoomIn());
        zoomPanel.add(zoomInButton);

        JButton zoomOutButton = createModernButton("Zoom Out", "secondary");
        zoomOutButton.addActionListener(e -> tilePanel.zoomOut());
        zoomPanel.add(zoomOutButton);

        JButton resetZoomButton = createModernButton("Reset Zoom", "secondary");
        resetZoomButton.addActionListener(e -> tilePanel.resetZoom());
        zoomPanel.add(resetZoomButton);

        JButton centerViewButton = createModernButton("Center View", "secondary");
        centerViewButton.addActionListener(e -> tilePanel.centerView());
        zoomPanel.add(centerViewButton);

        return zoomPanel;
    }

    /**
     * Creates the simulation control panel with modern styled buttons
     * @return JPanel containing simulation controls
     */
    private JPanel createSimulationControlPanel() {
        JPanel simPanel = new JPanel(new GridLayout(2, 2, 8, 8));
        simPanel.setBackground(PANEL_COLOR);

        simulateButton = createModernButton("Start Simulation", "success");
        simulateButton.addActionListener(e -> toggleSimulation());
        simPanel.add(simulateButton);

        JButton clearPathButton = createModernButton("Clear Paths", "danger");
        clearPathButton.addActionListener(e -> {
            engine.reset();
            tilePanel.repaint();
        });
        simPanel.add(clearPathButton);

        JButton forwardTick = createModernButton("Next Tick →", "primary");
        forwardTick.addActionListener(e -> engine.tick());
        simPanel.add(forwardTick);

        JButton backwardTick = createModernButton("← Prev Tick", "primary");
        backwardTick.addActionListener(e -> engine.prevTick());
        simPanel.add(backwardTick);

        return simPanel;
    }

    /**
     * Creates the legend for understanding which UI elements represent which in-game structures
     * @return JPanel a subpanel for the legend.
     */
    private JPanel createLegend() {
        JPanel legend = new JPanel();
        legend.setLayout(new GridLayout(0, 1, 0, 4));
        legend.setBackground(PANEL_COLOR);

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
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        item.setBackground(PANEL_COLOR);

        JLabel colorBox = new JLabel("●");
        colorBox.setForeground(color);
        colorBox.setFont(new Font("SansSerif", Font.BOLD, 14));
        item.add(colorBox);

        item.add(Box.createHorizontalStrut(8));

        JLabel textLabel = new JLabel(text);
        textLabel.setForeground(TEXT_COLOR);
        textLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        item.add(textLabel);

        return item;
    }

    /**
     * Creates a modern styled instructions area
     * @return JTextArea with instructions
     */
    private JTextArea createInstructionsArea() {
        JTextArea instructions = new JTextArea(
                "Controls:\n\n" +
                        "• Left Click: Set Player Target\n" +
                        "• Right Click: Add/Remove Wall\n" +
                        "• Middle Click + Drag: Pan View\n" +
                        "• Mouse Wheel: Zoom In/Out\n" +
                        "• 'P' Key: Update Player Position\n" +
                        "• Hover: View Tile Information"
        );

        instructions.setEditable(false);
        instructions.setOpaque(true);
        instructions.setBackground(BACKGROUND_COLOR);
        instructions.setForeground(TEXT_COLOR);
        instructions.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        instructions.setAlignmentX(Component.LEFT_ALIGNMENT);
        instructions.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        return instructions;
    }

    /**
     * Toggles the state of the simulation. When the simulation is running this will
     * turn the simulation off (pause it). When it is off it will start the simulation.
     */
    private void toggleSimulation() {
        if (engine.isRunning()) {
            engine.stop();
            simulateButton.setText("Start Simulation");
            // Update button color to success (green)
            simulateButton.setBackground(SUCCESS_BUTTON_COLOR);
        } else {
            engine.start();
            simulateButton.setText("Stop Simulation");
            // Update button color to danger (red)
            simulateButton.setBackground(DANGER_BUTTON_COLOR);
        }
    }
}