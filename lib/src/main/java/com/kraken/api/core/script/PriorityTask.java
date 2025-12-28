package com.kraken.api.core.script;

/**
 * Represents a task that includes a priority level, allowing tasks to be prioritized
 * during selection or execution in workflows or script logic.
 * <p>
 * This class extends {@link AbstractTask}, inheriting capabilities such as access to a
 * Context instance for task configuration and runtime behavior.
 * It introduces an additional abstract method for retrieving the priority of the task.
 * </p>
 *
 * <h3>Key Characteristics:</h3>
 * <ul>
 *   <li>Must be subclassed to implement priority-based behavior.</li>
 *   <li>Integrates with the task execution framework through inheritance from
 *       {@link AbstractTask} and the {@link Task} interface.</li>
 * </ul>
 *
 * <h3>Priority Management:</h3>
 * Subclasses are required to define the {@code getPriority()} method, which returns
 * an integer value representing the priority level of the task. Higher priority
 * values generally indicate tasks that should be executed earlier or given precedence
 * over those with lower values. The specific interpretation of priority values is
 * determined by the execution context in which this task is used.
 */
public abstract class PriorityTask extends AbstractTask {
    public abstract int getPriority();
}
