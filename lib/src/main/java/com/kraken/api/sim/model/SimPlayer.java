package com.kraken.api.sim.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A model of a player within a simulation.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SimPlayer {

    @NonNull
    private Point position;

    @NonNull
    private Integer attackRange;

    @NonNull
    private Boolean runEnabled;

    @NonNull
    private Integer specRemaining;

    @NonNull
    private AttackStyle attackStyle;

    private Integer pathIndex = 0;
    private List<Point> currentPath = new ArrayList<>();
}
