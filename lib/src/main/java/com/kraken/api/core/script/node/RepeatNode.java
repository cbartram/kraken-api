package com.kraken.api.core.script.node;

import com.kraken.api.core.script.BehaviorNode;
import com.kraken.api.core.script.BehaviorResult;

public class RepeatNode extends DecoratorNode {
    private final int maxRepeats;
    private int currentRepeats = 0;

    public RepeatNode(BehaviorNode child, int maxRepeats) {
        super(child);
        this.maxRepeats = maxRepeats;
    }

    @Override
    public BehaviorResult execute() {
        while (currentRepeats < maxRepeats) {
            BehaviorResult result = child.execute();

            switch (result) {
                case FAILURE:
                    reset();
                    return BehaviorResult.FAILURE;
                case RUNNING:
                    return BehaviorResult.RUNNING;
                case SUCCESS:
                    currentRepeats++;
                    child.reset();
                    break;
            }
        }

        reset();
        return BehaviorResult.SUCCESS;
    }

    @Override
    public void reset() {
        currentRepeats = 0;
        super.reset();
    }
}
