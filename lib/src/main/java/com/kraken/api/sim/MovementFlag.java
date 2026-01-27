package com.kraken.api.sim;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.CollisionDataFlag;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This is a copy of the net.runelite.client.plugins.devtools.MovementFlag class that has private
 * access within the RuneLite client. It is replicated here so that it can be used within classes and methods
 * which need access to interpret collision data.
 */
@Getter
@AllArgsConstructor
public enum MovementFlag {
    BLOCK_MOVEMENT_NORTH_WEST(CollisionDataFlag.BLOCK_MOVEMENT_NORTH_WEST),
    BLOCK_MOVEMENT_NORTH(CollisionDataFlag.BLOCK_MOVEMENT_NORTH),
    BLOCK_MOVEMENT_NORTH_EAST(CollisionDataFlag.BLOCK_MOVEMENT_NORTH_EAST),
    BLOCK_MOVEMENT_EAST(CollisionDataFlag.BLOCK_MOVEMENT_EAST),
    BLOCK_MOVEMENT_SOUTH_EAST(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_EAST),
    BLOCK_MOVEMENT_SOUTH(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH),
    BLOCK_MOVEMENT_SOUTH_WEST(CollisionDataFlag.BLOCK_MOVEMENT_SOUTH_WEST),
    BLOCK_MOVEMENT_WEST(CollisionDataFlag.BLOCK_MOVEMENT_WEST),

    BLOCK_MOVEMENT_OBJECT(CollisionDataFlag.BLOCK_MOVEMENT_OBJECT),
    BLOCK_MOVEMENT_FLOOR_DECORATION(CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION),
    BLOCK_MOVEMENT_FLOOR(CollisionDataFlag.BLOCK_MOVEMENT_FLOOR),
    BLOCK_MOVEMENT_FULL(CollisionDataFlag.BLOCK_MOVEMENT_FULL);

    public static final int BLOCKING_FLAGS = CollisionDataFlag.BLOCK_MOVEMENT_FULL | CollisionDataFlag.BLOCK_MOVEMENT_OBJECT
            | CollisionDataFlag.BLOCK_MOVEMENT_FLOOR | CollisionDataFlag.BLOCK_MOVEMENT_FLOOR_DECORATION;

    private final int flag;

    /**
     * @param collisionData The tile collision flags.
     * @return The set of {@link net.runelite.client.plugins.devtools.MovementFlag}s that have been set.
     */
    public static Set<MovementFlag> getSetFlags(int collisionData) {
        return Arrays.stream(values())
                .filter(movementFlag -> (movementFlag.flag & collisionData) != 0)
                .collect(Collectors.toSet());
    }
}