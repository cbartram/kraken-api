package com.kraken.api.overlay;

import com.google.inject.Inject;
import com.kraken.api.interaction.movement.MovementService;
import com.kraken.api.interaction.movement.MovementStats;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;
import net.runelite.api.Point;

import java.awt.geom.Point2D;
import java.util.List;
import java.awt.geom.Line2D;

/**
 * Example RuneLite overlay for visualizing movement paths and status
 */
public class MovementOverlay extends Overlay {

    private final Client client;
    private final MovementService movementService;

    @Inject
    public MovementOverlay(Client client, MovementService movementService) {
        this.client = client;
        this.movementService = movementService;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        // Only render if we have an active movement
        if (!movementService.isMoving()) {
            return null;
        }

        MovementStats stats = movementService.getMovementStats();
        List<WorldPoint> path = movementService.getCalculatedPath();
        List<WorldPoint> remainingPath = movementService.getRemainingPath();

        // Render the path
        renderPath(graphics, path, remainingPath);

        // Render waypoint markers
        renderWaypoints(graphics, path, remainingPath);

        // Render target marker
        renderTarget(graphics, stats.getTarget());

        // Render next waypoint marker
        renderNextWaypoint(graphics, stats.getNextWaypoint());

        return null;
    }

    /**
     * Renders the path as connected lines
     */
    private void renderPath(Graphics2D graphics, List<WorldPoint> fullPath, List<WorldPoint> remainingPath) {
        if (fullPath.size() < 2) {
            return;
        }

        // Set up graphics
        graphics.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        WorldPoint playerPos = client.getLocalPlayer().getWorldLocation();
        Point playerScreenPos = worldToScreen(playerPos);

        // Draw completed path in green
        graphics.setColor(new Color(0, 255, 0, 100));
        Point lastScreenPos = playerScreenPos;

        for (int i = 0; i < fullPath.size() - remainingPath.size(); i++) {
            Point screenPos = worldToScreen(fullPath.get(i));
            if (lastScreenPos != null && screenPos != null) {
                Point2D p1 = new Point2D.Float(lastScreenPos.getX(), lastScreenPos.getY());
                Point2D p2 = new Point2D.Float(screenPos.getX(), screenPos.getY());
                graphics.draw(new Line2D.Float(p1, p2));
            }
            lastScreenPos = screenPos;
        }

        // Draw remaining path in yellow
        graphics.setColor(new Color(255, 255, 0, 150));
        for (int i = 0; i < remainingPath.size() - 1; i++) {
            Point startPos = worldToScreen(remainingPath.get(i));
            Point endPos = worldToScreen(remainingPath.get(i + 1));

            if (startPos != null && endPos != null) {
                Point2D p1 = new Point2D.Float(startPos.getX(), startPos.getY());
                Point2D p2 = new Point2D.Float(endPos.getX(), endPos.getY());
                graphics.draw(new Line2D.Float(p1, p2));
            }
        }
    }

    /**
     * Renders waypoint markers
     */
    private void renderWaypoints(Graphics2D graphics, List<WorldPoint> fullPath, List<WorldPoint> remainingPath) {
        // Draw completed waypoints as small green dots
        graphics.setColor(new Color(0, 255, 0, 200));
        for (int i = 0; i < fullPath.size() - remainingPath.size(); i++) {
            Point screenPos = worldToScreen(fullPath.get(i));
            if (screenPos != null) {
                graphics.fillOval((int) ((int) screenPos.getX() - 3), (int) ((int) screenPos.getY() - 3), 6, 6);
            }
        }

        // Draw remaining waypoints as yellow dots
        graphics.setColor(new Color(255, 255, 0, 200));
        for (WorldPoint waypoint : remainingPath) {
            Point screenPos = worldToScreen(waypoint);
            if (screenPos != null) {
                graphics.fillOval((int) (int) screenPos.getX() - 4, (int) (int) screenPos.getY() - 4, 8, 8);

                // Add waypoint numbers
                graphics.setColor(Color.BLACK);
                graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
                String waypointNum = String.valueOf(remainingPath.indexOf(waypoint) + 1);
                FontMetrics fm = graphics.getFontMetrics();
                int textWidth = fm.stringWidth(waypointNum);
                int textHeight = fm.getHeight();
                graphics.drawString(waypointNum,
                        (int) (int) screenPos.getX() - textWidth / 2,
                        (int) (int) screenPos.getY() + textHeight / 4);
                graphics.setColor(new Color(255, 255, 0, 200));
            }
        }
    }

    /**
     * Renders the final target marker
     */
    private void renderTarget(Graphics2D graphics, WorldPoint target) {
        if (target == null) {
            return;
        }

        Point screenPos = worldToScreen(target);
        if (screenPos == null) {
            return;
        }

        // Draw large red circle for target
        graphics.setColor(new Color(255, 0, 0, 200));
        graphics.setStroke(new BasicStroke(3.0f));
        graphics.drawOval((int) (int) screenPos.getX() - 8, (int) (int) screenPos.getY() - 8, 16, 16);

        // Draw crosshair
        graphics.drawLine((int) screenPos.getX() - 12, (int) screenPos.getY(), (int) screenPos.getX() + 12, (int) screenPos.getY());
        graphics.drawLine((int) screenPos.getX(), (int) screenPos.getY() - 12, (int) screenPos.getX(), (int) screenPos.getY() + 12);

        // Add "TARGET" text
        graphics.setColor(Color.RED);
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 12));
        graphics.drawString("TARGET", (int) screenPos.getX() + 15, (int) screenPos.getY() - 10);
    }

    /**
     * Renders the next waypoint marker
     */
    private void renderNextWaypoint(Graphics2D graphics, WorldPoint nextWaypoint) {
        if (nextWaypoint == null) {
            return;
        }

        Point screenPos = worldToScreen(nextWaypoint);
        if (screenPos == null) {
            return;
        }

        // Draw pulsing blue circle for next waypoint
        long time = System.currentTimeMillis();
        int alpha = (int) (128 + 127 * Math.sin(time * 0.005));
        graphics.setColor(new Color(0, 150, 255, alpha));
        graphics.setStroke(new BasicStroke(2.0f));
        graphics.drawOval((int) screenPos.getX() - 6, (int) screenPos.getY() - 6, 12, 12);

        // Add "NEXT" text
        graphics.setColor(new Color(0, 150, 255));
        graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 10));
        graphics.drawString("NEXT", (int) screenPos.getX() + 10, (int) screenPos.getY() - 8);
    }

    /**
     * Converts a WorldPoint to screen coordinates
     */
    private net.runelite.api.Point worldToScreen(WorldPoint worldPoint) {
        if (worldPoint == null) {
            return null;
        }

        LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), worldPoint);
        if (localPoint == null) {
            return null;
        }

        return Perspective.localToCanvas(client, localPoint, worldPoint.getPlane());
    }
}
