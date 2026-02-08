package com.kraken.api.plugins.packetmapper;

import lombok.extern.slf4j.Slf4j;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;

/**
 * UI Panel for controlling the packet mapping tool
 */
@Slf4j
public class PacketMappingPanel extends PluginPanel {

    private final PacketMappingTool mappingTool;
    private final PacketQueueMonitor queueMonitor;
    
    private JButton startMonitoringBtn;
    private JButton stopMonitoringBtn;
    private JButton exportAllBtn;
    private JButton clearMappingsBtn;
    private JTextArea mappingsTextArea;
    private JLabel statusLabel;
    private JComboBox<String> packetSelector;

    @Inject
    public PacketMappingPanel(PacketMappingTool mappingTool, PacketQueueMonitor queueMonitor) {
        this.mappingTool = mappingTool;
        this.queueMonitor = queueMonitor;
        
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(10, 10, 10, 10));
        
        add(createControlPanel(), BorderLayout.NORTH);
        add(createMappingsPanel(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);
    }

    /**
     * Creates the control panel with buttons
     */
    private JPanel createControlPanel() {
        JPanel panel = new JPanel(new GridLayout(0, 1, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Controls"));
        
        startMonitoringBtn = new JButton("Start Monitoring");
        startMonitoringBtn.addActionListener(e -> startMonitoring());
        
        stopMonitoringBtn = new JButton("Stop Monitoring");
        stopMonitoringBtn.setEnabled(false);
        stopMonitoringBtn.addActionListener(e -> stopMonitoring());
        
        exportAllBtn = new JButton("Export All Mappings");
        exportAllBtn.addActionListener(e -> exportAllMappings());
        
        JButton exportSelectedBtn = new JButton("Export Selected");
        exportSelectedBtn.addActionListener(e -> exportSelectedMapping());
        
        JButton copyToClipboardBtn = new JButton("Copy to Clipboard");
        copyToClipboardBtn.addActionListener(e -> copyToClipboard());
        
        clearMappingsBtn = new JButton("Clear Mappings");
        clearMappingsBtn.addActionListener(e -> clearMappings());
        
        panel.add(startMonitoringBtn);
        panel.add(stopMonitoringBtn);
        panel.add(exportAllBtn);
        panel.add(exportSelectedBtn);
        panel.add(copyToClipboardBtn);
        panel.add(clearMappingsBtn);
        
        return panel;
    }

    /**
     * Creates the panel displaying mappings
     */
    private JPanel createMappingsPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Packet Mappings"));
        
        // Packet selector dropdown
        packetSelector = new JComboBox<>();
        packetSelector.addActionListener(e -> updateMappingDisplay());
        panel.add(packetSelector, BorderLayout.NORTH);
        
        // Text area for displaying mapping
        mappingsTextArea = new JTextArea();
        mappingsTextArea.setEditable(false);
        mappingsTextArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        
        JScrollPane scrollPane = new JScrollPane(mappingsTextArea);
        scrollPane.setPreferredSize(new Dimension(400, 400));
        panel.add(scrollPane, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Creates the status panel
     */
    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Status"));
        
        statusLabel = new JLabel("Ready");
        panel.add(statusLabel, BorderLayout.CENTER);
        
        return panel;
    }

    /**
     * Starts packet monitoring
     */
    private void startMonitoring() {
        try {
            queueMonitor.startMonitoring();
            startMonitoringBtn.setEnabled(false);
            stopMonitoringBtn.setEnabled(true);
            statusLabel.setText("Monitoring active - perform actions in game");
            statusLabel.setForeground(Color.GREEN);
        } catch (Exception e) {
            log.error("Failed to start monitoring", e);
            JOptionPane.showMessageDialog(this, 
                "Failed to start monitoring: " + e.getMessage(),
                "Error", 
                JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Stops packet monitoring
     */
    private void stopMonitoring() {
        queueMonitor.stopMonitoring();
        startMonitoringBtn.setEnabled(true);
        stopMonitoringBtn.setEnabled(false);
        statusLabel.setText("Monitoring stopped");
        statusLabel.setForeground(Color.ORANGE);
        
        // Refresh the packet selector
        updatePacketSelector();
    }

    /**
     * Updates the packet selector dropdown
     */
    private void updatePacketSelector() {
        packetSelector.removeAllItems();
        packetSelector.addItem("-- Select a packet --");
        
        for (String packetName : mappingTool.getMappings().keySet()) {
            packetSelector.addItem(packetName);
        }
        
        statusLabel.setText("Found " + mappingTool.getMappings().size() + " unique packets");
    }

    /**
     * Updates the mapping display based on selected packet
     */
    private void updateMappingDisplay() {
        String selectedPacket = (String) packetSelector.getSelectedItem();
        
        if (selectedPacket == null || selectedPacket.startsWith("--")) {
            mappingsTextArea.setText("");
            return;
        }
        
        String mapping = mappingTool.exportMapping(selectedPacket);
        mappingsTextArea.setText(mapping);
    }

    /**
     * Exports all mappings to a file
     */
    private void exportAllMappings() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("PacketMappings.java"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try (FileWriter writer = new FileWriter(file)) {
                String allMappings = mappingTool.exportMappings();
                writer.write(allMappings);
                
                JOptionPane.showMessageDialog(this,
                    "Exported " + mappingTool.getMappings().size() + " mappings to " + file.getName(),
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
                    
                statusLabel.setText("Exported to: " + file.getAbsolutePath());
                
            } catch (Exception e) {
                log.error("Failed to export mappings", e);
                JOptionPane.showMessageDialog(this,
                    "Failed to export mappings: " + e.getMessage(),
                    "Export Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Exports the selected mapping to a file
     */
    private void exportSelectedMapping() {
        String selectedPacket = (String) packetSelector.getSelectedItem();
        
        if (selectedPacket == null || selectedPacket.startsWith("--")) {
            JOptionPane.showMessageDialog(this,
                "Please select a packet first",
                "No Selection",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File(selectedPacket + "_Mapping.java"));
        
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            
            try (FileWriter writer = new FileWriter(file)) {
                String mapping = mappingTool.exportMapping(selectedPacket);
                writer.write(mapping);
                
                JOptionPane.showMessageDialog(this,
                    "Exported mapping for " + selectedPacket,
                    "Export Successful",
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (Exception e) {
                log.error("Failed to export mapping", e);
                JOptionPane.showMessageDialog(this,
                    "Failed to export mapping: " + e.getMessage(),
                    "Export Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * Copies current mapping to clipboard
     */
    private void copyToClipboard() {
        String text = mappingsTextArea.getText();
        
        if (text.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "No mapping to copy",
                "Nothing to Copy",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        StringSelection selection = new StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
        
        statusLabel.setText("Copied to clipboard");
    }

    /**
     * Clears all mappings
     */
    private void clearMappings() {
        int result = JOptionPane.showConfirmDialog(this,
            "Are you sure you want to clear all " + mappingTool.getMappings().size() + " mappings?",
            "Confirm Clear",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
            
        if (result == JOptionPane.YES_OPTION) {
            mappingTool.clearMappings();
            queueMonitor.clearHistory();
            updatePacketSelector();
            mappingsTextArea.setText("");
            statusLabel.setText("All mappings cleared");
        }
    }
}
