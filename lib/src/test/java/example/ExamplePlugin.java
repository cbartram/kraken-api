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
import example.tests.service.CameraServiceTest;
import example.tests.service.MovementServiceTest;
import example.tests.service.PrayerServiceTest;
import example.tests.service.SpellServiceTest;
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

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

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
    private PrayerServiceTest prayerServiceTest;

    @Inject
    private BankTest bankQueryTest;

    @Inject
    private EquipmentTest equipmentQueryTest;

    @Inject
    private InventoryTest inventoryQueryTest;

    @Inject
    private BankInventoryTest bankInventoryQueryTest;

    @Inject
    private GameObjectTest gameObjectQueryTest;

    @Inject
    private NpcTest npcQueryTest;

    @Inject
    private GroundObjectTest groundObjectQueryTest;

    @Inject
    private PlayerTest playerQueryTest;

    @Inject
    private WidgetTest widgetQueryTest;

    @Inject
    private SpellServiceTest spellServiceTest;

    @Inject
    private MovementServiceTest movementServiceTest;

    @Inject
    private CameraServiceTest cameraServiceTest;

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

    @Inject private Pathfinder pathfinder;
    @Getter private List<WorldPoint> currentPath;

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

            if(event.getKey().equalsIgnoreCase("showMouse")) {
                if(config.showMouse()) {
                    overlayManager.add(mouseOverlay);
                } else {
                    overlayManager.remove(mouseOverlay);
                }
            }

            String key = event.getKey();
            if(key.equalsIgnoreCase("enablePrayer") && config.enablePrayerTests()) {
                testResultManager.startTest("PrayerServiceTest", prayerServiceTest.executeTest());
            } else if(key.equalsIgnoreCase("enableBankQuery") && config.enableBankQuery()) {
                testResultManager.startTest("BankQuery", bankQueryTest.executeTest());
            } else if(key.equalsIgnoreCase("enableInventoryQuery") && config.enableInventoryQuery()) {
                testResultManager.startTest("InventoryQuery", inventoryQueryTest.executeTest());
            } else if(key.equalsIgnoreCase("enableBankInventoryQuery") && config.enableBankInventoryQuery()) {
                testResultManager.startTest("BankInventoryTest", bankInventoryQueryTest.executeTest());
            } else if(key.equalsIgnoreCase("enableEquipmentQuery") && config.enableEquipmentQuery()) {
                testResultManager.startTest("EquipmentQuery", equipmentQueryTest.executeTest());
            } else if(key.equalsIgnoreCase("enableGameObjectQuery") && config.enableGameObjectQuery()) {
                testResultManager.startTest("GameObjectQuery", gameObjectQueryTest.executeTest());
            } else if(key.equalsIgnoreCase("enableGroundObjectQuery") && config.enableGroundObjectQuery()) {
                testResultManager.startTest("GroundObjectQuery", groundObjectQueryTest.executeTest());
            } else if(key.equalsIgnoreCase("enableNpcQuery") && config.enableNpcQuery()) {
                testResultManager.startTest("NpcQuery", npcQueryTest.executeTest());
            } else if(key.equalsIgnoreCase("enablePlayerQuery") && config.enablePlayerQuery()) {
                testResultManager.startTest("PlayerQuery", playerQueryTest.executeTest());
            } else if(key.equalsIgnoreCase("enableWidgetQuery") && config.enableWidgetQuery()) {
                testResultManager.startTest("WidgetQuery", widgetQueryTest.executeTest());
            } else if(key.equalsIgnoreCase("enableMovement") && config.enableMovementTests()) {
                testResultManager.startTest("MovementService", movementServiceTest.executeTest());
            } else if(key.equalsIgnoreCase("enableSpell") && config.enableSpellTests()) {
                testResultManager.startTest("SpellService", spellServiceTest.executeTest());
            } else if(key.equalsIgnoreCase("enableCamera") && config.enableCameraTests()) {
                testResultManager.startTest("CameraService", cameraServiceTest.executeTest());
            }

            if(event.getKey().equals("clearTests") && config.clearTests()) {
                testResultManager.cancelAllTests();
                testResultManager.getAllTestResults().clear();
            }
        }
    }

    @Override
    protected void startUp() {
        context.register();
        context.initializePackets();

        // Add overlays
        overlayManager.add(overlay);
        overlayManager.add(testApiOverlay);
        overlayManager.add(infoPanelOverlay);
        overlayManager.add(sceneOverlay);
        if(config.showMouse()) {
            overlayManager.add(mouseOverlay);
        }
    }

    @Override
    protected void shutDown() {
        testResultManager.cancelAllTests();

        // Remove overlays
        overlayManager.remove(overlay);
        overlayManager.remove(testApiOverlay);
        overlayManager.remove(infoPanelOverlay);
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(mouseOverlay);
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
}