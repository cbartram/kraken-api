package example;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.script.breakhandler.BreakConditions;
import com.kraken.api.core.script.breakhandler.BreakManager;
import com.kraken.api.core.script.breakhandler.BreakProfile;
import com.kraken.api.input.mouse.MouseRecorder;
import com.kraken.api.overlay.MouseOverlay;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.map.WorldMapService;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import com.kraken.api.service.ui.login.LoginService;
import com.kraken.api.sim.ui.SimulationVisualizer;
import example.overlay.InfoPanelOverlay;
import example.overlay.SceneOverlay;
import example.tests.input.MouseTest;
import example.tests.query.*;
import example.tests.service.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.ComponentID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.JagexColors;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ColorUtil;

import java.util.*;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "API Test",
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
    private InfoPanelOverlay infoPanelOverlay;

    @Inject
    private TestResultManager testResultManager;

    @Inject
    private SimulationVisualizer visualizer;

    @Inject
    private Client client;

    @Inject
    private SceneOverlay sceneOverlay;

    @Inject
    private MouseOverlay mouseOverlay;

    @Inject
    private LocalPathfinder pathfinder;

    @Inject
    private WorldMapService worldMapService;

    @Inject
    private ExampleScript exampleScript;

    @Inject
    private LoginService loginService;

    @Inject
    private MouseRecorder mouseRecorder;

    @Getter
    private WorldPoint targetTile;

    @Getter
    private List<WorldPoint> currentPath = new ArrayList<>();

    @Getter
    private WorldArea targetArea;

    @Inject
    private BreakManager breakManager;

    @Inject
    private BankService bankService;

    private WorldPoint trueTile;
    private static final String TARGET_TILE = ColorUtil.wrapWithColorTag("Target Tile", JagexColors.CHAT_PRIVATE_MESSAGE_TEXT_TRANSPARENT_BACKGROUND);
    private final Map<String, TestExecution> testExecutions = new HashMap<>();

    @Inject
    private void initializeTests(
            PrayerServiceTest prayerServiceTest, BankTest bankQueryTest, EquipmentTest equipmentQueryTest,
            InventoryTest inventoryQueryTest, BankInventoryTest bankInventoryQueryTest, GameObjectTest gameObjectQueryTest,
            NpcTest npcQueryTest, GroundObjectTest groundObjectQueryTest, PlayerTest playerQueryTest,
            WidgetTest widgetQueryTest, SpellServiceTest spellServiceTest, MovementServiceTest movementServiceTest,
            CameraServiceTest cameraServiceTest, PathfinderServiceTest pathfinderServiceTest, WorldQueryTest worldQueryTest,
            TaskChainTest taskChainTest, MouseTest mouseTest, DialogueServiceTest dialogueServiceTest, ProcessingServiceTest processingServiceTest,
            AreaServiceTest areaServiceTest
    ) {
        registerTest("enablePrayer", "PrayerServiceTest", config::enablePrayerTests, prayerServiceTest::executeTest);
        registerTest("enableBankQuery", "BankQuery", config::enableBankQuery, bankQueryTest::executeTest);
        registerTest("enableInventoryQuery", "InventoryQuery", config::enableInventoryQuery, inventoryQueryTest::executeTest);
        registerTest("enableBankInventoryQuery", "BankInventoryTest", config::enableBankInventoryQuery, bankInventoryQueryTest::executeTest);
        registerTest("enableEquipmentQuery", "EquipmentQuery", config::enableEquipmentQuery, equipmentQueryTest::executeTest);
        registerTest("enableGameObjectQuery", "GameObjectQuery", config::enableGameObjectQuery, gameObjectQueryTest::executeTest);
        registerTest("enableGroundObjectQuery", "GroundObjectQuery", config::enableGroundObjectQuery, groundObjectQueryTest::executeTest);
        registerTest("enableNpcQuery", "NpcQuery", config::enableNpcQuery, npcQueryTest::executeTest);
        registerTest("enablePlayerQuery", "PlayerQuery", config::enablePlayerQuery, playerQueryTest::executeTest);
        registerTest("enableWidgetQuery", "WidgetQuery", config::enableWidgetQuery, widgetQueryTest::executeTest);
        registerTest("enableMovement", "MovementService", config::enableMovementTests, movementServiceTest::executeTest);
        registerTest("enableSpell", "SpellService", config::enableSpellTests, spellServiceTest::executeTest);
        registerTest("enableCamera", "CameraService", config::enableCameraTests, cameraServiceTest::executeTest);
        registerTest("enablePathfinder", "PathfinderService", config::enablePathfinder, pathfinderServiceTest::executeTest);
        registerTest("enableWorldQuery", "WorldQuery", config::enableWorldQuery, worldQueryTest::executeTest);
        registerTest("enableTaskChain", "TaskChain", config::enableTaskChain, taskChainTest::executeTest);
        registerTest("enableMouseTest", "VirtualMouse", config::enableMouseTest, mouseTest::executeTest);
        registerTest("enableDialogueService", "DialogueService", config::enableDialogueService, dialogueServiceTest::executeTest);
        registerTest("enableProcessingService", "ProcessingService", config::enableProcessService, processingServiceTest::executeTest);
        registerTest("enableAreaService", "AreaService", config::enableAreaService, areaServiceTest::executeTest);
    }

    private void registerTest(String configKey, String testName, BooleanSupplier enabled, Supplier<java.util.concurrent.CompletableFuture<Boolean>> test) {
        testExecutions.put(configKey, new TestExecution(testName, enabled, test));
        testResultManager.registerTest(testName);
    }

    @Provides
    ExampleConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(ExampleConfig.class);
    }

    @Subscribe
    private void onMenuOptionClicked(MenuOptionClicked event) {
        if (config.showDebugInfo()) {
            log.info("Option={}, Target={}, Param0={}, Param1={}, MenuAction={}, ItemId={}, id={}, itemOp={}, str={}",
                    event.getMenuOption(), event.getMenuTarget(), event.getParam0(), event.getParam1(), event.getMenuAction().name(), event.getItemId(),
                    event.getId(), event.getItemOp(), event);
        }
    }

    @Subscribe
    private void onConfigChanged(final ConfigChanged event) {
        if (!event.getGroup().equals("testapi")) {
            return;
        }

        String key = event.getKey();

        if (key.equals("startPathfinding") && config.startPathfinding()) {
            String waypointLocation = config.waypointLocation();
            String[] coords = waypointLocation.split(",");
            if (coords.length == 3) {
                try {
                    int x = Integer.parseInt(coords[0]);
                    int y = Integer.parseInt(coords[1]);
                    int z = Integer.parseInt(coords[2]);
                    log.info("Setting target point: ({}, {}, {})", x, y, z);
                    this.targetTile = new WorldPoint(x, y, z);
                } catch (NumberFormatException e) {
                    log.error("Invalid waypoint location format", e);
                }
            } else {
                log.error("Invalid waypoint location format");
            }
        }

        if(key.equals("login") && config.login()) {
            log.info("Logging into the client...");
            loginService.login();
        }

        if(key.equals("logout") && config.logout()) {
            log.info("Logging out of the client...");
            context.players().local().logout();
        }

        if(key.equalsIgnoreCase("pauseScript") && config.pauseScript()) {
            exampleScript.pause();
        } else {
            exampleScript.resume();
        }

        if(key.equals("mouseRecord") && config.mouseRecord()) {
            log.info("Starting recording...");
            mouseRecorder.start("test");
        } else if(key.equals("mouseRecord") && !config.mouseRecord()) {
            log.info("Stopping recording");
            mouseRecorder.stop();
        }

        if (key.equals("simVisualizer")) {
            if (client.getGameState() == GameState.LOGGED_IN && config.showVisualizer()) {
                visualizer.init();
                visualizer.setVisible(true);
            }
        } else if (key.equalsIgnoreCase("showMouse")) {
            if (config.showMouse()) {
                overlayManager.add(mouseOverlay);
            } else {
                overlayManager.remove(mouseOverlay);
            }
        } if (key.equals("clearTests") && config.clearTests()) {
            testResultManager.clearAllResults();
        } else {
            TestExecution execution = testExecutions.get(key);
            if (execution != null) {
                if (execution.getEnabled().getAsBoolean()) {
                    testResultManager.startTest(execution.getTestName(), execution.getTest().get());
                } else {
                    testResultManager.cancelTest(execution.getTestName());
                }
            }
        }
    }

    @Override
    protected void startUp() {
        context.initializePackets();
        exampleScript.start();

        overlayManager.add(overlay);
        overlayManager.add(infoPanelOverlay);
        overlayManager.add(sceneOverlay);
        if (config.showMouse()) {
            overlayManager.add(mouseOverlay);
        }

        breakManager.initialize();

        BreakProfile profile = BreakProfile.builder()
                .name("Jewelry Profile")
                .minRuntime(java.time.Duration.ofMinutes(2))
                .maxRuntime(java.time.Duration.ofMinutes(4))
                .minBreakDuration(java.time.Duration.ofMinutes(2))
                .maxBreakDuration(java.time.Duration.ofMinutes(5))
                .logoutDuringBreak(true)
                .randomizeTimings(true)
                .addBreakCondition(BreakConditions.onLevelReached(context.getClient(), Skill.CRAFTING, 54))
                .addBreakCondition(BreakConditions.onBankEmpty(bankService, context, 1603))
                .build();

        breakManager.attachScript(exampleScript, profile);
    }

    @Override
    protected void shutDown() {
        testResultManager.cancelAllTests();
        exampleScript.stop();

        overlayManager.remove(overlay);
        overlayManager.remove(infoPanelOverlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(mouseOverlay);

        if (!breakManager.isOnBreak()) {
            breakManager.detachScript();
        }
    }

    @Subscribe
    private void onGameStateChanged(final GameStateChanged event) {
        if (event.getGameState() == GameState.LOGGED_IN) {
            startUp();
        } else if (event.getGameState() == GameState.LOGIN_SCREEN || event.getGameState() == GameState.HOPPING) {
            shutDown();
        }
    }

    @Subscribe
    public void onGameTick(GameTick event) {
        if (targetTile != null) {
            this.targetArea = new WorldArea(targetTile, 5, 5);
            this.currentPath = pathfinder.findPath(client.getLocalPlayer().getWorldLocation(), this.targetTile);
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened event) {
        trueTile = getSelectedWorldPoint();
    }

    @Subscribe
    private void onMenuEntryAdded(MenuEntryAdded event) {
        // Handles a user setting custom destination for movement test within the game world by Shift + clicking a tile
        if (client.isKeyPressed(KeyCode.KC_SHIFT) && event.getOption().equals("Walk here") && event.getTarget().isEmpty()) {
            addMenuEntry(event, "Set", TARGET_TILE, 1);
        }

        // If a right click occurs on the world map get the WorldPoint of where the click occurred
        // and allow players to set a destination that way.
        final Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        if(map == null) return;

        Point lastMenuOpenedPoint = client.getMouseCanvasPosition();
        final WorldPoint wp = worldMapService.mapClickToWorldPoint(lastMenuOpenedPoint.getX(), lastMenuOpenedPoint.getY());

        if (wp != null) {
            client.getMenu().createMenuEntry(0)
                    .setOption("Set")
                    .setTarget(ColorUtil.wrapWithColorTag(wp.toString(), JagexColors.CHAT_PRIVATE_MESSAGE_TEXT_TRANSPARENT_BACKGROUND))
                    .setParam0(event.getActionParam0())
                    .setParam1(event.getActionParam1())
                    .setIdentifier(event.getIdentifier())
                    .setType(MenuAction.RUNELITE)
                    .onClick(e -> {
                        if(e.getOption().equalsIgnoreCase("Set")) {
                            targetTile = wp;
                        }
                    });
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

    @Getter
    private static class TestExecution {
        private final String testName;
        private final BooleanSupplier enabled;
        private final Supplier<java.util.concurrent.CompletableFuture<Boolean>> test;

        public TestExecution(String testName, BooleanSupplier enabled, Supplier<java.util.concurrent.CompletableFuture<Boolean>> test) {
            this.testName = testName;
            this.enabled = enabled;
            this.test = test;
        }
    }
}
