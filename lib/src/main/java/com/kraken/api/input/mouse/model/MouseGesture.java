package com.kraken.api.input.mouse.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;

@Data
@AllArgsConstructor
public class MouseGesture {
    private String label;
    private long durationMs;
    private int startX, startY;
    private int endX, endY;
    // The specific button clicked at the end (1=Left, 3=Right)
    private int button;
    // The series of points leading to the click
    private List<RecordedPoint> points;
}