package com.kraken.api;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.interaction.groundobject.GroundObjectService;
import com.kraken.api.interaction.movement.MinimapService;
import com.kraken.api.interaction.movement.MovementService;
import com.kraken.api.interaction.npc.NpcService;
import com.kraken.api.overlay.MouseTrackerOverlay;
import com.kraken.api.overlay.MovementOverlay;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
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
        name = "Test Plugin",
        enabledByDefault = false,
        description = "A dummy example plugin used to test the API before releasing.",
        tags = {"example", "automation", "kraken"}
)
public class ExamplePlugin extends Plugin {

    @Getter
    @Inject
    private ClientThread clientThread;

    @Inject
    private OverlayManager overlayManager;

    @Inject
    private MovementOverlay movementOverlay;

    @Inject
    private MovementService movementService;

    @Inject
    private GroundObjectService groundObjectService;

    @Inject
    private NpcService npcService;

    @Inject
    private Context context;

    @Inject
    private ExampleConfig config;

    @Inject
    private MouseTrackerOverlay overlay;

    @Provides
    ExampleConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(ExampleConfig.class);
    }

    @Subscribe
    private void onMenuOptionClicked(MenuOptionClicked event) {
        log.debug("Option={}, Target={}, Param0={}, Param1={}, MenuAction={}, ItemId={}, id={}, itemOp={}, str={}",
                event.getMenuOption(), event.getMenuTarget(), event.getParam0(), event.getParam1(), event.getMenuAction().name(), event.getItemId(),
                event.getId(), event.getItemOp(), event);
    }

    @Subscribe
    private void onConfigChanged(final ConfigChanged event) {
        if (event.getGroup().equals("testapi")) {
            if(event.getKey().equals("start")) {
                if (config.start()) {
                    log.info("Starting...");
                    clientThread.invoke(() -> {
                        boolean clicked = npcService.interact("Guard", "Attack");
                        log.info("Clicked: {}", clicked);
                    });
                } else {
                    log.info("Stopping...");
                    movementService.resetPath();
                }
            }
        }
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
