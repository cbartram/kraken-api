package example.overlay;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.query.groundobject.GroundObjectEntity;
import com.kraken.api.service.movement.Pathfinder;
import example.ExampleConfig;
import example.ExamplePlugin;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.*;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class SceneOverlay extends Overlay {
    private final ExamplePlugin plugin;
    private final Pathfinder pathfinder;
    private final Context ctx;
    private final ExampleConfig config;

    @Inject
    public SceneOverlay(ExamplePlugin plugin, Pathfinder pathfinder, Context ctx, ExampleConfig config) {
        this.plugin = plugin;
        this.pathfinder = pathfinder;
        this.ctx = ctx;
        this.config = config;

        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(OverlayPriority.MED);
    }

    @Override
    public Dimension render(Graphics2D graphics) {
        List<WorldPoint> path = plugin.getCurrentPath();
        pathfinder.renderPath(path, graphics);

        if (config.showGameObjects()) {
            renderGameObjects(graphics);
        }

        if(config.showGroundObjects()) {
            renderGroundItems(graphics);
        }

        return null;
    }

    private void renderGameObjects(Graphics2D graphics) {
        for(GameObjectEntity entity : ctx.gameObjects().within(config.gameObjectRange()).interactable().list()) {
            LocalPoint playerLoc = ctx.players().local().raw().getLocalLocation();
            LocalPoint objLoc = entity.raw().getLocalLocation();

            int distance = playerLoc.distanceTo(objLoc) / Perspective.LOCAL_TILE_SIZE;
            boolean isReachable = ctx.getTileService().isObjectReachable(entity.raw());

            String[] rawActions = entity.getObjectComposition().getActions();
            String actionString = "[]";
            if (rawActions != null) {
                actionString = Arrays.toString(Arrays.stream(rawActions)
                        .filter(s -> s != null && !s.isEmpty())
                        .toArray());
            }

            String overlayText = String.format("%s | Dist: %d | R: %b | %s",
                    entity.getName(),
                    distance,
                    isReachable,
                    actionString);

            net.runelite.api.Point textLocation = entity.raw().getCanvasTextLocation(graphics, overlayText, 0);

            if (textLocation != null) {
                Color textColor = isReachable ? Color.GREEN : Color.RED;

                OverlayUtil.renderTextLocation(graphics, textLocation, overlayText, textColor);

                if (entity.raw().getClickbox() != null) {
                    OverlayUtil.renderPolygon(graphics, entity.raw().getClickbox(), textColor);
                }
            }
        }
    }

    private void renderGroundItems(Graphics2D graphics) {
        // Group items by tile location
        Map<WorldPoint, List<GroundObjectEntity>> itemsByTile = ctx.groundItems()
                .within(config.groundObjectRange())
                .stream()
                .collect(Collectors.groupingBy(entity -> entity.raw().getLocation()));

        for(Map.Entry<WorldPoint, List<GroundObjectEntity>> entry : itemsByTile.entrySet()) {
            WorldPoint tile = entry.getKey();
            List<GroundObjectEntity> items = entry.getValue();

            LocalPoint pt = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), tile);
            if (pt == null) {
                continue;
            }

            int offsetIndex = 0;
            for(GroundObjectEntity entity : items) {
                String name = entity.getName();
                int qty = entity.raw().getQuantity();
                int gePrice = entity.raw().getGePrice() * qty;
                int haPrice = entity.raw().getHaPrice() * qty;

                boolean isReachable = ctx.getTileService().isTileReachable(entity.raw().getLocation());

                // Format: Name (Qty) | GE: 100 | HA: 50
                StringBuilder sb = new StringBuilder();
                sb.append(name);
                if (qty > 1) {
                    sb.append("(").append(qty).append(")");
                }
                sb.append(" | GE: ").append(formatValue(gePrice));
                sb.append(" | HA: ").append(formatValue(haPrice));

                // Color Coding
                Color textColor = Color.WHITE;
                if (!isReachable) {
                    textColor = Color.RED;
                } else if (gePrice > 10000) {
                    textColor = new Color(217, 5, 250);
                }

                // Get base text location
                net.runelite.api.Point textLocation = Perspective.getCanvasTextLocation(
                        ctx.getClient(),
                        graphics,
                        pt,
                        sb.toString(),
                        20 // Z-offset (height)
                );

                if (textLocation != null) {
                    // Offset each item vertically by 15 pixels per item
                    net.runelite.api.Point offsetLocation = new net.runelite.api.Point(
                            textLocation.getX(),
                            textLocation.getY() + (offsetIndex * 15)
                    );
                    OverlayUtil.renderTextLocation(graphics, offsetLocation, sb.toString(), textColor);
                    offsetIndex++;
                }
            }
        }
    }

    // Helper to make numbers readable (1000 -> 1k)
    private String formatValue(int value) {
        if (value >= 1_000_000) {
            return String.format("%.1fM", value / 1_000_000.0);
        } else if (value >= 1_000) {
            return String.format("%.1fk", value / 1_000.0);
        }
        return String.valueOf(value);
    }
}