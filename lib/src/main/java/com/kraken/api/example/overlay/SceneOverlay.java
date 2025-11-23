package com.kraken.api.example.overlay;

import com.google.inject.Inject;
import com.kraken.api.example.ExamplePlugin;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;
import java.util.List;

import java.awt.*;


public class SceneOverlay extends Overlay {
    private final Client client;
    private final ExamplePlugin plugin; // Assuming your plugin holds the calculated path

    @Inject
    public SceneOverlay(Client client, ExamplePlugin plugin) {
        this.client = client;
        this.plugin = plugin;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        List<WorldPoint> path = plugin.getCurrentPath();

        if (path == null || path.isEmpty()) {
            return null;
        }

        Color pathColor = new Color(0, 255, 0, 150);
        Color endColor = new Color(255, 0, 0, 150);

        int i = 0;
        for (WorldPoint point : path) {
            // 1. Validate Planes (don't draw if player is on z=0 and path is on z=1)
            if (point.getPlane() != client.getTopLevelWorldView().getPlane()) {
                continue;
            }

            // 2. Convert WorldPoint to LocalPoint
            // This is necessary because drawing is done relative to the camera in the scene
            LocalPoint lp = LocalPoint.fromWorld(client, point);
            if (lp == null) {
                continue; // Point is outside the currently loaded scene
            }

            // 3. Get the Polygon for the tile
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            if (poly != null) {
                // Determine color based on position in path
                if (i == path.size() - 1) {
                    graphics.setColor(endColor);
                    graphics.fill(poly); // Fill the last tile
                } else {
                    graphics.setColor(pathColor);
                    graphics.setStroke(new BasicStroke(2));
                    graphics.draw(poly); // Outline the path tiles
                }
            }
            i++;
        }

        // 4. Optional: Draw lines connecting the tiles for a "route" look
        renderPathLines(graphics, path);

        return null;
    }

    private void renderPathLines(Graphics2D graphics, List<WorldPoint> path) {
        if (path.size() < 2) return;

        graphics.setColor(Color.WHITE);
        graphics.setStroke(new BasicStroke(1));

        net.runelite.api.Point prevCenter = null;

        // Add player location as the start of the line
        LocalPoint playerLp = client.getLocalPlayer().getLocalLocation();
        if (playerLp != null) {
            prevCenter = Perspective.localToCanvas(client, playerLp, client.getTopLevelWorldView().getPlane());
        }

        for (WorldPoint point : path) {
            LocalPoint lp = LocalPoint.fromWorld(client, point);
            if (lp == null) continue;

            net.runelite.api.Point center = Perspective.localToCanvas(client, lp, client.getTopLevelWorldView().getPlane());
            if (center != null && prevCenter != null) {
                graphics.drawLine(prevCenter.getX(), prevCenter.getY(), center.getX(), center.getY());
            }
            prevCenter = center;
        }
    }
}