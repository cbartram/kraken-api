package com.kraken.api.plugins.packetmapper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("packetmapping")
public interface PacketMappingConfig extends Config {

    @ConfigItem(
        keyName = "autoStart",
        name = "Auto-start monitoring",
        description = "Automatically start packet monitoring when the plugin loads"
    )
    default boolean autoStart() {
        return false;
    }

    @ConfigItem(
        keyName = "showDebugLogs",
        name = "Show debug logs",
        description = "Display detailed debug information in the console"
    )
    default boolean showDebugLogs() {
        return true;
    }

    @ConfigItem(
        keyName = "correlationTimeout",
        name = "Action correlation timeout (ms)",
        description = "Time window to correlate game actions with packets"
    )
    default int correlationTimeout() {
        return 100;
    }

    @ConfigItem(
        keyName = "maxPacketHistory",
        name = "Max packet history",
        description = "Maximum number of packets to keep in history"
    )
    default int maxPacketHistory() {
        return 1000;
    }

    @ConfigItem(
        keyName = "autoExportPath",
        name = "Auto-export path",
        description = "Directory path to automatically export mappings (leave empty to disable)"
    )
    default String autoExportPath() {
        return "";
    }

    @ConfigItem(
        keyName = "exportOnStop",
        name = "Export on stop",
        description = "Automatically export mappings when monitoring is stopped"
    )
    default boolean exportOnStop() {
        return false;
    }
}
