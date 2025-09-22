package com.kraken.api.sim.ui;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.sim.engine.SimulationEngine;
import com.kraken.api.sim.model.AttackStyle;
import com.kraken.api.sim.model.SimNpc;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
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

    private static final Color BACKGROUND_COLOR = new Color(45, 45, 50);
    private static final Color PANEL_COLOR = new Color(60, 60, 65);
    private static final Color PRIMARY_BUTTON_COLOR = new Color(70, 130, 180);
    private static final Color SECONDARY_BUTTON_COLOR = new Color(105, 105, 115);
    private static final Color DANGER_BUTTON_COLOR = new Color(220, 53, 69);
    private static final Color SUCCESS_BUTTON_COLOR = new Color(40, 167, 69);
    private static final Color TEXT_COLOR = new Color(245, 245, 245);
    private static final Color ACCENT_COLOR = new Color(108, 117, 125);

    private JCheckBox npcPlacementMode;
    private JTextField npcNameField;
    private JButton npcColorButton;
    private Color selectedNpcColor = Color.BLUE;
    private JCheckBox canPathfindCheckbox;
    private JComboBox<AttackStyle> npcAttackStyleCombo;
    private JSpinner attackRangeSpinner;
    private DefaultListModel<SimNpc> npcListModel;
    private JList<SimNpc> npcList;
    private JButton simulateButton;

    @Getter
    private JSpinner sizeSpinner;

    @Getter
    private JCheckBox showGridCheckbox;

    @Getter
    private JCheckBox showFlagsCheckbox;

    @Getter
    private JCheckBox showTooltip;

    @Getter
    private JCheckBox playerRunning;

    private final SimulationEngine engine;
    private final TilePanel tilePanel;
    private final SimulationUIState state;

    @Inject
    public SimulationVisualizer(SimulationEngine engine, TilePanel tilePanel, SimulationUIState state) {
        this.engine = engine;
        this.tilePanel = tilePanel;
        this.state = state;

        setupModernLookAndFeel();
        setTitle("OSRS Simulator");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Set custom icon (you can replace this path with your own icon)
        setIconImage(loadWindowIcon());

        // Set dark background
        getContentPane().setBackground(BACKGROUND_COLOR);
        setupLayout();
        initializeTilePanelConnection();
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
        Color bgColor;
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

        Border roundedBorder = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(bgColor.darker(), 1),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)
        );
        button.setBorder(roundedBorder);

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

        showTooltip = createModernCheckBox("Show Tooltip", false);
        showTooltip.addActionListener(e -> {
            state.setShowTooltip(showTooltip.isSelected());
            tilePanel.repaint();
        });
        showTooltip.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(showTooltip);

        panel.add(Box.createVerticalStrut(20));

        // Simulation controls
        JLabel simLabel = createSectionLabel("Simulation Controls");
        panel.add(simLabel);

        JPanel simPanel = createSimulationControlPanel();
        simPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(simPanel);

        playerRunning = createModernCheckBox("Player Running", false);
        playerRunning.addActionListener(e -> {
            state.setPlayerRunning(playerRunning.isSelected());
            engine.setPlayerRunning(playerRunning.isSelected());
            tilePanel.repaint();
        });
        playerRunning.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(playerRunning);

        panel.add(Box.createVerticalStrut(20));

        // NPC Management Section
        JLabel npcLabel = createSectionLabel("NPC Management");
        panel.add(npcLabel);

        // NPC Placement Mode
        npcPlacementMode = createModernCheckBox("NPC Placement Mode", false);
        npcPlacementMode.addActionListener(e -> updateNpcPlacementMode());
        npcPlacementMode.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(npcPlacementMode);

        panel.add(Box.createVerticalStrut(10));

        // NPC Properties Panel
        JPanel npcPropsPanel = createNpcPropertiesPanel();
        npcPropsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(npcPropsPanel);

        panel.add(Box.createVerticalStrut(10));

        // NPC Action Buttons
        JPanel npcActionsPanel = createNpcActionPanel();
        npcActionsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(npcActionsPanel);

        panel.add(Box.createVerticalStrut(10));

        // NPC List
        JPanel npcListPanel = createNpcListPanel();
        npcListPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(npcListPanel);

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

    /**
     * Creates the NPC properties configuration panel
     */
    private JPanel createNpcPropertiesPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new GridBagLayout());
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR),
                "New NPC Properties",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                TEXT_COLOR
        ));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // Name field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(TEXT_COLOR);
        nameLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(nameLabel, gbc);

        gbc.gridx = 1;
        npcNameField = new JTextField("NPC", 8);
        npcNameField.setBackground(BACKGROUND_COLOR);
        npcNameField.setForeground(TEXT_COLOR);
        npcNameField.setCaretColor(TEXT_COLOR);
        panel.add(npcNameField, gbc);

        // Color button
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel colorLabel = new JLabel("Color:");
        colorLabel.setForeground(TEXT_COLOR);
        colorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(colorLabel, gbc);

        gbc.gridx = 1;
        npcColorButton = createColorButton();
        panel.add(npcColorButton, gbc);

        // Size field
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel sizeLabel = new JLabel("Size:");
        sizeLabel.setForeground(TEXT_COLOR);
        sizeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(sizeLabel, gbc);

        gbc.gridx = 1;
        sizeSpinner = new JSpinner(new SpinnerNumberModel(1, 1, 20, 1));
        styleSpinner(sizeSpinner);
        panel.add(sizeSpinner, gbc);

        // Attack Style
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel styleLabel = new JLabel("Attack Style:");
        styleLabel.setForeground(TEXT_COLOR);
        styleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(styleLabel, gbc);

        gbc.gridx = 1;
        npcAttackStyleCombo = new JComboBox<>(AttackStyle.values());
        npcAttackStyleCombo.setBackground(BACKGROUND_COLOR);
        npcAttackStyleCombo.setForeground(TEXT_COLOR);
        panel.add(npcAttackStyleCombo, gbc);

        // Attack Range
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel rangeLabel = new JLabel("Attack Range:");
        rangeLabel.setForeground(TEXT_COLOR);
        rangeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(rangeLabel, gbc);

        gbc.gridx = 1;
        attackRangeSpinner = new JSpinner(new SpinnerNumberModel(1, 0, 100, 1));
        styleSpinner(attackRangeSpinner);
        panel.add(attackRangeSpinner, gbc);

        // Pathfinding
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel pathfindingLabel = new JLabel("Smart Pathfinding:");
        pathfindingLabel.setForeground(TEXT_COLOR);
        pathfindingLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        panel.add(pathfindingLabel, gbc);

        gbc.gridx = 1;
        canPathfindCheckbox = new JCheckBox();
        panel.add(canPathfindCheckbox, gbc);

        return panel;
    }

    /**
     * Creates action buttons for NPC management
     */
    private JPanel createNpcActionPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 5, 5));
        panel.setBackground(PANEL_COLOR);

        JButton clearAllNpcs = createModernButton("Clear All", "danger");
        clearAllNpcs.addActionListener(e -> clearAllNpcs());
        panel.add(clearAllNpcs);

        JButton randomNpc = createModernButton("Random NPC", "primary");
        randomNpc.addActionListener(e -> addRandomNpc());
        panel.add(randomNpc);

        JButton editSelected = createModernButton("Edit Selected", "secondary");
        editSelected.addActionListener(e -> editSelectedNpc());
        panel.add(editSelected);

        return panel;
    }

    /**
     * Creates the NPC list display
     */
    private JPanel createNpcListPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(PANEL_COLOR);
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(ACCENT_COLOR),
                "Active NPCs",
                TitledBorder.LEFT,
                TitledBorder.TOP,
                new Font("Segoe UI", Font.BOLD, 12),
                TEXT_COLOR
        ));

        npcListModel = new DefaultListModel<>();
        npcList = new JList<>(npcListModel);
        npcList.setBackground(BACKGROUND_COLOR);
        npcList.setForeground(TEXT_COLOR);
        npcList.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        npcList.setSelectionBackground(PRIMARY_BUTTON_COLOR);
        npcList.setSelectionForeground(TEXT_COLOR);

        // Custom cell renderer to show NPC info nicely
        npcList.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index,
                                                          boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

                if (value instanceof SimNpc) {
                    SimNpc npc = (SimNpc) value;
                    setText(String.format("%s [%d,%d] %s",
                            npc.getName(),
                            npc.getPosition().x,
                            npc.getPosition().y,
                            npc.getAttackStyle().name())
                    );

                    // Show color indicator
                    if (!isSelected) {
                        setForeground(npc.getColor());
                    }
                }

                setBackground(isSelected ? PRIMARY_BUTTON_COLOR : BACKGROUND_COLOR);
                return this;
            }
        });

        JScrollPane scrollPane = new JScrollPane(npcList);
        scrollPane.setPreferredSize(new Dimension(200, 120));
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * Creates a color selection button
     */
    private JButton createColorButton() {
        JButton button = new JButton("    ");
        button.setBackground(selectedNpcColor);
        button.setPreferredSize(new Dimension(40, 25));
        button.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR));
        button.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(this, "Choose NPC Color", selectedNpcColor);
            if (newColor != null) {
                selectedNpcColor = newColor;
                button.setBackground(newColor);
            }
        });
        return button;
    }

    /**
     * Styles a spinner component
     */
    private void styleSpinner(JSpinner spinner) {
        spinner.getEditor().getComponent(0).setBackground(BACKGROUND_COLOR);
        spinner.getEditor().getComponent(0).setForeground(TEXT_COLOR);
    }

    /**
     * Updates the UI when NPC placement mode changes
     */
    private void updateNpcPlacementMode() {
        boolean placementMode = npcPlacementMode.isSelected();
        tilePanel.setNpcPlacementMode(placementMode);

        // Update cursor or visual feedback
        if (placementMode) {
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    /**
     * Clears all NPCs from the simulation
     */
    private void clearAllNpcs() {
        int result = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to remove all NPCs?",
                "Clear All NPCs",
                JOptionPane.YES_NO_OPTION);

        if (result == JOptionPane.YES_OPTION) {
            engine.reset();
            engine.getNpcs().clear();
            npcListModel.clear();
            repaint();
        }
    }

    /**
     * Adds a random NPC to the simulation
     */
    private void addRandomNpc() {
        Random random = new Random();
        int[][] data = engine.getCollisionData();

        // Find a random empty tile
        Point randomPos;
        int attempts = 0;
        do {
            randomPos = new Point(
                    random.nextInt(data[0].length),
                    random.nextInt(data.length)
            );
            attempts++;
        } while (data[randomPos.y][randomPos.x] != 0 && attempts < 100);

        if (attempts < 100) {
            Color randomColor = new Color(
                    random.nextInt(256),
                    random.nextInt(256),
                    random.nextInt(256)
            );

            SimNpc npc = new SimNpc(randomPos, randomColor, "Random NPC " + (npcListModel.size() + 1));
            addNpcToSimulation(npc);
        }
    }

    /**
     * Opens edit dialog for selected NPC
     */
    private void editSelectedNpc() {
        SimNpc selected = npcList.getSelectedValue();
        if (selected != null) {
            openNpcEditDialog(selected);
        } else {
            JOptionPane.showMessageDialog(this, "Please select an NPC to edit.");
        }
    }

    /**
     * Adds an NPC to the simulation and UI list
     */
    public void addNpcToSimulation(SimNpc npc) {
        engine.addNpc(npc);
        npcListModel.addElement(npc);
        repaint();
    }

    /**
     * Creates a new NPC with current property settings
     */
    public SimNpc createNpcFromCurrentSettings(Point position) {
        String name = npcNameField.getText().trim();
        if (name.isEmpty()) {
            name = "NPC " + (npcListModel.size() + 1);
        }

        SimNpc npc = new SimNpc(position, selectedNpcColor, name);
        npc.setSize((int) sizeSpinner.getValue());
        npc.setCanPathfind(canPathfindCheckbox.isSelected());
        npc.setAttackStyle((AttackStyle) npcAttackStyleCombo.getSelectedItem());
        npc.setAttackRange((Integer) attackRangeSpinner.getValue());
        return npc;
    }


    /**
     * Removes an NPC from the UI list
     */
    public void removeNpcFromList(SimNpc npc) {
        npcListModel.removeElement(npc);
    }

    /**
     * Selects an NPC in the UI list
     */
    public void selectNpcInList(SimNpc npc) {
        npcList.setSelectedValue(npc, true);

        // Optionally populate the property fields with selected NPC's values
        populateFieldsFromNpc(npc);
    }

    private void populateFieldsFromNpc(SimNpc npc) {
        npcNameField.setText(npc.getName());
        selectedNpcColor = npc.getColor();
        npcColorButton.setBackground(selectedNpcColor);
        npcAttackStyleCombo.setSelectedItem(npc.getAttackStyle());
        attackRangeSpinner.setValue(npc.getAttackRange());
    }
    /**
     * Opens detailed edit dialog for an NPC
     */
    public void openNpcEditDialog(SimNpc npc) {
        JDialog dialog = new JDialog(this, "Edit NPC: " + npc.getName(), true);
        dialog.setLayout(new BorderLayout());
        dialog.getContentPane().setBackground(PANEL_COLOR);

        // Create form panel
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBackground(PANEL_COLOR);
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.WEST;

        // Name field
        gbc.gridx = 0; gbc.gridy = 0;
        JLabel nameLabel = new JLabel("Name:");
        nameLabel.setForeground(TEXT_COLOR);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(nameLabel, gbc);

        gbc.gridx = 1;
        JTextField nameField = new JTextField(npc.getName(), 15);
        nameField.setBackground(BACKGROUND_COLOR);
        nameField.setForeground(TEXT_COLOR);
        nameField.setCaretColor(TEXT_COLOR);
        formPanel.add(nameField, gbc);

        // Position fields
        gbc.gridx = 0; gbc.gridy = 1;
        JLabel posLabel = new JLabel("Position:");
        posLabel.setForeground(TEXT_COLOR);
        posLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(posLabel, gbc);

        gbc.gridx = 1;
        JPanel posPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        posPanel.setBackground(PANEL_COLOR);

        JSpinner xSpinner = new JSpinner(new SpinnerNumberModel(npc.getPosition().x, 0,
                engine.getCollisionData()[0].length - 1, 1));
        styleSpinner(xSpinner);
        xSpinner.setPreferredSize(new Dimension(60, 25));

        JSpinner ySpinner = new JSpinner(new SpinnerNumberModel(npc.getPosition().y, 0,
                engine.getCollisionData().length - 1, 1));
        styleSpinner(ySpinner);
        ySpinner.setPreferredSize(new Dimension(60, 25));

        JLabel commaLabel = new JLabel(", ");
        commaLabel.setForeground(TEXT_COLOR);

        posPanel.add(new JLabel("("));
        posPanel.add(xSpinner);
        posPanel.add(commaLabel);
        posPanel.add(ySpinner);
        posPanel.add(new JLabel(")"));

        formPanel.add(posPanel, gbc);

        // Color field
        gbc.gridx = 0; gbc.gridy = 2;
        JLabel colorLabel = new JLabel("Color:");
        colorLabel.setForeground(TEXT_COLOR);
        colorLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(colorLabel, gbc);

        gbc.gridx = 1;
        Color currentColor = npc.getColor();
        JButton colorButton = new JButton("    ");
        colorButton.setBackground(currentColor);
        colorButton.setPreferredSize(new Dimension(50, 25));
        colorButton.setBorder(BorderFactory.createLineBorder(ACCENT_COLOR));
        colorButton.addActionListener(e -> {
            Color newColor = JColorChooser.showDialog(dialog, "Choose NPC Color", currentColor);
            if (newColor != null) {
                colorButton.setBackground(newColor);
            }
        });
        formPanel.add(colorButton, gbc);

        // Attack Style
        gbc.gridx = 0; gbc.gridy = 3;
        JLabel styleLabel = new JLabel("Attack Style:");
        styleLabel.setForeground(TEXT_COLOR);
        styleLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(styleLabel, gbc);

        gbc.gridx = 1;
        JComboBox<AttackStyle> styleCombo = new JComboBox<>(AttackStyle.values());
        styleCombo.setSelectedItem(npc.getAttackStyle());
        styleCombo.setBackground(BACKGROUND_COLOR);
        styleCombo.setForeground(TEXT_COLOR);
        formPanel.add(styleCombo, gbc);

        // Attack Range
        gbc.gridx = 0; gbc.gridy = 4;
        JLabel rangeLabel = new JLabel("Attack Range:");
        rangeLabel.setForeground(TEXT_COLOR);
        rangeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(rangeLabel, gbc);

        gbc.gridx = 1;
        JSpinner rangeSpinner = new JSpinner(new SpinnerNumberModel(npc.getAttackRange(), 0, 50, 1));
        styleSpinner(rangeSpinner);
        formPanel.add(rangeSpinner, gbc);

        // Size field
        gbc.gridx = 0; gbc.gridy = 5;
        JLabel sizeLabel = new JLabel("Size:");
        sizeLabel.setForeground(TEXT_COLOR);
        sizeLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(sizeLabel, gbc);

        gbc.gridx = 1;
        JSpinner sizeSpinner = new JSpinner(new SpinnerNumberModel(npc.getSize(), 1, 5, 1));
        styleSpinner(sizeSpinner);
        formPanel.add(sizeSpinner, gbc);

        // Size field
        gbc.gridx = 0; gbc.gridy = 6;
        JLabel pathfindingLabel = new JLabel("Smart Pathfinding:");
        pathfindingLabel.setForeground(TEXT_COLOR);
        pathfindingLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        formPanel.add(pathfindingLabel, gbc);

        gbc.gridx = 1;
        JCheckBox pathfinding = new JCheckBox();
        pathfinding.setSelected(npc.isCanPathfind());
        formPanel.add(pathfinding, gbc);

        // Buttons panel
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.setBackground(PANEL_COLOR);

        JButton saveButton = createModernButton("Save Changes", "success");
        saveButton.addActionListener(e -> {
            // Apply changes to NPC
            String newName = nameField.getText().trim();
            if (!newName.isEmpty()) {
                npc.setName(newName);
            }

            Point newPosition = new Point((Integer) xSpinner.getValue(), (Integer) ySpinner.getValue());
            if (engine.getCollisionData()[newPosition.y][newPosition.x] == 0) {
                npc.setPosition(newPosition);
            }

            npc.setColor(colorButton.getBackground());
            npc.setAttackStyle((AttackStyle) styleCombo.getSelectedItem());
            npc.setAttackRange((Integer) rangeSpinner.getValue());
            npc.setSize((Integer) sizeSpinner.getValue());
            npc.setCanPathfind(pathfinding.isSelected());

            // Refresh UI
            npcList.repaint();
            tilePanel.repaint();

            dialog.dispose();
        });
        buttonPanel.add(saveButton);

        JButton cancelButton = createModernButton("Cancel", "secondary");
        cancelButton.addActionListener(e -> dialog.dispose());
        buttonPanel.add(cancelButton);

        JButton deleteButton = createModernButton("Delete NPC", "danger");
        deleteButton.addActionListener(e -> {
            int result = JOptionPane.showConfirmDialog(dialog,
                    "Are you sure you want to delete this NPC?",
                    "Delete NPC",
                    JOptionPane.YES_NO_OPTION);

            if (result == JOptionPane.YES_OPTION) {
                engine.removeNpc(npc);
                removeNpcFromList(npc);
                tilePanel.repaint();
                dialog.dispose();
            }
        });
        buttonPanel.add(deleteButton);

        // Add components to dialog
        dialog.add(formPanel, BorderLayout.CENTER);
        dialog.add(buttonPanel, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    /**
     * Updates the instructions area to include NPC controls
     */
    private JTextArea createInstructionsArea() {
        JTextArea instructions = new JTextArea(
                "Controls:\n\n" +
                        "• Left Click: Set Player Target\n" +
                        "• Right Click: Add/Remove Wall\n" +
                        "• Middle Click + Drag: Pan View\n" +
                        "• Mouse Wheel: Zoom In/Out\n" +
                        "• 'P' Key: Update Player Position\n" +
                        "• Hover: View Tile Information\n\n" +
                        "NPC Controls:\n" +
                        "• Shift + Left Click: Place NPC\n" +
                        "• Ctrl + Right Click: Remove NPC\n" +
                        "• Alt + Left Click: Select NPC\n" +
                        "• Double Click NPC: Edit Properties"
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
     * Initialize the connection between TilePanel and SimulationVisualizer
     * Call this in your constructor after creating the tilePanel
     */
    private void initializeTilePanelConnection() {
        tilePanel.setVisualizer(this);
    }
}