package com.kraken.api.input.mouse.strategy.wind;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WindMouseConfig {
    /**
     * gravitational pull towards the target.
     * Higher = faster, more direct movement.
     * Lower = more "floaty", prone to overshooting.
     */
    @Builder.Default
    private double gravity = 9.0;

    /**
     * The magnitude of chaos/randomness.
     * Higher = more shaking and "noise" in the path.
     */
    @Builder.Default
    private double wind = 3.0;

    /**
     * Minimum wait time (ms) per loop.
     */
    @Builder.Default
    private double minWait = 2.0;

    /**
     * Maximum wait time (ms) per loop.
     */
    @Builder.Default
    private double maxWait = 5.0;

    /**
     * The maximum pixels the mouse can move in a single step.
     * Limits the speed to prevent instant teleportation.
     */
    @Builder.Default
    private double maxStep = 10.0;

    /**
     * The distance (in pixels) from the target where the algorithm stops
     * or snaps to the final point.
     */
    @Builder.Default
    private double targetArea = 8.0;
}
