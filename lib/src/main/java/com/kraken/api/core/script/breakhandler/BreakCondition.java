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
     * @return The custom description for the reason why the break was taken. This shows up in the logs.
     */
    default String getDescription() {
        return "Custom break condition";
    }
}