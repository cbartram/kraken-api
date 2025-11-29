
package com.kraken.api.example;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;

@ConfigGroup("testapi")
public interface ExampleConfig extends Config {
    @ConfigItem(
            name = "Start Tests",
            keyName = "start",
            description = "Start the execution of the configured tests.",
            position = -999
    )
    default boolean start() {
        return false;
    }

    @ConfigItem(
            name = "Show Sim Visualizer",
            keyName = "simVisualizer",
            description = "Shows the simulation visualizer UI.",
            position = -3
    )
    default boolean showVisualizer() {
        return false;
    }

    @ConfigSection(
            name = "General",
            description = "General settings for individual testing",
            position = 0
    )
    String general = "General";

    @ConfigItem(
            name = "Send NPC Attack",
            keyName = "attackNpc",
            description = "Test NPC attack packet send.",
            position = -1,
            section = general
    )
    default boolean prayerOn() {
        return false;
    }

    @ConfigItem(
            name = "Send Spell Packet",
            keyName = "magicSpellCast",
            description = "Test magic spell cast",
            position = 0,
            section = general
    )
    default boolean magicSpellCast() {
        return false;
    }

    @ConfigItem(
            name = "Deposit One Item",
            keyName = "depositOneItem",
            description = "Test depositing one item into the bank from the inventory",
            position = 1,
            section = general
    )
    default String depositOneItem() {
        return "";
    }

    @ConfigItem(
            name = "Start Deposit One",
            keyName = "depositOneCheck",
            description = "Test depositing one item into the bank from the inventory",
            position = 2,
            section = general
    )
    default boolean depositOneCheck() {
        return false;
    }

    @ConfigItem(
            name = "Deposit All Item",
            keyName = "depositAllItem",
            description = "Test depositing all of a single item into the bank from the inventory",
            position = 3,
            section = general
    )
    default String depositAllItem() {
        return "";
    }

    @ConfigItem(
            name = "Start Deposit All",
            keyName = "depositAllCheck",
            description = "Test depositing one item into the bank from the inventory",
            position = 4,
            section = general
    )
    default boolean depositAllCheck() {
        return false;
    }

    // =========== Bank Tests SECTION ==========
    @ConfigSection(
            name = "Bank Tests",
            description = "Settings for Bank tests.",
            position = 6
    )
    String BankTests = "Bank Tests";

    @ConfigItem(
            keyName = "enableBankTests",
            name = "Enable Bank Tests",
            description = "Enable Bank service testing functionality",
            section = BankTests,
            position = 1
    )
    default boolean enableBankTests() {
        return true;
    }

    // =========== Movement Tests ================
    @ConfigSection(
            name = "Movement Tests",
            description = "Settings for Movement tests.",
            position = 7
    )
    String movement = "Movement Tests";

    @ConfigItem(
            keyName = "enableMovement",
            name = "Enable Movement",
            description = "Enable movement tests",
            section = movement,
            position = 1
    )
    default boolean enableMovementTests() {
        return true;
    }

    @ConfigItem(
            keyName = "enableMovementOverlay",
            name = "Enable Movement Overlay",
            description = "Enables the target point overlay",
            section = movement,
            position = 2
    )
    default boolean enableMovementOverlay() {
        return false;
    }

    @ConfigItem(
            keyName = "fromWorldInstance",
            name = "Walk to Target",
            description = "Walk to Target tile selected by shift + right clicking and selecting the target tile.",
            section = movement,
            position = 4
    )
    default boolean fromWorldInstance() {
        return false;
    }

    // =========== Prayer Tests ================
    @ConfigSection(
            name = "Prayer Tests",
            description = "Settings for Prayer tests.",
            position = 8
    )
    String prayer = "Prayer Tests";

    @ConfigItem(
            keyName = "enablePrayer",
            name = "Enable Prayer",
            description = "Enable Prayer tests",
            section = prayer,
            position = 1
    )
    default boolean enablePrayerTests() {
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
}