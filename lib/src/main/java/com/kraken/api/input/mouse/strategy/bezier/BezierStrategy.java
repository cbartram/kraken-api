package com.kraken.api.input.mouse.strategy.bezier;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.input.mouse.strategy.MoveableMouse;
import com.kraken.api.input.mouse.strategy.instant.InstantStrategy;
import com.kraken.api.service.util.RandomService;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Point;

import java.awt.*;

@Singleton
public class BezierStrategy implements MoveableMouse {

    @Inject
    private InstantStrategy instantStrategy;

    @Inject
    private Client client;

    @Getter
    private Point lastPoint;

    @Override
    public Canvas getCanvas() {
        return client.getCanvas();
    }

    /**
     * Moves the mouse cursor along a cubic Bezier curve path to the specified target point.
     *
     * <p>This method simulates a natural, human-like cursor movement by calculating and
     * following a cubic Bezier curve between the mouse's current position and the target
     * point. The curve includes two intermediate control points for smooth motion and incorporates
     * random variations to add unpredictability. The motion speed and duration are dynamically
     * calculated using Fitts's Law principles and easing functions.</p>
     *
     * <ul>
     *   <li>The motion begins at the mouse's current position (start).</li>
     *   <li>Two random control points (p1, p2) determine the curvature.</li>
     *   <li>The motion transitions smoothly to the target position using easing functions.</li>
     * </ul>
     *
     * @param target The target position to which the mouse cursor will move.
     *               This is represented as a {@code Point}.
     */
    @Override
    public void move(Point target) {
        Point start;
        if(getCanvas().getMousePosition() == null) {
            start = new Point(0, 0);
        } else {
            start = new Point(getCanvas().getMousePosition().x, getCanvas().getMousePosition().y);
        }

        // Add randomness so the curve varies.
        Point p1 = generateControlPoint(start, target);
        Point p2 = generateControlPoint(start, target);

        // Calculate Distance for Fitts's Law approximation
        double distance = start.distanceTo(target);

        // Determine Duration (in ms) using: Min duration + (distance * speedFactor)
        long duration = (long) (RandomService.between(100, 200) + (distance * 0.5));
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < duration) {
            long elapsed = System.currentTimeMillis() - startTime;
            double t = (double) elapsed / duration;

            // Apply easing function, this makes the movement start slow, go fast, end slow
            double easedT = -(Math.cos(Math.PI * t) - 1) / 2;

            Point p = getBezierPoint(start, p1, p2, target, easedT);
            instantStrategy.move(p);
            try {
                Thread.sleep(RandomService.between(3, 8));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Ensure we land exactly on the target at the end
        instantStrategy.move(target);
        lastPoint = target;
    }

    /**
     * Calculates a point on a cubic Bezier curve at a specific parameter {@code t}.
     *
     * <p>This method generates the coordinates of a point on a Bezier curve defined by four control
     * points. The parameter {@code t} determines the position along the curve, where {@code t = 0}
     * corresponds to the start of the curve, and {@code t = 1} corresponds to the end of the curve.
     *
     * <ul>
     *   <li>The curve starts at {@code p0} and ends at {@code p3}.</li>
     *   <li>{@code p1} and {@code p2} are the two intermediate control points that define the shape of the curve.</li>
     * </ul>
     *
     * @param p0 The starting point of the Bezier curve.
     * @param p1 The first intermediate control point influencing the shape of the curve.
     * @param p2 The second intermediate control point influencing the shape of the curve.
     * @param p3 The ending point of the Bezier curve.
     * @param t  The parameter (0.0 <= {@code t} <= 1.0) indicating the position along the curve.
     * @return The {@code Point} on the Bezier curve corresponding to the parameter {@code t}.
     */
    private Point getBezierPoint(Point p0, Point p1, Point p2, Point p3, double t) {
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
     * @param start The starting point of the movement segment.
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
}
