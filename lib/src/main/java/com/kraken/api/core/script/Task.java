package com.kraken.api.core.script;

/**
 * Represents a generic task that can be executed as part of a workflow or script logic.
 * <p>
 * This interface provides three primary methods that allow implementers to define:
 * <ul>
 *   <li>Validation logic to determine whether the task should be executed.</li>
 *   <li>The core execution logic of the task.</li>
 *   <li>A status string for display or tracking purposes.</li>
 * </ul>
 */
public interface Task {
    /**
     * Checks if this task should currently be executed.
     * @return true if the task is valid, false otherwise.
     */
    boolean validate();

    /**
     * Executes the task logic.
     * @return The number of milliseconds to sleep after execution.
     */
    int execute();

    /**
     * Returns the name of the status for display.
     * @return Status string.
     */
    String status();
}
