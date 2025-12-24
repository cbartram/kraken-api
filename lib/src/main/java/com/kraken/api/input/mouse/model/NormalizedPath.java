package com.kraken.api.input.mouse.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class NormalizedPath {
    // The label (e.g., "mining_drop") ensures we don't mix behaviors
    private String label;

    // The original distance/duration are kept as metadata for "Fitts's Law" filtering later.
    // We shouldn't stretch a 50px movement to fit a 1000px gap.
    private double originalDistance;
    private long originalDuration;

    // The generic shape data
    private List<UnitPoint> points;
}