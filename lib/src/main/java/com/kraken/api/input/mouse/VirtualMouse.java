package com.kraken.api.input.mouse;

import com.google.inject.Singleton;
import com.kraken.api.input.mouse.strategy.MouseMovementStrategy;
import com.kraken.api.input.mouse.strategy.linear.LinearStrategy;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.service.ui.UIService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.kraken.api.input.mouse.strategy.replay.PathLibrary.DATA_DIR;

@Slf4j
@Singleton
public class VirtualMouse implements MouseListener {

    private final UIService uiService;
    private final Client client;

    @Getter
    private Point lastPoint;

    private static MouseMovementStrategy defaultMouseMovementStrategy = MouseMovementStrategy.BEZIER;


    @Inject
    public VirtualMouse(UIService uiService, MouseManager mouseManager, Client client) {
        this.uiService = uiService;
        this.client = client;
        mouseManager.registerMouseListener(this);
        updatePosition();
    }

    private void updatePosition() {
        if (client.getMouseCanvasPosition() != null) {
            this.lastPoint = client.getMouseCanvasPosition();
        } else if (this.lastPoint == null) {
            this.lastPoint = new Point(0, 0);
        }
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent e) {
        lastPoint = new Point(e.getX(), e.getY());
        return e;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent e) {
        lastPoint = new Point(e.getX(), e.getY());
        return e;
    }

    @Override
    public MouseEvent mouseEntered(MouseEvent e) {
        lastPoint = new Point(e.getX(), e.getY());
        return e;
    }

    @Override
    public MouseEvent mouseExited(MouseEvent e) {
        lastPoint = new Point(e.getX(), e.getY());
        return e;
    }

    @Override
    public MouseEvent mousePressed(MouseEvent e) {
        lastPoint = new Point(e.getX(), e.getY());
        return e;
    }

    @Override
    public MouseEvent mouseReleased(MouseEvent e) {
        lastPoint = new Point(e.getX(), e.getY());
        return e;
    }

    @Override
    public MouseEvent mouseClicked(MouseEvent e) {
        lastPoint = new Point(e.getX(), e.getY());
        return e;
    }

    /**
     * Loads a replay library for the REPLAY mouse movement strategy. Any other strategy will be
     * ignored.
     *
     * @param libraryName The name of the library to load.
     */
    public static void loadLibrary(String libraryName) {
        MouseMovementStrategy.REPLAY.loadLibrary(libraryName);
    }

    /**
     * Sets the strategy to be used for mouse movement in the system.
     * <p>
     * This method updates the default mouse movement behavior by replacing it
     * with the provided {@literal @}MouseMovementStrategy implementation.
     * The new strategy will dictate how mouse movements are handled globally.
     *
     * <p><b>Note:</b> It is the caller's responsibility to ensure that the provided
     * strategy implementation is valid and behaves as expected. Passing a {@code null}
     * value may lead to unexpected behavior.
     *
     * @param strategy The {@literal @}MouseMovementStrategy to be set as the default
     *                 mouse movement strategy. Must not be {@code null}.
     */
    public static void setMouseMovementStrategy(MouseMovementStrategy strategy) {
        if(strategy == null) {
            log.error("Null mouse movement strategy provided as default.");
            return;
        }

        // Defaults to 50 steps. Users should override this for longer or shorter mouse movement if they go with
        // the linear movement pattern.
        if(strategy == MouseMovementStrategy.LINEAR) {
            LinearStrategy linear = (LinearStrategy) strategy.getStrategy();
            linear.setSteps(50);
        }

        defaultMouseMovementStrategy = strategy;
    }

    /**
     * Scans a specified directory for JSON files and returns a list of library names derived from the filenames.
     * <p>
     * This method attempts to list all files in a predefined directory, extract the filenames,
     * and remove the ".json" extension to identify the libraries.
     * </p>
     *
     * <p>In the event of an error during file scanning or processing, it logs the error and returns an empty list.</p>
     *
     * @return A {@literal List<String>} containing the names of the libraries (filenames without the ".json" extension),
     *         or an empty list if an error occurs or no libraries are found.
     */
    public static List<String> findLibraries() {
        try (Stream<Path> stream = Files.list(Paths.get(DATA_DIR))) {
            return stream.map(path -> path.getFileName().toString().replace(".json", "")).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("failed to find mouse gesture libraries: ", e);
        }

        return Collections.emptyList();
    }

     /**
     * Moves the mouse to the specified target position using the provided movement strategy.
     *
     * <p>This method uses the given {@code MouseMovementStrategy} to dictate how the mouse moves
     * to the specified {@code Point} target. The movement behavior varies depending on the selected
     * strategy (e.g., linear, bezier, instant, etc.).</p>
     *
     * <ul>
     *   <li>If the movement strategy is not properly initialized, unexpected behavior may occur.</li>
     *   <li>The mouse's final position will be the given {@code Point} after the strategy completes.</li>
     * </ul>
     *
     * @param target   The target position represented as a {@link Point} to which the mouse will be moved.
     * @param strategy The {@link MouseMovementStrategy} defining how the mouse moves to the target point.
     * @return The {@link VirtualMouse} instance for method chaining.
     */
    public VirtualMouse move(Point target, MouseMovementStrategy strategy) {
        // Only update position if we haven't tracked any movement yet (e.g. startup)
        // Otherwise, we rely on the internal tracking which allows for chained virtual movements
        // independent of the physical mouse.
        if (lastPoint.getX() == 0 && lastPoint.getY() == 0) {
            updatePosition();
        }

        log.info("Last point is: ({}, {})", lastPoint.getX(), lastPoint.getY());
        strategy.getStrategy().move(lastPoint, target);
        this.lastPoint = target;
        return this;
    }

    /**
     * Moves the mouse to the specified target position using the default mouse movement strategy.
     *
     * <p>This method leverages the default {@code MouseMovementStrategy} to determine how the mouse
     * moves. The behavior of the movement may include various strategies (e.g., linear, curved,
     * instant, etc.) depending on the configuration of the default strategy.</p>
     *
     * <ul>
     *   <li>If the default mouse movement strategy is not initialized, the behavior may be undefined.</li>
     *   <li>The mouse's final position will be the specified target after the strategy completes its execution.</li>
     * </ul>
     *
     * @param target The target position represented as a {@link Point} to which the mouse will move.
     * @return The {@link VirtualMouse} instance for method chaining.
     */
    public VirtualMouse move(Point target) {
        return move(target, defaultMouseMovementStrategy);
    }

    /**
     * Moves the mouse to the specified Actor.
     *
     * @param actor The actor to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Actor actor) {
        return move(actor, defaultMouseMovementStrategy);
    }

    /**
     * Moves the mouse to the specified Actor using the provided movement strategy.
     *
     * @param actor            The actor to move to.
     * @param mouseMovementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Actor actor, MouseMovementStrategy mouseMovementStrategy) {
        Point p = uiService.getClickbox(actor);
        move(p, mouseMovementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified ContainerItem.
     *
     * @param item The container item to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(ContainerItem item) {
        return move(item, defaultMouseMovementStrategy);
    }

    /**
     * Moves the mouse to the specified ContainerItem using the provided movement strategy.
     *
     * @param item             The container item to move to.
     * @param mouseMovementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(ContainerItem item, MouseMovementStrategy mouseMovementStrategy) {
        Point p = uiService.getClickbox(item);
        move(p, mouseMovementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified TileObject.
     *
     * @param tileObject The tile object to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(TileObject tileObject) {
        return move(tileObject, defaultMouseMovementStrategy);
    }

    /**
     * Moves the mouse to the specified TileObject using the provided movement strategy.
     *
     * @param tileObject       The tile object to move to.
     * @param mouseMovementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(TileObject tileObject, MouseMovementStrategy mouseMovementStrategy) {
        Point p = uiService.getClickbox(tileObject);
        move(p, mouseMovementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified Tile.
     *
     * @param tile The tile to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Tile tile) {
        return move(tile, defaultMouseMovementStrategy);
    }

    /**
     * Moves the mouse to the specified Tile using the provided movement strategy.
     *
     * @param tile             The tile to move to.
     * @param mouseMovementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Tile tile, MouseMovementStrategy mouseMovementStrategy) {
        Point p = uiService.getClickbox(tile);
        move(p, mouseMovementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified Widget.
     *
     * @param widget The widget to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Widget widget) {
        return move(widget, defaultMouseMovementStrategy);
    }

    /**
     * Moves the mouse to the specified Widget using the provided movement strategy.
     *
     * @param widget           The widget to move to.
     * @param mouseMovementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Widget widget, MouseMovementStrategy mouseMovementStrategy) {
        Point p = uiService.getClickbox(widget);
        move(p, mouseMovementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified WorldPoint.
     *
     * @param worldPoint The world point to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(WorldPoint worldPoint) {
        return move(worldPoint, defaultMouseMovementStrategy);
    }

    /**
     * Moves the mouse to the specified WorldPoint using the provided movement strategy.
     *
     * @param worldPoint       The world point to move to.
     * @param mouseMovementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(WorldPoint worldPoint, MouseMovementStrategy mouseMovementStrategy) {
        Point p = uiService.getClickbox(worldPoint);
        move(p, mouseMovementStrategy);
        return this;
    }

    /**
     * Moves the mouse to the specified LocalPoint.
     *
     * @param localPoint The local point to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(LocalPoint localPoint) {
        return move(localPoint, defaultMouseMovementStrategy);
    }

    /**
     * Moves the mouse to the specified LocalPoint using the provided movement strategy.
     *
     * @param localPoint       The local point to move to.
     * @param mouseMovementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(LocalPoint localPoint, MouseMovementStrategy mouseMovementStrategy) {
        Point p = uiService.getClickbox(localPoint);
        move(p, mouseMovementStrategy);
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
        return move(localPoint, plane, defaultMouseMovementStrategy);
    }

    /**
     * Moves the mouse to the specified LocalPoint at a specific plane using the provided movement strategy.
     *
     * @param localPoint       The local point to move to.
     * @param plane            The plane to move to.
     * @param mouseMovementStrategy The movement strategy to use.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(LocalPoint localPoint, int plane, MouseMovementStrategy mouseMovementStrategy) {
        Point p = uiService.getClickbox(localPoint, plane);
        move(p, mouseMovementStrategy);
        return this;
    }
    
    /**
     * Moves the mouse to the center of the specified rectangle.
     *
     * @param rect The rectangle to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Rectangle rect) {
        int x = (int) rect.getX() + (int) (rect.getWidth() * (new Random().nextGaussian() * 0.15 + 0.5));
        int y = (int) rect.getY() + (int) (rect.getHeight() * (new Random().nextGaussian() * 0.15 + 0.5));
        return move(new Point(x, y));
    }

    /**
     * Moves the mouse to the center of the specified polygon.
     *
     * @param polygon The polygon to move to.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse move(Polygon polygon) {
        int x = (int) polygon.getBounds().getX() + (int) (polygon.getBounds().getWidth() * (new Random().nextGaussian() * 0.15 + 0.5));
        int y = (int) polygon.getBounds().getY() + (int) (polygon.getBounds().getHeight() * (new Random().nextGaussian() * 0.15 + 0.5));
        return move(new Point(x, y));
    }

    /**
     * Clicks the mouse at the current position.
     *
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse click() {
        if (client.getCanvas() == null) {
            return this;
        }

        Point point = lastPoint;
        Canvas canvas = client.getCanvas();
        long time = System.currentTimeMillis();

        MouseEvent pressed = new MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, time, InputEvent.BUTTON1_DOWN_MASK, point.getX(), point.getY(), 1, false, MouseEvent.BUTTON1);
        canvas.dispatchEvent(pressed);

        MouseEvent released = new MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, time, 0, point.getX(), point.getY(), 1, false, MouseEvent.BUTTON1);
        canvas.dispatchEvent(released);

        MouseEvent clicked = new MouseEvent(canvas, MouseEvent.MOUSE_CLICKED, time, 0, point.getX(), point.getY(), 1, false, MouseEvent.BUTTON1);
        canvas.dispatchEvent(clicked);

        return this;
    }
}
