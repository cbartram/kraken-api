package com.kraken.api.interaction.movement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.widget.WidgetService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.SpriteID;
import net.runelite.api.Varbits;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.SpriteManager;

import javax.annotation.Nullable;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class MinimapService extends AbstractService {

    @Inject
    private WidgetService widgetService;

    @Inject
    private SpriteManager spriteManager;

    public boolean walkMiniMap(WorldPoint worldPoint) {
        return walkMiniMap(worldPoint, 5.0f);
    }

    /**
     * Walks to a world point via a click on the Minimap. This must have a fixed mode client and the stretched mode plugin
     * turned off to work correctly. Zoom values can be between 2 and 8.
     * @param worldPoint
     * @param zoomDistance
     * @return
     */
    public boolean walkMiniMap(WorldPoint worldPoint, float zoomDistance) {
        float zoom;
        if(zoomDistance < 2.0) {
            zoom = 2.0f;
        } else if(zoomDistance > 8.0) {
            zoom = 8.0f;
        } else {
            zoom = zoomDistance;
        }

        if (client.getMinimapZoom() != zoom)
            client.setMinimapZoom(zoom);

        Point point = worldToMinimap(worldPoint);

        if (point == null) {
            log.error("Failed to convert world point: {} to minimap coords", worldPoint);
            return false;
        }
        if (!isPointInsideMinimap(point)) {
            log.error("Minimap point is out of bounds");
            return false;
        }

        context.getMouse().click(point);
        return true;
    }

    /**
     * Checks if a given point is inside the minimap clipping area.
     *
     * @param point The point to check.
     * @return {@code true} if the point is within the minimap bounds, {@code false} otherwise.
     */
    public boolean isPointInsideMinimap(Point point) {
        Shape minimapClipArea = getMinimapClipArea();
        return minimapClipArea != null && minimapClipArea.contains(point.getX(), point.getY());
    }

    /**
     * Converts a {@link WorldPoint} to a minimap coordinate {@link Point}.
     *
     * @param worldPoint The world point to convert.
     * @return The corresponding minimap point, or {@code null} if conversion fails.
     */
    @Nullable
    public Point worldToMinimap(WorldPoint worldPoint) {
        if (worldPoint == null) return null;

        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);

        if (localPoint == null) {
            log.error("Tried to walk worldpoint {} using the canvas but WP -> LP conversion failed.", worldPoint);
            return null;
        }

        return context.runOnClientThreadOptional(() -> Perspective.localToMinimap(client, localPoint)).orElse(null);
    }

    /**
     * Returns a simple elliptical clip area for the minimap.
     *
     * @return A {@link Shape} representing the minimap clip area.
     */
    private Shape getMinimapClipAreaSimple() {
        Widget minimapDrawArea = getMinimapDrawWidget();
        if (minimapDrawArea == null) {
            return null;
        }
        Rectangle bounds = minimapDrawArea.getBounds();
        return new Ellipse2D.Double(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    /**
     * Retrieves the minimap clipping area as a polygon derived from the minimap alpha mask sprite,
     * and scales it inward to avoid overlapping edge elements.
     *
     * @param scale The scale factor to shrink the polygon (e.g., 0.94 for 94% of the original size).
     * @return A {@link Shape} representing the scaled minimap clickable area, or a fallback shape if the sprite is unavailable.
     */
    public Shape getMinimapClipArea(double scale) {
        Widget minimapWidget = getMinimapDrawWidget();
        if (minimapWidget == null) {
            return null;
        }

        boolean isResized = client.isResized();

        BufferedImage minimapSprite = context.runOnClientThreadOptional(() ->
                spriteManager.getSprite(isResized ? SpriteID.RESIZEABLE_MODE_MINIMAP_ALPHA_MASK : SpriteID.FIXED_MODE_MINIMAP_ALPHA_MASK, 0)).orElse(null);

        if (minimapSprite == null) {
            return getMinimapClipAreaSimple();
        }

        Shape rawClipArea = bufferedImageToPolygon(minimapSprite, minimapWidget.getBounds());
        return shrinkShape(rawClipArea, scale);
    }

    /**
     * Retrieves the minimap draw widget based on the current game view mode.
     *
     * @return The minimap draw widget, or {@code null} if not found.
     */
    public Widget getMinimapDrawWidget() {
        if (client.isResized()) {
            // Side panels
            if (context.getVarbitValue(4607) == 1) {
                // ComponentID.RESIZABLE_VIEWPORT_BOTTOM_LINE_MINIMAP_DRAW_AREA
                return widgetService.getWidget(10747934);
            }
            // ComponentID.RESIZABLE_VIEWPORT_MINIMAP_DRAW_AREA
            return widgetService.getWidget(10551326);
        }

        // ComponentID.FIXED_VIEWPORT_MINIMAP_DRAW_AREA
        return widgetService.getWidget(35913749);
    }

    /**
     * Retrieves the minimap clipping area as a {@link Shape}, scaled to slightly reduce its size.
     * <p>
     * This is useful for rendering overlays within the minimap without overlapping UI elements such as the globe icon.
     *
     * @return A {@link Shape} representing the scaled minimap clip area, or {@code null} if the minimap widget is unavailable.
     */
    public Shape getMinimapClipArea() {
        return getMinimapClipArea(client.isResized() ? 0.94 : 1.0);
    }

    /**
     * Converts a BufferedImage to a polygon by detecting the border based on the outside color.
     *
     * @param image         The image to convert.
     * @param minimapBounds The bounds of the minimap widget.
     * @return A polygon representing the minimap's clickable area.
     */
    private Polygon bufferedImageToPolygon(BufferedImage image, Rectangle minimapBounds) {
        Color outsideColour = null;
        Color previousColour;
        final int width = image.getWidth();
        final int height = image.getHeight();
        List<java.awt.Point> points = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            previousColour = outsideColour;
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int a = (rgb & 0xff000000) >>> 24;
                int r = (rgb & 0x00ff0000) >> 16;
                int g = (rgb & 0x0000ff00) >> 8;
                int b = (rgb & 0x000000ff);
                Color colour = new Color(r, g, b, a);
                if (x == 0 && y == 0) {
                    outsideColour = colour;
                    previousColour = colour;
                }
                if (!colour.equals(outsideColour) && previousColour.equals(outsideColour)) {
                    points.add(new java.awt.Point(x, y));
                }
                if ((colour.equals(outsideColour) || x == (width - 1)) && !previousColour.equals(outsideColour)) {
                    points.add(0, new java.awt.Point(x, y));
                }
                previousColour = colour;
            }
        }

        int offsetX = minimapBounds.x;
        int offsetY = minimapBounds.y;
        Polygon polygon = new Polygon();
        for (java.awt.Point point : points) {
            polygon.addPoint(point.x + offsetX, point.y + offsetY);
        }
        return polygon;
    }

    /**
     * Shrinks the given shape toward its center by the specified scale factor.
     *
     * @param shape The original shape to shrink.
     * @param scale The scale factor (e.g., 0.94 = 94% size). Must be > 0 and < 1 to reduce the shape.
     * @return A new {@link Shape} that is scaled inward toward its center.
     */
    private Shape shrinkShape(Shape shape, double scale) {
        Rectangle bounds = shape.getBounds();
        double centerX = bounds.getCenterX();
        double centerY = bounds.getCenterY();

        AffineTransform shrink = AffineTransform.getTranslateInstance(centerX, centerY);
        shrink.scale(scale, scale);
        shrink.translate(-centerX, -centerY);

        return shrink.createTransformedShape(shape);
    }

}
