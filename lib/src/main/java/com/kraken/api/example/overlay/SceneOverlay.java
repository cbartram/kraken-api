package com.kraken.api.example.overlay;

import com.google.inject.Inject;
import com.kraken.api.example.ExampleConfig;
import com.kraken.api.example.ExamplePlugin;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;

public class SceneOverlay extends Overlay {

    private final ExampleConfig config;
    private final ExamplePlugin plugin;


    @Inject
    public SceneOverlay(ExampleConfig config, ExamplePlugin plugin) {
        this.config = config;
        this.plugin = plugin;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.start()) {
            return null;
        }

        Font font = new Font(Font.SANS_SERIF, Font.BOLD, config.fontSize());
        graphics.setFont(font);

        try {
            net.runelite.api.Point point = plugin.getPoint();

            if (point != null) {
                renderCrosshairs(graphics, new Point(point.getX(), point.getY()));
            }

        } catch (Exception e) {
            graphics.setColor(Color.RED);
            graphics.drawString("Overlay Error: " + e.getMessage(), 10, 50);
        }

        return null;
    }

    private void renderCrosshairs(Graphics2D g, Point center) {
        int crosshairSize = 20; // Length of each line from center
        int gap = 3; // Gap between center and crosshair lines

        // Set crosshair color and stroke
        g.setColor(Color.CYAN);
        g.setStroke(new BasicStroke(2));

        // Draw horizontal lines (left and right)
        g.drawLine(center.x - crosshairSize, center.y, center.x - gap, center.y);
        g.drawLine(center.x + gap, center.y, center.x + crosshairSize, center.y);

        // Draw vertical lines (top and bottom)
        g.drawLine(center.x, center.y - crosshairSize, center.x, center.y - gap);
        g.drawLine(center.x, center.y + gap, center.x, center.y + crosshairSize);

        // Optional: Draw a center dot
        g.fillOval(center.x - 2, center.y - 2, 4, 4);
    }
}