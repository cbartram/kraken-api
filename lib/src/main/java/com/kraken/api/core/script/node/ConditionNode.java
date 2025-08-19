package com.kraken.api.core.script.node;

import com.kraken.api.core.script.BehaviorNode;
import com.kraken.api.core.script.BehaviorResult;

@FunctionalInterface
public interface ConditionNode extends BehaviorNode {
    boolean checkCondition();

    @Override
    default BehaviorResult execute() {
        return checkCondition() ? BehaviorResult.SUCCESS : BehaviorResult.FAILURE;
    }
}
