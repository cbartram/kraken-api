package com.kraken.api.core.script.node;

import com.kraken.api.core.script.BehaviorNode;

public abstract class DecoratorNode implements BehaviorNode {
    protected final BehaviorNode child;

    public DecoratorNode(BehaviorNode child) {
        this.child = child;
    }

    @Override
    public void reset() {
        child.reset();
    }
}
