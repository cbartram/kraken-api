package com.kraken.api.example;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.example.overlay.InfoPanelOverlay;
import com.kraken.api.example.overlay.TestApiOverlay;
import com.kraken.api.example.tests.GroundObjectServiceTest;
import com.kraken.api.example.tests.InventoryServiceTest;
import com.kraken.api.example.tests.NpcServiceTest;
import com.kraken.api.example.tests.PlayerServiceTest;
import com.kraken.api.interaction.groundobject.GroundItem;
import com.kraken.api.interaction.groundobject.GroundObjectService;
import com.kraken.api.interaction.inventory.InventoryService;
import com.kraken.api.interaction.npc.NpcService;
import com.kraken.api.interaction.inventory.InventoryItem;
import com.kraken.api.interaction.player.PlayerService;
import com.kraken.api.overlay.MouseTrackerOverlay;
import com.kraken.api.overlay.MovementOverlay;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameState;
import net.runelite.api.NPC;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.stream.Collectors;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Test Plugin",
        enabledByDefault = false,
        description = "A comprehensive example plugin used to test the API with enhanced overlays and configuration.",
        tags = {"example", "automation", "kraken", "testing"}
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
    private Context context;

    @Inject
    private ExampleConfig config;

    @Inject
    private MouseTrackerOverlay overlay;

    @Inject
    private TestApiOverlay testApiOverlay;

    @Inject
    private InfoPanelOverlay infoPanelOverlay;

    @Inject
    private TestResultManager testResultManager;

    @Inject
    private NpcServiceTest npcServiceTest;

    @Inject
    private InventoryServiceTest inventoryServiceTest;

    @Inject
    private PlayerServiceTest playerServiceTest;

    @Inject
    private GroundObjectServiceTest groundObjectServiceTest;

    @Provides
    ExampleConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(ExampleConfig.class);
    }

    @Subscribe
    private void onMenuOptionClicked(MenuOptionClicked event) {
        if(config.showDebugInfo()) {
            log.info("Option={}, Target={}, Param0={}, Param1={}, MenuAction={}, ItemId={}, id={}, itemOp={}, str={}",
                    event.getMenuOption(), event.getMenuTarget(), event.getParam0(), event.getParam1(), event.getMenuAction().name(), event.getItemId(),
                    event.getId(), event.getItemOp(), event);
        }
    }

    @Subscribe
    private void onConfigChanged(final ConfigChanged event) {
        if (event.getGroup().equals("testapi")) {
            if(event.getKey().equals("start")) {
                if (config.start()) {
                    log.info("Starting API tests...");

                    // Run tests based on configuration
                    if (config.enableNpcTests()) {
                        testResultManager.startTest("NpcServiceTest", npcServiceTest.executeTest());
                    }

                    if (config.enableInventoryTests()) {
                        testResultManager.startTest("InventoryServiceTest", inventoryServiceTest.executeTest());
                    }

                    if (config.enablePlayerTests()) {
                        testResultManager.startTest("PlayerServiceTest", playerServiceTest.executeTest());
                    }

                    if (config.enableGroundItemTests()) {
                        testResultManager.startTest("GroundItemServiceTest", groundObjectServiceTest.executeTest());
                    }
                } else {
                    log.info("Stopping API tests...");
                    testResultManager.cancelAllTests();
                    testResultManager.getAllTestResults().clear();
                }
            }
        }
    }

    @Override
    protected void startUp() {
        log.info("Starting up Example Plugin...");
        context.register();
        context.loadHooks();

        // Add overlays
        overlayManager.add(movementOverlay);
        overlayManager.add(overlay);
        overlayManager.add(testApiOverlay);
        overlayManager.add(infoPanelOverlay);
    }

    @Override
    protected void shutDown() {
        log.info("Shutting down Example Plugin...");
        testResultManager.cancelAllTests();

        // Remove overlays
        overlayManager.remove(movementOverlay);
        overlayManager.remove(overlay);
        overlayManager.remove(testApiOverlay);
        overlayManager.remove(infoPanelOverlay);
    }

    @Subscribe
    private void onGameStateChanged(final GameStateChanged event) {
        final GameState gameState = event.getGameState();

        switch (gameState) {
            case LOGGED_IN:
                log.info("Logged in - initializing plugin...");
                startUp();
                break;
            case HOPPING:
            case LOGIN_SCREEN:
                log.info("Logged out - shutting down plugin...");
                shutDown();
                break;
            default:
                break;
        }
    }
}