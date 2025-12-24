package com.kraken.api.input.mouse;

import com.google.inject.Singleton;
import com.kraken.api.input.mouse.strategy.MovementStrategy;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.service.ui.UIService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Random;

@Slf4j
@Singleton
public class VirtualMouse {

    private final Client client;
    private final UIService uiService;
    
    @Setter
    private MovementStrategy defaultMovementStrategy = MovementStrategy.BEZIER;

    @Getter
    private final Canvas canvas;

    @Getter
    @Setter
    private Point lastMove = new Point(-1, -1);

    @Inject
    public VirtualMouse(Client client, UIService uiService) {
        this.client = client;
        this.uiService = uiService;
        this.canvas = client.getCanvas();
    }

    public VirtualMouse move(Point target, MovementStrategy strategy) {
        strategy.getStrategy().move(target);
        return this;
    }

    /**
     * Moves the mouse to the specified Actor.
     *
     * @param actor The actor to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Actor actor) {
        return move(actor, defaultMovementStrategy);
    }

    /**
     * Moves the mouse to the specified Actor using the provided movement strategy.
     *
     * @param actor            The actor to move to.
     * @param movementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Actor actor, MovementStrategy movementStrategy) {
        Point p = uiService.getClickbox(actor);
        move(p, movementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified ContainerItem.
     *
     * @param item The container item to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(ContainerItem item) {
        return move(item, defaultMovementStrategy);
    }

    /**
     * Moves the mouse to the specified ContainerItem using the provided movement strategy.
     *
     * @param item             The container item to move to.
     * @param movementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(ContainerItem item, MovementStrategy movementStrategy) {
        Point p = uiService.getClickbox(item);
        move(p, movementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified TileObject.
     *
     * @param tileObject The tile object to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(TileObject tileObject) {
        return move(tileObject, defaultMovementStrategy);
    }

    /**
     * Moves the mouse to the specified TileObject using the provided movement strategy.
     *
     * @param tileObject       The tile object to move to.
     * @param movementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(TileObject tileObject, MovementStrategy movementStrategy) {
        Point p = uiService.getClickbox(tileObject);
        move(p, movementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified Tile.
     *
     * @param tile The tile to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Tile tile) {
        return move(tile, defaultMovementStrategy);
    }

    /**
     * Moves the mouse to the specified Tile using the provided movement strategy.
     *
     * @param tile             The tile to move to.
     * @param movementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Tile tile, MovementStrategy movementStrategy) {
        Point p = uiService.getClickbox(tile);
        move(p, movementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified Widget.
     *
     * @param widget The widget to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Widget widget) {
        return move(widget, defaultMovementStrategy);
    }

    /**
     * Moves the mouse to the specified Widget using the provided movement strategy.
     *
     * @param widget           The widget to move to.
     * @param movementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Widget widget, MovementStrategy movementStrategy) {
        Point p = uiService.getClickbox(widget);
        move(p, movementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified WorldPoint.
     *
     * @param worldPoint The world point to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(WorldPoint worldPoint) {
        return move(worldPoint, defaultMovementStrategy);
    }

    /**
     * Moves the mouse to the specified WorldPoint using the provided movement strategy.
     *
     * @param worldPoint       The world point to move to.
     * @param movementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(WorldPoint worldPoint, MovementStrategy movementStrategy) {
        Point p = uiService.getClickbox(worldPoint);
        move(p, movementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified LocalPoint.
     *
     * @param localPoint The local point to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(LocalPoint localPoint) {
        return move(localPoint, defaultMovementStrategy);
    }

    /**
     * Moves the mouse to the specified LocalPoint using the provided movement strategy.
     *
     * @param localPoint       The local point to move to.
     * @param movementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(LocalPoint localPoint, MovementStrategy movementStrategy) {
        Point p = uiService.getClickbox(localPoint);
        move(p, movementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified LocalPoint at a specific plane.
     *
     * @param localPoint The local point to move to.
     * @param plane      The plane to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(LocalPoint localPoint, int plane) {
        return move(localPoint, plane, defaultMovementStrategy);
    }

    /**
     * Moves the mouse to the specified LocalPoint at a specific plane using the provided movement strategy.
     *
     * @param localPoint       The local point to move to.
     * @param plane            The plane to move to.
     * @param movementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(LocalPoint localPoint, int plane, MovementStrategy movementStrategy) {
        Point p = uiService.getClickbox(localPoint, plane);
        move(p, movementStrategy);
        return this;
    }
    
    /**
     * Moves the mouse to the center of the specified rectangle.
     *
     * @param rect The rectangle to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse moveSimple(Rectangle rect) {
        int x = (int) rect.getX() + (int) (rect.getWidth() * (new Random().nextGaussian() * 0.15 + 0.5));
        int y = (int) rect.getY() + (int) (rect.getHeight() * (new Random().nextGaussian() * 0.15 + 0.5));
        MouseEvent mouseMove = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false);
        getCanvas().dispatchEvent(mouseMove);

        return this;
    }

    /**
     * Moves the mouse to the center of the specified polygon.
     *
     * @param polygon The polygon to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse moveSimple(Polygon polygon) {
        int x = (int) polygon.getBounds().getX() + (int) (polygon.getBounds().getWidth() * (new Random().nextGaussian() * 0.15 + 0.5));
        int y = (int) polygon.getBounds().getY() + (int) (polygon.getBounds().getHeight() * (new Random().nextGaussian() * 0.15 + 0.5));

        MouseEvent mouseMove = new MouseEvent(getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, x, y, 0, false);
        getCanvas().dispatchEvent(mouseMove);

        return this;
    }

    /**
     * Gets the current mouse position from the AWT Canvas component.
     *
     * @return The system mouse position relative to the canvas.
     */
    public java.awt.Point getCanvasMousePosition() {
       return client.getCanvas().getMousePosition();
    }

    private synchronized void pressed(Point point, int button) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, button);
        getCanvas().dispatchEvent(event);
    }

    private synchronized void released(Point point, int button) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, button);
        getCanvas().dispatchEvent(event);
    }

    private synchronized void clicked(Point point, int button) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 1, false, button);
        getCanvas().dispatchEvent(event);
    }

    private synchronized void exited(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_EXITED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        getCanvas().dispatchEvent(event);
    }

    private synchronized void entered(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_ENTERED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        getCanvas().dispatchEvent(event);
    }

    private synchronized void moved(Point point) {
        MouseEvent event = new MouseEvent(client.getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        getCanvas().dispatchEvent(event);
        setLastMove(point);
    }
}
