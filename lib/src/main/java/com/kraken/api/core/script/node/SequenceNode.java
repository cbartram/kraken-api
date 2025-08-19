package com.kraken.api.core.script.node;

import com.kraken.api.core.script.BehaviorResult;

public class SequenceNode extends CompositeBehaviorNode {
    private int currentIndex = 0;

    @Override
    public BehaviorResult execute() {
        while (currentIndex < children.size()) {
            BehaviorResult result = children.get(currentIndex).execute();

            switch (result) {
                case FAILURE:
                    reset();
                    return BehaviorResult.FAILURE;
                case RUNNING:
                    return BehaviorResult.RUNNING;
                case SUCCESS:
                    currentIndex++;
                    break;
            }
        }

        reset();
        return BehaviorResult.SUCCESS;
    }

    @Override
    public void reset() {
        currentIndex = 0;
        super.reset();
    }
}