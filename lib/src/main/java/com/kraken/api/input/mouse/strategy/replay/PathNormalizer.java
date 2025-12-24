package com.kraken.api.input.mouse.strategy.replay;

import com.kraken.api.input.mouse.model.MouseGesture;
import com.kraken.api.input.mouse.model.NormalizedPath;
import com.kraken.api.input.mouse.model.RecordedPoint;
import com.kraken.api.input.mouse.model.UnitPoint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * The {@literal PathNormalizer} class is responsible for standardizing a raw mouse gesture path
 * into a normalized and reusable format. Normalized paths simplify comparisons
 * of different gestures by transforming them into a generic dataset that operates
 * in a normalized coordinate and time space.
 * </p>
 *
 * <p>
 * This allows the resulting path to be device-independent and facilitates further analysis,
 * validation, and categorization of gesture behavior.
 * </p>
 *
 * <p>
 * The {@literal normalize()} method returns a {@literal NormalizedPath}, which encapsulates
 * relevant metadata and the transformed points representing the gesture.
 * </p>
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PathNormalizer {

    /**
     * Normalizes a {@literal @}link MouseGesture into a unit-scaled {@literal @}link NormalizedPath representation.
     * <p>
     * This method processes the raw motion data of a gesture to create a scaled, translated, and rotated form
     * that aligns with the X-axis and maps all data points into a 0.0 to 1.0 range for spatial and temporal dimensions.
     * The normalization ensures geometric and temporal consistency for gesture comparison.
     * </p>
     *
     * @param raw The {@literal @}link MouseGesture instance containing raw input data to be normalized.
     *            Must include all point data, start and end coordinates, duration, and other metadata.
     *            If the input gesture has zero or near-zero distance, {@code null} is returned.
     *
     * @return A normalized {@literal @}link NormalizedPath object containing:
     *         <ul>
     *         <li>The gesture's original label.</li>
     *         <li>The original distance and duration.</li>
     *         <li>A list of {@literal @}link UnitPoint instances, each representing a normalized spatial
     *             and temporal data point.</li>
     *         </ul>
     *         If the gesture distance is near zero, {@code null} is returned.
     */
    public static NormalizedPath normalize(MouseGesture raw) {
        // 1. Calculate the geometry of the raw movement
        double dx = raw.getEndX() - raw.getStartX();
        double dy = raw.getEndY() - raw.getStartY();

        double distance = Math.sqrt(dx * dx + dy * dy);

        // Prevent division by zero for instant/zero-distance clicks
        if (distance < 1.0) {
            return null;
        }

        // Calculate the angle required to rotate this path to the X-axis
        // We want to rotate 'down' by the angle of the path.
        double angleToXAxis = -Math.atan2(dy, dx);

        List<UnitPoint> normalizedPoints = new ArrayList<>();

        // Transform every point along the path
        for (RecordedPoint p : raw.getPoints()) {

            // Translate the point by shift point so start is at (0,0)
            double tx = p.getX() - raw.getStartX();
            double ty = p.getY() - raw.getStartY();

            // Rotate the point by applying a 2D rotation matrix
            // x' = x cos(θ) - y sin(θ)
            // y' = x sin(θ) + y cos(θ)
            double rx = tx * Math.cos(angleToXAxis) - ty * Math.sin(angleToXAxis);
            double ry = tx * Math.sin(angleToXAxis) + ty * Math.cos(angleToXAxis);

            // Scale the point by dividing by the total distance to fit in [0.0, 1.0) range
            double sx = rx / distance;
            double sy = ry / distance;

            // Finally, normalize the time to [0.0, 1.0)
            double st = (double) p.getTimeOffset() / raw.getDurationMs();

            normalizedPoints.add(new UnitPoint(sx, sy, st));
        }

        // Ensure the last point is exactly (1.0, 0.0) to prevent floating point drift
        if (!normalizedPoints.isEmpty()) {
            UnitPoint last = normalizedPoints.get(normalizedPoints.size() - 1);
            last.setX(1.0);
            last.setY(0.0);
            last.setT(1.0);
        }

        return new NormalizedPath(
                raw.getLabel(),
                distance,
                raw.getDurationMs(),
                normalizedPoints
        );
    }
}
