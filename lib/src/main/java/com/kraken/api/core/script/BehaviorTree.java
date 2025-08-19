package com.kraken.api.core.script;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BehaviorTree {
    private final BehaviorNode rootNode;
    private BehaviorResult lastResult = BehaviorResult.FAILURE;

    public BehaviorTree(BehaviorNode rootNode) {
        this.rootNode = rootNode;
    }

    /**
     * Execute one tick of the behavior tree
     * @return The result of the tree execution
     */
    public BehaviorResult tick() {
        try {
            lastResult = rootNode.execute();
            return lastResult;
        } catch (Exception e) {
            log.error("Exception during behavior tree execution", e);
            return BehaviorResult.FAILURE;
        }
    }

    /**
     * Reset the entire tree to its initial state
     */
    public void reset() {
        rootNode.reset();
        lastResult = BehaviorResult.FAILURE;
    }

    /**
     * Get the result of the last tree execution
     */
    public BehaviorResult getLastResult() {
        return lastResult;
    }

    /**
     * Check if the tree is currently running
     */
    public boolean isRunning() {
        return lastResult == BehaviorResult.RUNNING;
    }
}
