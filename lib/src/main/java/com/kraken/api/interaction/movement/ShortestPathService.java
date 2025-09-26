package com.kraken.api.interaction.movement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import shortestpath.WorldPointUtil;
import shortestpath.pathfinder.Pathfinder;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides programmatic access to the ShortestPathPlugin's pathfinding.
 * Lets you set a target and retrieve the computed path as a list of WorldPoints.
 */
@Slf4j
@Singleton
public class ShortestPathService {
    private final Client client;
    private final ClientThread clientThread;
    private Pathfinder pathfinder;

    @Inject
    public ShortestPathService(Client client, ClientThread clientThread) {
        this.client = client;
        this.clientThread = clientThread;
    }

    /**
     * Programmatically set a pathfinding target.
     * @param target the target WorldPoint
     */
    public void setTarget(WorldPoint target) {
        if (target == null) {
            restartPathfinding(
                    WorldPointUtil.UNDEFINED,
                    new HashSet<>()
            );
            return;
        }

        Player local = client.getLocalPlayer();
        if (local == null) {
            return;
        }

        int start = WorldPointUtil.fromLocalInstance(client, local.getLocalLocation());
        int packedTarget = WorldPointUtil.packWorldPoint(target);
        Set<Integer> targets = new HashSet<>();
        targets.add(packedTarget);

        restartPathfinding(start, targets);
    }

    /**
     * Cancels the current path.
     */
    public void cancel() {
        setTarget(null);
    }

    /**
     * Restarts the current pathfinding with a new start position and end positions
     * @param start The packed start position
     * @param ends The packet end positions
     */
    public void restartPathfinding(int start, Set<Integer> ends) {
        clientThread.invokeLater(() -> {
            this.pathfinder = new Pathfinder(start, ends);
            this.pathfinder.run();
        });
    }

    /**
     * Get the current computed path as WorldPoints.
     * Returns empty list if no path is found or not yet computed.
     * @return The list of world points representing the currently calculated path
     */
    public List<WorldPoint> getCurrentPath() {
        if (pathfinder == null || pathfinder.getPath() == null) {
            return new ArrayList<>();
        }

        List<WorldPoint> result = new ArrayList<>();
        for (int packed : pathfinder.getPath()) {
            result.add(WorldPointUtil.unpackWorldPoint(packed));
        }
        return result;
    }

    /**
     * Returns whether a path is currently available.
     * @return True if a path is available and false otherwise
     */
    public boolean hasPath() {
        return pathfinder != null
                && pathfinder.getPath() != null
                && !pathfinder.getPath().isEmpty();
    }
}