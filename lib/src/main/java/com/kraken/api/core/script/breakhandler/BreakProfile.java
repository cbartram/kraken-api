package com.kraken.api.core.script.breakhandler;


import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.time.Duration;
import java.util.List;
import java.util.Random;

@Getter
@Builder
public class BreakProfile {
    private final String name;

    private final Duration minRuntime;
    private final Duration maxRuntime;
    private final Duration minBreakDuration;
    private final Duration maxBreakDuration;

    @Singular(value = "addBreakCondition")
    private final List<BreakCondition> breakConditions;

    @Builder.Default
    private final boolean logoutDuringBreak = true;

    @Builder.Default
    private final boolean randomizeTimings = true;

    private static final Random random = new Random();

    /**
     * Calculates the next runtime duration based on profile settings.
     * @return The duration of the next run
     */
    public Duration getNextRunDuration() {
        if (!randomizeTimings) {
            return maxRuntime;
        }
        long minMillis = minRuntime.toMillis();
        long maxMillis = maxRuntime.toMillis();
        long randomMillis = minMillis + (long) (random.nextDouble() * (maxMillis - minMillis));
        return Duration.ofMillis(randomMillis);
    }

    /**
     * Calculates the next break duration based on profile settings.
     * @return The duration of the next break
     */
    public Duration getNextBreakDuration() {
        if (!randomizeTimings) {
            return maxBreakDuration;
        }
        long minMillis = minBreakDuration.toMillis();
        long maxMillis = maxBreakDuration.toMillis();
        long randomMillis = minMillis + (long) (random.nextDouble() * (maxMillis - minMillis));
        return Duration.ofMillis(randomMillis);
    }

    public static BreakProfile createConservative() {
        return BreakProfile.builder()
                .name("Conservative")
                .minRuntime(Duration.ofMinutes(45))
                .maxRuntime(Duration.ofMinutes(90))
                .minBreakDuration(Duration.ofMinutes(5))
                .maxBreakDuration(Duration.ofMinutes(15))
                .logoutDuringBreak(true)
                .randomizeTimings(true)
                .build();
    }

    public static BreakProfile createAggressive() {
        return BreakProfile.builder()
                .name("Aggressive")
                .minRuntime(Duration.ofHours(2))
                .maxRuntime(Duration.ofHours(4))
                .minBreakDuration(Duration.ofMinutes(2))
                .maxBreakDuration(Duration.ofMinutes(5))
                .logoutDuringBreak(true)
                .randomizeTimings(true)
                .build();
    }

    public static BreakProfile createBalanced() {
        return BreakProfile.builder()
                .name("Balanced")
                .minRuntime(Duration.ofMinutes(60))
                .maxRuntime(Duration.ofMinutes(120))
                .minBreakDuration(Duration.ofMinutes(3))
                .maxBreakDuration(Duration.ofMinutes(10))
                .logoutDuringBreak(true)
                .randomizeTimings(true)
                .build();
    }
}