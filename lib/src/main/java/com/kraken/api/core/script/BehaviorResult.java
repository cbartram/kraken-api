package com.kraken.api.core.script;

/**
 * Enum representing the result of behavior execution
 */
public enum BehaviorResult {
    SUCCESS,    // Node completed successfully
    FAILURE,    // Node failed to complete
    RUNNING     // Node is still executing (for async operations)
}
