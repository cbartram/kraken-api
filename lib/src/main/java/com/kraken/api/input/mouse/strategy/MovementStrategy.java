package com.kraken.api.input.mouse.strategy;

import com.kraken.api.input.mouse.strategy.bezier.BezierStrategy;
import com.kraken.api.input.mouse.strategy.instant.InstantStrategy;
import com.kraken.api.input.mouse.strategy.linear.LinearStrategy;
import com.kraken.api.input.mouse.strategy.none.NoMovementStrategy;
import com.kraken.api.input.mouse.strategy.replay.ReplayStrategy;
import lombok.Getter;
import net.runelite.client.RuneLite;

public enum MovementStrategy {
    NO_MOVEMENT(NoMovementStrategy.class),
    INSTANT(InstantStrategy.class),
    LINEAR(LinearStrategy.class),
    BEZIER(BezierStrategy.class),
    REPLAY(ReplayStrategy.class);
    
    @Getter
    private final MouseMovementStrategy strategy;
    
    MovementStrategy(Class<? extends MouseMovementStrategy> clazz) {
        this.strategy = RuneLite.getInjector().getInstance(clazz);
    }
}
