package com.kraken.api.interaction.movement;

/**
 * Represents the current state of a movement operation
 */
public enum MovementState {
    /**
     * Successfully arrived at the target destination
     */
    ARRIVED,

    /**
     * Currently walking towards the target
     */
    WALKING,

    /**
     * Movement is blocked (obstacle, no path found, etc.)
     */
    BLOCKED,

    /**
     * Movement failed due to error or timeout
     */
    FAILED,

    /**
     * Player is idle/not moving
     */
    IDLE
}