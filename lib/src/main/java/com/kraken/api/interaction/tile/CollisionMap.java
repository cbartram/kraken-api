package com.kraken.api.interaction.tile;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Collision map data container with all necessary information.
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class CollisionMap {
    private int[][] data;
    private int minX, minY, maxX, maxY;
    private int playerX, playerY;
    private int plane;

    public int getWidth() {
        return maxX - minX + 1;
    }

    public int getHeight() {
        return maxY - minY + 1;
    }

    public boolean isEmpty() {
        return data.length == 0;
    }

    /**
     * Gets collision flags at array coordinates.
     *
     * @param x Array x coordinate
     * @param y Array y coordinate
     * @return Collision flags, or 0 if out of bounds
     */
    public int getCollisionAt(int x, int y) {
        if (y >= 0 && y < data.length && x >= 0 && x < data[0].length) {
            return data[y][x];
        }
        return 0;
    }

    /**
     * Gets collision flags at world coordinates.
     *
     * @param worldX World x coordinate
     * @param worldY World y coordinate
     * @return Collision flags, or 0 if out of bounds
     */
    public int getCollisionAtWorld(int worldX, int worldY) {
        int localX = worldX - minX;
        int localY = worldY - minY;
        int flippedY = getHeight() - 1 - localY;
        return getCollisionAt(localX, flippedY);
    }

    /**
     * Converts world coordinates to array coordinates.
     *
     * @param worldX World x coordinate
     * @param worldY World y coordinate
     * @return Array coordinates as [x, y], or null if out of bounds
     */
    public int[] worldToArray(int worldX, int worldY) {
        int localX = worldX - minX;
        int localY = worldY - minY;
        int flippedY = getHeight() - 1 - localY;

        if (localX >= 0 && localX < getWidth() && flippedY >= 0 && flippedY < getHeight()) {
            return new int[]{localX, flippedY};
        }
        return null;
    }

    /**
     * Converts array coordinates to world coordinates.
     *
     * @param arrayX Array x coordinate
     * @param arrayY Array y coordinate
     * @return World coordinates as [x, y], or null if out of bounds
     */
    public int[] arrayToWorld(int arrayX, int arrayY) {
        if (arrayX >= 0 && arrayX < getWidth() && arrayY >= 0 && arrayY < getHeight()) {
            int worldX = arrayX + minX;
            int worldY = (getHeight() - 1 - arrayY) + minY;
            return new int[]{worldX, worldY};
        }
        return null;
    }
}