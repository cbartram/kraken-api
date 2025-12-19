package example;

import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.overlay.MouseOverlay;
import com.kraken.api.service.movement.Pathfinder;
import com.kraken.api.sim.ui.SimulationVisualizer;
import example.overlay.InfoPanelOverlay;
import example.overlay.SceneOverlay;
import example.overlay.TestApiOverlay;
import example.tests.query.*;
import example.tests.service.*;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
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
    private TestApiOverlay testApiOverlay;

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

    @Getter
    private WorldPoint targetTile;

    @Inject
    private Pathfinder pathfinder;

    @Inject
    private ExampleScript exampleScript;

    @Getter
    private List<WorldPoint> currentPath = new ArrayList<>();

    private WorldPoint trueTile;
    private static final String TARGET_TILE = ColorUtil.wrapWithColorTag("Target Tile", JagexColors.CHAT_PRIVATE_MESSAGE_TEXT_TRANSPARENT_BACKGROUND);

    private final Map<String, TestExecution> testExecutions = new HashMap<>();

    @Inject
    private void initializeTests(
            PrayerServiceTest prayerServiceTest, BankTest bankQueryTest, EquipmentTest equipmentQueryTest,
            InventoryTest inventoryQueryTest, BankInventoryTest bankInventoryQueryTest, GameObjectTest gameObjectQueryTest,
            NpcTest npcQueryTest, GroundObjectTest groundObjectQueryTest, PlayerTest playerQueryTest,
            WidgetTest widgetQueryTest, SpellServiceTest spellServiceTest, MovementServiceTest movementServiceTest,
            CameraServiceTest cameraServiceTest, PathfinderServiceTest pathfinderServiceTest
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

        if(key.equalsIgnoreCase("pauseScript") && config.pauseScript()) {
            exampleScript.pause();
        } else {
            exampleScript.resume();
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
        context.register();
        context.initializePackets();
        exampleScript.start();

        overlayManager.add(overlay);
        overlayManager.add(testApiOverlay);
        overlayManager.add(infoPanelOverlay);
        overlayManager.add(sceneOverlay);
        if (config.showMouse()) {
            overlayManager.add(mouseOverlay);
        }
    }

    @Override
    protected void shutDown() {
        testResultManager.cancelAllTests();
        exampleScript.stop();

        overlayManager.remove(overlay);
        overlayManager.remove(testApiOverlay);
        overlayManager.remove(infoPanelOverlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(mouseOverlay);
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
            this.currentPath = pathfinder.findSparsePath(client.getLocalPlayer().getWorldLocation(), targetTile);
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
