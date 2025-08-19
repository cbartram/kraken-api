package com.kraken.api.core.script;

/**
 * Base interface for all behavior tree nodes
 */
public interface BehaviorNode {
    /**
     * Execute this behavior node
     * @return The result of the execution
     */
    BehaviorResult execute();

    /**
     * Reset the node to its initial state
     */
    default void reset() {
        // Default implementation does nothing
    }
}