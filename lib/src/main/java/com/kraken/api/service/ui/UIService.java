package com.kraken.api.service.ui;

import com.kraken.api.Context;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.service.util.RandomService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;

import javax.inject.Singleton;
import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Service for handling UI interactions and clickbox calculations.
 * Provides methods to retrieve clickboxes for various game objects and calculate click points.
 */
@Slf4j
@Singleton
public class UIService {
    
    private final static Context ctx = RuneLite.getInjector().getInstance(Context.class);

    /**
     * Returns a default rectangle representing the entire game canvas with a slight random offset.
     *
     * @return Rectangle covering the canvas area
     */
    public static Rectangle getDefaultRectangle() {
        int randomValue = ThreadLocalRandom.current().nextInt(3) - 1;
        return new Rectangle(randomValue, randomValue, ctx.getClient().getCanvasWidth(), ctx.getClient().getCanvasHeight());
    }

    /**
     * Gets the clickbox for an Actor (NPC or Player).
     *
     * @param actor the actor to get the clickbox for
     * @return the actor's clickbox, or default rectangle if unavailable
     */
    public static Rectangle getActorClickbox(Actor actor) {
        LocalPoint lp = actor.getLocalLocation();
        if (lp == null) {
            return getDefaultRectangle();
        }

        Shape clickbox = ctx.runOnClientThreadOptional(() -> Perspective.getClickbox(ctx.getClient(), ctx.getClient().getTopLevelWorldView(), actor.getModel(), actor.getCurrentOrientation(), lp.getX(), lp.getY(),
                        Perspective.getTileHeight(ctx.getClient(), lp, actor.getWorldLocation().getPlane())))
                .orElse(null);

        if (clickbox == null) return getDefaultRectangle();
        return new Rectangle(clickbox.getBounds());
    }

    /**
     * Gets the clickbox for a TileObject (GameObject, WallObject, GroundObject, or DecorativeObject).
     *
     * @param object the tile object to get the clickbox for
     * @return the object's clickbox, or default rectangle if unavailable
     */
    public static Rectangle getObjectClickbox(TileObject object) {
        if (object == null) return getDefaultRectangle();
        Shape clickbox = ctx.runOnClientThreadOptional(object::getClickbox).orElse(null);
        if (clickbox == null) return getDefaultRectangle();
        if (clickbox.getBounds() == null) return getDefaultRectangle();

        return new Rectangle(clickbox.getBounds());
    }

    /**
     * Gets the clickbox for a Tile.
     *
     * @param tile the tile to get the clickbox for
     * @return the tile's clickbox, or default rectangle if unavailable
     */
    public static Rectangle getTileClickbox(Tile tile) {
        if (tile == null) return getDefaultRectangle();

        LocalPoint localPoint = tile.getLocalLocation();
        if (localPoint == null) return getDefaultRectangle();

        // Get the screen point of the tile center
        Point screenPoint = Perspective.localToCanvas(ctx.getClient(), localPoint, ctx.getClient().getTopLevelWorldView().getPlane());

        if (screenPoint == null) return getDefaultRectangle();

        int tileSize = Perspective.LOCAL_TILE_SIZE;
        int halfSize = tileSize / 4;

        return new Rectangle(screenPoint.getX() - halfSize, screenPoint.getY() - halfSize, tileSize / 2, tileSize / 2);
    }

    /**
     * Gets the clickbox for a WorldPoint by converting it to screen coordinates.
     * Creates a small rectangle around the point to allow for clicking.
     *
     * @param worldPoint the world point to get the clickbox for
     * @return a small clickbox around the world point's screen location, or default rectangle if unavailable
     */
    public static Rectangle getWorldPointClickbox(WorldPoint worldPoint) {
        if (worldPoint == null) return getDefaultRectangle();

        LocalPoint localPoint = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), worldPoint);
        if (localPoint == null) return getDefaultRectangle();

        Point screenPoint = Perspective.localToCanvas(ctx.getClient(), localPoint, worldPoint.getPlane());
        if (screenPoint == null) return getDefaultRectangle();

        // TODO Different resolutions may cause problems here
        int size = 20;
        int halfSize = size / 2;
        return new Rectangle(screenPoint.getX() - halfSize, screenPoint.getY() - halfSize, size, size);
    }

    /**
     * Gets the clickbox for a LocalPoint by converting it to screen coordinates.
     * Creates a small rectangle around the point to allow for clicking.
     *
     * @param localPoint the local point to get the clickbox for
     * @return a small clickbox around the local point's screen location, or default rectangle if unavailable
     */
    public static Rectangle getLocalPointClickbox(LocalPoint localPoint) {
        if (localPoint == null) return getDefaultRectangle();

        Point screenPoint = Perspective.localToCanvas(ctx.getClient(), localPoint, ctx.getClient().getTopLevelWorldView().getPlane());
        if (screenPoint == null) return getDefaultRectangle();

        // Create a small clickable area around the point (20x20 pixels)
        // TODO Different resolutions may cause problems here
        int size = 20;
        int halfSize = size / 2;
        return new Rectangle(screenPoint.getX() - halfSize, screenPoint.getY() - halfSize, size, size);
    }

    /**
     * Gets the clickbox for a LocalPoint at a specific plane by converting it to screen coordinates.
     * Creates a small rectangle around the point to allow for clicking.
     *
     * @param localPoint the local point to get the clickbox for
     * @param plane the plane/height level
     * @return a small clickbox around the local point's screen location, or default rectangle if unavailable
     */
    public static Rectangle getLocalPointClickbox(LocalPoint localPoint, int plane) {
        if (localPoint == null) return getDefaultRectangle();

        Point screenPoint = Perspective.localToCanvas(ctx.getClient(), localPoint, plane);
        if (screenPoint == null) return getDefaultRectangle();

        // Create a small clickable area around the point (20x20 pixels)
        // TODO Different resolutions may cause problems here
        int size = 20;
        int halfSize = size / 2;
        return new Rectangle(screenPoint.getX() - halfSize, screenPoint.getY() - halfSize, size, size);
    }

    /**
     * Gets the clickbox for a Widget.
     *
     * @param widget the widget to get the clickbox for
     * @return the widget's bounds, or default rectangle if unavailable
     */
    public static Rectangle getWidgetClickbox(Widget widget) {
        if(widget == null) return getDefaultRectangle();
        return widget.getBounds();
    }

    /**
     * Gets the clickbox for an Actor with randomization enabled by default.
     *
     * @param actor the actor to get the clickbox for
     * @return a randomized point within the actor's clickbox
     */
    public static Point getClickbox(Actor actor) {
        return getClickbox(actor, true);
    }

    /**
     * Gets the clickbox for an inventory item.
     * @param item The inventory item
     * @return The canvas point for the inventory items clickbox (randomizes the point).
     */
    public static Point getClickbox(ContainerItem item) {
        return getClickbox(item, true);
    }

    /**
     * Gets the clickbox for an inventory item with optional randomization
     * @param item The item to get the clickbox for
     * @param randomize True if the point should be randomized. If false it will return the center point.
     * @return Center point or random point within the bounds of the inventory item.
     */
    public static Point getClickbox(ContainerItem item, boolean randomize) {
        Rectangle bounds = item.getBounds(ctx, ctx.getClient());
        if(bounds == null) return getClickingPoint(getDefaultRectangle(), true);
        return getClickingPoint(bounds, randomize);
    }

    /**
     * Gets the clickbox for an Actor with optional randomization.
     *
     * @param actor the actor to get the clickbox for
     * @param randomize whether to randomize the point within the clickbox
     * @return a point within the actor's clickbox (randomized or centered)
     */
    public static Point getClickbox(Actor actor, boolean randomize) {
        Rectangle clickbox = getActorClickbox(actor);
        return getClickingPoint(clickbox, randomize);
    }

    /**
     * Gets the clickbox for a TileObject with randomization enabled by default.
     *
     * @param object the tile object to get the clickbox for
     * @return a randomized point within the object's clickbox
     */
    public static Point getClickbox(TileObject object) {
        return getClickbox(object, true);
    }

    /**
     * Gets the clickbox for a TileObject with optional randomization.
     *
     * @param object the tile object to get the clickbox for
     * @param randomize whether to randomize the point within the clickbox
     * @return a point within the object's clickbox (randomized or centered)
     */
    public static Point getClickbox(TileObject object, boolean randomize) {
        Rectangle clickbox = getObjectClickbox(object);
        return getClickingPoint(clickbox, randomize);
    }

    /**
     * Gets the clickbox for a Tile with randomization enabled by default.
     *
     * @param tile the tile to get the clickbox for
     * @return a randomized point within the tile's clickbox
     */
    public static Point getClickbox(Tile tile) {
        return getClickbox(tile, true);
    }

    /**
     * Gets the clickbox for a Tile with optional randomization.
     *
     * @param tile the tile to get the clickbox for
     * @param randomize whether to randomize the point within the clickbox
     * @return a point within the tile's clickbox (randomized or centered)
     */
    public static Point getClickbox(Tile tile, boolean randomize) {
        Rectangle clickbox = getTileClickbox(tile);
        return getClickingPoint(clickbox, randomize);
    }

    /**
     * Gets the clickbox for a Widget with randomization enabled by default.
     *
     * @param widget the widget to get the clickbox for
     * @return a randomized point within the widget's clickbox
     */
    public static Point getClickbox(Widget widget) {
        return getClickbox(widget, true);
    }

    /**
     * Gets the clickbox for a Widget with optional randomization.
     *
     * @param widget the widget to get the clickbox for
     * @param randomize whether to randomize the point within the clickbox
     * @return a point within the widget's clickbox (randomized or centered)
     */
    public static Point getClickbox(Widget widget, boolean randomize) {
        Rectangle clickbox = getWidgetClickbox(widget);
        return getClickingPoint(clickbox, randomize);
    }

    /**
     * Gets the clickbox for a WorldPoint with randomization enabled by default.
     *
     * @param worldPoint the world point to get the clickbox for
     * @return a randomized point within the world point's clickbox
     */
    public static Point getClickbox(WorldPoint worldPoint) {
        return getClickbox(worldPoint, true);
    }

    /**
     * Gets the clickbox for a WorldPoint with optional randomization.
     *
     * @param worldPoint the world point to get the clickbox for
     * @param randomize whether to randomize the point within the clickbox
     * @return a point within the world point's clickbox (randomized or centered)
     */
    public static Point getClickbox(WorldPoint worldPoint, boolean randomize) {
        Rectangle clickbox = getWorldPointClickbox(worldPoint);
        return getClickingPoint(clickbox, randomize);
    }

    /**
     * Gets the clickbox for a LocalPoint with randomization enabled by default.
     *
     * @param localPoint the local point to get the clickbox for
     * @return a randomized point within the local point's clickbox
     */
    public static Point getClickbox(LocalPoint localPoint) {
        return getClickbox(localPoint, true);
    }

    /**
     * Gets the clickbox for a LocalPoint with optional randomization.
     *
     * @param localPoint the local point to get the clickbox for
     * @param randomize whether to randomize the point within the clickbox
     * @return a point within the local point's clickbox (randomized or centered)
     */
    public static Point getClickbox(LocalPoint localPoint, boolean randomize) {
        Rectangle clickbox = getLocalPointClickbox(localPoint);
        return getClickingPoint(clickbox, randomize);
    }

    /**
     * Gets the clickbox for a LocalPoint at a specific plane with randomization enabled by default.
     *
     * @param localPoint the local point to get the clickbox for
     * @param plane the plane/height level
     * @return a randomized point within the local point's clickbox
     */
    public static Point getClickbox(LocalPoint localPoint, int plane) {
        return getClickbox(localPoint, plane, true);
    }

    /**
     * Gets the clickbox for a LocalPoint at a specific plane with optional randomization.
     *
     * @param localPoint the local point to get the clickbox for
     * @param plane the plane/height level
     * @param randomize whether to randomize the point within the clickbox
     * @return a point within the local point's clickbox (randomized or centered)
     */
    public static Point getClickbox(LocalPoint localPoint, int plane, boolean randomize) {
        Rectangle clickbox = getLocalPointClickbox(localPoint, plane);
        return getClickingPoint(clickbox, randomize);
    }

    /**
     * Calculates a click point from a rectangle with optional randomization.
     * When randomized, uses RandomService.randomPointEx with a distribution factor of 0.78.
     *
     * @param rectangle the rectangle to calculate the point from
     * @param randomize whether to randomize the point within the rectangle
     * @return a point within the rectangle (randomized or centered)
     */
    public static Point getClickingPoint(Rectangle rectangle, boolean randomize) {
        if (rectangle == null) return new Point(1, 1);
        if (rectangle.getX() == 1 && rectangle.getY() == 1) return new Point(1, 1);
        if (rectangle.getX() == 0 && rectangle.getY() == 0) return new Point(1, 1);

        if (!randomize) return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY());

        return RandomService.randomPointEx(new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY()), rectangle, 0.78);
    }
}