package com.kraken.api.example.overlay;

import com.google.inject.Inject;
import com.kraken.api.example.ExampleConfig;
import com.kraken.api.example.TestResultManager;
import com.kraken.api.interaction.inventory.InventoryItem;
import com.kraken.api.interaction.inventory.InventoryService;
import com.kraken.api.interaction.player.PlayerService;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class InfoPanelOverlay extends OverlayPanel {

    private final Client client;
    private final ExampleConfig config;
    private final InventoryService inventoryService;
    private final PlayerService playerService;
    private final TestResultManager testResultManager;

    @Inject
    public InfoPanelOverlay(Client client, ExampleConfig config,
                            InventoryService inventoryService, PlayerService playerService, TestResultManager testResultManager) {
        this.client = client;
        this.config = config;
        this.inventoryService = inventoryService;
        this.playerService = playerService;
        this.testResultManager = testResultManager;

        // Set overlay position based on config
        switch (config.overlayPosition().toUpperCase()) {
            case "TOP_RIGHT":
                setPosition(OverlayPosition.TOP_RIGHT);
                break;
            case "BOTTOM_LEFT":
                setPosition(OverlayPosition.BOTTOM_LEFT);
                break;
            case "BOTTOM_RIGHT":
                setPosition(OverlayPosition.BOTTOM_RIGHT);
                break;
            default:
                setPosition(OverlayPosition.TOP_LEFT);
                break;
        }
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.start()) {
            return null;
        }

        try {
            panelComponent.getChildren().clear();

            // Add title
            panelComponent.getChildren().add(TitleComponent.builder()
                    .text("Test Info")
                    .color(Color.CYAN)
                    .build());

            // Add test results if manager is available
            if (!testResultManager.getAllTestResults().isEmpty()) {
                addTestResults();
            }

            // Add player stats if enabled
            if (config.showPlayerStats() && config.enablePlayerTests()) {
                addPlayerStats();
            }

            // Add inventory info if enabled
            if (config.showInventoryOverlay() && config.enableInventoryTests()) {
                addInventoryInfo();
            }

            // Add debug info if enabled
            if (config.showDebugInfo()) {
                addDebugInfo();
            }

        } catch (Exception e) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Error:")
                    .right(e.getMessage())
                    .rightColor(Color.RED)
                    .build());
        }

        return super.render(graphics);
    }

    private void addTestResults() {
        try {
            // Overall status
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Test Status:")
                    .right(testResultManager.getOverallStatus())
                    .rightColor(getOverallStatusColor())
                    .build());

            // Test counts summary
            int passed = testResultManager.getPassedTestCount();
            int failed = testResultManager.getFailedTestCount();
            int running = testResultManager.getRunningTestCount();

            if (running > 0) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Running:")
                        .right(String.valueOf(running))
                        .rightColor(Color.YELLOW)
                        .build());
            }

            if (passed > 0 || failed > 0) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Results:")
                        .right(String.format("✓%d ✗%d", passed, failed))
                        .rightColor(Color.WHITE)
                        .build());
            }

            // Individual test results
            Map<String, TestResultManager.TestResult> results = testResultManager.getAllTestResults();

            for (TestResultManager.TestResult result : results.values()) {
                Color statusColor = getStatusColor(result.getStatus());
                String statusText = result.getStatus().getDisplayName();

                // Add execution time for completed tests
                if (result.getStatus() == TestResultManager.TestStatus.PASSED ||
                        result.getStatus() == TestResultManager.TestStatus.FAILED) {
                    statusText += String.format(" (%dms)", result.getExecutionTimeMs());
                }

                panelComponent.getChildren().add(LineComponent.builder()
                        .left(result.getTestName() + ":")
                        .right(statusText)
                        .rightColor(statusColor)
                        .build());

                // Show error message for failed tests if debug is enabled
                if (config.showDebugInfo() &&
                        result.getStatus() == TestResultManager.TestStatus.FAILED &&
                        result.getErrorMessage() != null) {

                    String errorMsg = result.getErrorMessage();
                    if (errorMsg.length() > 30) {
                        errorMsg = errorMsg.substring(0, 27) + "...";
                    }

                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("  Error:")
                            .right(errorMsg)
                            .rightColor(Color.RED)
                            .build());
                }
            }

        } catch (Exception e) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Test Results Error:")
                    .right(e.getMessage())
                    .rightColor(Color.RED)
                    .build());
        }
    }

    private Color getOverallStatusColor() {
        if (testResultManager.areAnyTestsRunning()) {
            return Color.YELLOW;
        }

        int failed = testResultManager.getFailedTestCount();
        int passed = testResultManager.getPassedTestCount();

        if (failed == 0 && passed > 0) {
            return Color.GREEN;
        } else if (failed > 0) {
            return Color.RED;
        } else {
            return Color.WHITE;
        }
    }

    private Color getStatusColor(TestResultManager.TestStatus status) {
        switch (status) {
            case PASSED:
                return Color.GREEN;
            case FAILED:
                return Color.RED;
            case RUNNING:
                return Color.YELLOW;
            case DISABLED:
                return Color.GRAY;
            case NOT_STARTED:
            default:
                return Color.WHITE;
        }
    }

    private void addPlayerStats() {
        try {
            // Health information
            double healthPercent = playerService.getHealthPercentage();
            Color healthColor = healthPercent < config.lowHealthThreshold() ? Color.RED :
                    healthPercent < 50 ? Color.ORANGE : Color.GREEN;

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Health:")
                    .right(String.format("%.1f%% (%d/%d)",
                            healthPercent,
                            playerService.getHealthRemaining(),
                            playerService.getMaxHealth()))
                    .rightColor(healthColor)
                    .build());

            // Special attack
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Special:")
                    .right(playerService.isSpecEnabled() ? "Enabled" : "Disabled")
                    .rightColor(playerService.isSpecEnabled() ? Color.GREEN : Color.RED)
                    .build());

            // Movement status
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Moving:")
                    .right(playerService.isMoving() ? "Yes" : "No")
                    .rightColor(playerService.isMoving() ? Color.GREEN : Color.WHITE)
                    .build());

            // Run status
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Run:")
                    .right(playerService.isRunEnabled() ? "Enabled" : "Disabled")
                    .rightColor(playerService.isRunEnabled() ? Color.GREEN : Color.RED)
                    .build());

            // Poisoned status
            if (playerService.isPoisoned()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Status:")
                        .right("POISONED")
                        .rightColor(Color.RED)
                        .build());
            }

        } catch (Exception e) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Player Stats Error:")
                    .right(e.getMessage())
                    .rightColor(Color.RED)
                    .build());
        }
    }

    private void addInventoryInfo() {
        try {
            // Food count
            List<InventoryItem> foodItems = new ArrayList<>(inventoryService.getFood());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Food Items:")
                    .right(String.valueOf(foodItems.size()))
                    .rightColor(config.foodOverlayColor())
                    .build());

            // Has food check
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Has Food:")
                    .right(inventoryService.hasFood() ? "Yes" : "No")
                    .rightColor(inventoryService.hasFood() ? Color.GREEN : Color.RED)
                    .build());

            // Total items count
            List<InventoryItem> allItems = new ArrayList<>(inventoryService.all());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Total Items:")
                    .right(String.valueOf(allItems.size()))
                    .rightColor(config.inventoryOverlayColor())
                    .build());

            // Check for test item
            InventoryItem testItem = inventoryService.get(config.testItemName());
            panelComponent.getChildren().add(LineComponent.builder()
                    .left(config.testItemName() + ":")
                    .right(testItem != null ? "Found (x" + testItem.getQuantity() + ")" : "Not Found")
                    .rightColor(testItem != null ? Color.GREEN : Color.RED)
                    .build());

            // Show first few items if debug is enabled
            if (config.showDebugInfo() && !allItems.isEmpty()) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("--- Inventory ---")
                        .build());

                int count = 0;
                for (InventoryItem item : allItems) {
                    if (count >= 5) break; // Limit to first 5 items

                    Color itemColor = config.inventoryOverlayColor();

                    // Highlight food items
                    if (config.highlightFood()) {
                        for (InventoryItem foodItem : foodItems) {
                            if (foodItem.getName().equals(item.getName())) {
                                itemColor = config.foodOverlayColor();
                                break;
                            }
                        }
                    }

                    panelComponent.getChildren().add(LineComponent.builder()
                            .left("  " + item.getName())
                            .right("x" + item.getQuantity())
                            .rightColor(itemColor)
                            .build());
                    count++;
                }
            }

        } catch (Exception e) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Inventory Error:")
                    .right(e.getMessage())
                    .rightColor(Color.RED)
                    .build());
        }
    }

    private void addDebugInfo() {
        try {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("--- Debug ---")
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Game State:")
                    .right(client.getGameState().toString())
                    .rightColor(Color.YELLOW)
                    .build());

            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Logged In:")
                    .right(client.getLocalPlayer() != null ? "Yes" : "No")
                    .rightColor(client.getLocalPlayer() != null ? Color.GREEN : Color.RED)
                    .build());

            if (client.getLocalPlayer() != null) {
                panelComponent.getChildren().add(LineComponent.builder()
                        .left("Player Name:")
                        .right(client.getLocalPlayer().getName())
                        .rightColor(Color.WHITE)
                        .build());
            }

        } catch (Exception e) {
            panelComponent.getChildren().add(LineComponent.builder()
                    .left("Debug Error:")
                    .right(e.getMessage())
                    .rightColor(Color.RED)
                    .build());
        }
    }
}