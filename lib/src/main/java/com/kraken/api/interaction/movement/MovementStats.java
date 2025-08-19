package com.kraken.api.interaction.movement;

import lombok.AllArgsConstructor;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

/**
 * Contains detailed statistics about the current movement operation
 */
@Data
@AllArgsConstructor
public class MovementStats {
    private final MovementState state;
    private final String description;
    private final double progress;
    private final int completedWaypoints;
    private final int totalWaypoints;
    private final double distanceToTarget;
    private final double distanceToNextWaypoint;
    private final long timeSinceLastMovement;
    private final WorldPoint target;
    private final WorldPoint nextWaypoint;

    /**
     * Gets progress as a percentage string
     */
    public String getProgressPercentage() {
        return String.format("%.1f%%", progress * 100);
    }

    /**
     * Gets waypoint progress as a string (e.g., "5/12")
     */
    public String getWaypointProgress() {
        return String.format("%d/%d", completedWaypoints, totalWaypoints);
    }

    /**
     * Checks if movement appears to be stuck
     */
    public boolean isStuck() {
        return timeSinceLastMovement > 3000 && state == MovementState.WALKING;
    }

    /**
     * Gets estimated time remaining based on current progress (rough estimate)
     */
    public long getEstimatedTimeRemaining() {
        if (progress <= 0 || timeSinceLastMovement <= 0) {
            return -1; // Cannot estimate
        }

        double timePerProgress = timeSinceLastMovement / progress;
        return (long) (timePerProgress * (1.0 - progress));
    }
}