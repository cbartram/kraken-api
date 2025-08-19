package com.kraken.api;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.kraken.api.core.SleepService;
import com.kraken.api.input.VirtualMouse;
import com.kraken.api.interaction.bank.BankService;
import com.kraken.api.interaction.camera.CameraService;
import com.kraken.api.interaction.equipment.GearService;
import com.kraken.api.interaction.gameobject.GameObjectService;
import com.kraken.api.interaction.inventory.InventoryService;
import com.kraken.api.interaction.movement.MovementService;
import com.kraken.api.interaction.npc.NpcService;
import com.kraken.api.interaction.prayer.PrayerService;
import com.kraken.api.interaction.spells.SpellService;
import com.kraken.api.interaction.ui.TabService;
import com.kraken.api.interaction.ui.UIService;
import com.kraken.api.interaction.widget.WidgetService;
import com.kraken.api.loader.HooksLoader;
import com.kraken.api.model.NewMenuEntry;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Point;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.EventBus;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.PluginManager;
import shortestpath.ShortestPathConfig;
import shortestpath.ShortestPathPlugin;

import java.awt.*;
import java.lang.reflect.Method;
import java.util.Collections;
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

    private final Injector injector;
    private final EventBus eventBus;
    private final HooksLoader loader;

    @Getter
    @Setter
    private VirtualMouse mouse;

    @Getter
    private boolean isRegistered = false;

    @Getter
    private boolean hooksLoaded = false;

    public static MenuEntry targetMenu;

    private final Set<Class<?>> EVENTBUS_LISTENERS = Set.of(
            GearService.class,
            BankService.class,
            CameraService.class,
            GameObjectService.class,
            InventoryService.class,
            MovementService.class,
            NpcService.class,
            PrayerService.class,
            SpellService.class,
            UIService.class,
            TabService.class,
            WidgetService.class
    );

    protected ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
    public Future<?> scheduledFuture;

    @Inject
    public Context(final Client client, final ClientThread clientThread, final ConfigManager configManager, final VirtualMouse mouse, final EventBus eventBus, final Injector injector, final HooksLoader loader) {
        this.client = client;
        this.clientThread = clientThread;
        this.mouse = mouse;
        this.injector = injector;
        this.eventBus = eventBus;
        this.loader = loader;

        injector.createChildInjector(binder -> binder.bind(ShortestPathConfig.class)
                .toInstance(configManager.getConfig(ShortestPathConfig.class)));
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
     * Registers all Service classes that have methods annotated with @Subscribe to the EventBus.
     * This allows the API to listen for key RuneLite events and respond accordingly.
     */
    public void register() {
        try {
            for (Class<?> clazz : EVENTBUS_LISTENERS) {
                // Check if class has @Subscribe methods
                for (Method method : clazz.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(Subscribe.class)) {
                        Object handler = injector.getInstance(clazz);
                        if (handler != null) {
                            eventBus.register(handler);
                            log.info("Registered class: {} with eventbus", clazz.getSimpleName());
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
     * @param method
     */
    @SneakyThrows
    public void runOnSeperateThread(Callable<?> method) {
        if (scheduledFuture != null && !scheduledFuture.isDone()) return;
        scheduledFuture = scheduledExecutorService.submit(method);
    }


    public int getVarbitValue(int varbit) {
        return runOnClientThread(() -> client.getVarbitValue(varbit));
    }

    public void doInvoke(NewMenuEntry entry) {
        doInvoke(entry, null);
    }

    public void doInvoke(NewMenuEntry entry, Rectangle rectangle) {
        doInvoke(entry, rectangle, true);
    }

    public void doInvoke(NewMenuEntry entry, Rectangle rectangle, boolean randomPoint) {
        Point pt;
        if(rectangle == null) {
            pt = new Point(1, 1);
        } else {
            pt = UIService.getClickingPoint(rectangle, randomPoint);
        }

        try {
            Context.targetMenu = entry;
            mouse.click(pt);

            // This almost always invokes this while NOT on the client thread. So the sleep will occur
            if (!client.isClientThread()) {
                SleepService.sleep(10L, 30L);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            log.error("Index out of bounds exception for KrakenClient: {}", e.getMessage());
        }
    }

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
     * Run a method on the client thread, returning an optional of the result.
     * @param method
     * @return
     * @param <T>
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
            log.error("Failed to run method on client thread: {}, message: {}", e.getCause(), e.getMessage());
            return Optional.empty();
        }
    }
}
