package com.kraken.api.input.mouse.strategy;

import net.runelite.api.Point;

import java.awt.*;

public interface MoveableMouse {
    Canvas getCanvas();
    void move(Point target);
    Point getLastPoint();
}
