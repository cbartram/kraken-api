package example.overlay;

import com.google.inject.Inject;
import com.kraken.api.service.tile.TileService;
import example.ExampleConfig;
import example.ExamplePlugin;
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
        renderTargetTile(graphics);
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