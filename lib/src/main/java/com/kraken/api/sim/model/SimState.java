package com.kraken.api.sim.model;

import lombok.Builder;
import lombok.Data;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a frozen moment in the simulation.
 * This is the "Node" in your decision tree.
 */
@Data
@Builder(toBuilder = true)
public class SimState {
    private int tick;
    private SimPlayer player;
    private List<SimNpc> npcs;
    private double heuristicScore;

    /**
     * Creates a totally independent copy of the state.
     * Crucial for branching in decision trees.
     */
    public SimState deepCopy() {
        return SimState.builder()
                .tick(this.tick)
                // Assumes SimPlayer has a copy constructor or clone method
                .player(this.player.copy())
                // Assumes SimNpc has a copy constructor or clone method
                .npcs(this.npcs.stream().map(SimNpc::copy).collect(Collectors.toList()))
                .playerCurrentPath(new ArrayList<>(this.playerCurrentPath))
                .heuristicScore(this.heuristicScore)
                .build();
    }
}