
package example;

import com.kraken.api.input.mouse.strategy.MouseMovementStrategy;
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

    @ConfigItem(
            name = "Pause Script",
            keyName = "pauseScript",
            description = "Pauses the example script loop which runs in the background.",
            position = -5
    )
    default boolean pauseScript() {
        return false;
    }

    @ConfigItem(
            name = "Login",
            keyName = "login",
            description = "Logs into the client using the preloaded jagex account.",
            position = -2
    )
    default boolean login() {
        return false;
    }

    @ConfigItem(
            name = "Logout",
            keyName = "logout",
            description = "Logs out of the client."
    )
    default boolean logout() {
        return false;
    }


    // =========== Tests Section ================
    @ConfigSection(
            name = "Pathfinding Tests",
            description = "Settings for enabling and testing pathfinding across the world",
            position = 1
    )
    String pathfinding = "pathfinding";

    @ConfigItem(
            name = "Start Pathfinding",
            keyName = "startPathfinding",
            description = "Starts the movement traversal to the specified waypoint",
            position = -1,
            section = pathfinding
    )
    default boolean startPathfinding() {
        return false;
    }

    @ConfigItem(
            name = "Pathfinding Location",
            keyName = "pathfindingLocation",
            description = "The location in the format: x,y,z for the waypoint to traverse to.",
            position = 0,
            section = pathfinding
    )
    default String waypointLocation() {
        return "0,0,0";
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

    @ConfigItem(
            keyName = "enableWorldQuery",
            name = "Start World Tests",
            description = "Enable World object query tests",
            section = tests,
            position = 10
    )
    default boolean enableWorldQuery() {
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

    @ConfigItem(
            keyName = "enableCamera",
            name = "Start Camera Service Tests",
            description = "Enable camera service tests",
            section = serviceTests,
            position = 4
    )
    default boolean enableCameraTests() {
        return true;
    }

    @ConfigItem(
            keyName = "enablePathfinder",
            name = "Start Pathfinder Tests",
            description = "Enable pathfinder service tests",
            section = serviceTests,
            position = 5
    )
    default boolean enablePathfinder() {
        return true;
    }

    @ConfigItem(
            keyName = "enableTaskChain",
            name = "Start Task Chain Tests",
            description = "Enable task chain tests",
            section = serviceTests,
            position = 6
    )
    default boolean enableTaskChain() {
        return true;
    }

    @ConfigItem(
            keyName = "enableDialogueService",
            name = "Start Dialogue Service Tests",
            description = "Enable dialogue service tests",
            section = serviceTests,
            position = 7
    )
    default boolean enableDialogueService() {
        return true;
    }

    @ConfigItem(
            keyName = "enableProcessingService",
            name = "Start Process Service Tests",
            description = "Enable process service tests",
            section = serviceTests,
            position = 8
    )
    default boolean enableProcessService() {
        return true;
    }

    @ConfigItem(
            keyName = "enableAreaService",
            name = "Start Area Service Tests",
            description = "Enable area service tests",
            section = serviceTests,
            position = 9
    )
    default boolean enableAreaService() {
        return true;
    }

    // ==============================================
    // ========== MOUSE SETTINGS ==========
    // ==============================================
    @ConfigSection(
            name = "Mouse Settings",
            description = "Settings for testing mouse movement, pathing, and recording.",
            position = 80
    )
    String mouseSettings = "Mouse Settings";

    @ConfigItem(
            keyName = "mouseRecord",
            name = "Start Recording",
            description = "Starts or stops the mouse recording. When checked mouse recording " +
                    "is happening. When un-checked mouse recording stops.",
            section = mouseSettings,
            position = 1
    )
    default boolean mouseRecord() {
        return true;
    }

    @ConfigItem(
            keyName = "mouseStrategy",
            name = "Mouse Movement Strategy",
            description = "Determines the strategy to use for moving the mouse.",
            section = mouseSettings,
            position = 2
    )
    default MouseMovementStrategy mouseStrategy() {
        return MouseMovementStrategy.NO_MOVEMENT;
    }

    @ConfigItem(
            keyName = "enableMouseTest",
            name = "Start Mouse Test",
            description = "Starts the mouse movement test, using the \"test\" recording data",
            section = mouseSettings,
            position = 3
    )
    default boolean enableMouseTest() {
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

    @ConfigItem(
            keyName = "showLocalPlayer",
            name = "Show Local Player",
            description = "Display information about the local player in the scene.",
            section = overlaySettings,
            position = 9
    )
    default boolean showSelf() {
        return false;
    }

    @Range(min = 1, max = 25)
    @ConfigItem(
            keyName = "playerRange",
            name = "Player Range",
            description = "The range at which Players are highlighted.",
            section = overlaySettings,
            position = 10
    )
    default int playerRange() {
        return 3;
    }

    @ConfigItem(
            keyName = "showaAreaService",
            name = "Show Game Areas",
            description = "Show game areas rendered from the Area service tests.",
            section = overlaySettings,
            position = 11
    )
    default boolean showAreaService() {
        return true;
    }

    @ConfigItem(
            name = "Show Mouse Overlay",
            keyName = "showMouse",
            description = "Shows an overlay of the mouse position and trail.",
            position = 12,
            section = overlaySettings
    )
    default boolean showMouse() {
        return false;
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
            keyName = "renderCurrentPath",
            name = "Show Current Path",
            description = "Displays the current path calculated by the local pathfinder.",
            section = overlaySettings,
            position = 14
    )
    default boolean renderCurrentPath() {
        return false;
    }

    @ConfigItem(
            name = "Show Sim Visualizer",
            keyName = "simVisualizer",
            description = "Shows the simulation visualizer UI.",
            position = 15,
            section = overlaySettings
    )
    default boolean showVisualizer() {
        return false;
    }
}
