# Automation Scripts

The `Script` class is the foundational component for any automated process within the Kraken API.
It acts as the entry point and primary orchestrator for your automation script's logic,
providing a structured way to manage its lifecycle and interact with the game world.

## Core Concepts

At its heart, a `Script` is designed to be a long-running process that executes a series of actions and works based off of
RuneScape's natural tick (0.6s) system. It provides methods for initialization, execution, and termination, allowing you to define the behavior of your automation script
from start to finish.

### Lifecycle Methods

Every `Script` implementation will typically override the following methods:

-   **`onStart()`**: This method is called once when the script is first started. It's the ideal place to perform any initial setup, such as:
    -   Loading configuration settings.
    -   Initializing variables.
    -   Setting up event listeners.
    -   Displaying initial GUI elements.

-   **`start()`**: This method should be called once when the plugin is first started. This calls `onStart()` under the hood.

-   **`stop()`**: This method should be called once when the plugin is stopped. This calls `onStop()` under the hood.

-   **`pause()`**: This method pauses the execution of the script's main loop. This will not pause methods that have been subscribed to within the RuneLite plugin.

-   **`resume()`**: This method resumes the execution of the script's main loop.

-   **`loop()`**: This is the core execution method of the script. It's called repeatedly in a continuous loop every game tick as long as the script is running. Within `loop()`, you'll typically:
    -   Check the current state of the game.
    -   Determine the next action to take.
    -   Execute game interactions (e.g., clicking, walking, using items).
    -   Manage the flow of your script's logic.

-   **`onStop()`**: This method is called once when the script is stopped, either manually by the user or programmatically. Use `onStop()` to:
    -   Clean up resources (e.g., closing files, releasing network connections).
    -   Save any persistent data.
    -   Remove event listeners.
    -   Display final messages or statistics.

## Script Loop

The script's methodology warrants particular notice. The loop method operates asynchronously with each game tick,
making it safe for `SleepService` and `Thread.sleep()` calls. Due to its asynchronous execution, calls will not run on the client thread unless the `Context` object is utilized.

For example:

```java
@Singleton
public class MyScript extends Script {
    
    @Inject
    private Context ctx;
    
    @Inject
    private SleepService sleepService;
    
    @Override
    public int loop() {
        String playerName = ctx.runOnClientThread(() -> ctx.getClient().getLocalPlayer().getName());
        
        if(playerName.equalsIgnoreCase("foo")) {
            sleepService.sleep(1000);
            return 0;
        }
        
        return 600;
    }
}
```

The entire query and service API's are designed to be thread-safe, so any queries, filters, method calls, or interactions can be run on non-client threads. When
callable methods need to execute on RuneLite's client thread, they will be scheduled there, blocking until the method executes.
This helps ensure your plugin code is fully thread-safe, predictable, and easy to read.

The loop method returns an integer which can be used to sleep the script for the specified number of milliseconds. Since the loop method is only called
once every game tick (0.6 seconds) any value less than 600 will execute on the next game tick regardless. You can use the return value of the loop
to sleep various durations depending on your scripts actions and requirements.


## Using a `Script` inside a `Plugin`

Kraken Scripts are designed to be used directly inside RuneLite plugins. The following example shows how to setup
a script within the familiar RuneLite plugin API:

```java
@PluginDescriptor(name = "Example Plugin")
public class FishingPlugin extends Plugin {

    @Inject
    private MyScript script;

    @Inject
    private Context ctx;
    
    @Inject
    private MyPluginConfig config;

    @Provides
    MyPluginConfig provideConfig(final ConfigManager configManager) {
        return configManager.getConfig(MyPluginConfig.class);
    }

    @Override
    protected void startUp() {
        ctx.initializePackets();
        script.start(); // Calls Script.onStart()
    }

    @Override
    protected void shutDown() {
        script.stop(); // Calls Script.onStop()
    }
    
    // TODO Add a panel for your plugin with stop, start, pause, resume buttons to your Scripts UI.
    // control these UI elements with script.resume(), script.pause(), script.stop(), and script.start(), the skies the limit!

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

    public String getStatus() {
        return script.getStatus();
    }
}
```

## Extending `Script` with the Loop and Task System

While you can implement all your logic directly within `loop()`, for more complex scripts, this can quickly lead to
unmanageable and difficult-to-debug code. Kraken provides a powerful **Loop and Task system** to help you structure your script's logic in a modular and maintainable way.

The Loop and Task system encourages breaking down your script's overall goal into smaller, discrete units of work called `Task`s.
These tasks are then managed and executed by the `Loop` within your `Script`.

### The `Task` Interface

The `Task` interface defines the contract for any individual unit of work your script needs to perform. It typically includes methods like:

-   **`validate()`**: This method determines if the task is currently applicable or ready to be executed. For example, a "chop tree" task might validate if there's a tree nearby and if the player has an axe.
-   **`execute()`**: This method contains the actual logic for performing the task. Following the "chop tree" example, this would involve clicking the tree and waiting for the chopping animation.

### The `AbstractTask` Class

The Kraken API provides an `AbstractTask` class that offers a convenient base implementation with the game `Context` pre-injected for your tasks.

### Integrating Tasks into Your `Script`

To use the Loop and Task system, your `Script` will typically maintain a list of `Task` objects. In your `loop()` method, you'll iterate through these tasks, validate them, and execute the first valid task.

Here's a conceptual example of how you might structure a `Script` using `Task`s:

```java
import com.kraken.script.Script;
import com.kraken.task.Task;
import com.kraken.task.AbstractTask;

import java.util.ArrayList;
import java.util.List;

public class MyBotScript extends Script {

    private List<Task> tasks = new ArrayList<>();

    @Override
    public void onStart() {
        log.info("MyBotScript started!");
        // Initialize and add your tasks or inject in the constructor with Guice!
        tasks.add(new WalkToBankTask());
        tasks.add(new DepositItemsTask());
        tasks.add(new WithdrawItemsTask());
        tasks.add(new ChopTreeTask());
        // ... add more tasks in order of priority or logical flow
    }

    @Override
    public void loop() {
        for (Task task : tasks) {
            if (task.validate()) {
                log.debug("Executing task: " + task.getClass().getSimpleName());
                task.execute();
                // After executing a task, we typically break to re-evaluate tasks in the next loop iteration
                // This ensures that higher-priority tasks are always checked first.
                break;
            }
        }
    }

    @Override
    public void onStop() {
        log.info("MyBotScript stopped!");
        // Perform any cleanup
    }
}
```

## Break Management

Having your scripts take regular breaks can help reduce detection, add humanization, and ultimately reduce bans. The Kraken API
exposes a high level `BreakManager` class to help you faciliate breaking within your scripts.

Breaks can be configured using pre-defined or custom `BreakProfiles`. A `BreakProfile` defines exactly how your breaks are
executed. For example you can configure,

- The min and max runtime of your script
- The min and max time of your break
- Whether to logout or just go idle during your breaks
- Custom break conditions like:
  - Reaching a certain level
  - Reaching a certain exp threshold
  - Bank being depleted of a specific material
  - Any custom condition you can think of

Here is an example of building a custom profile for breaking within a crafting plugin:

```java
BreakProfile profile = BreakProfile.builder()
        .name("Crafting Profile")
        .minRuntime(java.time.Duration.ofMinutes(20))
        .maxRuntime(java.time.Duration.ofMinutes(40))
        .minBreakDuration(java.time.Duration.ofMinutes(5))
        .maxBreakDuration(java.time.Duration.ofMinutes(19))
        .logoutDuringBreak(true)
        .randomizeTimings(true)
        .addBreakCondition(BreakConditions.onLevelReached(context.getClient(), Skill.CRAFTING, 54))
        .addBreakCondition(BreakConditions.customCondition(() -> ctx.groundItems().within(20).valueAbove(100000).first() != null, "I saw an item worth lots of GP"))
        .build();
```

It is recommended to initialize and configure the break manager in your `Plugin` class within the `startUp()` method like so:

```java

class MyPlugin extends Plugin {
    
    @Inject
    private BreakManager breakManager;
    
    @Inject
    private MyCustomScript exampleScript;

    @Override
    public void startUp() {
        breakManager.initialize();
        BreakProfile profile = BreakProfile.builder()
                .name("Crafting Profile")
                .minRuntime(java.time.Duration.ofMinutes(20))
                .maxRuntime(java.time.Duration.ofMinutes(40))
                .minBreakDuration(java.time.Duration.ofMinutes(5))
                .maxBreakDuration(java.time.Duration.ofMinutes(19))
                .logoutDuringBreak(true)
                .randomizeTimings(true)
                .addBreakCondition(BreakConditions.onLevelReached(context.getClient(), Skill.CRAFTING, 54))
                .build();

        breakManager.attachScript(exampleScript, profile);
    }
    
    
    @Override
    public void shutDown() {
        breakManager.detachScript(exampleScript);
    }
}
```

## Continued Reading

As you develop your skills as a scripter, you will likely run into issues where tasks need to be executed in some priority order where
certain tasks take precedence over other ones. The Kraken API ships with a `PriorityTask` abstraction to help you implement your task
execution logic using a data structure like an ordered list or a priority queue.

By using the Loop and Task system, you achieve:

-   **Modularity**: Each task is a self-contained unit of logic.
-   **Readability**: It's easier to understand what your script is doing by looking at its individual tasks.
-   **Maintainability**: Changes to one task are less likely to affect others.
-   **Testability**: Individual tasks can be tested in isolation.
-   **Flexibility**: You can reorder, add, or remove tasks to change your script's behavior.

If you are interested, you should check out the [Kraken Example Plugin,](https://github.com/cbartram/kraken-example-plugin) which provides a complete, best-practice
example of using the API within a RuneLite plugin to create a fully automated script.