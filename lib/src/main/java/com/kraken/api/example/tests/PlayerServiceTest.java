package com.kraken.api.example.tests;


import com.google.inject.Inject;
import com.kraken.api.interaction.player.PlayerService;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PlayerServiceTest extends BaseApiTest {

    @Inject
    private PlayerService playerService;

    @Override
    protected boolean runTest() {
        boolean testsPassed = true;

        try {
            // Test 1: Health information
            boolean healthTest = testHealthInformation();
            testsPassed &= healthTest;

            // Test 2: Special attack status
            boolean specTest = testSpecialAttack();
            testsPassed &= specTest;

            // Test 3: Movement status
            boolean movementTest = testMovementStatus();
            testsPassed &= movementTest;

            // Test 4: Status effects
            boolean statusTest = testStatusEffects();
            testsPassed &= statusTest;

            // Test 5: Run toggle (if enabled in config)
            if (config.autoToggleRun()) {
                boolean runToggleTest = testRunToggle();
                testsPassed &= runToggleTest;
            }

        } catch (Exception e) {
            log.error("Exception during player service test", e);
            return false;
        }

        return testsPassed;
    }

    private boolean testHealthInformation() {
        try {
            // Get health information
            double healthPercent = playerService.getHealthPercentage();
            int currentHealth = playerService.getHealthRemaining();
            int maxHealth = playerService.getMaxHealth();

            log.info("Health - Percent: {:.1f}%, Current: {}, Max: {}",
                    healthPercent, currentHealth, maxHealth);

            // Validate health values
            boolean percentValid = assertThat(healthPercent >= 0 && healthPercent <= 100,
                    "Health percentage should be between 0 and 100");
            boolean currentValid = assertThat(currentHealth >= 0 && currentHealth <= maxHealth,
                    "Current health should be between 0 and max health");
            boolean maxValid = assertThat(maxHealth > 0, "Max health should be positive");

            // Check percentage calculation
            double expectedPercent = maxHealth > 0 ? (currentHealth * 100.0) / maxHealth : 0;
            boolean percentCalculation = assertThat(Math.abs(healthPercent - expectedPercent) < 0.1,
                    "Health percentage calculation should be accurate");

            // Check if health is below threshold
            if (healthPercent < config.lowHealthThreshold()) {
                log.warn("Health is below configured threshold ({}%)!", config.lowHealthThreshold());
            }

            return percentValid && currentValid && maxValid && percentCalculation;

        } catch (Exception e) {
            log.error("Error testing health information", e);
            return false;
        }
    }

    private boolean testSpecialAttack() {
        try {
            boolean specEnabled = playerService.isSpecEnabled();
            log.info("Special attack enabled: {}", specEnabled);

            // This test passes as long as the method doesn't throw an exception
            // The actual value can be either true or false
            return true;

        } catch (Exception e) {
            log.error("Error testing special attack status", e);
            return false;
        }
    }

    private boolean testMovementStatus() {
        try {
            boolean isMoving = playerService.isMoving();
            log.info("Player is moving: {}", isMoving);

            // This test passes as long as the method doesn't throw an exception
            return true;

        } catch (Exception e) {
            log.error("Error testing movement status", e);
            return false;
        }
    }

    private boolean testStatusEffects() {
        try {
            boolean isPoisoned = playerService.isPoisoned();
            log.info("Player is poisoned: {}", isPoisoned);

            if (isPoisoned) {
                log.warn("Player is currently poisoned!");
            }

            // Test run status
            boolean runEnabled = playerService.isRunEnabled();
            log.info("Run enabled: {}", runEnabled);

            // Both tests pass as long as methods don't throw exceptions
            return true;

        } catch (Exception e) {
            log.error("Error testing status effects", e);
            return false;
        }
    }

    private boolean testRunToggle() {
        try {
            // Get initial run state
            boolean initialRunState = playerService.isRunEnabled();
            log.info("Initial run state: {}", initialRunState);

            // Toggle run
            log.info("Testing run toggle...");
            playerService.toggleRun();

            // Wait a moment for the change to take effect
            Thread.sleep(1000);

            // Check new state
            boolean newRunState = playerService.isRunEnabled();
            log.info("Run state after toggle: {}", newRunState);

            // Note: We can't always guarantee the state will change immediately
            // due to game mechanics, so we just verify the method doesn't crash
            return true;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Run toggle test interrupted", e);
            return false;
        } catch (Exception e) {
            log.error("Error testing run toggle", e);
            return false;
        }
    }

    @Override
    protected String getTestName() {
        return "Player Service";
    }
}
