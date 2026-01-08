package com.kraken.api.core.script.breakhandler;


import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class BreakState {
    private boolean onBreak = false;
    private Instant scriptStartTime;
    private Instant breakStartTime;
    private Instant breakEndTime;
    private Instant nextBreakTime;
    private String breakReason;
    private boolean awaitingLogin = false;

    public void reset() {
        onBreak = false;
        scriptStartTime = null;
        breakStartTime = null;
        breakEndTime = null;
        nextBreakTime = null;
        breakReason = null;
        awaitingLogin = false;
    }

    /**
     * Checks if we should resume after login (break ended while logged out).
     * @return True if the script should resume after re-logging in from a break
     */
    public boolean shouldResumeAfterLogin() {
        return awaitingLogin && breakEndTime != null && Instant.now().isAfter(breakEndTime);
    }
}
