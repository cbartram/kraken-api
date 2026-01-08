package com.kraken.api.core.script.breakhandler;

@FunctionalInterface
public interface BreakCondition {
    /**
     * Determines if a break should be triggered.
     *
     * @return true if the condition for taking a break is met
     */
    boolean shouldBreak();

    /**
     * Optional description of the condition for logging purposes.
     */
    default String getDescription() {
        return "Custom break condition";
    }
}