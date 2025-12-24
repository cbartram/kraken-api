package com.kraken.api.service.map;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;

import java.awt.*;

@Slf4j
@Singleton
public class WorldMapService {
    
    @Inject
    private Context ctx;

    /**
     * Converts the screen-space click coordinates on the world map interface to a corresponding
     * {@code WorldPoint} in the game world.
     *
     * <p>This method takes the x and y coordinates of a mouse click and determines the equivalent
     * world map coordinates by accounting for the map's zoom level, current position, and bounds.
     * If the click is outside the map's bounds or the world map is unavailable, the method returns {@code null}.
     *
     * @param clickX the x-coordinate of the mouse click on the world map interface
     * @param clickY the y-coordinate of the mouse click on the world map interface
     * @return the {@code WorldPoint} corresponding to the clicked coordinates, or {@code null} if the
     *         conversion cannot be performed
     */
    public WorldPoint mapClickToWorldPoint(int clickX, int clickY) {
        Client client = ctx.getClient();
        WorldMap worldMap = client.getWorldMap();
        if (worldMap == null) {
            return null;
        }

        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (mapWidget == null) {
            return null;
        }

        float zoom = worldMap.getWorldMapZoom();
        Point mapWorldPosition = worldMap.getWorldMapPosition();
        Rectangle mapBounds = mapWidget.getBounds();

        if(!mapBounds.contains(clickX, clickY)) {
            return null;
        }

        WorldPoint mapCenterWorldPoint = new WorldPoint(mapWorldPosition.getX(), mapWorldPosition.getY(), 0);
        Integer centerScreenX = worldPointToMapPointX(mapCenterWorldPoint);
        Integer centerScreenY = worldPointToMapPointY(mapCenterWorldPoint);

        if (centerScreenX == null || centerScreenY == null) {
            return null;
        }

        int deltaX = clickX - centerScreenX;
        int deltaY = clickY - centerScreenY;
        int wDeltaX = (int) (deltaX / zoom);
        int wDeltaY = (int) (-deltaY / zoom);

        return new WorldPoint(mapWorldPosition.getX() + wDeltaX, mapWorldPosition.getY() + wDeltaY, 0);
    }

    /**
     * Maps the click coordinates on the world map to a {@code WorldPoint} in the game world,
     * with a specified plane (altitude).
     *
     * <p>This method converts user interaction on the world map interface (e.g., clicking)
     * into corresponding game world coordinates while ensuring the plane value is constrained
     * between valid ranges (0 to 3). If the conversion fails (e.g., invalid input or map state),
     * {@code null} is returned.
     *
     * @param clickX the x-coordinate of the mouse click on the world map interface
     * @param clickY the y-coordinate of the mouse click on the world map interface
     * @param plane  the desired plane (altitude) for the resulting {@code WorldPoint};
     *               values outside the range [0, 3] will be clamped
     *
     * @return the {@code WorldPoint} corresponding to the click coordinates and specified plane,
     *         or {@code null} if the conversion fails
     */
    public WorldPoint mapClickToWorldPoint(int clickX, int clickY, int plane) {
        WorldPoint basePoint = mapClickToWorldPoint(clickX, clickY);
        if (basePoint == null) {
            return null;
        }

        int valid = Math.max(0, Math.min(3, plane));
        return new WorldPoint(basePoint.getX(), basePoint.getY(), valid);
    }

    /**
     * Converts a {@code WorldPoint} to its corresponding screen X coordinate on the world map.
     *
     * <p>This method calculates the X coordinate of a given {@code WorldPoint} relative to the
     * world map interface, considering the map's zoom level, current position, and bounds.
     * If the world map or any related components are unavailable, the method returns {@code null}.
     *
     * @param worldPoint the {@code WorldPoint} to convert; must not be {@code null}
     * @return the screen X coordinate on the world map, or {@code null} if the conversion cannot
     *         be performed
     */
    public Integer worldPointToMapPointX(WorldPoint worldPoint) {
        Client client = ctx.getClient();
        if (worldPoint == null) {
            return null;
        }

        WorldMap worldMap = client.getWorldMap();
        if (worldMap == null) {
            return null;
        }

        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (mapWidget == null) {
            return null;
        }

        float ppT = worldMap.getWorldMapZoom();
        Rectangle worldMapRect = mapWidget.getBounds();
        Point worldMapPosition = worldMap.getWorldMapPosition();

        int widthInTiles = (int) Math.ceil(worldMapRect.getWidth() / ppT);
        int xTileOffset = worldPoint.getX() + widthInTiles / 2 - worldMapPosition.getX();

        int xGraphDiff = (int) (xTileOffset * ppT);
        xGraphDiff += (int) (ppT - Math.ceil(ppT / 2));
        xGraphDiff += (int) worldMapRect.getX();

        return xGraphDiff;
    }

    /**
     * Converts a {@code WorldPoint} in the game world to its corresponding screen Y coordinate
     * on the world map interface.
     *
     * <p>This method calculates the screen Y position of the given {@code WorldPoint} in relation
     * to the world map widget, taking into account the map's zoom level, current position, and
     * widget bounds. If the {@code WorldPoint} or world map components are unavailable, the
     * method returns {@code null}.
     *
     * @param worldPoint the {@code WorldPoint} to convert; must not be {@code null}.
     *                   If {@code null}, the method will return {@code null}.
     * @return the screen Y coordinate on the world map corresponding to the given {@code WorldPoint},
     *         or {@code null} if the conversion cannot be performed due to invalid input or unavailable
     *         map state.
     */
    public Integer worldPointToMapPointY(WorldPoint worldPoint) {
        Client client = ctx.getClient();
        if (worldPoint == null) {
            return null;
        }

        WorldMap worldMap = client.getWorldMap();
        if (worldMap == null) {
            return null;
        }

        Widget mapWidget = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if (mapWidget == null) {
            return null;
        }

        float pixelsPerTile = worldMap.getWorldMapZoom();
        Rectangle worldMapRect = mapWidget.getBounds();
        Point worldMapPosition = worldMap.getWorldMapPosition();

        int heightInTiles = (int) Math.ceil(worldMapRect.getHeight() / pixelsPerTile);
        int yTileMax = worldMapPosition.getY() - heightInTiles / 2;
        int yTileOffset = (yTileMax - worldPoint.getY() - 1) * -1;

        int yGraphDiff = (int) (yTileOffset * pixelsPerTile);
        yGraphDiff -= (int) (pixelsPerTile - Math.ceil(pixelsPerTile / 2));
        yGraphDiff = worldMapRect.height - yGraphDiff;
        yGraphDiff += (int) worldMapRect.getY();

        return yGraphDiff;
    }

    /**
     * Determines whether the world map interface is currently open and visible.
     *
     * <p>This method checks the state of the world map widget within the game client to assess
     * its visibility. If the widget corresponding to the world map is not available or is
     * hidden, it returns {@code false}, indicating that the world map is closed or inaccessible.
     * Otherwise, it returns {@code true}.
     *
     * @return {@code true} if the world map interface is open and visible; {@code false} otherwise.
     *         This includes cases where the widget is absent or hidden.
     */
    public boolean isWorldMapOpen() {
        Widget mapWidget = ctx.getClient().getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if(mapWidget == null) {
            return false;
        }

        return !mapWidget.isHidden();
    }
}
