package com.kraken.api.example.tests;

import com.google.inject.Inject;
import com.kraken.api.interaction.inventory.InventoryItem;
import com.kraken.api.interaction.inventory.InventoryService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class InventoryServiceTest extends BaseApiTest {

    @Inject
    private InventoryService inventoryService;

    @Override
    protected boolean runTest() {
        boolean testsPassed = true;

        try {
            // Test 1: Get all inventory items
            List<InventoryItem> allItems = new ArrayList<>(inventoryService.all());

            log.info("Found {} items in inventory", allItems.size());

            // Test 2: Verify each item has valid properties
            int validItemCount = 0;
            for (InventoryItem item : allItems) {
                if (item.getName() != null && !item.getName().isEmpty() && item.getQuantity() > 0) {
                    validItemCount++;
                    log.debug("Valid item: {} (Quantity: {})", item.getName(), item.getQuantity());
                } else {
                    log.warn("Invalid item found: name='{}', quantity={}", item.getName(), item.getQuantity());
                }
            }

            if (!allItems.isEmpty()) {
                testsPassed &= assertThat(validItemCount == allItems.size(),
                        "All inventory items should have valid names and quantities");
            }

            // Test 3: Food detection
            boolean foodDetectionTest = testFoodDetection();
            testsPassed &= foodDetectionTest;

            // Test 4: Specific item lookup
            boolean itemLookupTest = testSpecificItemLookup();
            testsPassed &= itemLookupTest;

            // Test 5: Item interaction (if debug mode is enabled)
            if (config.showDebugInfo() && !allItems.isEmpty()) {
                boolean interactionTest = testItemInteraction(allItems.get(0));
                testsPassed &= interactionTest;
            }

        } catch (Exception e) {
            log.error("Exception during inventory service test", e);
            return false;
        }

        return testsPassed;
    }

    private boolean testFoodDetection() {
        try {
            // Test hasFood() method
            boolean hasFood = inventoryService.hasFood();
            log.info("Inventory has food: {}", hasFood);

            // Test getFood() method
            List<InventoryItem> foodItems = new ArrayList<>(inventoryService.getFood());

            log.info("Found {} food items", foodItems.size());

            // Verify consistency between hasFood() and getFood()
            boolean consistencyCheck = (hasFood && !foodItems.isEmpty()) || (!hasFood && foodItems.isEmpty());
            boolean testPassed = assertThat(consistencyCheck,
                    "hasFood() and getFood() should be consistent");

            // Log food items found
            for (InventoryItem foodItem : foodItems) {
                log.info("Food item: {} (Quantity: {})", foodItem.getName(), foodItem.getQuantity());
            }

            return testPassed;

        } catch (Exception e) {
            log.error("Error testing food detection", e);
            return false;
        }
    }

    private boolean testSpecificItemLookup() {
        try {
            String testItemName = config.testItemName();
            InventoryItem testItem = inventoryService.get(testItemName);

            if (testItem != null) {
                log.info("Test item '{}' found - Quantity: {}", testItemName, testItem.getQuantity());

                // Verify the item properties
                boolean nameCheck = assertNotNull(testItem.getName(), "Test item should have a name");
                boolean quantityCheck = assertThat(testItem.getQuantity() > 0,
                        "Test item should have positive quantity");

                return nameCheck && quantityCheck;
            } else {
                log.info("Test item '{}' not found in inventory (this may be expected)", testItemName);
                // Not finding the item is not necessarily a failure
                return true;
            }

        } catch (Exception e) {
            log.error("Error testing specific item lookup", e);
            return false;
        }
    }

    private boolean testItemInteraction(InventoryItem item) {
        try {
            log.info("Testing interaction with: {}", item.getName());

            // Test the interact method (this should not throw an exception)
            inventoryService.interactReflect(item, "Use");

            // If we get here without exception, the test passed
            log.debug("Item interaction test completed successfully");
            return true;

        } catch (Exception e) {
            log.error("Error testing item interaction", e);
            return false;
        }
    }

    @Override
    protected String getTestName() {
        return "Inventory Service";
    }
}
