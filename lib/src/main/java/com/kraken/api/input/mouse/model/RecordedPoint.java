package com.kraken.api.input.mouse.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RecordedPoint {
    private int x;
    private int y;
    // Time in MS relative to the start of this specific gesture
    private long timeOffset;
}

