package com.kraken.api.core.script;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BehaviorTree {
    private final BehaviorNode rootNode;

    @Getter
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
     * Check if the tree is currently running
     * @return True if the behavior tree is running and false otherwise
     */
    public boolean isRunning() {
        return lastResult == BehaviorResult.RUNNING;
    }
}
