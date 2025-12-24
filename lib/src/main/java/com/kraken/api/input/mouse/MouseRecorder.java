package com.kraken.api.input.mouse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.kraken.api.input.mouse.model.MouseGesture;
import com.kraken.api.input.mouse.model.RecordedPoint;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.input.MouseListener;
import net.runelite.client.input.MouseManager;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.event.MouseEvent;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The <code>MouseRecorder</code> class is responsible for recording mouse gestures
 * such as clicks, movements, and drags. It captures the sequence of mouse events,
 * organizes them into gestures, and writes them to disk for later analysis.
 *
 * <p>This class is designed as a singleton and interacts closely with the
 * <code>MouseManager</code> to register and deregister itself as a listener for mouse events.
 * The recorded gestures can be categorized using labels provided during the recording start,
 * and the gestures are stored in JSON format in a designated directory.
 *
 * <p>Some features of the <code>MouseRecorder</code> include:
 * <ul>
 * <li>Categorizing gestures with user-defined labels</li>
 * <li>Buffering gestures in memory and flushing them to disk in batches</li>
 * <li>Asynchronous writing to avoid blocking event-handling threads</li>
 * </ul>
 *
 * <h3>Thread-Safety</h3>
 * <p>While most operations are single-threaded, writing gestures to the disk is
 * executed asynchronously to ensure that mouse event processing is not blocked.
 * Internal buffers for gesture storage are synchronized to ensure thread safety during flush operations.
 */
@Slf4j
@Singleton
public class MouseRecorder implements MouseListener {

    private final MouseManager mouseManager;
    private final Gson gson;

    private boolean isRecording = false;
    private String currentLabel = "default";

    // Immediate buffer for the current drag/move action
    private final List<RecordedPoint> movementBuffer = new ArrayList<>();

    // Buffer for completed gestures waiting to be written to disk
    private final List<MouseGesture> gestureBuffer = Collections.synchronizedList(new ArrayList<>());

    private long gestureStartTime = -1;

    // Flush to disk every X gestures to save memory
    private static final int BATCH_SIZE = 500;

    private static final String DATA_DIR = System.getProperty("user.home") + "/.runelite/kraken/mouse_data/";

    @Inject
    public MouseRecorder(MouseManager mouseManager) {
        this.mouseManager = mouseManager;
        this.gson = new GsonBuilder().create();

        try {
            Files.createDirectories(Paths.get(DATA_DIR));
        } catch (IOException e) {
            log.error("Failed to create mouse data directory", e);
        }
    }

    /**
     * Starts recording mouse movements and gestures with the given label.
     * <p>
     * This method initializes buffers, sets the current label, and begins recording mouse events.
     *
     * @param label The label to identify the recording session. Spaces in the label are replaced with underscores.
     */
    public void start(String label) {
        if (isRecording) return;

        this.currentLabel = label.replaceAll(" ", "_");
        this.isRecording = true;
        this.movementBuffer.clear();
        this.gestureBuffer.clear();
        this.gestureStartTime = System.currentTimeMillis();

        mouseManager.registerMouseListener(this);
        log.info("Mouse Recording STARTED: {}", label);
    }

    /**
     * Stops the recording of mouse movements and gestures.
     * <p>
     * This method halts the recording session, flushes any pending gestures
     * to disk, and unregisters the mouse listener to stop capturing events.
     * <p>
     * Steps performed:
     * <ul>
     *   <li>Checks if recording is active and proceeds only if it is.</li>
     *   <li>Flushes unsaved gestures to persistent storage.</li>
     *   <li>Unregisters the mouse listener to stop event monitoring.</li>
     *   <li>Logs the action completion with the associated label.</li>
     * </ul>
     */
    public void stop() {
        if (!isRecording) return;

        this.isRecording = false;
        flushGestures(true);
        mouseManager.unregisterMouseListener(this);
        log.info("Mouse Recording STOPPED: {}", currentLabel);
    }

    @Override
    public MouseEvent mousePressed(MouseEvent e) {
        if (!isRecording) return e;

        // A press indicates the previous movement/hover is 'complete' or constitutes a click action
        saveGesture(e);

        movementBuffer.clear();
        gestureStartTime = System.currentTimeMillis();

        return e;
    }

    @Override
    public MouseEvent mouseMoved(MouseEvent e) {
        if (!isRecording) return e;
        recordPoint(e);
        return e;
    }

    @Override
    public MouseEvent mouseDragged(MouseEvent e) {
        if (!isRecording) return e;
        recordPoint(e);
        return e;
    }

    /**
     * Records the coordinates and time offset of a mouse event.
     * <p>
     * This method captures the position of the mouse when triggered by a {@link MouseEvent}
     * and adds it to the movement buffer for tracking. It prevents duplicate entries if
     * the mouse position has not changed since the last recorded point.
     *
     * @param e The {@link MouseEvent} containing the current mouse position to record.
     */
    private void recordPoint(MouseEvent e) {
        if (gestureStartTime == -1) {
            gestureStartTime = System.currentTimeMillis();
        }

        long offset = System.currentTimeMillis() - gestureStartTime;

        if (!movementBuffer.isEmpty()) {
            RecordedPoint last = movementBuffer.get(movementBuffer.size() - 1);
            if (last.getX() == e.getX() && last.getY() == e.getY()) {
                return;
            }
        }

        movementBuffer.add(new RecordedPoint(e.getX(), e.getY(), offset));
    }

    /**
     * Saves the recorded mouse gesture triggered by an event.
     * <p>
     * This method captures a snapshot of the mouse movement data accumulated
     * during a gesture and stores it as a {@link MouseGesture} object for later use.
     * If the buffer reaches its defined batch size, it triggers a memory flush.
     *
     * @param triggerEvent The {@link MouseEvent} that finalized the gesture, e.g., a mouse click.
     */
    private void saveGesture(MouseEvent triggerEvent) {
        if (movementBuffer.isEmpty()) return;

        RecordedPoint startPoint = movementBuffer.get(0);
        long totalDuration = System.currentTimeMillis() - gestureStartTime;
        // Create a copy of points for the object
        List<RecordedPoint> pointsCopy = new ArrayList<>(movementBuffer);

        MouseGesture gesture = new MouseGesture(
                currentLabel,
                totalDuration,
                startPoint.getX(), startPoint.getY(),
                triggerEvent.getX(), triggerEvent.getY(),
                triggerEvent.getButton(),
                pointsCopy
        );

        gestureBuffer.add(gesture);

        // Check if we need to flush memory
        if (gestureBuffer.size() >= BATCH_SIZE) {
            log.info("Gesture buffer full, flushing to disk");
            flushGestures(false);
        }
    }

    /**
     * Flushes the current in-memory buffer to the disk.
     * Use CompletableFuture to avoid blocking the MouseListener (EDT) with File I/O.
     */
    private void flushGestures(boolean sync) {
        List<MouseGesture> batchToWrite;

        // Synchronize just long enough to swap the lists
        synchronized (gestureBuffer) {
            if (gestureBuffer.isEmpty()) return;
            batchToWrite = new ArrayList<>(gestureBuffer);
            gestureBuffer.clear();
        }

        Runnable writeTask = () -> {
            String fileName = currentLabel + ".json";
            Path path = Paths.get(DATA_DIR + fileName);

            try (BufferedWriter writer = Files.newBufferedWriter(path, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                for (MouseGesture g : batchToWrite) {
                    writer.write(gson.toJson(g));
                    writer.newLine();
                }
            } catch (IOException e) {
                log.error("Failed to flush mouse gestures to disk", e);
            }
        };

        if (sync) {
            writeTask.run(); // Run immediately if stopping
        } else {
            CompletableFuture.runAsync(writeTask); // Run in background if mid-recording
        }
    }

    @Override public MouseEvent mouseReleased(MouseEvent e) { return e; }
    @Override public MouseEvent mouseClicked(MouseEvent e) { return e; }
    @Override public MouseEvent mouseEntered(MouseEvent e) { return e; }
    @Override public MouseEvent mouseExited(MouseEvent e) { return e; }
}