package com.kraken.api.sim;

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
    private Point target;
}

