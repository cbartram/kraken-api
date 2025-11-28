package com.kraken.api.core;

public interface Interactable {
    /**
     * Interacts with the entity using the given action verb.
     * @param action The menu action to trigger (e.g., "Attack", "Talk-to", "Take")
     * @return true if the interaction packet was successfully queued/sent
     */
    boolean interact(String action);
    
    // You might also want these common methods
    String getName();
    boolean isNull();
}