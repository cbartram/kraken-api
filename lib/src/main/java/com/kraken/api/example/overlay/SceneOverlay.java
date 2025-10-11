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
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, config.fontSize());
        graphics.setFont(font);

        try {

        } catch (Exception e) {
            graphics.setColor(Color.RED);
            graphics.drawString("Overlay Error: " + e.getMessage(), 10, 50);
        }

        return null;
    }
}