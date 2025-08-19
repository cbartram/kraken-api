package com.kraken.api.core.script.node;

import com.kraken.api.core.script.BehaviorNode;

import java.util.ArrayList;
import java.util.List;

public abstract class CompositeBehaviorNode implements BehaviorNode {
    protected final List<BehaviorNode> children = new ArrayList<>();

    public CompositeBehaviorNode addChild(BehaviorNode child) {
        children.add(child);
        return this;
    }

    public CompositeBehaviorNode addChildren(BehaviorNode... children) {
        for (BehaviorNode child : children) {
            this.children.add(child);
        }
        return this;
    }

    @Override
    public void reset() {
        children.forEach(BehaviorNode::reset);
    }
}
