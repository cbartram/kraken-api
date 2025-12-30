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
     */
    public boolean contains(WorldPoint point) {
        return tiles.contains(point);
    }

    /**
     * Returns a random tile from the area.
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
     */
    public GameArea union(GameArea other) {
        Set<WorldPoint> newSet = new HashSet<>(this.tiles);
        newSet.addAll(other.getTiles());
        return new GameArea(newSet, this.centerRef);
    }

    /**
     * Removes the tiles of another area from this one (Difference).
     */
    public GameArea subtract(GameArea other) {
        Set<WorldPoint> newSet = new HashSet<>(this.tiles);
        newSet.removeAll(other.getTiles());
        return new GameArea(newSet, this.centerRef);
    }

    /**
     * Visualizes the area on the game screen.
     * CALL THIS from your Overlay's render method.
     *
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
     * Renders the area on the minimap.
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