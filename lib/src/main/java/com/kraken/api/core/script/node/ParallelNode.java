package com.kraken.api.core.script.node;

import com.kraken.api.core.script.BehaviorNode;
import com.kraken.api.core.script.BehaviorResult;

public class ParallelNode extends CompositeBehaviorNode {
    public enum ParallelPolicy {
        REQUIRE_ONE,  // Succeed when at least one child succeeds
        REQUIRE_ALL   // Succeed only when all children succeed
    }

    private final ParallelPolicy successPolicy;
    private final ParallelPolicy failurePolicy;

    public ParallelNode(ParallelPolicy successPolicy, ParallelPolicy failurePolicy) {
        this.successPolicy = successPolicy;
        this.failurePolicy = failurePolicy;
    }

    @Override
    public BehaviorResult execute() {
        int successCount = 0;
        int failureCount = 0;
        int runningCount = 0;

        for (BehaviorNode child : children) {
            BehaviorResult result = child.execute();
            switch (result) {
                case SUCCESS:
                    successCount++;
                    break;
                case FAILURE:
                    failureCount++;
                    break;
                case RUNNING:
                    runningCount++;
                    break;
            }
        }

        // Check failure conditions first
        if (failurePolicy == ParallelPolicy.REQUIRE_ONE && failureCount > 0) {
            return BehaviorResult.FAILURE;
        }
        if (failurePolicy == ParallelPolicy.REQUIRE_ALL && failureCount == children.size()) {
            return BehaviorResult.FAILURE;
        }

        // Check success conditions
        if (successPolicy == ParallelPolicy.REQUIRE_ONE && successCount > 0) {
            return BehaviorResult.SUCCESS;
        }
        if (successPolicy == ParallelPolicy.REQUIRE_ALL && successCount == children.size()) {
            return BehaviorResult.SUCCESS;
        }

        return BehaviorResult.RUNNING;
    }
}