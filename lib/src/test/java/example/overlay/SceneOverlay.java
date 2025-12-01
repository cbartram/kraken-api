package example.overlay;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.query.gameobject.GameObjectEntity;
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
}