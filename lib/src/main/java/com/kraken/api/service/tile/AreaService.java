package com.kraken.api.service.tile;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

import java.awt.*;
import java.util.*;

@Slf4j
@Singleton
public class AreaService {

    @Inject
    private Client client;

    @Inject
    private TileService tileService; // Assuming your BFS code is in TileService

    /**
     * Creates a square area around a center point.
     */
    public GameArea createAreaFromRadius(WorldPoint center, int radius) {
        Set<WorldPoint> points = new HashSet<>();
        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                points.add(center.dx(x).dy(y));
            }
        }
        return new GameArea(points, center);
    }

    /**
     * Creates an area based on movement reachability (BFS).
     * Uses your existing TileService logic.
     */
    public GameArea createReachableArea(WorldPoint center, int range, boolean ignoreCollision) {
        Map<WorldPoint, Integer> reachableMap = tileService.getReachableTilesFromTile(center, range, ignoreCollision);
        return new GameArea(reachableMap.keySet(), center);
    }

    /**
     * Creates a complex shape (e.g., L-shape) from vertices.
     * This rasterizes the polygon: it finds all discrete tiles inside the shape.
     */
    public GameArea createPolygonArea(WorldPoint... vertices) {
        if (vertices.length < 3) {
            log.warn("Polygon area requires at least 3 vertices");
            return new GameArea(Collections.emptySet(), vertices[0]);
        }

        // 1. Convert WorldPoints to a Java AWT Polygon for math
        Polygon polygon = new Polygon();
        int plane = vertices[0].getPlane();
        int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
        int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;

        for (WorldPoint wp : vertices) {
            polygon.addPoint(wp.getX(), wp.getY());
            // Track bounds to optimize the loop
            if (wp.getX() < minX) minX = wp.getX();
            if (wp.getX() > maxX) maxX = wp.getX();
            if (wp.getY() < minY) minY = wp.getY();
            if (wp.getY() > maxY) maxY = wp.getY();
        }

        // 2. Iterate through the bounding box and check which tiles are inside
        Set<WorldPoint> points = new HashSet<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                if (polygon.contains(x, y)) {
                    points.add(new WorldPoint(x, y, plane));
                }
            }
        }

        // Use the first vertex as a reference center, or calculate geometric center if needed
        return new GameArea(points, vertices[0]);
    }

    /**
     * Creates an area from a raw collection of points.
     */
    public GameArea createFromPoints(Collection<WorldPoint> points) {
        return new GameArea(new HashSet<>(points), points.stream().findFirst().orElse(null));
    }
}
