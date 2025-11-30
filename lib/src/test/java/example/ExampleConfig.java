
package example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

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
            name = "Tests",
            description = "Settings for enabling specific API tests.",
            position = 2
    )
    String tests = "Tests";

    @ConfigItem(
            keyName = "enablePrayer",
            name = "Start Prayer Service",
            description = "Enable Prayer tests",
            section = tests,
            position = 1
    )
    default boolean enablePrayerTests() {
        return true;
    }

    @ConfigItem(
            keyName = "enableBankQuery",
            name = "Start Bank Query",
            description = "Enable Bank Query Tests",
            section = tests,
            position = 2
    )
    default boolean enableBankQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableInventoryQuery",
            name = "Start Inventory Query",
            description = "Enable Inventory Query Tests",
            section = tests,
            position = 3
    )
    default boolean enableInventoryQuery() {
        return true;
    }

    @ConfigItem(
            keyName = "enableBankInventoryQuery",
            name = "Start Bank Inventory Query",
            description = "Enable Bank inventory Query Tests",
            section = tests,
            position = 4
    )
    default boolean enableBankInventoryQuery() {
        return true;
    }


    // ========== GENERAL OVERLAY SETTINGS ==========
    @ConfigSection(
            name = "Overlay Settings",
            description = "General overlay configuration.",
            position = 99
    )
    String overlaySettings = "Overlay Settings";

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
            name = "Show Sim Visualizer",
            keyName = "simVisualizer",
            description = "Shows the simulation visualizer UI.",
            position = 4,
            section = overlaySettings
    )
    default boolean showVisualizer() {
        return false;
    }
}