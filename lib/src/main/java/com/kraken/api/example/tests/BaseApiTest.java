package com.kraken.api.example.tests;

import com.google.inject.Inject;
import com.kraken.api.example.ExampleConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class BaseApiTest {

    @Inject
    protected ExampleConfig config;

    /**
     * Executes the test in a separate thread and returns a CompletableFuture with the result
     * @return CompletableFuture<Boolean> - true if test passed, false if failed
     */
    public CompletableFuture<Boolean> executeTest() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting {} test...", getTestName());
                boolean result = runTest();
                log.info("{} test completed with result: {}", getTestName(), result ? "PASSED" : "FAILED");
                return result;
            } catch (Exception e) {
                log.error("{} test failed with exception", getTestName(), e);
                return false;
            }
        });
    }

    /**
     * The actual test implementation - should be overridden by concrete test classes
     * @return true if test passed, false if failed
     */
    protected abstract boolean runTest();

    /**
     * Returns the name of this test for logging purposes
     * @return test name
     */
    protected abstract String getTestName();

    /**
     * Helper method to perform assertion-style checks
     * @param condition the condition to check
     * @param message the message to log if condition fails
     * @return the condition result
     */
    protected boolean assertThat(boolean condition, String message) {
        if (!condition) {
            log.error("Test assertion failed: {}", message);
        } else if (config.showDebugInfo()) {
            log.debug("Test assertion passed: {}", message);
        }
        return condition;
    }

    /**
     * Helper method to check if a value is not null
     * @param value the value to check
     * @param message the message to log if value is null
     * @return true if value is not null
     */
    protected boolean assertNotNull(Object value, String message) {
        return assertThat(value != null, message + " (was null)");
    }

    /**
     * Helper method to check if two values are equal
     * @param expected the expected value
     * @param actual the actual value
     * @param message the message to log if values don't match
     * @return true if values are equal
     */
    protected boolean assertEquals(Object expected, Object actual, String message) {
        boolean equal = (expected == null && actual == null) ||
                (expected != null && expected.equals(actual));
        return assertThat(equal, message + " (expected: " + expected + ", actual: " + actual + ")");
    }
}