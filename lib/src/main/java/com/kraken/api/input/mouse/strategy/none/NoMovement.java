package com.kraken.api.input.mouse.strategy.none;

import com.google.inject.Inject;
import com.kraken.api.input.mouse.strategy.MoveableMouse;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.Point;

import java.awt.*;

public class NoMovement implements MoveableMouse {

    @Inject
    private Client client;

    @Getter
    private final Point lastPoint = new Point(-1, -1);

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
