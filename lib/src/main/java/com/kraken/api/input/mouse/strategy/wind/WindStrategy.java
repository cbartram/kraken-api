package com.kraken.api.input.mouse.strategy.wind;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.input.mouse.strategy.MoveableMouse;
import com.kraken.api.input.mouse.strategy.instant.InstantStrategy;
import com.kraken.api.service.util.RandomService;
import net.runelite.api.Client;
import net.runelite.api.Point;

import java.awt.*;

@Singleton
public class WindStrategy implements MoveableMouse {

    @Inject
    private InstantStrategy instantStrategy;

    @Inject
    private Client client;

    private final double SQRT_3 = Math.sqrt(3);
    private final double SQRT_5 = Math.sqrt(5);

    @Override
    public Canvas getCanvas() {
        return client.getCanvas();
    }

    @Override
    public void move(Point start, Point target) {
        move(start, target, WindMouseConfig.builder().build());
    }

    /**
     * Configurable move for custom physics profiles.
     * @param start The starting mouse position
     * @param target The target mouse position
     * @param config WindMouseConfig custom configuration
     */
    public void move(Point start, Point target, WindMouseConfig config) {
        double currentX = start.getX();
        double currentY = start.getY();
        double targetX = target.getX();
        double targetY = target.getY();

        double vX = 0, vY = 0; // Velocity
        double wX = 0, wY = 0; // Wind (Chaos)

        double dist = Math.hypot(currentX - targetX, currentY - targetY);

        while (dist > 1.0) {
            // Calculate Wind (Chaos)
            // The wind fluctuates based on the config.wind parameter.
            // We reduce wind as we get closer (dist < targetArea) to ensure we can actually hit the target.
            double windMagnitude = config.getWind();
            if (dist < config.getTargetArea()) {
                // Dampen wind when close to target to simulate fine motor control
                windMagnitude = windMagnitude / Math.sqrt(config.getTargetArea());
            }

            // Algorithm for continuous chaotic fluctuation
            wX = wX / SQRT_3 + (Math.random() * (windMagnitude * 2D + 1D) - windMagnitude) / SQRT_5;
            wY = wY / SQRT_3 + (Math.random() * (windMagnitude * 2D + 1D) - windMagnitude) / SQRT_5;

            // Gravity pulls the cursor towards the target.
            double gravity = config.getGravity();

            // Add forces to velocity
            // Velocity = OldVelocity + Wind + Gravity
            vX += wX + gravity * (targetX - currentX) / dist;
            vY += wY + gravity * (targetY - currentY) / dist;

            // Ensure we don't move faster than maxStep (physically impossible for a mouse to jump too far)
            double velMag = Math.hypot(vX, vY);
            if (velMag > config.getMaxStep()) {
                double factor = config.getMaxStep() / velMag;
                vX *= factor;
                vY *= factor;
            }

            // Apply to Position
            currentX += vX;
            currentY += vY;

            // Move Cursor
            int roundX = (int) Math.round(currentX);
            int roundY = (int) Math.round(currentY);

            instantStrategy.move(null, new Point(roundX, roundY));

            // Recalculate distance for next loop
            dist = Math.hypot(currentX - targetX, currentY - targetY);

            // If we are within 3 pixels (or whatever small threshold), snap to target.
            // This prevents "orbiting" the pixel infinitely.
            if (dist < 3.0) {
                break;
            }

            // We sleep longer if moving slowly (fine motor) and shorter if moving fast (ballistic).
            try {
                long sleep = Math.round(RandomService.between(
                        (int)config.getMinWait(),
                        (int)config.getMaxWait()
                ));
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Final snap to ensure 100% accuracy at the end
        instantStrategy.move(null, target);
    }
}