package com.kraken.api.example.tests;

import com.google.inject.Inject;
import com.kraken.api.interaction.groundobject.GroundItem;
import com.kraken.api.interaction.groundobject.GroundObjectService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GroundObjectServiceTest extends BaseApiTest {

    @Inject
    private GroundObjectService groundObjectService;

    @Override
    protected boolean runTest() {
        boolean testsPassed = true;

        try {
            // Test 1: Get all ground items
            boolean allItemsTest = testAllGroundItems();
            testsPassed &= allItemsTest;

            // Test 2: Specific item lookup
            boolean specificItemTest = testSpecificItemLookup();
            testsPassed &= specificItemTest;

            // Test 3: Item interaction (if debug mode is enabled)
            if (config.showDebugInfo()) {
                boolean interactionTest = testItemInteraction();
                testsPassed &= interactionTest;
            }

            // Test 4: Valuable item detection
            if (config.highlightValuableItems()) {
                boolean valuableItemTest = testValuableItemDetection();
                testsPassed &= valuableItemTest;
            }

        } catch (Exception e) {
            log.error("Exception during ground object service test", e);
            return false;
        }

        return testsPassed;
    }

    private boolean testAllGroundItems() {
        try {
            List<GroundItem> allItems = new ArrayList<>();
            for (GroundItem item : groundObjectService.all()) {
                allItems.add(item);
            }

            log.info("Found {} ground items", allItems.size());

            int validItemCount = 0;
            int itemsToCheck = Math.min(allItems.size(), 20);

            for (int i = 0; i < itemsToCheck; i++) {
                GroundItem item = allItems.get(i);

                // Basic validation
                boolean hasValidId = item.getId() > 0;
                boolean hasValidLocation = item.getLocation() != null;
                boolean hasValidQuantity = item.getQuantity() > 0;

                if (hasValidId && hasValidLocation && hasValidQuantity) {
                    validItemCount++;

                    String itemName = item.getName();
                    log.debug("Ground Item: {} (ID: {}, Quantity: {})",
                            itemName != null ? itemName : "Unknown", item.getId(), item.getQuantity());
                } else {
                    log.warn("Invalid ground item found: ID={}, Location={}, Quantity={}",
                            item.getId(), item.getLocation(), item.getQuantity());
                }
            }

            if (itemsToCheck > 0) {
                return assertThat(validItemCount > 0,
                        "At least some ground items should be valid");
            }

            // No items found is not necessarily a failure
            return true;

        } catch (Exception e) {
            log.error("Error testing all ground items", e);
            return false;
        }
    }

    private boolean testSpecificItemLookup() {
        try {
            String targetItem = config.targetGroundItem();
            GroundItem item = groundObjectService.get(targetItem);

            if (item != null) {
                log.info("Target ground item '{}' found (ID: {}, Quantity: {})",
                        targetItem, item.getId(), item.getQuantity());

                // Validate the found item
                boolean validId = assertThat(item.getId() > 0, "Target item should have valid ID");
                boolean validLocation = assertNotNull(item.getLocation(), "Target item should have location");
                boolean validQuantity = assertThat(item.getQuantity() > 0, "Target item should have positive quantity");

                return validId && validLocation && validQuantity;
            } else {
                log.info("Target ground item '{}' not found in current area (this may be expected)", targetItem);
                // Not finding the item is not necessarily a failure
                return true;
            }

        } catch (Exception e) {
            log.error("Error testing specific item lookup", e);
            return false;
        }
    }

    private boolean testItemInteraction() {
        try {
            String targetItem = config.targetGroundItem();
            GroundItem item = groundObjectService.get(targetItem);

            if (item != null) {
                log.info("Testing interaction with ground item: {}", targetItem);
                groundObjectService.interact(item);
                log.debug("Ground item interaction test completed successfully");
                return true;
            } else {
                log.info("No target ground item found for interaction test");
                return true;
            }

        } catch (Exception e) {
            log.error("Error testing ground item interaction", e);
            return false;
        }
    }

    private boolean testValuableItemDetection() {
        try {
            List<GroundItem> allItems = new ArrayList<>();
            for (GroundItem item : groundObjectService.all()) {
                allItems.add(item);
            }

            int valuableCount = 0;
            int itemsToCheck = Math.min(allItems.size(), 20);

            for (int i = 0; i < itemsToCheck; i++) {
                GroundItem item = allItems.get(i);
                String itemName = item.getName();

                if (itemName != null && isValuableItem(item)) {
                    valuableCount++;
                    log.info("Valuable item detected: {} (ID: {}, Quantity: {})",
                            itemName, item.getId(), item.getQuantity());
                }
            }

            log.info("Found {} valuable items out of {} checked", valuableCount, itemsToCheck);
            return true;

        } catch (Exception e) {
            log.error("Error testing valuable item detection", e);
            return false;
        }
    }

    private boolean isValuableItem(GroundItem item) {
        return item.getGrandExchangePrice() > config.minItemValue();
    }

    @Override
    protected String getTestName() {
        return "Ground Object Service";
    }
}
