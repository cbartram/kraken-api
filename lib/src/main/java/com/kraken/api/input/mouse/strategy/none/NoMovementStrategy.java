package com.kraken.api.input.mouse.strategy.none;

import com.google.inject.Inject;
import com.kraken.api.input.mouse.strategy.MouseMovementStrategy;
import net.runelite.api.Client;
import net.runelite.api.Point;

import java.awt.*;

public class NoMovementStrategy implements MouseMovementStrategy {

    @Inject
    private Client client;

    @Override
    public Canvas getCanvas() {
        return client.getCanvas();
    }

    // Purposely does nothing. This class exists so users can opt to not have any mouse movement in their plugins
    // if so desired. They can use this strategy to skip mouse movement all-together while maintaing the ability
    // for users to switch strategies on the fly in their plugins.
    @Override
    public void move(Point target) {}
}
