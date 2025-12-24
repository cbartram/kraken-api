package com.kraken.api.input.mouse;

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
     * Transforms a normalized path into a specific screen path.
     *
     * @param template The normalized unit path.
     * @param start    Current mouse position.
     * @param end      Target mouse position.
     * @param duration The desired duration of the movement in ms.
     * @return A list of screen coordinates with absolute timestamps.
     */
    public static List<TimedPoint> transform(NormalizedPath template, Point start, Point end, long duration) {
        List<TimedPoint> realPath = new ArrayList<>();

        // 1. Calculate Geometry
        double totalDistance = start.distanceTo(end);
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double angle = Math.atan2(dy, dx); // The angle of the new path

        double sin = Math.sin(angle);
        double cos = Math.cos(angle);

        long startTime = System.currentTimeMillis();

        for (UnitPoint p : template.getPoints()) {
            // A. SCALE: Stretch the unit vector to the target distance
            // We scale X (forward progress) and Y (jitter/deviation) separately if needed,
            // but usually scaling both by totalDistance maintains the "shape".
            double sx = p.getX() * totalDistance;
            double sy = p.getY() * totalDistance; // Keeps jitter proportional

            // B. ROTATE: Rotate the point to match the destination angle
            // Standard 2D Rotation Matrix
            double rx = sx * cos - sy * sin;
            double ry = sx * sin + sy * cos;

            // C. TRANSLATE: Add the start position
            int finalX = (int) (start.getX() + rx);
            int finalY = (int) (start.getY() + ry);

            // D. TIME: Map normalized time (0.0-1.0) to actual duration
            long timeOffset = (long) (p.getT() * duration);

            realPath.add(new TimedPoint(finalX, finalY, startTime + timeOffset));
        }

        return realPath;
    }

    // Simple helper class for the result
    @AllArgsConstructor
    public static class TimedPoint {
        public final int x, y;
        public final long time;
    }
}
