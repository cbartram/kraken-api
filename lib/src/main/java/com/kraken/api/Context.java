package com.kraken.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.kraken.api.core.loader.HooksLoader;
import com.kraken.api.core.loader.PacketUtilsLoader;
import com.kraken.api.input.VirtualMouse;
import com.kraken.api.interaction.bank.BankService;
import com.kraken.api.interaction.camera.CameraService;
import com.kraken.api.interaction.equipment.EquipmentService;
import com.kraken.api.interaction.gameobject.GameObjectService;
import com.kraken.api.interaction.groundobject.GroundObjectService;
import com.kraken.api.interaction.inventory.InventoryService;
import com.kraken.api.interaction.movement.MinimapService;
import com.kraken.api.interaction.movement.MovementService;
import com.kraken.api.interaction.movement.ShortestPathService;
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
import net.runelite.api.MenuEntry;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;

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

    // Reflection based hooks
    @Getter
    private boolean hooksLoaded = false;

    @Getter
    private boolean packetsLoaded = false;

    public static MenuEntry targetMenu;

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
            ShortestPathService.class,
            PlayerService.class,
            NpcService.class,
            PrayerService.class,
            SpellService.class,
            UIService.class,
            TabService.class,
            WidgetService.class
    );

    protected ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    public Future<?> scheduledFuture;
    private final Injector injector;
    private final EventBus eventBus;
    private final HooksLoader loader;
    private final PacketUtilsLoader packetUtilsLoader;

    @Inject
    public Context(final Client client, final ClientThread clientThread, final VirtualMouse mouse,
                   final EventBus eventBus, final Injector injector, final HooksLoader loader, final PacketUtilsLoader packetUtilsLoader) {
        this.client = client;
        this.clientThread = clientThread;
        this.mouse = mouse;
        this.injector = injector;
        this.eventBus = eventBus;
        this.loader = loader;
        this.packetUtilsLoader = packetUtilsLoader;
    }

    /**
     * Loads hooks containing key obfuscated RuneLite client methods for use in the API. This is called by the Script class
     * to ensure that the hooks are loaded and set before any scripts are run. This ensures that reflection based operations like
     * prayer flicking, movement, and other interactions with the RuneLite client can be performed correctly by the implementing script.
     */
    public void loadHooks() {
        if (hooksLoaded) {
            log.warn("Hooks already loaded, skipping.");
            return;
        }

        try {
            loader.loadHooks();
            if(loader.getHooks() == null) {
                log.error("Hooks failed to load, cannot proceed.");
                return;
            }
            hooksLoaded = true;
            loader.setHooks();
            log.info("Hooks loaded and set successfully.");
        } catch (Exception e) {
            log.error("Failed to load hooks with exception: {}", e.getMessage());
        }
    }

    /**
     * Loads the PacketUtils class and plugin which contains key methods for sending packets to the RuneLite client.
     */
    public void loadPacketUtils() {
        if (packetsLoaded) {
            log.warn("Packets already loaded, skipping.");
            return;
        }

        try {
            packetUtilsLoader.loadPacketUtils();
            packetsLoaded = true;
            log.info("Packet utils loaded successfully.");
        } catch (Exception e) {
            log.error("Failed to load packet utils with exception: {}", e.getMessage());
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
     * Run a method on the client thread, returning the result.
     * @param method The method to call
     */
    @SneakyThrows
    public void runOnSeperateThread(Callable<?> method) {
        if (scheduledFuture != null && !scheduledFuture.isDone()) return;
        scheduledFuture = scheduledExecutorService.submit(method);
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
}
