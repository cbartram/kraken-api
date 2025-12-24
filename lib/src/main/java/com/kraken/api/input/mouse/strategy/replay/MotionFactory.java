package com.kraken.api.input.mouse.strategy.replay;

import com.kraken.api.input.mouse.model.NormalizedPath;
import com.kraken.api.input.mouse.model.UnitPoint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import net.runelite.api.Point;

import java.util.ArrayList;
import java.util.List;


@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MotionFactory {

    /**
     * Transforms a normalized path into a real-world path by scaling, rotating, translating,
     * and timing its points based on the given start and end coordinates and the duration of the motion.
     * <p>
     * The transformation is achieved in the following steps:
     * <ul>
     *   <li>Scaling: Scales the normalized coordinates to match the target distance between the start and end points.</li>
     *   <li>Rotation: Rotates the points to align with the angle between the start and end points.</li>
     *   <li>Translation: Translates the points to the starting position.</li>
     *   <li>Timing: Maps the normalized time of each point to the provided duration.</li>
     * </ul>
     * </p>
     *
     * @param template The {@literal @link NormalizedPath} containing the normalized unit points to be transformed.
     *                 It defines the shape and time distribution of the motion path.
     * @param start The {@literal @link Point} specifying the starting position of the motion.
     * @param end The {@literal @link Point} specifying the ending position of the motion.
     * @param duration A {@literal long} value representing the total duration of the motion in milliseconds.
     * @return A {@literal List<TimedPoint>} where each {@literal TimedPoint} represents a point on the real-world motion path
     *         with x, y coordinates and an associated timestamp.
     */
    public static List<TimedPoint> transform(NormalizedPath template, Point start, Point end, long duration) {
        List<TimedPoint> realPath = new ArrayList<>();

        double totalDistance = start.distanceTo(end);
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double angle = Math.atan2(dy, dx); // The angle of the new path

        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        long startTime = System.currentTimeMillis();

        for (UnitPoint p : template.getPoints()) {
            // Scale and stretch the unit vector to the target distance
            // We scale X (forward progress) and Y (jitter/deviation)
            double sx = p.getX() * totalDistance;
            double sy = p.getY() * totalDistance; // Keeps jitter proportional

            // Rotate the point to match the destination angle
            // Standard 2D Rotation Matrix
            double rx = sx * cos - sy * sin;
            double ry = sx * sin + sy * cos;

            // Add the start position
            int finalX = (int) (start.getX() + rx);
            int finalY = (int) (start.getY() + ry);

            // Map normalized time [0.0-1.0) to actual duration
            long timeOffset = (long) (p.getT() * duration);

            realPath.add(new TimedPoint(finalX, finalY, startTime + timeOffset));
        }

        return realPath;
    }

    @AllArgsConstructor
    public static class TimedPoint {
        public final int x, y;
        public final long time;
    }
}
