package com.kraken.api.input.mouse.strategy.linear;

import com.google.inject.Inject;
import com.kraken.api.input.mouse.strategy.MoveableMouse;
import com.kraken.api.input.mouse.strategy.instant.InstantStrategy;
import com.kraken.api.service.util.RandomService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;

import java.awt.*;

@Slf4j
public class LinearStrategy implements MoveableMouse {

    @Inject
    private Client client;

    @Inject
    private InstantStrategy instantStrategy;

    @Setter
    private int steps;

    @Getter
    private Point lastPoint;

    @Override
    public Canvas getCanvas() {
        return client.getCanvas();
    }

    @Override
    public void move(Point target) {
        if(steps <= 0) {
            log.error("Steps must be greater than 0. Use setSteps() to set the appropriate amount of intermediate" +
                    "points on the line.");
            return;
        }

        Point startPoint;

        if(client.getCanvas().getMousePosition() == null) {
            startPoint = new Point(0, 0);
        } else {
            startPoint = new Point(client.getCanvas().getMousePosition().x, client.getCanvas().getMousePosition().y);
        }

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            int x = (int) (startPoint.getX() + (target.getX() - startPoint.getX()) * t);
            int y = (int) (startPoint.getY() + (target.getY() - startPoint.getY()) * t);
            Point p = new Point(x, y);
            instantStrategy.move(p);

            if(i == steps) {
                lastPoint = p;
            }

            try {
                Thread.sleep(RandomService.between(3, 10));
            } catch (InterruptedException e) {
                log.error("Failed to sleep while using linear mouse movement strategy: ", e);
            }
        }
    }
}
