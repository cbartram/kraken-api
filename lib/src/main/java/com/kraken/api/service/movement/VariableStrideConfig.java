package com.kraken.api.service.movement;

import lombok.Builder;
import lombok.Data;
import lombok.Getter;

import java.util.concurrent.ThreadLocalRandom;

@Data
@Builder
public class VariableStrideConfig {
    @Builder.Default
    private int minStride = 5;

    @Builder.Default
    private int maxStride = 14;

    @Builder.Default
    private int meanStride = 10;

    @Builder.Default
    private int standardDeviation = 3;

    @Getter
    private boolean tileDeviation;

    public VariableStrideConfig withTileDeviation() {
        this.tileDeviation = true;
        return this;
    }

    /**
     * Computes a random stride value based on the configured mean, standard deviation, and min/max bounds.
     * Most results will cluster around the mean, with fewer results at the extremes.
     * @return The computed stride value.
     */
    public int computeStride() {
        double val = ThreadLocalRandom.current().nextGaussian() * this.standardDeviation + this.meanStride;
        int rounded = (int) Math.round(val);

        return Math.max(this.minStride, Math.min(this.maxStride, rounded));
    }
}
