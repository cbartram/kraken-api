package com.kraken.api.example.overlay;

import com.google.inject.Inject;
import com.kraken.api.example.ExamplePlugin;
import com.kraken.api.service.movement.Pathfinder;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import java.awt.*;
import java.util.List;


public class SceneOverlay extends Overlay {
    private final ExamplePlugin plugin;
    private final Pathfinder pathfinder;

    @Inject
    public SceneOverlay(ExamplePlugin plugin, Pathfinder pathfinder) {
        this.plugin = plugin;
        this.pathfinder = pathfinder;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        List<WorldPoint> path = plugin.getCurrentPath();
        return pathfinder.renderPath(path, graphics);
    }
}