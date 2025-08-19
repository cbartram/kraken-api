package com.kraken.api.core.script.node;

import com.kraken.api.core.script.BehaviorNode;
import com.kraken.api.core.script.BehaviorResult;

public class RetryNode extends DecoratorNode {
    private final int maxRetries;
    private int currentRetries = 0;

    public RetryNode(BehaviorNode child, int maxRetries) {
        super(child);
        this.maxRetries = maxRetries;
    }

    @Override
    public BehaviorResult execute() {
        while (currentRetries <= maxRetries) {
            BehaviorResult result = child.execute();

            switch (result) {
                case SUCCESS:
                    reset();
                    return BehaviorResult.SUCCESS;
                case RUNNING:
                    return BehaviorResult.RUNNING;
                case FAILURE:
                    currentRetries++;
                    if (currentRetries <= maxRetries) {
                        child.reset();
                    }
                    break;
            }
        }

        reset();
        return BehaviorResult.FAILURE;
    }

    @Override
    public void reset() {
        currentRetries = 0;
        super.reset();
    }
}
