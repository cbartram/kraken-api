package com.kraken.api.example.overlay;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.example.ExampleConfig;
import com.kraken.api.example.ExamplePlugin;
import com.kraken.api.interaction.gameobject.GameObjectService;
import com.kraken.api.interaction.groundobject.GroundItem;
import com.kraken.api.interaction.groundobject.GroundObjectService;
import com.kraken.api.interaction.inventory.InventoryService;
import com.kraken.api.interaction.npc.NpcService;
import com.kraken.api.interaction.player.PlayerService;
import com.kraken.api.interaction.tile.TileService;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import java.awt.*;
import java.util.stream.Stream;

import static net.runelite.client.ui.overlay.OverlayUtil.renderPolygon;

public class TestApiOverlay extends Overlay {

    private final Client client;
    private final ExampleConfig config;
    private final Context context;
    private final GroundObjectService groundObjectService;
    private final NpcService npcService;
    private final GameObjectService gameObjectService;
    private final ExamplePlugin plugin;
    private final TileService tileService;

    @Inject
    public TestApiOverlay(Client client, ExampleConfig config, Context context, TileService tileService, GroundObjectService groundObjectService, NpcService npcService, GameObjectService gameObjectService, ExamplePlugin plugin) {
        this.client = client;
        this.config = config;
        this.context = context;
        this.groundObjectService = groundObjectService;
        this.tileService = tileService;
        this.npcService = npcService;
        this.gameObjectService = gameObjectService;
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

            // Render ground item overlays
            if (config.showGroundItemOverlay() && config.enableGroundItemTests()) {
                renderGroundItemOverlays(graphics);
            }

            if(config.enableGameObjectOverlay() && config.enableGameObjectTests()) {
                renderGameObjectOverlays(graphics);
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

    private void renderGameObjectOverlays(Graphics2D graphics) {
        java.util.List<TileObject> gameObjects = gameObjectService.all();
        for(TileObject gameObject : gameObjects) {
            if (gameObject == null) continue;

            LocalPoint localPoint = gameObject.getLocalLocation();
            Point canvasPoint = Perspective.localToCanvas(client, localPoint, client.getTopLevelWorldView().getPlane());
            if (canvasPoint == null) continue;
            // Determine color based on object type
            Color color = config.gameObjectOverlayColor();

            Color transparentColor = new Color(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    255
            );

            graphics.setColor(transparentColor);
            String name = "ID: " + gameObject.getId();

            // Draw object id
            FontMetrics metrics = graphics.getFontMetrics();
            int textWidth = metrics.stringWidth(name);
            int textX = canvasPoint.getX() - textWidth / 2;
            int textY = canvasPoint.getY() - 20;

            // Draw background for better readability
            graphics.setColor(new Color(0, 0, 0, 100));
            graphics.fillRect(textX - 2, textY - metrics.getHeight() + 2, textWidth + 4, metrics.getHeight());

            // Draw text
            graphics.setColor(transparentColor);
            graphics.drawString(name, textX, textY);
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

    private void renderGroundItemOverlays(Graphics2D graphics) {
        try {
            for (GroundItem item : groundObjectService.all()) {
                if (item == null) continue;

                LocalPoint localPoint = LocalPoint.fromWorld(client.getTopLevelWorldView(), item.getLocation());
                if (localPoint == null) continue;

                Point canvasPoint = Perspective.localToCanvas(client, localPoint, client.getTopLevelWorldView().getPlane());
                if (canvasPoint == null) continue;

                String itemName = item.getName();
                if (itemName == null || itemName.isEmpty()) {
                    itemName = "Unknown Item";
                }

                // Determine color based on item value/type
                Color color = config.groundItemOverlayColor();

                // Highlight valuable items if enabled
                if (config.highlightValuableItems()) {
                    // You would need to implement item value checking here
                    // For now, just highlight certain item names
                    if (itemName.toLowerCase().contains("rune") ||
                            itemName.toLowerCase().contains("dragon") ||
                            itemName.toLowerCase().contains("coins")) {
                        color = config.valuableItemColor();
                    }
                }

                Color transparentColor = new Color(
                        color.getRed(),
                        color.getGreen(),
                        color.getBlue(),
                        255
                );

                graphics.setColor(transparentColor);

                // Draw item name
                FontMetrics metrics = graphics.getFontMetrics();
                int textWidth = metrics.stringWidth(itemName);
                int textX = canvasPoint.getX() - textWidth / 2;
                int textY = canvasPoint.getY() + 20;

                // Draw background for better readability
                graphics.setColor(new Color(0, 0, 0, 100));
                graphics.fillRect(textX - 2, textY - metrics.getHeight() + 2, textWidth + 4, metrics.getHeight());

                // Draw text
                graphics.setColor(transparentColor);
                graphics.drawString(itemName, textX, textY);

                // Show debug info if enabled
                if (config.showDebugInfo()) {
                    String debugInfo = String.format("ID: %d, Qty: %d",
                            item.getId(),
                            item.getQuantity());
                    graphics.drawString(debugInfo, textX, textY + 15);
                }
            }
        } catch (Exception e) {
            // Handle any errors in ground item rendering
        }
    }
}