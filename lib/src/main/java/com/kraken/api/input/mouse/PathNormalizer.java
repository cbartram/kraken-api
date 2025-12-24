package com.kraken.api.input.mouse;

import com.kraken.api.input.mouse.model.MouseGesture;
import com.kraken.api.input.mouse.model.NormalizedPath;
import com.kraken.api.input.mouse.model.RecordedPoint;
import com.kraken.api.input.mouse.model.UnitPoint;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * <p>
 * The {@literal PathNormalizer} class is responsible for standardizing a raw mouse gesture path
 * into a normalized and reusable format. Normalized paths simplify comparisons
 * of different gestures by transforming them into a generic dataset that operates
 * in a normalized coordinate and time space.
 * </p>
 *
 * <p>
 * Specifically, this class performs the following transformation steps:
 * </p>
 *
 * <ul>
 *     <li><b>Translation:</b> Shifts the entire path so its origin starts at (0,0).</li>
 *     <li><b>Rotation:</b> Rotates the path such that it aligns with the X-axis.</li>
 *     <li><b>Scaling:</b> Scales the path to fit within a unit size of 1.0 while preserving proportions.</li>
 *     <li><b>Time Normalization:</b> Adjusts the time dimension to a 0.0 to 1.0 range.</li>
 * </ul>
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
public class PathNormalizer {

    /**
     * Normalizes a list of {@literal @<MouseGesture>} instances by converting each gesture
     * into a {@literal @<NormalizedPath>} using the {@code normalize} method.
     * This allows mouse gesture data to be transformed into a standard, reusable format.
     *
     * <p>The normalization process involves scaling and rotating the raw movement paths
     * so that they fit into a unit-length path starting at (0, 0) and ending at (1, 0).
     * This ensures consistency across gestures while retaining key shape and timing characteristics.
     *
     * @param gestures A list of {@literal @<MouseGesture>} objects representing raw
     *                 gesture inputs recorded during user interactions.
     *                 Each gesture contains positional, timing, and metadata on the movement.
     *
     * @return A list of {@literal @<NormalizedPath>} objects, where each path corresponds
     *         to a normalized representation of an input gesture. The result retains
     *         the key characteristics of the input gestures but is guaranteed to conform
     *         to a standardized format.
     */
    public List<NormalizedPath> normalizeAll(List<MouseGesture> gestures) {
        return gestures.stream().map(this::normalize).collect(Collectors.toList());
    }

    /**
     * Converts a raw, specific mouse gesture into a generic, reusable unit path.
     *
     * @param raw The raw recording from the MouseRecorder.
     * @return A normalized path starting at (0,0) and ending at (1,0).
     */
    public NormalizedPath normalize(MouseGesture raw) {
        // 1. Calculate the geometry of the raw movement
        double dx = raw.getEndX() - raw.getStartX();
        double dy = raw.getEndY() - raw.getStartY();

        double distance = Math.sqrt(dx * dx + dy * dy);

        // Prevent division by zero for instant/zero-distance clicks
        if (distance < 1.0) {
            return null; // Or handle as a special "zero move" case
        }

        // 2. Calculate the angle required to rotate this path to the X-axis
        // We want to rotate 'down' by the angle of the path.
        double angleToXAxis = -Math.atan2(dy, dx);

        List<UnitPoint> normalizedPoints = new ArrayList<>();

        // 3. Transform every point
        for (RecordedPoint p : raw.getPoints()) {

            // A. TRANSLATE: Shift point so start is at (0,0)
            double tx = p.getX() - raw.getStartX();
            double ty = p.getY() - raw.getStartY();

            // B. ROTATE: Apply 2D rotation matrix
            // x' = x cos(θ) - y sin(θ)
            // y' = x sin(θ) + y cos(θ)
            double rx = tx * Math.cos(angleToXAxis) - ty * Math.sin(angleToXAxis);
            double ry = tx * Math.sin(angleToXAxis) + ty * Math.cos(angleToXAxis);

            // C. SCALE: Divide by total distance to fit in 0.0 - 1.0 range
            double sx = rx / distance;
            double sy = ry / distance;

            // D. NORMALIZE TIME: 0.0 to 1.0
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
