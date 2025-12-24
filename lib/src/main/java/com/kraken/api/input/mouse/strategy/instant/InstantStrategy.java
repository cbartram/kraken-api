package com.kraken.api.input.mouse.strategy.instant;

import com.google.inject.Inject;
import com.kraken.api.input.mouse.strategy.MouseMovementStrategy;
import net.runelite.api.Client;
import net.runelite.api.Point;

import java.awt.*;
import java.awt.event.MouseEvent;

public class InstantStrategy implements MouseMovementStrategy {

    @Inject
    private Client client;

    @Override
    public Canvas getCanvas() {
        return this.client.getCanvas();
    }

    @Override
    public void move(Point target) {
        MouseEvent event = new MouseEvent(getCanvas(), MouseEvent.MOUSE_MOVED, System.currentTimeMillis(), 0, target.getX(), target.getY(), 0, false);
        getCanvas().dispatchEvent(event);
    }
}