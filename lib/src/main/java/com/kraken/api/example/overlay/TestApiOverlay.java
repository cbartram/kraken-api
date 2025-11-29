package com.kraken.api.example.overlay;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.example.ExampleConfig;
import com.kraken.api.example.ExamplePlugin;
import com.kraken.api.query.npc.NpcService;
import com.kraken.api.service.tile.TileService;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import java.awt.*;
import java.util.stream.Stream;

import static net.runelite.client.ui.overlay.OverlayUtil.renderPolygon;

public class TestApiOverlay extends Overlay {

    private final Client client;
    private final ExampleConfig config;
    private final Context context;
    private final NpcService npcService;
    private final ExamplePlugin plugin;
    private final TileService tileService;

    @Inject
    public TestApiOverlay(Client client, ExampleConfig config, Context context, TileService tileService, NpcService npcService, ExamplePlugin plugin) {
        this.client = client;
        this.config = config;
        this.context = context;
        this.tileService = tileService;
        this.npcService = npcService;
        this.plugin = plugin;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        if (!config.start()) {
            return null;
        }

        // Set font size
        Font font = new Font(Font.SANS_SERIF, Font.BOLD, config.fontSize());
        graphics.setFont(font);

        try {
            // Render NPC overlays
            if (config.showNpcOverlay() && config.enableNpcTests()) {
                renderNpcOverlays(graphics);
            }

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

    private void renderNpcOverlays(Graphics2D graphics) {
        Stream<NPC> npcs = npcService.getNpcs();

        npcs.forEach(npc -> {
            try {
                String npcName = context.runOnClientThreadOptional(npc::getName).orElse(null);
                if (npcName == null || npcName.isEmpty()) {
                    return;
                }

                LocalPoint localPoint = npc.getLocalLocation();
                if (localPoint == null) {
                    return;
                }

                Point canvasPoint = Perspective.localToCanvas(client, localPoint, client.getTopLevelWorldView().getPlane());
                if (canvasPoint == null) {
                    return;
                }

                // Determine color based on NPC type
                Color color = config.npcOverlayColor();
                if (config.highlightAttackableNpcs() && npcName.toLowerCase().contains("guard")) {
                    color = Color.RED;
                }

                // Set transparency
                Color transparentColor = new Color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        255
                );
                graphics.setColor(transparentColor);

                // Draw NPC name
                FontMetrics metrics = graphics.getFontMetrics();
                int textWidth = metrics.stringWidth(npcName);
                int textX = canvasPoint.getX() - textWidth / 2;
                int textY = canvasPoint.getY() - 20;

                // Draw background rectangle for better readability
                graphics.setColor(new Color(0, 0, 0, 100));
                graphics.fillRect(textX - 2, textY - metrics.getHeight() + 2, textWidth + 4, metrics.getHeight());

                // Draw text
                graphics.setColor(transparentColor);
                graphics.drawString(npcName, textX, textY);

                // Show debug info if enabled
                if (config.showDebugInfo()) {
                    String debugInfo = String.format("ID: %d, HP: %d",
                            npc.getId(),
                            npc.getHealthRatio());
                    graphics.drawString(debugInfo, textX, textY + 15);
                }

            } catch (Exception e) {
                // Skip this NPC if there's an error
            }
        });
    }
}