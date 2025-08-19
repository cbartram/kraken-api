package com.kraken.api.interaction.movement;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Provides;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import shortestpath.ShortestPathConfig;
import shortestpath.WorldPointUtil;
import shortestpath.pathfinder.Pathfinder;
import shortestpath.pathfinder.PathfinderConfig;

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
    private final ClientThread clientThread;
    private final PathfinderConfig config;
    private Pathfinder pathfinder;

    @Inject
    public ShortestPathService(Client client, ClientThread clientThread, PathfinderConfig config) {
        this.client = client;
        this.clientThread = clientThread;
        this.config = config;
    }

    @Provides
    ShortestPathConfig provideShortestPathConfig(final ConfigManager configManager) {
        return configManager.getConfig(ShortestPathConfig.class);
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

    public void restartPathfinding(int start, Set<Integer> ends) {
        clientThread.invokeLater(() -> {
            this.pathfinder = new Pathfinder(config, start, ends);
            this.pathfinder.run();
        });
    }

    /**
     * Get the current computed path as WorldPoints.
     * Returns empty list if no path is found or not yet computed.
     */
    public List<WorldPoint> getCurrentPath() {
        if (pathfinder == null
                || pathfinder.getPath() == null) {
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
     */
    public boolean hasPath() {
        return pathfinder != null
                && pathfinder.getPath() != null
                && !pathfinder.getPath().isEmpty();
    }
}