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

    private String name = "Unknown";
    private int size = 1;
    private AttackStyle attackStyle = AttackStyle.MELEE;
    private int attackRange = 1;
    private boolean canPathfind = false;
    private Point target;

    public SimNpc(@NonNull Point position, @NonNull Color color, String name) {
        this.position = position;
        this.color = color;
        this.name = name;

        if(this.name == null || this.name.isEmpty()) {
            this.name = "Unknown";
        }
    }
}

