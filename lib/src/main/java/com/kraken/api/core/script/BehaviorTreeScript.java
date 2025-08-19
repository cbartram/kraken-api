package com.kraken.api.core.script;

import com.kraken.api.Context;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * Base class for scripts that use a behavior tree for their logic.
 * Users must implement the buildBehaviorTree() method to define their behavior tree structure.
 */
@Slf4j
public abstract class BehaviorTreeScript extends Script {

    @Getter
    private BehaviorTree behaviorTree;

    public BehaviorTreeScript(final Context context) {
        super(context);
    }

    /**
     * Override this method to build your behavior tree
     * @return The root node of your behavior tree
     */
    protected abstract BehaviorNode buildBehaviorTree();

    @Override
    public void onStart() {
        BehaviorNode rootNode = buildBehaviorTree();
        if (rootNode == null) {
            throw new IllegalStateException("buildBehaviorTree() must return a non-null root node");
        }

        behaviorTree = new BehaviorTree(rootNode);
        log.info("Behavior tree initialized for script: {}", this.getClass().getSimpleName());

        onBehaviorTreeStart();
    }

    @Override
    public final long loop() {
        if (behaviorTree == null) {
            log.error("Behavior tree not initialized");
            return -1; // Stop execution
        }

        BehaviorResult result = behaviorTree.tick();

        // Handle different results
        switch (result) {
            case SUCCESS:
                return onBehaviorTreeSuccess();
            case FAILURE:
                return onBehaviorTreeFailure();
            case RUNNING:
                return onBehaviorTreeRunning();
            default:
                return getDefaultLoopDelay();
        }
    }

    @Override
    public void onEnd() {
        if (behaviorTree != null) {
            behaviorTree.reset();
        }
        onBehaviorTreeEnd();
    }

    /**
     * Called after the behavior tree is initialized but before the first tick
     */
    protected void onBehaviorTreeStart() {
        // Default implementation does nothing
    }

    /**
     * Called when the behavior tree returns SUCCESS
     * @return Delay in milliseconds before the next loop iteration
     */
    protected long onBehaviorTreeSuccess() {
        return getDefaultLoopDelay();
    }

    /**
     * Called when the behavior tree returns FAILURE
     * @return Delay in milliseconds before the next loop iteration
     */
    protected long onBehaviorTreeFailure() {
        return getDefaultLoopDelay();
    }

    /**
     * Called when the behavior tree returns RUNNING
     * @return Delay in milliseconds before the next loop iteration
     */
    protected long onBehaviorTreeRunning() {
        return getDefaultLoopDelay();
    }

    /**
     * Called when the script is ending
     */
    protected void onBehaviorTreeEnd() {
        // Default implementation does nothing
    }

    /**
     * Get the default delay between loop iterations
     * @return Default delay in milliseconds (600ms by default)
     */
    protected long getDefaultLoopDelay() {
        return 600;
    }
}