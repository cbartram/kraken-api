package com.kraken.api.input.mouse;

import com.google.inject.Singleton;
import com.kraken.api.input.mouse.model.NormalizedPath;
import com.kraken.api.service.util.RandomService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Random;

@Slf4j
@Singleton
public class VirtualMouse {

    private final Client client;
    private final PathLibrary pathLibrary;
    private String label;

    @Getter
    private final Canvas canvas;

    @Getter
    @Setter
    private Point lastMove = new Point(-1, -1);

    @Inject
    public VirtualMouse(Client client, PathLibrary pathLibrary) {
        this.client = client;
        this.canvas = client.getCanvas();
        this.pathLibrary = pathLibrary;
    }

    public void load(String label) {
        this.pathLibrary.load(label);
        this.label = label;
    }

    public VirtualMouse move(Point target) {
        if(label == null) {
            log.error("Cannot move mouse, no library was loaded. Use load(String label) to load a library.");
            return this;
        }

        Point start = new Point(getCanvasMousePosition().x, getCanvasMousePosition().y);
        double distance = start.distanceTo(target);

        // 1. Try to get a human path
        NormalizedPath template = pathLibrary.getBestPath(label, distance);

        if (template != null) {
            // 2. Determine Speed (Fitts's Law approximation or reuse human speed)
            // It is safer to calculate a NEW duration based on distance, rather than reusing
            // the recorded duration, because we might be stretching the path.
            long duration = calculateFittsDuration(distance);
            List<MotionFactory.TimedPoint> path = MotionFactory.transform(template, start, target, duration);
            executePath(path);
        } else {
            log.warn("No mouse data found for label '{}' distance {}. Using Bezier Fallback.", label, distance);
            moveBezier(target);
        }

        return this;
    }

    private void executePath(List<MotionFactory.TimedPoint> path) {
        for (MotionFactory.TimedPoint p : path) {
            // Busy-wait loop for precision (Thread.sleep is too inaccurate for <10ms)
            // or use a high-precision timer.
            long timeToWait = p.time - System.currentTimeMillis();

            if (timeToWait > 0) {
                try {
                    // Only sleep if wait is significant, otherwise spin
                    if (timeToWait > 2) Thread.sleep(timeToWait);
                    else while(System.currentTimeMillis() < p.time);
                } catch (InterruptedException e) { return; }
            }

            MouseEvent event = new MouseEvent(
                    getCanvas(), MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(),
                    0, p.x, p.y, 0, false
            );
            getCanvas().dispatchEvent(event);
            setLastMove(new Point(p.x, p.y));
        }
    }

    /**
     * Moves the mouse to the specified point.
     *
     * @param point The destination point.
     * @return The VirtualMouse instance for chaining.
     */
    public VirtualMouse moveSimple(Point point) {
        setLastMove(point);
        MouseEvent mouseMove = new MouseEvent(getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, point.getX(), point.getY(), 0, false);
        mouseMove.setSource("Kraken");
        getCanvas().dispatchEvent(mouseMove);

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

    public void moveBezier(Point target) {
        Point start = new Point(getCanvasMousePosition().x, getCanvasMousePosition().y);

        // 1. Generate Control Points (P1, P2)
        // Add randomness so the curve varies.
        // A good heuristic is to pick points between start and target, offset by a "variance".
        Point p1 = generateControlPoint(start, target);
        Point p2 = generateControlPoint(start, target);

        // 2. Calculate Distance for Fitts's Law approximation
        double distance = start.distanceTo(target);

        // 3. Determine Duration (in ms)
        // Heuristic: Min duration + (distance * speedFactor)
        long duration = (long) (RandomService.between(100, 200) + (distance * 0.5));

        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < duration) {
            long elapsed = System.currentTimeMillis() - startTime;

            // Normalize time (0.0 to 1.0)
            double t = (double) elapsed / duration;

            // Apply easing function (Sinusoidal for smooth accel/decel)
            // This makes the movement start slow, go fast, end slow
            double easedT = -(Math.cos(Math.PI * t) - 1) / 2;

            // Dispatch the event
            move(getBezierPoint(start, p1, p2, target, easedT));

            // Sleep to throttle updates (60Hz - 120Hz feel)
            try {
                Thread.sleep(RandomService.between(5, 10));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Ensure we land exactly on the target at the end
        move(target);
    }

    public static Point getBezierPoint(Point p0, Point p1, Point p2, Point p3, double t) {
        double x = Math.pow(1 - t, 3) * p0.getX() +
                3 * Math.pow(1 - t, 2) * t * p1.getX() +
                3 * (1 - t) * t * t * p2.getX() +
                Math.pow(t, 3) * p3.getX();

        double y = Math.pow(1 - t, 3) * p0.getY() +
                3 * Math.pow(1 - t, 2) * t * p1.getY() +
                3 * (1 - t) * t * t * p2.getY() +
                Math.pow(t, 3) * p3.getY();

        return new Point((int) x, (int) y);
    }

    /**
     * Generates a Bezier control point that creates a natural arc between start and target.
     * * @param start The starting point of the movement segment.
     * @param target The destination of the movement segment.
     * @return A calculated control point with perpendicular deviation.
     */
    private Point generateControlPoint(Point start, Point target) {
        double dist = start.distanceTo(target);

        // 1. Pick a pivot point along the line connecting start and target.
        // We restrict t to 0.2 - 0.8 to ensure the control point isn't too close to the endpoints,
        // which creates tight, unnatural hooks.
        double t = 0.2 + (Math.random() * 0.6);

        double lerpX = start.getX() + (target.getX() - start.getX()) * t;
        double lerpY = start.getY() + (target.getY() - start.getY()) * t;

        // 2. Calculate the perpendicular vector for the arc offset.
        // Direction vector (dx, dy)
        double dx = target.getX() - start.getX();
        double dy = target.getY() - start.getY();

        // Perpendicular vector is (-dy, dx)
        double perpX = -dy;
        double perpY = dx;

        // 3. Determine the arc height (variance).
        // Heuristic: The arc height should be proportional to the distance.
        // A 1000px move allows for a wider arc (e.g., 200px) than a 20px move.
        // We use a factor (e.g., 0.15) to keep the arc subtle.
        double maxArcHeight = dist * 0.15;

        // Randomize the offset between -maxArcHeight and +maxArcHeight
        // This decides if the arc curves "left" or "right" relative to travel.
        double deviation = (Math.random() * maxArcHeight * 2) - maxArcHeight;

        // Normalize the perpendicular vector and apply the deviation
        // (Avoiding division by zero if start == target)
        if (dist > 0) {
            perpX /= dist;
            perpY /= dist;
        }

        int controlX = (int) (lerpX + (perpX * deviation));
        int controlY = (int) (lerpY + (perpY * deviation));

        return new Point(controlX, controlY);
    }

    private long calculateFittsDuration(double distance) {
        // Base reaction + movement time based on distance
        return (long) (120 + (distance * 0.4) + (Math.random() * 50));
    }
}


