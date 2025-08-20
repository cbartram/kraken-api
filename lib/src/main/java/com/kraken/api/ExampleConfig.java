package com.kraken.api;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("testapi")
public interface ExampleConfig extends Config {
    @ConfigItem(
            name = "X Coordinate",
            keyName = "x",
            description = "The X coordinate for the mining area.",
            position = 0
    )
    default String xCoordinate() {
        return "";
    }

    @ConfigItem(
            name = "Y Coordinate",
            keyName = "y",
            description = "The Y coordinate for the mining area.",
            position = 1
    )
    default String yCoordinate() {
        return "";
    }

    @ConfigItem(
            name = "Movement Type",
            keyName = "movementType",
            description = "The implementation to use for movement.",
            position = 2
    )
    default MovementType movementType() {
        return MovementType.SCENE_WALK_SP;
    }

    enum MovementType {
        SCENE_WALK_SP,
        MINIMAP_WALK_WP,
        WALK_LOCAL_LP,
        WALK_CANVAS_WP
    }

    @ConfigItem(
            name = "Start Path",
            keyName = "start",
            description = "Start Pathfinding algorithm.",
            position = 3
    )
    default boolean start() {
        return false;
    }
}
