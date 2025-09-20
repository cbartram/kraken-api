package com.kraken.api.sim.model;

import lombok.*;

import java.awt.*;

/**
 * NPC entity class
 */
@Data
@RequiredArgsConstructor
public class SimNpc {
    @NonNull
    private Point position;

    @NonNull
    private Color color;

    @NonNull
    private String name;

    private boolean aggressive = true;
    private int size = 1;
    private int aggressionRadius = 10;
    private int wanderRadius = 10;
    private Point target;
}

