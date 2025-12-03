
package example;

import net.runelite.client.config.*;

@ConfigGroup("testapi")
public interface ExampleConfig extends Config {
    @ConfigItem(
            name = "Clear Tests",
            keyName = "clearTests",
            description = "clear the execution of the configured tests.",
            position = -999
    )
    default boolean clearTests() {
        return false;
    }

    // =========== Tests Section ================
    @ConfigSection(
            name = "Query Tests",
            description = "Settings for enabling specific API query tests.",
            position = 2
    )
    String tests = "Query Tests";

    @ConfigItem(
            keyName = "enableBankQuery",
            name = "Start Bank Tests",
            description = "Enable Bank Query Tests",
            section = tests,
            position = 2
    )
    default boolean enableBankQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableInventoryQuery",
            name = "Start Inventory Tests",
            description = "Enable Inventory Query Tests",
            section = tests,
            position = 3
    )
    default boolean enableInventoryQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableBankInventoryQuery",
            name = "Start Bank Inventory Tests",
            description = "Enable Bank inventory Query Tests",
            section = tests,
            position = 4
    )
    default boolean enableBankInventoryQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableEquipmentQuery",
            name = "Start Equipment Tests",
            description = "Enable Equipment Query Tests",
            section = tests,
            position = 5
    )
    default boolean enableEquipmentQuery() {
        return true;
    }


    @ConfigItem(
            keyName = "enableGameObjectQuery",
            name = "Start Game Object Tests",
            description = "Enable game object query tests",
            section = tests,
            position = 6
    )
    default boolean enableGameObjectQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableGroundObjectQuery",
            name = "Start Ground Object Tests",
            description = "Enable Ground object query tests",
            section = tests,
            position = 6
    )
    default boolean enableGroundObjectQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableNpcQuery",
            name = "Start Npc Tests",
            description = "Enable Npc object query tests",
            section = tests,
            position = 7
    )
    default boolean enableNpcQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enablePlayerQuery",
            name = "Start Player Tests",
            description = "Enable Player object query tests",
            section = tests,
            position = 8
    )
    default boolean enablePlayerQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableWidgetQuery",
            name = "Start Widget Tests",
            description = "Enable Widget object query tests",
            section = tests,
            position = 9
    )
    default boolean enableWidgetQuery() {
        return true;
    }

    // ==============================================
    // ========== SERVICE TEST SETTINGS ==========
    // ==============================================
    @ConfigSection(
            name = "Service Tests",
            description = "Options for configuring service class tests",
            position = 3
    )
    String serviceTests = "Service Tests";

    @ConfigItem(
            keyName = "enablePrayer",
            name = "Start Prayer Service Tests",
            description = "Enable Prayer tests",
            section = serviceTests,
            position = 1
    )
    default boolean enablePrayerTests() {
        return true;
    }

    @ConfigItem(
            keyName = "enableMovement",
            name = "Start Movement Service Tests",
            description = "Enable movement service tests",
            section = serviceTests,
            position = 2
    )
    default boolean enableMovementTests() {
        return true;
    }

    @ConfigItem(
            keyName = "enableSpell",
            name = "Start Spell Service Tests",
            description = "Enable spell service tests",
            section = serviceTests,
            position = 3
    )
    default boolean enableSpellTests() {
        return true;
    }

    // ==============================================
    // ========== GENERAL OVERLAY SETTINGS ==========
    // ==============================================
    @ConfigSection(
            name = "Overlay Settings",
            description = "General overlay configuration for tests, debugging, and sim visualization",
            position = 99
    )
    String overlaySettings = "Overlay Settings";

    @ConfigItem(
            keyName = "showGameObjects",
            name = "Show Game Objects",
            description = "Display game objects in the scene.",
            section = overlaySettings,
            position = 1
    )
    default boolean showGameObjects() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            keyName = "gameObjectRange",
            name = "Game Object Range",
            description = "The range at which game objects are highlighted.",
            section = overlaySettings,
            position = 2
    )
    default int gameObjectRange() {
        return 3;
    }

    @ConfigItem(
            keyName = "showGroundObjects",
            name = "Show Ground Objects",
            description = "Display Ground objects in the scene.",
            section = overlaySettings,
            position = 4
    )
    default boolean showGroundObjects() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            keyName = "groundObjectRange",
            name = "Ground Object Range",
            description = "The range at which ground objects are highlighted.",
            section = overlaySettings,
            position = 5
    )
    default int groundObjectRange() {
        return 3;
    }

    @ConfigItem(
            keyName = "showNpcObjects",
            name = "Show Npc Objects",
            description = "Display Npc's within the scene.",
            section = overlaySettings,
            position = 6
    )
    default boolean showNpcs() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            keyName = "npcRange",
            name = "Npc Range",
            description = "The range at which Npcs are highlighted.",
            section = overlaySettings,
            position = 7
    )
    default int npcRange() {
        return 3;
    }

    @ConfigItem(
            keyName = "showPlayerObjects",
            name = "Show Players",
            description = "Display Player's within the scene.",
            section = overlaySettings,
            position = 8
    )
    default boolean showPlayers() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            keyName = "playerRange",
            name = "Player Range",
            description = "The range at which Players are highlighted.",
            section = overlaySettings,
            position = 9
    )
    default int playerRange() {
        return 3;
    }

    @ConfigItem(
            keyName = "fontSize",
            name = "Font Size",
            description = "Font size for overlay text",
            section = overlaySettings,
            position = 12
    )
    default int fontSize() {
        return 12;
    }

    @ConfigItem(
            keyName = "showDebugInfo",
            name = "Show Debug Info",
            description = "Display additional debug information in overlays",
            section = overlaySettings,
            position = 13
    )
    default boolean showDebugInfo() {
        return false;
    }

    @ConfigItem(
            name = "Show Sim Visualizer",
            keyName = "simVisualizer",
            description = "Shows the simulation visualizer UI.",
            position = 14,
            section = overlaySettings
    )
    default boolean showVisualizer() {
        return false;
    }
}