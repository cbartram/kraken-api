package com.kraken.api.example.overlay;

import com.google.inject.Inject;
import com.kraken.api.example.ExampleConfig;
import com.kraken.api.example.ExamplePlugin;
import com.kraken.api.service.tile.TileService;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;

import static net.runelite.client.ui.overlay.OverlayUtil.renderPolygon;

public class TestApiOverlay extends Overlay {

    private final Client client;
    private final ExampleConfig config;
    private final ExamplePlugin plugin;
    private final TileService tileService;

    @Inject
    public TestApiOverlay(Client client, ExampleConfig config, TileService tileService, ExamplePlugin plugin) {
        this.client = client;
        this.config = config;
        this.tileService = tileService;
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
            if(config.enableMovementOverlay() && config.enableMovementTests()) {
                renderTargetTile(graphics);
            }

        } catch (Exception e) {
            // Catch any exceptions to prevent overlay crashes
            graphics.setColor(Color.RED);
            graphics.drawString("Overlay Error: " + e.getMessage(), 10, 50);
        }

        return null;
    }

    private void renderTargetTile(Graphics2D g) {
        if(plugin.getTargetTile() != null) {
            LocalPoint lp;
            if(client.getTopLevelWorldView().isInstance()) {
                lp = tileService.fromWorldInstance(plugin.getTargetTile());
            } else {
                lp = LocalPoint.fromWorld(client.getTopLevelWorldView(), plugin.getTargetTile());
            }

            if(lp == null) return;
            Polygon polygon = Perspective.getCanvasTilePoly(client, lp);
            if(polygon == null) return;

            renderPolygon(g, polygon, new Color(241, 160, 9), new Color(241, 160, 9, 20), new BasicStroke(2));
        }
    }
}