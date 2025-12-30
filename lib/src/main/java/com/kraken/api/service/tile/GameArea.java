package com.kraken.api.service.tile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Encapsulates a set of tiles representing an area.
 * Provides helper methods for checking containment, retrieval, and visualization.
 */
@RequiredArgsConstructor
public class GameArea {

    @Getter
    private final Set<WorldPoint> tiles;
    private final WorldPoint centerRef;

    /**
     * Checks if the given world point is inside this area.
     * @param point The world point to check
     * @return true if the world point is contained within the set of tiles and false otherwise.
     */
    public boolean contains(WorldPoint point) {
        return tiles.contains(point);
    }

    /**
     * Returns a random tile from the area.
     * @return A random tile within the set of tiles
     */
    public WorldPoint getRandomTile() {
        if (tiles.isEmpty()) return null;
        int size = tiles.size();
        int item = new Random().nextInt(size);
        int i = 0;
        for (WorldPoint obj : tiles) {
            if (i == item) return obj;
            i++;
        }
        return null;
    }

    /**
     * Combines this area with another (Union).
     * @param other Another game area to check the union of
     * @return GameArea the union tiles of the two game areas (the tiles that overlap).
     */
    public GameArea union(GameArea other) {
        Set<WorldPoint> newSet = new HashSet<>(this.tiles);
        newSet.addAll(other.getTiles());
        return new GameArea(newSet, this.centerRef);
    }

    /**
     * Removes the tiles of another area from this one (Difference).
     * @param other Another game area to check
     * @return GameArea the tiles which are not in the other game area
     */
    public GameArea subtract(GameArea other) {
        Set<WorldPoint> newSet = new HashSet<>(this.tiles);
        newSet.removeAll(other.getTiles());
        return new GameArea(newSet, this.centerRef);
    }

    /**
     * Visualizes the area on the game screen.
     * This should be called from a RuneLite Overlay's {@code render()} method.
     * @param client an instance of the game client
     * @param graphics The graphics context
     * @param color The fill color (alpha is handled automatically if needed, but best to pass a translucent color)
     * @param outline Whether to draw just the outline or fill the tiles
     */
    public void render(Client client, Graphics2D graphics, Color color, boolean outline) {
        if (tiles.isEmpty()) return;

        // Don't render if we are on a different plane
        if (client.getTopLevelWorldView().getPlane() != tiles.iterator().next().getPlane()) return;

        for (WorldPoint wp : tiles) {
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp == null) continue;

            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly != null) {
                graphics.setColor(color);
                if (outline) {
                    graphics.draw(poly);
                } else {
                    graphics.fill(poly);
                }
            }
        }
    }

    /**
     * Renders the area on the minimap. This should be called from a RuneLite Overlay's {@code render()} method
     * @param client an instance of the game client
     * @param graphics The graphics context
     * @param color The fill color (alpha is handled automatically if needed, but best to pass a translucent color)
     */
    public void renderMinimap(Client client, Graphics2D graphics, Color color) {
        if (client == null || tiles.isEmpty()) return;

        graphics.setColor(color);
        for (WorldPoint wp : tiles) {
            LocalPoint lp = LocalPoint.fromWorld(client, wp);
            if (lp == null) continue;

            net.runelite.api.Point mini = Perspective.localToMinimap(client, lp);
            if (mini != null) {
                graphics.fillOval(mini.getX(), mini.getY(), 4, 4);
            }
        }
    }
}