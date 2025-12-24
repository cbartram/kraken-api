package com.kraken.api.service.map;

import com.google.inject.Singleton;
import net.runelite.api.coords.WorldPoint;

/**
 * A utility class for compressing and decompressing WorldPoint objects into and from integers.
 */
@Singleton
public class WorldPointService {

    /**
     * Compresses a WorldPoint into a single integer.
     * @param wp the WorldPoint to compress
     * @return the compressed WorldPoint
     */
    public static int pack(WorldPoint wp) {
        return wp.getX() | wp.getY() << 14 | wp.getPlane() << 29;
    }

    /**
     * Compresses x, y, z coordinates into a single integer.
     * @param x the x coordinate (0 - 16383)
     * @param y the y coordinate (0 - 32767)
     * @param z the z coordinate (0 - 7)
     * @return the compressed coordinates
     */
    public static int pack(int x, int y, int z) {
        return x | y << 14 | z << 29;
    }

    /**
     * Decompresses a compressed WorldPoint integer back into a WorldPoint.
     * @param packed the packed WorldPoint
     * @return the unpacked WorldPoint
     */
    public static WorldPoint fromPacked(int packed) {
        int x = packed & 0x3FFF;
        int y = (packed >>> 14) & 0x7FFF;
        int z = (packed >>> 29) & 7;
        return new WorldPoint(x, y, z);
    }

    /**
     * Overloaded method wrapping {@code fromPacked}.
     * @param packed The compressed WorldPoint
     * @return the unpacked WorldPoint
     */
    public static WorldPoint unpack(int packed) {
        return fromPacked(packed);
    }

    /**
     * Extracts the X coordinate from a compressed WorldPoint integer.
     * @param packed the compressed WorldPoint
     * @return the X coordinate
     */
    public static short getPackedX(int packed) {
        return (short) (packed & 0x3FFF);
    }

    /**
     * Extracts the Y coordinate from a compressed WorldPoint integer.
     * @param packed the compressed WorldPoint
     * @return the Y coordinate
     */
    public static short getPackedY(int packed) {
        return (short) ((packed >>> 14) & 0x7FFF);
    }

    /**
     * Extracts the plane from a compressed WorldPoint integer.
     * @param packed the compressed WorldPoint
     * @return the plane
     */
    public static byte getPackedPlane(int packed) {
        return (byte)((packed >>> 29) & 7);
    }

    /**
     * Offsets the compressed WorldPoint by the given amounts in each dimension.
     * @param packed the compressed WorldPoint
     * @param offset the amount to offset in the X direction
     * @return the new compressed WorldPoint
     */
    public static int dx(int packed, int offset) {
        return packed + offset;
    }

    /**
     * Offsets the compressed WorldPoint by the given amounts in each dimension.
     * @param packed the compressed WorldPoint
     * @param offset the amount to offset in the Y direction
     * @return the new compressed WorldPoint
     */
    public static int dy(int packed, int offset) {
        return packed + (offset << 14);
    }

    /**
     * Offsets the compressed WorldPoint by the given amounts in each dimension.
     * @param packed the compressed WorldPoint
     * @param offsetX the amount to offset in the X direction
     * @param offsetY the amount to offset in the Y direction
     * @return the new compressed WorldPoint
     */
    public static int dxy(int packed, int offsetX, int offsetY) {
        return packed + offsetX + (offsetY << 14);
    }
}
