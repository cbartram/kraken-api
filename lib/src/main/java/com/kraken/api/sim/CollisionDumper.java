package com.kraken.api.sim;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.npc.NpcService;
import com.kraken.api.sim.model.AttackStyle;
import com.kraken.api.sim.model.SimNpc;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.NPC;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;

import java.awt.*;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles collision data dumping, loading, and conversion between different formats.
 * Supports both in-game data collection and file-based operations.
 */
@Slf4j
@Singleton
public class CollisionDumper {

    @Inject
    private Gson gson;

    @Inject
    private NpcService npcService;

    @Inject
    private Context context;

    /**
     * Collects collision data from the game within default distance (104 tiles).
     *
     * @return CollisionMap containing the collision data
     */
    public CollisionMap collect() {
        return collect(104);
    }

    /**
     * Collects collision data from the game within specified distance.
     * Builds the collision map directly without intermediate storage.
     *
     * @param distance Maximum distance from player to collect data
     * @return CollisionMap containing the collision data
     */
    public CollisionMap collect(int distance) {
        Client client = RuneLite.getInjector().getInstance(Client.class);
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        List<SimNpc> simNpcs = new ArrayList<>();
        Map<WorldPoint, NPC> npcs = npcService.getNpcs()
                .collect(Collectors.toMap(NPC::getWorldLocation, Function.identity()));
        final HashMap<WorldPoint, Integer> tileDistances = new HashMap<>();
        tileDistances.put(playerLocation, 0);

        // Track bounds as we go
        int minX = playerLocation.getX();
        int minY = playerLocation.getY();
        int maxX = playerLocation.getX();
        int maxY = playerLocation.getY();

        // Store collision flags by world coordinates
        HashMap<WorldPoint, Integer> collisionFlags = new HashMap<>();

        // Breadth-first search to collect collision data
        for (int i = 0; i <= distance; i++) {
            int currentDistance = i;

            for (var entry : tileDistances.entrySet().stream()
                    .filter(x -> x.getValue() == currentDistance)
                    .collect(Collectors.toList())) {

                WorldPoint point = entry.getKey();
                LocalPoint localPoint = getLocalPoint(client, point);

                if (localPoint != null) {
                    CollisionData collisionData = getCollisionData(client);
                    if (collisionData != null) {
                        int flags = getCollisionFlags(collisionData, localPoint);
                        Set<MovementFlag> movementFlags = MovementFlag.getSetFlags(flags);

                        // Store the collision data
                        collisionFlags.put(point, flags);

                        // Update bounds
                        minX = Math.min(minX, point.getX());
                        minY = Math.min(minY, point.getY());
                        maxX = Math.max(maxX, point.getX());
                        maxY = Math.max(maxY, point.getY());

                        // Add adjacent tiles for next iteration
                        if (entry.getValue() < distance) {
                            addAdjacentTiles(point, movementFlags, tileDistances, currentDistance + 1);
                        }
                    }
                }
            }
        }

        // Build the collision map array
        int width = maxX - minX + 1;
        int height = maxY - minY + 1;
        int[][] collisionArray = new int[height][width];

        // Fill the array with collision data
        for (var entry : collisionFlags.entrySet()) {
            WorldPoint point = entry.getKey();
            NPC potentialNpc = npcs.get(point);
            int localX = point.getX() - minX;
            int localY = point.getY() - minY;
            int flippedY = height - 1 - localY; // Flip Y coordinate for array indexing

            if(potentialNpc != null) {
                String name = context.runOnClientThread(potentialNpc::getName);
                // TODO Attack range doesn't seem to return the correct value
                int attackRange = context.runOnClientThread(() -> potentialNpc.getComposition().getIntValue(13));
                int attackSpeed = context.runOnClientThread(() -> potentialNpc.getComposition().getIntValue(14));

                log.info("Adding NPC: {}, Attack Range = {}, Attack Speed = {}", name, attackRange, attackSpeed);

                SimNpc s = new SimNpc(worldToSim(point, minX, minY, width, height), Color.WHITE, name);
                s.setSize(potentialNpc.getComposition().getSize());
                s.setCanPathfind(false);
                s.setAggressive(false);
                s.setAttackRange(attackRange == 0 ? 1 : attackRange);
                s.setAttackSpeed(attackSpeed);
                s.setAttackStyle(attackRange > 1 ? AttackStyle.RANGE : AttackStyle.MELEE);
                simNpcs.add(s);
            }

            collisionArray[flippedY][localX] = entry.getValue();
        }

        // Calculate player position in the array
        int playerLocalX = playerLocation.getX() - minX;
        int playerLocalY = playerLocation.getY() - minY;
        int playerFlippedY = height - 1 - playerLocalY;

        log.info("Collected collision data for {} tiles. Bounds: X[{},{}] Y[{},{}]. Player at: ({},{}), NPCs: {}",
                collisionFlags.size(), minX, maxX, minY, maxY, playerLocalX, playerFlippedY, simNpcs.size());

        return new CollisionMap(collisionArray, simNpcs, minX, minY, maxX, maxY,
                playerLocalX, playerFlippedY, playerLocation.getPlane());
    }

    /**
     * Converts world coordinates to simulation coordinates.
     * @param point The world point to convert
     * @param minX The minimum x value of all points in the currently loaded scene
     * @param minY The minimum y value of all points in the currently loaded scene
     * @param width The width of the scene
     * @param height The height of the scene
     * @return Array coordinates as [x, y], or null if out of bounds
     */
    public Point worldToSim(WorldPoint point, int minX, int minY, int width, int height) {
        int localX = point.getX() - minX;
        int localY = point.getY() - minY;
        int flippedY = height - 1 - localY;
        if (localX >= 0 && localX < width && flippedY >= 0 && flippedY < height) {
            return new Point(localX, flippedY);
        }

        return new Point(1, 1);
    }


    /**
     * Saves collision map to a JSON file.
     *
     * @param collisionMap The collision map to save
     * @param filePath Path to the output file
     * @return true if successful, false otherwise
     */
    public boolean saveToFile(CollisionMap collisionMap, String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(collisionMap, writer);
            log.info("Saved collision map ({}x{}) to {}",
                    collisionMap.getWidth(), collisionMap.getHeight(), filePath);
            return true;
        } catch (IOException e) {
            log.error("Failed to save collision map to file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Loads collision map from a JSON file.
     *
     * @param filePath Path to the input file
     * @return CollisionMap, or empty map if failed
     */
    public CollisionMap loadFromFile(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            CollisionMap map = gson.fromJson(reader, CollisionMap.class);

            if (map != null && map.getData() != null) {
                log.info("Loaded collision map ({}x{}) from {}",
                        map.getWidth(), map.getHeight(), filePath);
                return map;
            }
        } catch (IOException e) {
            log.error("Failed to load collision map from file: {}", filePath, e);
        }

        return new CollisionMap();
    }

    /**
     * Convenience method to collect from game and save to file.
     *
     * @param filePath Path to the output file
     * @param distance Maximum distance from player to collect data
     * @return true if successful, false otherwise
     */
    public boolean collectAndSave(String filePath, int distance) {
        CollisionMap map = collect(distance);
        return saveToFile(map, filePath);
    }

    /**
     * Convenience method to collect from game and save to file with default distance.
     *
     * @param filePath Path to the output file
     * @return true if successful, false otherwise
     */
    public boolean collectAndSave(String filePath) {
        return collectAndSave(filePath, 104);
    }

    private LocalPoint getLocalPoint(Client client, WorldPoint point) {
        if (client.getTopLevelWorldView().isInstance()) {
            WorldPoint worldPoint = WorldPoint.toLocalInstance(client.getTopLevelWorldView(), point)
                    .stream()
                    .findFirst()
                    .orElse(null);
            if (worldPoint == null) return null;
            return LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
        } else {
            return LocalPoint.fromWorld(client.getTopLevelWorldView(), point);
        }
    }

    private CollisionData getCollisionData(Client client) {
        CollisionData[] collisionMap = client.getTopLevelWorldView().getCollisionMaps();
        if (collisionMap != null) {
            return collisionMap[client.getTopLevelWorldView().getPlane()];
        }
        return null;
    }

    private int getCollisionFlags(CollisionData collisionData, LocalPoint localPoint) {
        int[][] flags = collisionData.getFlags();
        return flags[localPoint.getSceneX()][localPoint.getSceneY()];
    }

    private void addAdjacentTiles(WorldPoint point, Set<MovementFlag> movementFlags,
                                  HashMap<WorldPoint, Integer> tileDistances, int distance) {
        if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_EAST))
            tileDistances.putIfAbsent(point.dx(1), distance);
        if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_WEST))
            tileDistances.putIfAbsent(point.dx(-1), distance);
        if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_NORTH))
            tileDistances.putIfAbsent(point.dy(1), distance);
        if (!movementFlags.contains(MovementFlag.BLOCK_MOVEMENT_SOUTH))
            tileDistances.putIfAbsent(point.dy(-1), distance);
    }
}