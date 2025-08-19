package com.kraken.api.interaction.movement;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import shortestpath.ShortestPathPlugin;
import shortestpath.WorldPointUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Provides programmatic access to the ShortestPathPlugin's pathfinding.
 * Lets you set a target and retrieve the computed path as a list of WorldPoints.
 */
@Slf4j
public class ShortestPathService {
    private final Client client;
    private final ShortestPathPlugin shortestPathPlugin;

    @Inject
    public ShortestPathService(Client client, ShortestPathPlugin shortestPathPlugin) {
        this.client = client;
        this.shortestPathPlugin = shortestPathPlugin;
    }

    /**
     * Programmatically set a pathfinding target.
     * @param target the target WorldPoint
     */
    public void setTarget(WorldPoint target) {
        if (target == null) {
            shortestPathPlugin.restartPathfinding(
                    WorldPointUtil.UNDEFINED,
                    new HashSet<>(),
                    false
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

        shortestPathPlugin.restartPathfinding(start, targets, false);
    }

    /**
     * Get the current computed path as WorldPoints.
     * Returns empty list if no path is found or not yet computed.
     */
    public List<WorldPoint> getCurrentPath() {
        if (shortestPathPlugin.getPathfinder() == null
                || shortestPathPlugin.getPathfinder().getPath() == null) {
            return new ArrayList<>();
        }

        List<WorldPoint> result = new ArrayList<>();
        for (int packed : shortestPathPlugin.getPathfinder().getPath()) {
            result.add(WorldPointUtil.unpackWorldPoint(packed));
        }
        return result;
    }

    /**
     * Returns whether a path is currently available.
     */
    public boolean hasPath() {
        return shortestPathPlugin.getPathfinder() != null
                && shortestPathPlugin.getPathfinder().getPath() != null
                && !shortestPathPlugin.getPathfinder().getPath().isEmpty();
    }
}