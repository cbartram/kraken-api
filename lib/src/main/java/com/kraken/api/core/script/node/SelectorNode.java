package com.kraken.api.core.script.node;

import com.kraken.api.core.script.BehaviorResult;

public class SelectorNode extends CompositeBehaviorNode {
    private int currentIndex = 0;

    @Override
    public BehaviorResult execute() {
        while (currentIndex < children.size()) {
            BehaviorResult result = children.get(currentIndex).execute();

            switch (result) {
                case SUCCESS:
                    reset();
                    return BehaviorResult.SUCCESS;
                case RUNNING:
                    return BehaviorResult.RUNNING;
                case FAILURE:
                    currentIndex++;
                    break;
            }
        }

        reset();
        return BehaviorResult.FAILURE;
    }

    @Override
    public void reset() {
        currentIndex = 0;
        super.reset();
    }
}