package com.kraken.api.sim;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.awt.Point;
import java.util.List;

/**
 * Represents a snapshot of the game state at a specific point in time.
 * Used for implementing time travel functionality (prev/next tick).
 */
@Data
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