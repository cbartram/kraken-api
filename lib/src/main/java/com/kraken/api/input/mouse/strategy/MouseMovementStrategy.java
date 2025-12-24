package com.kraken.api.input.mouse.strategy;

import net.runelite.api.Point;

import java.awt.*;

public interface MouseMovementStrategy {
    Canvas getCanvas();
    void move(Point target);
}
