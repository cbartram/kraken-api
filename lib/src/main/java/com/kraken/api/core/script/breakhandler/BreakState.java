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
    private Instant nextBreakTime;
    private String breakReason;

    public void reset() {
        onBreak = false;
        scriptStartTime = null;
        breakStartTime = null;
        nextBreakTime = null;
        breakReason = null;
    }
}
