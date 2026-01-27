package com.kraken.api.sim.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Actor;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

/**
 * A model of a player within a simulation.
 */
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class SimPlayer implements SimActor {

    @NonNull
    private Player player;

    @Override
    public WorldPoint getLocation() {
        return player.getWorldLocation();
    }

    @Override
    public WorldArea getWorldArea() {
        return new WorldArea(player.getWorldLocation(), 1, 1);
    }

    @Override
    public Actor copy() {
        return player;
    }
}
