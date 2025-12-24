package com.kraken.api.input.mouse.strategy.replay;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.input.mouse.model.NormalizedPath;
import com.kraken.api.input.mouse.strategy.MoveableMouse;
import com.kraken.api.input.mouse.strategy.bezier.BezierStrategy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;

@Slf4j
@Singleton
public class ReplayStrategy implements MoveableMouse {

    @Inject
    private Client client;

    @Inject
    private BezierStrategy bezierStrategy;

    @Getter
    private Point lastPoint = new Point(0, 0);

    private String libraryName;
    private List<NormalizedPath> library;

    @Override
    public Canvas getCanvas() {
        return client.getCanvas();
    }

    @Override
    public void move(Point target) {
    if(libraryName == null || library == null || library.isEmpty()) {
            log.error("Cannot move mouse, no library was loaded or loaded library contained no usable data. Use loadLibrary() to load a specific library.");
            return;
        }

        // TODO Always starts with (0,0)
        Point start;
        if(getCanvas().getMousePosition() == null) {
            start = new Point(0, 0);
        } else {
            start = new Point(getCanvas().getMousePosition().x, getCanvas().getMousePosition().y);
        }

        double distance = start.distanceTo(target);
        NormalizedPath template = PathLibrary.getSimilarPath(library, distance);

        if (template != null) {
            // It is safer to calculate a NEW duration based on distance rather than reusing
            // the recorded duration. This avoids stretching the path
            long duration = calculateDuration(distance);
            List<MotionFactory.TimedPoint> path = MotionFactory.transform(template, start, target, duration);
            executePath(path);
        } else {
            log.warn("No similar mouse gesture found for library '{}' distance {}, falling back to bezier movement", libraryName, distance);
            bezierStrategy.move(target);
            lastPoint = target;
        }
    }

    /**
     * Loads a specified library by name and initializes it within the current context.
     *
     * <p>This method sets the library name to the provided parameter and delegates
     * the actual loading process to the {@literal PathLibrary} class.</p>
     *
     * @param library The name of the library to be loaded. This must be a valid,
     *                non-null string representing the name or path of the library.
     */
    public void loadLibrary(String library) {
        this.libraryName = library;
        this.library = PathLibrary.load(library);
    }

    /**
     * Executes a sequence of mouse movements along a timed path, simulating realistic motion.
     *
     * <p>This method processes a list of {@literal MotionFactory.TimedPoint} objects, where each point specifies
     * a target position (x, y) and a time at which the mouse event should occur. The method calculates
     * and waits for the appropriate time before dispatching a {@literal MouseEvent} to the
     * AWT {@literal Canvas} associated with the VirtualMouse. The mouse cursor is moved to each point
     * in the sequence with high precision, mimicking human-like behavior.</p>
     *
     * <ul>
     *   <li>If sufficient time is available before the next scheduled point, the thread sleeps for accuracy.</li>
     *   <li>For very short delays (less than 2 milliseconds), a busy-wait loop is used.</li>
     *   <li>Dispatches a {@literal MouseEvent} with type {@literal MouseEvent.MOUSE_MOVED} for each point, updating the mouse's position.</li>
     * </ul>
     *
     * @param path A {@literal List<MotionFactory.TimedPoint>} representing the movement path.
     *             Each {@literal TimedPoint} contains:
     *             <ul>
     *               <li>{@code x}: The x-coordinate of the destination point.</li>
     *               <li>{@code y}: The y-coordinate of the destination point.</li>
     *               <li>{@code time}: The timestamp (in milliseconds) at which the movement should occur.</li>
     *             </ul>
     */
    private void executePath(List<MotionFactory.TimedPoint> path) {
        MouseEvent event = new MouseEvent(
                getCanvas(), MouseEvent.MOUSE_MOVED,
                System.currentTimeMillis(),
                0, 0, 0, 0, false
        );
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

            event = new MouseEvent(
                    getCanvas(), MouseEvent.MOUSE_MOVED,
                    System.currentTimeMillis(),
                    0, p.x, p.y, 0, false
            );
            getCanvas().dispatchEvent(event);
        }

        lastPoint = new Point(event.getX(), event.getY());
    }


    /**
     * Calculates the duration needed for a movement of a specific distance.
     *
     * <p>The duration is determined by combining a base reaction time, a distance-based factor,
     * and a random variation. The result simulates a natural and imperfect human-like movement.</p>
     *
     * @param distance The distance in an abstract unit that represents the movement to perform.
     *                 This must be a non-negative value.
     * @return The calculated duration in milliseconds as a {@code long}.
     */
    private long calculateDuration(double distance) {
        // Base reaction + movement time based on distance
        return (long) (120 + (distance * 0.4) + (Math.random() * 50));
    }
}
