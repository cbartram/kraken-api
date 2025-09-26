package com.kraken.api.sim.model;

import com.kraken.api.interaction.tile.CollisionMap;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.awt.*;
import java.util.List;

/**
 * Represents a snapshot of the game state at a specific point in time.
 * Used for implementing time travel functionality (prev/next tick).
 */
@Data
@Builder
@AllArgsConstructor
public class GameState {

    /**
     * The tick number when this state was captured
     */
    public final int tick;

    /**
     * The player's position at this state
     */
    public final Point playerPosition;

    /**
     * The player's current path at this state
     */
    public final List<Point> playerPath;

    /**
     * The player's path index at this state
     */
    public final int playerPathIndex;

    /**
     * The positions of all NPCs at this state
     */
    public final List<Point> npcPositions;
}