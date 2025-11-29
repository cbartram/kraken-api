package com.kraken.api.example.overlay;

import com.google.inject.Inject;
import com.kraken.api.example.ExampleConfig;
import com.kraken.api.example.TestResultManager;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

import java.awt.*;
import java.util.Map;

public class InfoPanelOverlay extends OverlayPanel {

    private final Client client;
    private final ExampleConfig config;
    private final TestResultManager testResultManager;

    @Inject
    public InfoPanelOverlay(Client client, ExampleConfig config, TestResultManager testResultManager) {
        this.client = client;
        this.config = config;
        this.testResultManager = testResultManager;
        setPosition(OverlayPosition.TOP_RIGHT);
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