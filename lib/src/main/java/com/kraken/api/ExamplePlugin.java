package com.kraken.api;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
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
import net.runelite.api.TileObject;
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
    private InventoryService inventoryService;

    @Inject
    private PlayerService playerService;

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

    private Thread thread;

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
                    runNpcServiceTest();
                    runInventoryServiceTest();
                    runPlayerServiceTest();
                    runGroundObjectServiceTest();
                } else {
                    log.info("Stopping...");
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

    private void runNpcServiceTest() {
        thread = new Thread(() -> {
            log.info("Starting npc service test in new thread...");
            npcService
                    .getAttackableNpcs("Guard")
                    .filter(n -> !n.isInteracting())
                    .findFirst().ifPresent(guard -> log.info("Guard found"));

            java.util.List<NPC> npcs = npcService.getNpcs().collect(Collectors.toList());

            for(NPC npc : npcs) {
                log.info("Npc found: {}", context.runOnClientThreadOptional(npc::getName));
            }

        });

        thread.start();
    }


    private void runInventoryServiceTest() {
        thread = new Thread(() -> {
            log.info("Starting inventory service test in new thread...");
            log.info("Has food: {}", inventoryService.hasFood());

            int idx = 0;
            for(InventoryItem item : inventoryService.all()) {
                log.info("Item found: {}", item.getName());
                if(idx == 0) {
                    log.info("Interacting with: {}", item.getName());
                    inventoryService.interactReflect(item, "Use");
                }
                idx++;
            }

            idx = 0;
            for(InventoryItem i : inventoryService.getFood()) {
                log.info("Food found: {}", i.getName());
                idx++;
            }

            int runePlateQuantity = inventoryService.get("Rune Platebody").getQuantity();
            log.info("Rune Platebody quantity: {}", runePlateQuantity);
            inventoryService.drop("Swordfish");
        });

        thread.start();
    }

    private void runPlayerServiceTest() {
        thread = new Thread(() -> {
            log.info("Starting player service test in new thread...");
            double healthPercent = playerService.getHealthPercentage();
            log.info("Health percent: {}, total: {}, curr, {}", healthPercent, playerService.getMaxHealth(), playerService.getHealthRemaining());
            log.info("Spec enabled: {}", playerService.isSpecEnabled());
            log.info("Is moving: {}", playerService.isMoving());
            log.info("Is poisoned: {}", playerService.isPoisoned());
            log.info("Is Run enabled: {}", playerService.isRunEnabled());
            playerService.toggleRun();
        });

        thread.start();
    }

    private void runGroundObjectServiceTest() {
        thread = new Thread(() -> {
            log.info("Starting ground object service test in new thread...");
            groundObjectService.get("Bones").stream().findFirst().ifPresent(bones -> {
                log.info("Bones found on ground, interacting...");
                groundObjectService.interactReflect(bones, "Take");
            });

            for (TileObject tileObject : groundObjectService.all()) {
                log.info("Tile object: {}", tileObject.getId());
            }
        });

        thread.start();
    }
}
