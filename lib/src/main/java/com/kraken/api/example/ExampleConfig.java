
package com.kraken.api.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import java.awt.Color;

@ConfigGroup("testapi")
public interface ExampleConfig extends Config {
    @ConfigItem(
            name = "Start Tests",
            keyName = "start",
            description = "Start the execution of the configured tests.",
            position = 0
    )
    default boolean start() {
        return false;
    }

    // ========== NPC TESTS SECTION ==========
    @ConfigSection(
            name = "NPC Tests",
            description = "Settings for NPC tests.",
            position = 1
    )
    String npcTests = "NPC Tests";

    @ConfigItem(
            keyName = "enableNpcTests",
            name = "Enable NPC Tests",
            description = "Enable NPC service testing functionality",
            section = npcTests,
            position = 1
    )
    default boolean enableNpcTests() {
        return true;
    }

    @ConfigItem(
            keyName = "targetNpcName",
            name = "Target NPC Name",
            description = "Name of the NPC to target for testing (e.g., 'Guard')",
            section = npcTests,
            position = 2
    )
    default String targetNpcName() {
        return "Guard";
    }

    @ConfigItem(
            keyName = "showNpcOverlay",
            name = "Show NPC Overlay",
            description = "Display NPC names and information overlay",
            section = npcTests,
            position = 3
    )
    default boolean showNpcOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "npcOverlayColor",
            name = "NPC Overlay Color",
            description = "Color for NPC name overlays",
            section = npcTests,
            position = 4
    )
    default Color npcOverlayColor() {
        return Color.CYAN;
    }

    @ConfigItem(
            keyName = "highlightAttackableNpcs",
            name = "Highlight Attackable NPCs",
            description = "Highlight NPCs that can be attacked",
            section = npcTests,
            position = 5
    )
    default boolean highlightAttackableNpcs() {
        return true;
    }

    // ========== INVENTORY TESTS SECTION ==========
    @ConfigSection(
            name = "Inventory Tests",
            description = "Settings for Inventory tests.",
            position = 2
    )
    String inventoryTests = "Inventory Tests";

    @ConfigItem(
            keyName = "enableInventoryTests",
            name = "Enable Inventory Tests",
            description = "Enable inventory service testing functionality",
            section = inventoryTests,
            position = 1
    )
    default boolean enableInventoryTests() {
        return true;
    }

    @ConfigItem(
            keyName = "showInventoryOverlay",
            name = "Show Inventory Overlay",
            description = "Display inventory items overlay",
            section = inventoryTests,
            position = 2
    )
    default boolean showInventoryOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "highlightFood",
            name = "Highlight Food Items",
            description = "Highlight food items in inventory with different color",
            section = inventoryTests,
            position = 3
    )
    default boolean highlightFood() {
        return true;
    }

    @ConfigItem(
            keyName = "foodOverlayColor",
            name = "Food Overlay Color",
            description = "Color for food item overlays",
            section = inventoryTests,
            position = 4
    )
    default Color foodOverlayColor() {
        return Color.GREEN;
    }

    @ConfigItem(
            keyName = "inventoryOverlayColor",
            name = "Inventory Overlay Color",
            description = "Color for regular inventory item overlays",
            section = inventoryTests,
            position = 5
    )
    default Color inventoryOverlayColor() {
        return Color.WHITE;
    }

    @ConfigItem(
            keyName = "testItemName",
            name = "Test Item Name",
            description = "Name of item to test interactions with (e.g., 'Rune platebody')",
            section = inventoryTests,
            position = 6
    )
    default String testItemName() {
        return "Rune platebody";
    }

    @ConfigItem(
            keyName = "dropItemName",
            name = "Drop Item Name",
            description = "Name of item to test dropping (e.g., 'Swordfish')",
            section = inventoryTests,
            position = 7
    )
    default String dropItemName() {
        return "Swordfish";
    }

    // ========== PLAYER TESTS SECTION ==========
    @ConfigSection(
            name = "Player Tests",
            description = "Settings for Player tests.",
            position = 3
    )
    String playerTests = "Player Tests";

    @ConfigItem(
            keyName = "enablePlayerTests",
            name = "Enable Player Tests",
            description = "Enable player service testing functionality",
            section = playerTests,
            position = 1
    )
    default boolean enablePlayerTests() {
        return true;
    }

    @ConfigItem(
            keyName = "showPlayerStats",
            name = "Show Player Stats Overlay",
            description = "Display player statistics overlay (health, spec, etc.)",
            section = playerTests,
            position = 2
    )
    default boolean showPlayerStats() {
        return true;
    }

    @ConfigItem(
            keyName = "playerStatsColor",
            name = "Player Stats Color",
            description = "Color for player statistics overlay",
            section = playerTests,
            position = 3
    )
    default Color playerStatsColor() {
        return Color.YELLOW;
    }

    @ConfigItem(
            keyName = "autoToggleRun",
            name = "Auto Toggle Run",
            description = "Automatically toggle run during player tests",
            section = playerTests,
            position = 4
    )
    default boolean autoToggleRun() {
        return false;
    }

    @ConfigItem(
            keyName = "lowHealthThreshold",
            name = "Low Health Threshold",
            description = "Health percentage threshold to highlight low health",
            section = playerTests,
            position = 5
    )
    default int lowHealthThreshold() {
        return 30;
    }

    // ========== GROUND ITEM TESTS SECTION ==========
    @ConfigSection(
            name = "Ground Item Tests",
            description = "Settings for Ground Item tests.",
            position = 4
    )
    String groundItem = "Ground Item Tests";

    @ConfigItem(
            keyName = "enableGroundItemTests",
            name = "Enable Ground Item Tests",
            description = "Enable ground object service testing functionality",
            section = groundItem,
            position = 1
    )
    default boolean enableGroundItemTests() {
        return true;
    }

    @ConfigItem(
            keyName = "showGroundItemOverlay",
            name = "Show Ground Items Overlay",
            description = "Display ground items overlay with names",
            section = groundItem,
            position = 2
    )
    default boolean showGroundItemOverlay() {
        return true;
    }

    @ConfigItem(
            keyName = "groundItemOverlayColor",
            name = "Ground Item Overlay Color",
            description = "Color for ground item overlays",
            section = groundItem,
            position = 3
    )
    default Color groundItemOverlayColor() {
        return Color.ORANGE;
    }

    @ConfigItem(
            keyName = "targetGroundItem",
            name = "Target Ground Item",
            description = "Name of ground item to target for testing (e.g., 'Bones')",
            section = groundItem,
            position = 4
    )
    default String targetGroundItem() {
        return "Bones";
    }

    @ConfigItem(
            keyName = "highlightValuableItems",
            name = "Highlight Valuable Items",
            description = "Highlight valuable ground items with different color",
            section = groundItem,
            position = 5
    )
    default boolean highlightValuableItems() {
        return true;
    }

    @ConfigItem(
            keyName = "valuableItemColor",
            name = "Valuable Item Color",
            description = "Color for valuable ground item overlays",
            section = groundItem,
            position = 6
    )
    default Color valuableItemColor() {
        return Color.MAGENTA;
    }

    @ConfigItem(
            keyName = "minItemValue",
            name = "Minimum Item Value",
            description = "Minimum GP value to consider an item valuable",
            section = groundItem,
            position = 7
    )
    default int minItemValue() {
        return 1000;
    }

    // ========== GENERAL OVERLAY SETTINGS ==========
    @ConfigSection(
            name = "Overlay Settings",
            description = "General overlay configuration.",
            position = 5
    )
    String overlaySettings = "Overlay Settings";

    @ConfigItem(
            keyName = "overlayOpacity",
            name = "Overlay Opacity",
            description = "Opacity of all overlays (0-255)",
            section = overlaySettings,
            position = 1
    )
    default int overlayOpacity() {
        return 255;
    }

    @ConfigItem(
            keyName = "fontSize",
            name = "Font Size",
            description = "Font size for overlay text",
            section = overlaySettings,
            position = 2
    )
    default int fontSize() {
        return 12;
    }

    @ConfigItem(
            keyName = "showDebugInfo",
            name = "Show Debug Info",
            description = "Display additional debug information in overlays",
            section = overlaySettings,
            position = 3
    )
    default boolean showDebugInfo() {
        return false;
    }

    @ConfigItem(
            keyName = "overlayPosition",
            name = "Overlay Position",
            description = "Position of information overlays on screen",
            section = overlaySettings,
            position = 4
    )
    default String overlayPosition() {
        return "TOP_LEFT";
    }
}