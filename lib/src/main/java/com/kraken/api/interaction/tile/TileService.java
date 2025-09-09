package com.kraken.api.interaction.tile;

import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Perspective;
import net.runelite.api.Tile;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import static net.runelite.api.Constants.CHUNK_SIZE;

@Slf4j
@Singleton
public class TileService extends AbstractService {
    
    /**
     * This method retrieves the tile at the specified coordinates (x, y) on the current plane.
     * It first creates a WorldPoint for the given coordinates and checks if the point is within
     * the scene using the `isInScene` method. If the WorldPoint is valid and within the scene,
     * it converts the WorldPoint to a LocalPoint, then retrieves and returns the corresponding
     * Tile from the game scene.
     *
     * If the WorldPoint is out of bounds or the LocalPoint is null, the method returns null
     * to indicate that no valid tile is found at the given coordinates.
     *
     * @param x The x-coordinate of the tile.
     * @param y The y-coordinate of the tile.
     * @return The Tile at the specified coordinates, or null if the tile is invalid or not in the scene.
     */
    public  Tile getTile(int x, int y) {
        WorldPoint worldPoint = new WorldPoint(x, y, client.getTopLevelWorldView().getPlane());
        LocalPoint localPoint;

        if (client.getTopLevelWorldView().getScene().isInstance()) {
            localPoint = fromWorldInstance(worldPoint);
        } else {
            localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
        }

        if (localPoint == null) return null;
        return client.getTopLevelWorldView().getScene().getTiles()[worldPoint.getPlane()][localPoint.getSceneX()][localPoint.getSceneY()];
    }

    /**
     * Used to convert a WorldPoint in an instance to a LocalPoint
     * @param worldPoint
     * @return
     */
    public LocalPoint fromWorldInstance(WorldPoint worldPoint)
    {
        int[][][] instanceTemplateChunks = client.getTopLevelWorldView().getInstanceTemplateChunks();
        // Extract the coordinates from the WorldPoint
        int worldX = worldPoint.getX();
        int worldY = worldPoint.getY();
        int worldPlane = client.getTopLevelWorldView().getPlane();

        // Loop through all chunks to find which one contains the world point
        for (int chunkX = 0; chunkX < instanceTemplateChunks[worldPlane].length; chunkX++)
        {
            for (int chunkY = 0; chunkY < instanceTemplateChunks[worldPlane][chunkX].length; chunkY++)
            {
                // Get the template chunk at this chunk position
                int templateChunk = instanceTemplateChunks[worldPlane][chunkX][chunkY];

                // Extract rotation, template chunk coordinates, and plane
                int rotation = (templateChunk >> 1) & 0x3;
                int templateChunkY = (templateChunk >> 3 & 0x7FF) * CHUNK_SIZE;
                int templateChunkX = (templateChunk >> 14 & 0x3FF) * CHUNK_SIZE;
                int templateChunkPlane = (templateChunk >> 24) & 0x3;

                // Check if the WorldPoint matches this chunk (after reversing rotation)
                WorldPoint rotatedWorldPoint = rotate(new WorldPoint(worldX, worldY, templateChunkPlane), rotation);

                if (rotatedWorldPoint.getX() >= templateChunkX && rotatedWorldPoint.getX() < templateChunkX + CHUNK_SIZE
                        && rotatedWorldPoint.getY() >= templateChunkY && rotatedWorldPoint.getY() < templateChunkY + CHUNK_SIZE)
                {
                    // Calculate local coordinates within the scene
                    int localX = (rotatedWorldPoint.getX() - templateChunkX) + (chunkX * CHUNK_SIZE);
                    int localY = (rotatedWorldPoint.getY() - templateChunkY) + (chunkY * CHUNK_SIZE);

                    // Return the corresponding LocalPoint
                    return  LocalPoint.fromScene(localX, localY, client.getTopLevelWorldView());
                }
            }
        }

        // Return null if no matching chunk is found
        return null;
    }

    /**
     * Rotate the coordinates in the chunk according to chunk rotation
     *
     * @param point point
     * @param rotation rotation
     * @return world point
     */
    public static WorldPoint rotate(WorldPoint point, int rotation)
    {
        int chunkX = point.getX() & ~(CHUNK_SIZE - 1);
        int chunkY = point.getY() & ~(CHUNK_SIZE - 1);
        int x = point.getX() & (CHUNK_SIZE - 1);
        int y = point.getY() & (CHUNK_SIZE - 1);
        switch (rotation)
        {
            case 1:
                return new WorldPoint(chunkX + y, chunkY + (CHUNK_SIZE - 1 - x), point.getPlane());
            case 2:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - x), chunkY + (CHUNK_SIZE - 1 - y), point.getPlane());
            case 3:
                return new WorldPoint(chunkX + (CHUNK_SIZE - 1 - y), chunkY + x, point.getPlane());
        }
        return point;
    }

    public static Integer worldToLocalDistance(int distance) {
        return distance * Perspective.LOCAL_TILE_SIZE;
    }
}
