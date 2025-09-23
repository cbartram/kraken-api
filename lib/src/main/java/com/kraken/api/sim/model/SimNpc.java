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

    private int size = 1;
    private AttackStyle attackStyle = AttackStyle.MELEE;
    private int attackRange = 1;
    private boolean canPathfind = false;
    private Point target;
}

