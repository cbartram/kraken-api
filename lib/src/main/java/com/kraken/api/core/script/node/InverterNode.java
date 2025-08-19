package com.kraken.api.core.script.node;

import com.kraken.api.core.script.BehaviorNode;
import com.kraken.api.core.script.BehaviorResult;

public class InverterNode extends DecoratorNode {
    public InverterNode(BehaviorNode child) {
        super(child);
    }

    @Override
    public BehaviorResult execute() {
        BehaviorResult result = child.execute();
        switch (result) {
            case SUCCESS:
                return BehaviorResult.FAILURE;
            case FAILURE:
                return BehaviorResult.SUCCESS;
            case RUNNING:
            default:
                return result;
        }
    }
}