package com.kraken.api.sim;

import com.google.gson.Gson;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.map.WorldPointService;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.RuneLite;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles collision data dumping, loading, and conversion between different formats.
 * Supports both in-game data collection and file-based operations.
 */
@Slf4j
@Singleton
public class LocalCollisionMap {

    private static final Gson gson = new Gson();
    private static final Int2IntOpenHashMap collisionMap = new Int2IntOpenHashMap();
    private static boolean loaded = false;

    /**
     * Collects collision data from the game building a mapping between 2 integers representing
     * the packed WorldPoint as the key and the collision flags as the value.
     */
    public static void load() {
        if(loaded) return;
        Context ctx = RuneLite.getInjector().getInstance(Context.class);
        WorldView wv = ctx.players().local().raw().getWorldView();
        if(wv.getCollisionMaps() == null || wv.getCollisionMaps()[wv.getPlane()] == null)
            return;

        int[][] flags = wv.getCollisionMaps()[wv.getPlane()].getFlags();
        WorldPoint point;
        for(int x = 0; x < flags.length; x++) {
            for(int y = 0; y < flags[x].length; y++) {
                point = WorldPoint.fromScene(wv, x, y, wv.getPlane());
                collisionMap.put(WorldPointService.pack(point.getX(), point.getY(), wv.getPlane()), flags[x][y]);
            }
        }

        loaded = true;
    }

    /**
     * Saves collision map to a JSON file.
     *
     * @param filePath Path to the output file
     * @return true if successful, false otherwise
     */
    public static boolean saveToFile(String filePath) {
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(collisionMap, writer);
            log.info("Saved collision map to {}", filePath);
            return true;
        } catch (IOException e) {
            log.error("Failed to save collision map to file: {}", filePath, e);
            return false;
        }
    }

    /**
     * Loads a collision map from a JSON file.
     *
     * @param filePath Path to the input file
     * @return CollisionMap, or empty map if failed
     */
    public static Int2IntOpenHashMap loadFromFile(String filePath) {
        try (FileReader reader = new FileReader(filePath)) {
            Int2IntOpenHashMap map = gson.fromJson(reader, Int2IntOpenHashMap.class);
            if (map != null) {
                return map;
            }
        } catch (IOException e) {
            log.error("Failed to load collision map from file: {}", filePath, e);
        }

        return null;
    }

    /**
     * Retrieves the collision flags for a given packed world point.
     * <p>
     * The method checks if the provided {@code packedWorldPoint} exists in the
     * internal collision map and returns the associated collision flags.
     * If the point is not found in the map, the method returns {@code -1}.
     * </p>
     *
     * @param packedWorldPoint The packed representation of a {@literal @}WorldPoint.
     *                         This integer combines the x, y, and z coordinates of
     *                         a {@literal @}WorldPoint into a single value for use
     *                         as a key in the collision map.
     * @return The collision flags associated with the given {@code packedWorldPoint}.
     *         Returns {@code -1} if no collision data is available.
     */
    public static int getCollisionFlags(int packedWorldPoint) {
        if(collisionMap.containsKey(packedWorldPoint)) {
            return collisionMap.get(packedWorldPoint);
        }

        return -1;
    }

    /**
     * Retrieves the collision flag for a given {@literal @}WorldPoint.
     * <p>
     * This method uses the provided {@literal @}WorldPoint object to compute
     * a packed integer representation of the point, which serves as the key
     * to query the collision data stored in the internal map.
     * </p>
     *
     * @param point The {@literal @}WorldPoint to check for collision data.
     *              It represents a specific location in the game world.
     * @return The collision flag associated with the given {@literal @}WorldPoint,
     *         or {@code -1} if no collision data exists for the specified point.
     */
    public static int getCollisionFor(WorldPoint point) {
        int packed = WorldPointService.pack(point);
        return getCollisionFlags(packed);
    }

    /**
     * Retrieves the collision flag for a given set of coordinates.
     * <p>
     * This method takes three integer parameters {@code x}, {@code y},
     * and {@code z}, representing the coordinates of a {@literal @}WorldPoint
     * in the game world. It converts these values into a {@literal @}WorldPoint
     * object and delegates the collision flag retrieval to the overloaded
     * {@link #getCollisionFor(WorldPoint)} method.
     * </p>
     *
     * @param x The x-coordinate of the {@literal @}WorldPoint.
     * @param y The y-coordinate of the {@literal @}WorldPoint.
     * @param z The z-level (plane) of the {@literal @}WorldPoint.
     * @return The collision flag associated with the given coordinates,
     *         or {@code -1} if no collision data exists for the specified point.
     */
    public static int getCollisionFor(int x, int y, int z) {
        return getCollisionFor(new WorldPoint(x, y, z));
    }
}