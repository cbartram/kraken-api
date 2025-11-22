package com.kraken.api.example;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.NPCPackets;
import com.kraken.api.example.overlay.InfoPanelOverlay;
import com.kraken.api.example.overlay.SceneOverlay;
import com.kraken.api.example.overlay.TestApiOverlay;
import com.kraken.api.example.tests.*;
import com.kraken.api.interaction.container.bank.BankService;
import com.kraken.api.interaction.movement.MovementService;
import com.kraken.api.interaction.npc.NpcService;
import com.kraken.api.interaction.spells.SpellService;
import com.kraken.api.interaction.spells.Spells;
import com.kraken.api.interaction.ui.UIService;
import com.kraken.api.overlay.MouseOverlay;
import com.kraken.api.sim.ui.SimulationVisualizer;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuEntryAdded;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.ComponentID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
    private Context context;

    @Inject
    private ExampleConfig config;

    @Inject
    private MouseOverlay overlay;

    @Inject
    private TestApiOverlay testApiOverlay;

    @Inject
    private InfoPanelOverlay infoPanelOverlay;

    @Inject
    private TestResultManager testResultManager;

    @Inject
    private NpcServiceTest npcServiceTest;

    @Inject
    private GameObjectServiceTest gameObjectServiceTest;

    @Inject
    private InventoryServiceTest inventoryServiceTest;

    @Inject
    private BankServiceTest bankServiceTest;

    @Inject
    private PlayerServiceTest playerServiceTest;

    @Inject
    private EquipmentServiceTest equipmentServiceTest;

    @Inject
    private MovementService movementService;

    @Inject
    private GroundObjectServiceTest groundObjectServiceTest;

    @Inject
    private PrayerServiceTest prayerServiceTest;

    @Inject
    private SimulationVisualizer visualizer;

    @Inject
    private Client client;

    @Inject
    private SceneOverlay sceneOverlay;

    @Getter
    private WorldPoint targetTile;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private NPCPackets npcPackets;

    @Inject
    private NpcService npcService;

    @Inject
    private SpellService spellService;

    @Inject
    private BankService bankService;

    private WorldPoint trueTile;
    private static final String TARGET_TILE = ColorUtil.wrapWithColorTag("Target Tile", JagexColors.CHAT_PRIVATE_MESSAGE_TEXT_TRANSPARENT_BACKGROUND);

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

            if(event.getKey().equals("simVisualizer")) {
                // Init will dump collision data and load the game state. This should only be called
                // if game state is logged in
                if(client.getGameState() == GameState.LOGGED_IN && config.showVisualizer()) {
                    visualizer.init();
                    visualizer.setVisible(true);
                }
            }

            String k = event.getKey();
            if(config.enableMovementTests()) {
                if(k.equals("fromWorldInstance") && config.fromWorldInstance()) {
                    movementService.moveTo(targetTile);
                }
            }

            if(event.getKey().equals("attackNpc") && config.prayerOn()) {
                NPC npc = npcService.getAttackableNpcs().findFirst().orElse(null);
                if(npc != null) {
                    Point pt = UIService.getClickingPoint(npc.getConvexHull().getBounds(),  true);
                    log.info("Attacking NPC");
                    mousePackets.queueClickPacket(pt.getX(), pt.getY());
                    npcPackets.queueNPCAction(npc, "Attack");
                }
            }

            if(event.getKey().equals("magicSpellCast") && config.magicSpellCast()) {
                log.info("Teleporting to Varrock");
                spellService.cast(Spells.VARROCK_TELEPORT);
            }

            if(event.getKey().equals("depositOneCheck") && config.depositOneCheck()) {
                log.info("Depositing one: {}", config.depositOneItem());
                bankService.depositOne(config.depositOneItem());

            }

            if(event.getKey().equals("depositAllCheck") && config.depositAllCheck()) {
                log.info("Depositing all: {}", config.depositAllItem());
                bankService.depositAll(config.depositAllItem());
            }

            if(event.getKey().equals("start")) {
                if (config.start()) {
                    log.info("Starting API tests...");

                    // Run tests based on configuration
                    if (config.enableNpcTests()) {
                        testResultManager.startTest("NpcServiceTest", npcServiceTest.executeTest());
                    }

                    if(config.enableGameObjectTests()) {
                        testResultManager.startTest("GameObjectServiceTest", gameObjectServiceTest.executeTest());
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

                    if(config.enableBankTests()) {
                        testResultManager.startTest("BankServiceTest", bankServiceTest.executeTest());
                    }

                    if(config.enablePrayerTests()) {
                        testResultManager.startTest("PrayerServiceTest", prayerServiceTest.executeTest());
                    }

                    if(config.enableEquipmentTests()) {
                        testResultManager.startTest("EquipmentServiceTest", equipmentServiceTest.executeTest());
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
        context.register();
        context.loadHooks();
        context.initializePackets();

        // Add overlays
        overlayManager.add(overlay);
        overlayManager.add(testApiOverlay);
        overlayManager.add(infoPanelOverlay);
        overlayManager.add(sceneOverlay);
    }

    @Override
    protected void shutDown() {
        testResultManager.cancelAllTests();

        // Remove overlays
        overlayManager.remove(overlay);
        overlayManager.remove(testApiOverlay);
        overlayManager.remove(infoPanelOverlay);
        overlayManager.remove(sceneOverlay);
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
                break;
            default:
                break;
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        trueTile = getSelectedWorldPoint();
    }

    @Subscribe
    private void onMenuEntryAdded(MenuEntryAdded event) {
        if (client.isKeyPressed(KeyCode.KC_SHIFT) && event.getOption().equals("Walk here") && event.getTarget().isEmpty()) {
            addMenuEntry(event, "Set", TARGET_TILE, 1);
        }
    }

    private void addMenuEntry(MenuEntryAdded event, String option, String target, int position) {
        List<MenuEntry> entries = new LinkedList<>(Arrays.asList(client.getMenu().getMenuEntries()));
        if (entries.stream().anyMatch(e -> e.getOption().equals(option) && e.getTarget().equals(target))) {
            return;
        }

        client.getMenu().createMenuEntry(position)
                .setOption(option)
                .setTarget(target)
                .setParam0(event.getActionParam0())
                .setParam1(event.getActionParam1())
                .setIdentifier(event.getIdentifier())
                .setType(MenuAction.RUNELITE)
                .onClick(this::onMenuOptionClicked);
    }

    private void onMenuOptionClicked(MenuEntry entry) {
        if (entry.getOption().equals("Set") && entry.getTarget().equals(TARGET_TILE)) {
            targetTile = trueTile;
        }
    }

    private WorldPoint getSelectedWorldPoint() {
        if (client.getWidget(ComponentID.WORLD_MAP_MAPVIEW) == null) {
            if (client.getTopLevelWorldView().getSelectedSceneTile() != null) {
                return client.getTopLevelWorldView().isInstance() ?
                        WorldPoint.fromLocalInstance(client, client.getTopLevelWorldView().getSelectedSceneTile().getLocalLocation()) :
                        client.getTopLevelWorldView().getSelectedSceneTile().getWorldLocation();
            }
        }
        return null;
    }
}