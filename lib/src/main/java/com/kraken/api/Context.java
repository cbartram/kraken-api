package com.kraken.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.PacketMethodLocator;
import com.kraken.api.core.packet.debug.PacketSpy;
import com.kraken.api.input.mouse.VirtualMouse;
import com.kraken.api.query.InteractionManager;
import com.kraken.api.query.container.bank.BankInventoryQuery;
import com.kraken.api.query.container.bank.BankQuery;
import com.kraken.api.query.container.inventory.InventoryQuery;
import com.kraken.api.query.equipment.EquipmentQuery;
import com.kraken.api.query.gameobject.GameObjectQuery;
import com.kraken.api.query.groundobject.GroundObjectQuery;
import com.kraken.api.query.npc.NpcQuery;
import com.kraken.api.query.player.LocalPlayerEntity;
import com.kraken.api.query.player.PlayerQuery;
import com.kraken.api.query.widget.WidgetQuery;
import com.kraken.api.query.world.WorldQuery;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.tile.TileService;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.EnumComposition;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.game.ItemManager;

import java.util.Optional;
import java.util.concurrent.*;

@Slf4j
@Singleton
public class Context {

    @Setter
    @Getter
    private VirtualMouse mouse;

    @Getter
    private final Client client;

    @Getter
    private final ClientThread clientThread;

    @Getter
    private boolean packetsLoaded = false;

    @Getter
    private final InteractionManager interactionManager;

    @Getter
    private final TileService tileService;

    @Getter
    private final LocalPlayerEntity localPlayer;

    @Getter
    private final ItemManager itemManager;

    private final Injector injector;

    @Inject
    public Context(final Client client, final ClientThread clientThread, final VirtualMouse mouse, final EventBus eventBus,
                   final Injector injector, final InteractionManager interactionManager, final TileService tileService,
                   final ItemManager itemManager, final BankService bankService, final PacketSpy packetSpy) {
        this.client = client;
        this.clientThread = clientThread;
        this.mouse = mouse;
        this.injector = injector;
        this.tileService = tileService;
        this.interactionManager = interactionManager;
        this.itemManager = itemManager;
        this.localPlayer = new LocalPlayerEntity(this);
        eventBus.register(this.localPlayer);
        eventBus.register(bankService);
        eventBus.register(packetSpy);
    }

    /**
     * Initializes packet queueing functionality by either loading the client packet
     * sending method from the cached json file or running an analysis on the RuneLite injected client
     * to determine the packet sending method.
     * <p>
     * This is required to be called before packets can actually be sent i.e. its necessary to know the packet
     * method in the client before calling it with reflection.
     */
    public void initializePackets() {
        if (packetsLoaded) return;

        try {
            PacketMethodLocator.initialize(client);
            packetsLoaded = true;
        } catch (Exception e) {
            log.error("failed to enable packet sending functionality with exception: {}", e.getMessage());
        }
    }

    /**
     * Returns a varbit value from the RuneLite client. This method is
     *  thread-safe and runs on the client thread to retrieve the value.
     * @param varbit The varbit value to retrieve.
     * @return The varbit value (either 0 for false/unset or 1 for true/set).
     */
    public int getVarbitValue(int varbit) {
        return runOnClientThread(() -> client.getVarbitValue(varbit));
    }

    /**
     * Returns a var player value from the RuneLite client. This method is
     *  thread-safe and runs on the client thread to retrieve the value.
     * @param varp The varp value to retrieve.
     * @return The varp value (either 0 for false/unset or 1 for true/set).
     */
    public int getVarpValue(int varp) {
        return runOnClientThread(() -> client.getVarpValue(varp));
    }

    /**
     * Retrieves a Widget from the RuneLite client. This method is
     *  thread-safe and will run on the client thread to retrieve the Widget.
     * @param widgetId int The widget id
     * @return Widget
     */
    public Widget getWidget(int widgetId) {
        return runOnClientThread(() -> client.getWidget(widgetId));
    }


    /**
     * Retrieves an enum composition from the RuneLite client thread. This method is
     * thread-safe and will run on the client thread to retrieve the EnumComposition.
     * @param enumId The enum id
     * @return EnumComposition
     */
    public EnumComposition getEnum(int enumId) {
        return runOnClientThread(() -> client.getEnum(enumId));
    }

    /**
     * Run a method on the client thread, returning the result directly.
     * @param method The method to call
     * @param <T> The type of the method's return value
     * @return The result from the called method
     */
    @SneakyThrows
    public <T> T runOnClientThread(Callable<T> method) {
        if(method == null) {
            log.error("callable method is null");
            return null;
        }

        if (client.isClientThread()) {
            return method.call();
        }

        final CompletableFuture<T> future = new CompletableFuture<>();

        clientThread.invoke(() -> {
            try {
                T result = method.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        try {
            return future.get(2000, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.error("Failed to run method on client thread: timeout after 2 seconds: {}", e.getMessage(), e);
            return null;
        } catch (ExecutionException e) {
            log.error("Failed to run method on client thread: {}, message: {}", e.getCause(), e.getMessage(), e);
            return null;
        }
    }

    /**
     * Runs a method on the client thread without returning a result.
     * @param method Runnable method to execute
     */
    public void runOnClientThread(Runnable method) {
        if (client.isClientThread()) {
            method.run();
            return;
        }

        clientThread.invoke(method);
    }

    /**
     * Run a method on the client thread, returning an optional of the result.
     * @param method The method to call
     * @param <T> The type of the method's return value
     * @return The result from the called method
     */
    @SneakyThrows
    public <T> Optional<T> runOnClientThreadOptional(Callable<T> method) {
        if (client.isClientThread()) {
            return Optional.ofNullable(method.call());
        }

        final CompletableFuture<T> future = new CompletableFuture<>();

        clientThread.invoke(() -> {
            try {
                T result = method.call();
                future.complete(result);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        try {
            return Optional.ofNullable(future.get(2000, TimeUnit.MILLISECONDS));
        } catch (TimeoutException e) {
            log.error("Failed to run method on client thread: timeout after 2 seconds: {}", e.getMessage());
            return Optional.empty();
        } catch (ExecutionException e) {
            log.error("Failed to run method on client thread: {}, message: {}", e.getCause(), e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Retrieves an instance of a specified service class.
     *
     * @param serviceClass The class of the service to retrieve.
     * @param <T>          The type of the service.
     * @return The instance of the service.
     */
    public <T> T getService(Class<T> serviceClass) {
        return injector.getInstance(serviceClass);
    }

    /**
     * Creates a new query builder for NPCs.
     * Usage: ctx.npcs().withName("Goblin").first().interact("Attack");
     *
     * @return NpcQuery object used to chain together predicates to select specific NPC's within the scene.
     */
    public NpcQuery npcs() {
        return new NpcQuery(this);
    }

    /**
     * Creates a new query builder for Players. This will also include the local player as well which can be
     * grabbed with {@code .local()}.
     * Usage: ctx.players().withName("Zezima").first().interact("Follow");
     * ctx.players().local().getName();
     *
     * @return PlayerQuery object used to chain together predicates to select specific Players's within the scene.
     */
    public PlayerQuery players() {
        return new PlayerQuery(this);
    }

    /**
     * Creates a new query builder for the standard Backpack Inventory. This is only for finding items in a players inventory and
     * should not be used when the Bank is open to deposit items. Instead, use {@code BankInventoryQuery} for depositing items.
     * Usage: ctx.inventory().withId(1234).count();
     *
     * @return InventoryQuery object used to chain together predicates to select specific items or groups of items within
     * the players inventory.
     */
    public InventoryQuery inventory() {
        return new InventoryQuery(this);
    }

    /**
     * Creates a new query builder for a Bank Inventory. This should only be used when the bank is open in order to
     * deposit items from the players inventory into the bank. A different parent widget is used for the players inventory
     * while the bank is open compared to the normal players inventory. For querying the players inventory to eat food,
     * interact with objects, or perform general actions without a bank use: {@code InventoryQuery}.
     * Usage: ctx.bankInventory().withId(1234).count();
     *
     * @return BankInventoryQuery object used to chain together predicates to select specific items or groups of items within
     * the players inventory while the bank interface is open.
     */
    public BankInventoryQuery bankInventory() {
        return new BankInventoryQuery(this);
    }

    /**
     * Creates a new query builder for the Bank interface.
     * Usage: ctx.bank().withId(1234).interact("Withdraw-X");
     * @return BankQuery object used to chain together predicates to select specific items or groups of items within the players
     * bank.
     */
    public BankQuery bank() {
        return new BankQuery(this);
    }

    /**
     * Creates a new query builder for the equipment interface.
     * Usage: ctx.equipment().inSlot(EquipmentInventorySlot.HEAD).interact("Remove");
     * ctx.equipment().withId(1234).interact("Wield");
     *
     * @return EquipmentQuery object used to chain together predicates to select specific items or groups of items within the players
     * equipment or inventory interface. Only items with the action "wield" or "wear" will be interactable using this query from the inventory.
     */
    public EquipmentQuery equipment() {
        return new EquipmentQuery(this);
    }

    /**
     * Creates a new query builder for game objects. Game objects are objects in the game world like: Trees, ore, or fishing
     * spots which exist on tiles, can be interacted with, but cannot be picked up by the player. Usage:
     * ctx.gameObjects().withName("Oak Tree").nearest().interact("Chop");
     *
     * @return GameObjectQuery used to chain together predicates to select specific game objects within the scene.
     */
    public GameObjectQuery gameObjects() {
        return new GameObjectQuery(this);
    }

    /**
     * Creates a new query builder for Ground Items. GroundItems are items that exist on a tile that the player can pick up
     * and store in their inventory. Examples include: bones dropped from an NPC or loot dropped by another player on a tile.
     * Usage: ctx.groundObjects().withName("Twisted Bow").nearest().interact("Take");
     *
     * @return GroundObjectQuery used to chain together predicates to select specific ground items within the scene.
     */
    public GroundObjectQuery groundItems() {
        return new GroundObjectQuery(this);
    }

    /**
     * Creates a new query builder for Widgets. Usage: ctx.widgets().withText("Log Out").interact();
     * @return WidgetQuery used to chain together predicates to select specific widgets within the client.
     */
    public WidgetQuery widgets() {
        return new WidgetQuery(this);
    }

    /**
     * Creates a new query builder for Worlds. A WorldQuery provides functionality
     * for filtering and selecting specific game worlds based on various criteria,
     * such as population, world type, or location.
     *
     * <p>Worlds are the game servers that players can connect to. Each world may
     * have unique properties, such as member status, high population, or special
     * game rules.</p>
     *
     * @return {@literal @}WorldQuery object used to chain together predicates to
     *         select specific game worlds.
     */
    public WorldQuery worlds() {
        return new WorldQuery(this);
    }
}
