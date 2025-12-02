package example.overlay;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.query.groundobject.GroundObjectEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.query.player.PlayerEntity;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.movement.Pathfinder;
import example.ExampleConfig;
import example.ExamplePlugin;
import net.runelite.api.Actor;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.*;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
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

        if(config.showNpcs()) {
            renderNpcs(graphics);
        }

        if(config.showPlayers()) {
            renderOtherPlayers(graphics);
            renderLocalPlayer(graphics);
        }

        if(config.showDebugInfo()) {
            renderApiDebug(graphics);
            renderWidgetDebug(graphics);
        }

        return null;
    }

    private void renderLocalPlayer(Graphics2D graphics) {
        var localEntity = ctx.players().local();
        if (localEntity != null && !localEntity.isNull()) {
            // Draw Blue box around self
            renderPlayerPolygon(graphics, localEntity, Color.BLUE, "Me");
        }
    }

    private void renderOtherPlayers(Graphics2D graphics) {
        List<PlayerEntity> players = ctx.players().stream().collect(Collectors.toList());

        for (PlayerEntity p : players) {
            Color color = Color.WHITE;
            String status = "Idle";

            Actor interacting = p.raw().getInteracting();

            if (interacting != null) {
                if (interacting == ctx.getClient().getLocalPlayer()) {
                    color = Color.RED; // Interacting with ME (Warning)
                    status = "Targeting Me";
                } else {
                    color = Color.YELLOW; // Interacting with someone else
                    status = "Busy";
                }
            }

            String text = String.format("%s (Lvl: %d) | %s",
                    p.getName(),
                    p.raw().getCombatLevel(),
                    status
            );

            renderPlayerPolygon(graphics, p, color, text);
        }
    }

    private void renderWidgetDebug(Graphics2D graphics) {
        // 1. Get current mouse position
        net.runelite.api.Point mouse = ctx.getClient().getMouseCanvasPosition();

        // 2. Find the top-most visible widget under the mouse
        // We filter for visible widgets, then check bounds manually or use API utils if available.
        // Since we want to test the Query API, let's use it to narrow down candidates,
        // though strictly 'widgets under point' is complex due to layering.
        // A simple approach for debugging is iterating visible widgets.
        WidgetEntity hovered = ctx.widgets().visible().stream()
                .filter(w -> {
                    Rectangle bounds = w.raw().getBounds();
                    return bounds != null && bounds.contains(mouse.getX(), mouse.getY());
                })
                // Sort by area size (smallest first) usually gives the specific button
                // rather than the container, or use depth logic if available.
                .min((w1, w2) -> {
                    Rectangle r1 = w1.raw().getBounds();
                    Rectangle r2 = w2.raw().getBounds();
                    return Double.compare(r1.getWidth() * r1.getHeight(), r2.getWidth() * r2.getHeight());
                })
                .orElse(null);


        if(hovered == null) {
            return;
        }

        Widget w = hovered.raw();
        Rectangle bounds = w.getBounds();

        // Highlight
        graphics.setColor(Color.MAGENTA);
        graphics.draw(bounds);

        // Info Panel
        int x = bounds.x + bounds.width + 5;
        int y = bounds.y;

        // Ensure info panel stays on screen
        if (x + 150 > ctx.getClient().getCanvasWidth()) {
            x = bounds.x - 155;
        }

        String[] info = new String[] {
                "ID: " + w.getId() + " (" + (w.getId() >> 16) + ":" + (w.getId() & 0xFFFF) + ")",
                "Index: " + w.getIndex(),
                "Text: " + w.getText(),
                "Name: " + w.getName(),
                "Actions: " + (w.getActions() != null ? String.join(", ", w.getActions()) : "null"),
                "Sprite: " + w.getSpriteId()
        };

        // Draw background for text
        graphics.setColor(new Color(0, 0, 0, 180));
        graphics.fillRect(x, y, 200, info.length * 15 + 5);

        graphics.setColor(Color.WHITE);
        for (int i = 0; i < info.length; i++) {
            graphics.drawString(info[i], x + 5, y + 15 + (i * 15));
        }
    }

    private void renderApiDebug(Graphics2D graphics) {
        // Debug 1: visualize the result of .nearest()
        // This draws a line from local player to the result of ctx.players().nearest()
        PlayerEntity nearest = ctx.players().nearest();

        if (nearest != null && !nearest.isNull()) {
            LocalPoint start = ctx.getClient().getLocalPlayer().getLocalLocation();
            LocalPoint end = nearest.raw().getLocalLocation();

            if (start != null && end != null) {
                net.runelite.api.Point p1 = Perspective.localToCanvas(ctx.getClient(), start, ctx.getClient().getTopLevelWorldView().getPlane());
                net.runelite.api.Point p2 = Perspective.localToCanvas(ctx.getClient(), end, ctx.getClient().getTopLevelWorldView().getPlane());

                if (p1 != null && p2 != null) {
                    graphics.setColor(Color.CYAN);
                    graphics.drawLine(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    OverlayUtil.renderTextLocation(graphics, new net.runelite.api.Point((p1.getX()+p2.getX())/2, (p1.getY()+p2.getY())/2), "Nearest", Color.CYAN);
                }
            }
        }

        // Debug 2: Count players interacting with me
        long targetingMe = ctx.players().interactingWith(ctx.getClient().getLocalPlayer()).stream().count();
        if (targetingMe > 0) {
            OverlayUtil.renderTextLocation(graphics, new net.runelite.api.Point(30, 30), "WARNING: " + targetingMe + " players targeting you!", Color.RED);
        }
    }

    private void renderPlayerPolygon(Graphics2D graphics, PlayerEntity entity, Color color, String label) {
        if (entity == null || entity.raw() == null) return;

        Shape poly = entity.raw().getConvexHull();
        if (poly != null) {
            graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
            graphics.fill(poly);
            graphics.setColor(color);
            graphics.draw(poly);
        }

        net.runelite.api.Point textLoc = entity.raw().getCanvasTextLocation(graphics, label, entity.raw().getLogicalHeight() + 40);
        if (textLoc != null) {
            OverlayUtil.renderTextLocation(graphics, textLoc, label, color);
        }
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

    /**
     * Renders all nearby NPCs with color coding based on their state.
     */
    private void renderNpcs(Graphics2D graphics) {
        // Use your API to get all valid NPCs within 15 tiles
        List<NpcEntity> nearbyNpcs = ctx.npcs().within(config.npcRange()).stream().collect(Collectors.toList());

        for (NpcEntity npcWrapper : nearbyNpcs) {
            NPC npc = npcWrapper.raw();

            Color color = Color.WHITE;
            String status = "Idle";

            boolean isReachable = ctx.getTileService().isTileReachable(npc.getWorldLocation());
            boolean isDead = npc.isDead();
            Actor interacting = npc.getInteracting();

            List<String> actions = Arrays.stream(npc.getComposition().getActions())
                    .filter(java.util.Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toList());
            boolean isAttackable = actions.contains("attack") && !isDead;

            if (interacting == ctx.getClient().getLocalPlayer()) {
                color = Color.RED;
                status = "Aggro";
            } else if (!isReachable) {
                color = Color.GRAY; // Unreachable
                status = "Unreachable";
            } else if (interacting != null) {
                color = Color.YELLOW; // Busy interacting with someone else
                status = "Busy";
            } else if (isAttackable) {
                color = Color.GREEN; // Ready to fight
                status = "Attackable";
            } else {
                color = Color.CYAN; // Default/Idle
            }

            Shape clickbox = npc.getConvexHull();
            if (clickbox != null) {
                graphics.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 100));
                graphics.fill(clickbox);
                graphics.setColor(color);
                graphics.draw(clickbox);
            }

            // Format: Name (Lvl: 10) | [Attack, Talk] | Status
            String actionString = actions.isEmpty() ? "[]" : actions.toString();
            String text = String.format("%s (Lvl: %d) | %s",
                    npc.getName(),
                    npc.getCombatLevel(),
                    status);

            net.runelite.api.Point textLoc = npc.getCanvasTextLocation(graphics, text, npc.getLogicalHeight() + 40);
            if (textLoc != null) {
                OverlayUtil.renderTextLocation(graphics, textLoc, text, color);

                // Draw secondary line for actions if needed
                net.runelite.api.Point actionLoc = new net.runelite.api.Point(textLoc.getX(), textLoc.getY() + 15);
                OverlayUtil.renderTextLocation(graphics, actionLoc, actionString, Color.LIGHT_GRAY);
            }
        }
    }

    private void renderGroundItems(Graphics2D graphics) {
        for(GroundObjectEntity entity : ctx.groundItems().within(config.groundObjectRange()).stream().collect(Collectors.toList())) {
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

            // 4. Color Coding
            // Pink for expensive (> 10k), White for normal, Red if unreachable
            Color textColor = Color.WHITE;
            if (!isReachable) {
                textColor = Color.RED;
            } else if (gePrice > 10000) {
                textColor = new Color(217, 5, 250);
            }

            LocalPoint pt = LocalPoint.fromWorld(ctx.getClient().getTopLevelWorldView(), entity.raw().getLocation());
            if (pt == null) {
                continue;
            }

            // 5. Render
            // We use getCanvasTextLocation. We offset Z slightly so it floats above the item.
            net.runelite.api.Point textLocation = Perspective.getCanvasTextLocation(
                    ctx.getClient(),
                    graphics,
                    pt,
                    sb.toString(),
                    20 // Z-offset (height)
            );

            if (textLocation != null) {
                OverlayUtil.renderTextLocation(graphics, textLocation, sb.toString(), textColor);
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