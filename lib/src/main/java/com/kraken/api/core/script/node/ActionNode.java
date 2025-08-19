package com.kraken.api.core.script.node;

import com.kraken.api.core.script.BehaviorNode;
import com.kraken.api.core.script.BehaviorResult;

@FunctionalInterface
public interface ActionNode extends BehaviorNode {
    BehaviorResult performAction();

    @Override
    default BehaviorResult execute() {
        return performAction();
    }
}
