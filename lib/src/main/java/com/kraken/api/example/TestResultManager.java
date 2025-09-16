package com.kraken.api.example;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.Map;

@Slf4j
@Getter
public class TestResultManager {

    @Getter
    @AllArgsConstructor
    public enum TestStatus {
        NOT_STARTED("Not Started"),
        RUNNING("Running..."),
        PASSED("Passed"),
        FAILED("Failed"),
        DISABLED("Disabled");
        private final String displayName;
    }

    public static class TestResult {
        @Getter
        private final String testName;
        @Getter
        private TestStatus status;
        @Getter
        private String lastRunTime;
        @Getter
        private String errorMessage;
        @Getter
        private long executionTimeMs;

        public TestResult(String testName) {
            this.testName = testName;
            this.status = TestStatus.NOT_STARTED;
            this.lastRunTime = "Never";
            this.errorMessage = null;
            this.executionTimeMs = 0;
        }

        public void setRunning() {
            this.status = TestStatus.RUNNING;
            this.errorMessage = null;
        }

        public void setCompleted(boolean passed, long executionTimeMs, String errorMessage) {
            this.status = passed ? TestStatus.PASSED : TestStatus.FAILED;
            this.lastRunTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            this.executionTimeMs = executionTimeMs;
            this.errorMessage = errorMessage;
        }

        public void setDisabled() {
            this.status = TestStatus.DISABLED;
        }
    }

    private final Map<String, TestResult> testResults = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Boolean>> runningTests = new ConcurrentHashMap<>();

    public Map<String, TestResult> getAllTestResults() {
        return new ConcurrentHashMap<>(testResults);
    }

    public void startTest(String testName, CompletableFuture<Boolean> testFuture) {
        TestResult result = new TestResult(testName);
        result.setRunning();
        runningTests.put(testName, testFuture);

        long startTime = System.currentTimeMillis();

        // Handle test completion
        testFuture.whenComplete((passed, throwable) -> {
            long executionTime = System.currentTimeMillis() - startTime;
            runningTests.remove(testName);

            if (throwable != null) {
                result.setCompleted(false, executionTime, throwable.getMessage());
                log.error("Test {} failed with exception", testName, throwable);
            } else {
                result.setCompleted(passed, executionTime, passed ? null : "Test assertions failed");
                log.info("Test {} completed: {} ({}ms)", testName,
                        passed ? "PASSED" : "FAILED", executionTime);
            }

            testResults.put(testName, result);
        });
    }

    public void setTestDisabled(String testName) {
        TestResult result = testResults.get(testName);
        if (result != null) {
            result.setDisabled();
        }
    }

    public boolean isTestRunning(String testName) {
        return runningTests.containsKey(testName);
    }

    public boolean areAnyTestsRunning() {
        return !runningTests.isEmpty();
    }

    public void cancelAllTests() {
        for (CompletableFuture<Boolean> future : runningTests.values()) {
            future.cancel(true);
        }
        runningTests.clear();

        // Reset any running tests to not started
        for (TestResult result : testResults.values()) {
            if (result.getStatus() == TestStatus.RUNNING) {
                result.status = TestStatus.NOT_STARTED;
            }
        }
    }

    public int getPassedTestCount() {
        return (int) testResults.values().stream()
                .filter(result -> result.getStatus() == TestStatus.PASSED)
                .count();
    }

    public int getFailedTestCount() {
        return (int) testResults.values().stream()
                .filter(result -> result.getStatus() == TestStatus.FAILED)
                .count();
    }

    public int getRunningTestCount() {
        return (int) testResults.values().stream()
                .filter(result -> result.getStatus() == TestStatus.RUNNING)
                .count();
    }

    public int getTotalEnabledTestCount() {
        return (int) testResults.values().stream()
                .filter(result -> result.getStatus() != TestStatus.DISABLED)
                .count();
    }

    public String getOverallStatus() {
        if (areAnyTestsRunning()) {
            return "Tests Running...";
        }

        int passed = getPassedTestCount();
        int failed = getFailedTestCount();
        int total = getTotalEnabledTestCount();

        if (passed + failed == 0) {
            return "No Tests Run";
        }

        if (failed == 0 && passed > 0) {
            return "All Tests Passed";
        } else if (passed == 0 && failed > 0) {
            return "All Tests Failed";
        } else {
            return String.format("%d/%d Passed", passed, total);
        }
    }
}
