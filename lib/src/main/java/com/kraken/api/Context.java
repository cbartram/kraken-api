package com.kraken.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.PacketMethodLocator;
import com.kraken.api.input.VirtualMouse;
import com.kraken.api.interaction.InteractionManager;
import com.kraken.api.interaction.camera.CameraService;
import com.kraken.api.interaction.container.bank.BankInventoryQuery;
import com.kraken.api.interaction.container.bank.BankQuery;
import com.kraken.api.interaction.container.bank.BankService;
import com.kraken.api.interaction.container.inventory.InventoryQuery;
import com.kraken.api.interaction.container.inventory.InventoryService;
import com.kraken.api.interaction.equipment.EquipmentService;
import com.kraken.api.interaction.gameobject.GameObjectService;
import com.kraken.api.interaction.groundobject.GroundObjectService;
import com.kraken.api.interaction.movement.MinimapService;
import com.kraken.api.interaction.movement.MovementService;
import com.kraken.api.interaction.npc.NpcQuery;
import com.kraken.api.interaction.npc.NpcService;
import com.kraken.api.interaction.player.PlayerService;
import com.kraken.api.interaction.prayer.PrayerService;
import com.kraken.api.interaction.spells.SpellService;
import com.kraken.api.interaction.ui.TabService;
import com.kraken.api.interaction.ui.UIService;
import com.kraken.api.interaction.widget.WidgetService;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.ItemManager;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.*;

@Slf4j
@Singleton
public class Context {

    @Getter
    private final Client client;

    @Getter
    private final ClientThread clientThread;

    @Getter
    @Setter
    private VirtualMouse mouse;

    @Getter
    private boolean isRegistered = false;

    @Getter
    private boolean packetsLoaded = false;

    private final Set<Class<?>> EVENTBUS_LISTENERS = Set.of(
            this.getClass(),
            EquipmentService.class,
            BankService.class,
            CameraService.class,
            GameObjectService.class,
            GroundObjectService.class,
            InventoryService.class,
            MovementService.class,
            MinimapService.class,
            PlayerService.class,
            NpcService.class,
            PrayerService.class,
            SpellService.class,
            UIService.class,
            TabService.class,
            WidgetService.class
    );

    private final Injector injector;
    private final EventBus eventBus;
    private final ItemManager itemManager;

    @Getter
    private final InteractionManager interactionManager;

    @Inject
    public Context(final Client client, final ClientThread clientThread, final VirtualMouse mouse,
                   final EventBus eventBus, final Injector injector,
                   final InteractionManager interactionManager, final ItemManager itemManager) {
        this.client = client;
        this.clientThread = clientThread;
        this.mouse = mouse;
        this.injector = injector;
        this.eventBus = eventBus;
        this.interactionManager = interactionManager;
        this.itemManager = itemManager;
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
     * Registers all Service classes that have methods annotated with @Subscribe to the EventBus.
     * This allows the API to listen for key RuneLite events and respond accordingly.
     */
    public void register() {
        try {
            for (Class<?> clazz : EVENTBUS_LISTENERS) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Subscribe.class)) {
                        Object handler = injector.getInstance(clazz);
                        if (handler != null) {
                            eventBus.register(handler);
                            log.debug("Registered class: {} with eventbus", clazz.getSimpleName());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error registering event handlers: {}", e.getMessage());
            return;
        }

        isRegistered = true;
    }

    /**
     * Unregisters all Service classes that have methods annotated with @Subscribe from the EventBus.
     */
    public void destroy() {
        try {
            for (Class<?> clazz : EVENTBUS_LISTENERS) {
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Subscribe.class)) {
                        Object handler = injector.getInstance(clazz);
                        if (handler != null) {
                            log.info("Unregistering {} from eventbus", clazz.getSimpleName());
                            eventBus.unregister(handler);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error un-registering event handlers: {}", e.getMessage());
            return;
        }

        isRegistered = false;
    }

    /**
     * Returns a varbit value from the RuneLite client. This method is
     * thread safe and runs on the client thread to retrieve the value.
     * @param varbit The varbit value to retrieve.
     * @return The varbit value (either 0 for false/unset or 1 for true/set).
     */
    public int getVarbitValue(int varbit) {
        return runOnClientThread(() -> client.getVarbitValue(varbit));
    }

    /**
     * Retrieves a Widget from the RuneLite client. This method is
     * thread safe and will run on the client thread to retrieve the Widget.
     * @param widgetId int The widget id
     * @return Widget
     */
    public Widget getWidget(int widgetId) {
        return runOnClientThread(() -> client.getWidget(widgetId));
    }

    /**
     * Run a method on the client thread, returning the result directly.
     * @param method The method to call
     * @param <T> The type of the method's return value
     * @return The result from the called method
     */
    @SneakyThrows
    public <T> T runOnClientThread(Callable<T> method) {
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
            log.error("Failed to run method on client thread: timeout after 2 seconds: {}", e.getMessage());
            return null;
        } catch (ExecutionException e) {
            log.error("Failed to run method on client thread: {}, message: {}", e.getCause(), e.getMessage());
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
     * Creates a new query builder for NPCs.
     * Usage: ctx.npcs().withName("Goblin").first().interact("Attack");
     *
     * @return NpcQuery object used to chain together predicates to select specific NPC's within the scene.
     */
    public NpcQuery npcs() {
        return new NpcQuery(this);
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
        return new BankQuery(this, itemManager);
    }
}
