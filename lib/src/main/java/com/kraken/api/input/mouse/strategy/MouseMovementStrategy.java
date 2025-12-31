package com.kraken.api.input.mouse.strategy;

import com.google.inject.Singleton;
import com.kraken.api.input.mouse.strategy.bezier.BezierStrategy;
import com.kraken.api.input.mouse.strategy.instant.InstantStrategy;
import com.kraken.api.input.mouse.strategy.linear.LinearStrategy;
import com.kraken.api.input.mouse.strategy.none.NoMovement;
import com.kraken.api.input.mouse.strategy.replay.ReplayStrategy;
import com.kraken.api.input.mouse.strategy.wind.WindStrategy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

@Slf4j
@Singleton
public enum MouseMovementStrategy {
    NO_MOVEMENT(NoMovement.class),
    INSTANT(InstantStrategy.class),
    LINEAR(LinearStrategy.class),
    BEZIER(BezierStrategy.class),
    WIND(WindStrategy.class),
    REPLAY(ReplayStrategy.class);
    
    @Getter
    private final MoveableMouse strategy;
    
    MouseMovementStrategy(Class<? extends MoveableMouse> clazz) {
        this.strategy = RuneLite.getInjector().getInstance(clazz);
    }

    /**
     * Loads a specified library into the current {@code MouseMovementStrategy} instance.
     *
     * <p>This method delegates the library loading process to the active movement strategy
     * if it is of type {@code ReplayStrategyMoveable}. The actual loading operation is
     * handled by the underlying {@code ReplayStrategyMoveable} object.</p>
     *
     * <ul>
     *   <li>If the active strategy is not an instance of {@code ReplayStrategyMoveable}, this method does nothing.</li>
     *   <li>If the specified library name is invalid or null, it may result in no operation or an error within the
     *       {@code ReplayStrategyMoveable} implementation.</li>
     * </ul>
     *
     * @param libraryName The name of the library to be loaded. This must be a valid,
     *                    non-null string representing the name or path of the library
     *                    to ensure successful loading.
     */
    public void loadLibrary(String libraryName) {
        if (strategy instanceof ReplayStrategy) {
            ((ReplayStrategy) strategy).loadLibrary(libraryName);
        } else {
            log.warn("Strategy: {} does not require mouse gesture libraries to be loaded.", name());
        }
    }
}
