package com.kraken.api;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.interaction.movement.MinimapService;
import com.kraken.api.interaction.movement.MovementService;
import com.kraken.api.overlay.MovementOverlay;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;

import java.awt.*;
import java.util.ArrayList;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Movement Test Plugin",
        enabledByDefault = false,
        description = "A dummy example plugin used to test the API before releasing.",
        tags = {"example", "automation", "kraken"}
)
public class ExamplePlugin extends Plugin {

    @Inject
    private Client client;

    @Getter
    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    // Helper Overlay for displaying movement paths from the MovementService within the API.
    @Inject
    private MovementOverlay movementOverlay;

    @Inject
    private MovementService movementService;

    @Inject
    private MinimapService minimapService;

    @Inject
    private Context context;

    @Inject
    private ExampleConfig config;

    @Inject
    private MouseTrackerOverlay overlay;

    private java.util.List<Point> mouseTrail = new ArrayList<>();
    private Point lastMousePosition = new Point();
    private static final int MAX_TRAIL_LENGTH = 100;

    public static final WorldPoint MINING_AREA = new WorldPoint(3287, 3367, 0);
    public static final WorldPoint BANK_AREA = new WorldPoint(3253, 3420, 0); // Varrock East bank

    @Provides
    ExampleConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(ExampleConfig.class);
    }

    @Subscribe
    private void onConfigChanged(final ConfigChanged event) {
        if (event.getGroup().equals("testapi")) {
            // Handle config changes if necessary
            log.info("Mining config changed: {}", event.getKey());
            if(event.getKey().equals("start")) {
                if (config.start()) {
                    log.info("Starting Movement...");
                    int x = Integer.parseInt(config.xCoordinate());
                    int y = Integer.parseInt(config.yCoordinate());
                    WorldPoint wp = new WorldPoint(x, y, client.getTopLevelWorldView().getPlane());
                    LocalPoint lp = new LocalPoint(x, y, client.getTopLevelWorldView().getPlane());
                    switch(config.movementType()) {
                        case SCENE_WALK_SP:
                            log.info("Using Scene Walk for movement.");
                            movementService.sceneWalk(x, y);
                            break;
                        case MINIMAP_WALK_WP:
                            log.info("Using Minimap Walk for movement. Curr Zoom = {}", client.getMinimapZoom());
                            minimapService.walkMiniMap(wp, 5.0);
                            break;
                        case WALK_LOCAL_LP:
                            log.info("Using Local Walk for movement.");
                            movementService.walkFastLocal(lp);
                            break;
                        case WALK_CANVAS_WP:
                            log.info("Using Canvas Walk for movement.");
                            movementService.walkFastCanvas(wp);
                            break;
                    }
                } else {
                    log.info("Stopping Movement...");
                    movementService.stopMovement();
                }
            }
        }
    }

    @Subscribe
    public void onGameTick(GameTick gameTick)
    {
        if (client.getCanvas() == null)
        {
            return;
        }

        // Get mouse position relative to canvas
        Point mousePos = client.getCanvas().getMousePosition();
        if (mousePos != null)
        {
            // Only add to trail if mouse has moved
            if (!mousePos.equals(lastMousePosition))
            {
                mouseTrail.add(new Point(mousePos));
                lastMousePosition = new Point(mousePos);

                // Limit trail length
                if (mouseTrail.size() > MAX_TRAIL_LENGTH)
                {
                    mouseTrail.remove(0);
                }
            }
        }
    }

    public java.util.List<Point> getMouseTrail() {
        return new ArrayList<>(mouseTrail);
    }

    public Point getCurrentMousePosition() {
        if (client.getCanvas() != null) {
            return client.getCanvas().getMousePosition();
        }
        return null;
    }

    @Override
    protected void startUp() {
        context.register();
        context.loadHooks();
        overlayManager.add(movementOverlay);
        overlayManager.add(overlay);
    }

    @Override
    protected void shutDown() {
        overlayManager.remove(movementOverlay);
        overlayManager.remove(overlay);
    }

    @Subscribe
    private void onGameStateChanged(final GameStateChanged event) {
        final GameState gameState = event.getGameState();

        switch (gameState) {
            case LOGGED_IN:
                startUp();
                break;
            case HOPPING:
            case LOGIN_SCREEN:
                shutDown();
            default:
                break;
        }
    }
}
