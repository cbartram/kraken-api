package com.kraken.api.core.script;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.events.GameTick;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
public abstract class Script implements Scriptable {

    @Inject
    private EventBus eventBus;

    private Future<?> future = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private boolean isRunning = false;
    private final String name;

    public Script() {
        this.name = this.getClass().getName();
    }

    /**
     * Executes a specific loop logic and returns an integer result based on the implementation.
     * <p>
     * This abstract method needs to be implemented by subclasses to define the specific
     * behavior of the loop.
     * </p>
     *
     * @return an integer value representing the amount of time to sleep in milliseconds. Since this is called every game
     *  tick, any value <= 600 will execute on the next game tick.
     *
     * <p><strong>Example Usage:</strong></p>
     * <pre>{@code
     * public class CustomScript extends Script {
     *     @Override
     *     public int loop() {
     *         // Do something to automate the game
     *         return 100;
     *     }
     * }
     * }</pre>
     */
    public abstract int loop();

    /**
     * Optional: Called when the script starts.
     */
    public void onStart() {}

    /**
     * Optional: Called when the script stops.
     */
    public void onStop() {}

    /**
     * Starts the script execution, initializing necessary components and marking the script as running.
     * <p>
     * This method transitions the script into a "running" state by performing the following steps:
     * </p>
     * <ul>
     *   <li>Verifies if the script is already running; if so, the method returns immediately.</li>
     *   <li>Sets the internal {@code isRunning} flag to {@code true} to indicate that the script is running.</li>
     *   <li>Registers the script instance to the {@code eventBus} for event handling.</li>
     *   <li>Generates a log entry indicating that the script has started.</li>
     *   <li>Invokes the {@link #onStart()} method to allow subclasses to define custom startup logic.</li>
     * </ul>
     *
     * <h3>Thread-Safety</h3>
     * <p>
     * This method ensures thread-safe initialization of the script's state. However, external synchronization may
     * be required if the method is invoked from multiple threads.
     * </p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>If the script is already running, no further actions are performed.</li>
     *   <li>Otherwise, the script is initialized, event handling is enabled, and startup logic is executed.</li>
     * </ul>
     *
     * <h3>Example Usage</h3>
     * <p>Used during the initialization process of a script:</p>
     * <pre>
     * <code>
     * // Called during the plugin's start-up phase to launch the script
     * {@literal @Override}
     * protected void startUp() {
     *     context.register();
     *     context.initializePackets();
     *     exampleScript.start(); // Start the script
     *
     *     overlayManager.add(overlay);
     * }
     * </code>
     * </pre>
     */
    public final void start() {
        if (isRunning) return;
        isRunning = true;
        eventBus.register(this);
        log.info("[{}] script started", this.name);
        onStart();
    }

    /**
     * Pauses the execution of the script.
     * <p>
     * This method halts the script's execution by setting the internal {@code isRunning} flag to {@code false}.
     * If the script is already paused, invoking this method will have no effect. A log entry is generated
     * to indicate the transition to the paused state.
     * </p>
     *
     * <h3>Behavior</h3>
     * <ul>
     *   <li>If the script is running ({@code isRunning == true}), the method sets {@code isRunning} to {@code false} and logs the pause action.</li>
     *   <li>If the script is already paused or not running, the method performs no actions.</li>
     * </ul>
     *
     * <h3>Thread-Safety</h3>
     * <p>
     * Ensure thread-safe access to the script's state when invoking this method to prevent race conditions.
     * </p>
     *
     * <h3>Example Usage</h3>
     * <p>Used when a configuration change should trigger the script to pause:</p>
     * <pre>
     * <code>
     * {@literal @Subscribe}
     * private void onConfigChanged(final ConfigChanged event) {
     *     if (event.getGroup().equals("testapi") && event.getKey().equalsIgnoreCase("pauseScript")) {
     *         if (config.pauseScript()) {
     *             exampleScript.pause();
     *         } else {
     *             exampleScript.resume();
     *         }
     *     }
     * }
     * </code>
     * </pre>
     */
    public final void pause() {
        if(isRunning) {
            isRunning = false;
            log.info("[{}] script paused", this.name);
        }
    }

    /**
     * Resumes the execution of the script if it is currently paused.
     * <p>
     * This method transitions the script's state to running by setting the
     * internal {@code isRunning} flag to {@code true}. During this process,
     * a log entry is generated to indicate that the script has been resumed.
     * If the script is already running, this method does nothing.
     * </p>
     * <h3>Behavior</h3>
     * <ul>
     * <li>If the script is paused ({@code isRunning == false}), the method
     * sets {@code isRunning} to {@code true} and logs the resumption.</li>
     * <li>If the script is already running, the method performs no actions.</li>
     * </ul>
     * <h3>Thread-Safety</h3>
     * <p>Ensure thread-safe access to the script's state before calling this method.</p>
     *
     * <h3>Example Usage:</h3>
     * <pre>
     * <code>
     * {@literal @Subscribe}
     * private void onConfigChanged(final ConfigChanged event) {
     *     if (event.getGroup().equals("testapi") && event.getKey().equalsIgnoreCase("pauseScript")) {
     *         if (config.pauseScript()) {
     *             exampleScript.pause();
     *         } else {
     *             exampleScript.resume();
     *         }
     *     }
     * }
     * </code>
     * </pre>
     */
    public final void resume() {
        if(!isRunning) {
            isRunning = true;
            log.info("[{}] script resumed", this.name);
        }
    }

    /**
     * Handles actions to be executed on each game tick event while the script is running.
     * <p>
     * This method is triggered by the {@code GameTick} event, which occurs at consistent 0.6s intervals in the game.
     * It coordinates the execution of the script's main logic by invoking the {@link #loop()} method on a separate thread.
     * </p>
     *
     * <h3>Key Behavior:</h3>
     * <ul>
     *     <li>Ensures that the script is running before proceeding. If {@code isRunning} is {@code false}, the method returns immediately.</li>
     *     <li>Skips execution if a previous {@code loop()} call is still in progress, indicated by the {@code future} object.</li>
     *     <li>Submits the {@code loop()} logic to an {@code executor} service for asynchronous execution.</li>
     *     <li>If a delay is set by the {@code loop()} method, the thread sleeps for the specified duration before proceeding.</li>
     *     <li>Gracefully handles and logs exceptions thrown during the loop execution.</li>
     *     <li>Cleans up thread-local resources by calling {@link RunnableTask#dispose()}.</li>
     * </ul>
     *
     * <h3>Threading Model:</h3>
     * <p>
     * The main game logic defined in {@link #loop()} is executed asynchronously to avoid blocking the main game thread.
     * This separation allows for the use of thread sleeps and other blocking operations within the loop's logic.
     * </p>
     *
     * @param event an instance of {@code GameTick} representing a single tick of the game clock.
     *             It triggers all logic tied to the game's periodic updates.
     *
     * <h3>Example Usage:</h3>
     * <pre>
     * <code>
     * public class ExampleScript extends Script {
     *
     *     {@literal @Override}
     *     public int loop() {
     *         log.info("Executing game logic...");
     *         // Perform actions such as pathfinding or combat
     *         return 1000; // Delay in milliseconds before the next execution
     *     }
     * }
     * </code>
     * </pre>
     */
    @Subscribe
    public final void onGameTick(GameTick event) {
        if (!isRunning) return;

        // If we are sleeping as part of loop() skip calling loop again this game tick.
        if (future != null && !future.isDone()) return;

        future = executor.submit(() -> {
            try {
                int delay = loop();
                if (delay > 0) {
                    Thread.sleep(delay);
                }
            } catch (InterruptedException e) {
                // Thread interrupted, likely due to stop() being called
            } catch (Exception e) {
                log.error("[{}] Error in script:", this.name, e);
            } finally {
                RunnableTask.dispose();
            }
        });
    }

    /**
     * Stops the current execution loop and performs the necessary cleanup.
     * This method safely shuts down the process, unregisters the instance
     * from the event bus, and triggers the provided callback after successful termination.
     *
     * <p>Behavior:</p>
     * <ul>
     *     <li>Sets the running status to {@code false} if the process is active.</li>
     *     <li>Unregisters the instance from the event bus.</li>
     *     <li>Cancels the associated {@code RunnableTask}.</li>
     *     <li>Waits for the asynchronous {@code future} to complete before invoking the {@code callback}.</li>
     * </ul>
     *
     * @param callback A {@code Runnable} that will execute after the stop operation is complete;
     *                 can be {@code null} if no action is required after stopping.
     */
    public void stop(Runnable callback) {
        if (!isRunning) return;
        isRunning = false;
        eventBus.unregister(this);

        if(future == null || future.isDone()) callback.run();

        log.info("[{}] Stopping script...", this.name);
        RunnableTask.cancel();
        executor.submit(() -> {
            try {
                while(!future.isDone()) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {}
                }
                log.info("[{}] Script stopped", this.name);
                onStop();
                if(callback != null) callback.run();
            } catch (Exception e) {
                log.error("[{}] Task execution failed: ", this.name, e);
            }
        });
    }

    /**
     * Gracefully stops a running asynchronous loop.
     */
    public void stop() {
        stop(() -> {});
    }
}