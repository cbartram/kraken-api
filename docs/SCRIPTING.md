# Scripting

This doc covers some basic principles and examples to start writing
scripts with the Kraken API.

## Script Structure

The main structure you can use for actually writing scripts with the Kraken API can be found in the `Script` class. The
`Script` class is designed to be directly extended by your plugin and it itself extends the RuneLite `Plugin` class.

The `Script` is based around the RuneScape game tick, which executes every 0.6 seconds. It provides a `loop()` method which
executes every tick and is safe to sleep in (as it executes on another thread). For example:

```java
import com.kraken.api.core.script.Script;

@Slf4j
@Singleton
@PluginDescriptor(
        name = "Mining Plugin",
        description = "Demonstrates an example of building a Mining automation plugin using the Kraken API."
)
public class MiningPlugin extends Script {
    @Override
    public void loop() {
        // ...
    }
}
```

As such, any code running within the `loop()` will not execute on the client thread. Most API methods within services and query packages
will automatically schedule and execute on the client thread if necessary. If you need to run additional code on the client thread from within `loop()` use: 

```java
@PluginDescriptor(
        name = "Example",
        description = "Example"
)
public class Example extends Script {
    @Inject
    private Context ctx;

    @Override
    public void loop() {
        ctx.runOnClientThread(() -> System.out.println("I'm on the client thread!"));
    }
}
```

You can use the `com.kraken.api.service.util.SleepService` to have access to various functions to sleep within the game `loop`.

## Task System

The Kraken API ships with some pre-built constructs to help facilitate Task-based scripts. A task is a repeatable in game action encapsulated
in its own class. For example, building a simple woodcutting script may involve the following tasks:

- Find & Interact with nearby trees
- Walk to the bank
- Open & deposit logs
- Walk back to tree chopping area
- etc...

Tasks should all extend the `AbstractTask` class which defines several key methods for a script to choose which tasks to execute:

```java
public interface Task {
    /**
     * Checks if this task should currently be executed.
     * @return true if the task is valid, false otherwise.
     */
    boolean validate();

    /**
     * Executes the task logic.
     * @return The number of milliseconds to sleep after execution.
     */
    int execute();

    /**
     * Returns the name of the status for display.
     * @return Status string.
     */
    String status();
}
```

An example tasks for a woodcutting script could look something like this: 

```java
public class ChopLogsTask extends AbstractTask {

    @Inject
    private WoodcuttingPlugin plugin;

    @Inject
    private WoodcuttingConfig config;

    @Inject
    private BankService bankService;

    @Override
    public boolean validate() {
        return ctx.players().local().isIdle()
                && !ctx.inventory().isFull() && !bankService.isOpen();
    }

    @Override
    public int execute() {
        GameObjectEntity tree = ctx.gameObjects()
                .within(config.treeRadius())
                .withName(config.treeName()).random();

        if(tree != null) {
            plugin.setTargetTree(tree.raw());
            ctx.getMouse().move(tree.raw());
            tree.interact("Chop down");
            SleepService.sleepUntil(() -> ctx.players().local().raw().getAnimation() != -1, RandomService.between(5000, 6000));
        }

        return 1200;
    }

    @Override
    public String status() {
        return "Chopping " + config.treeName();
    }
}
```

## Chaining Tasks Together

In order to build a full script, you will need to chain several `Tasks` together within the `loop()` method of your script.
The [Kraken Example Plugins](https://github.com/cbartram/kraken-example-plugin) showcase several examples of this however,
here is a barebones example as well:

```java

@Slf4j
public class WoodcuttingScript extends Script {

    private final List<Task> tasks;

    @Getter
    private String status = "Initializing";

    @Inject
    public WoodcuttingScript(BankTask bankTask, ChopLogsTask chopLogsTask, DepositLogsTask depositLogsTask, WalkToTrees walkToTrees) {
        this.tasks = List.of(
                chopLogsTask,
                bankTask,
                walkToTrees,
                depositLogsTask
        );
    }

    @Override
    public int loop() {
        for (Task task : tasks) {
            if (task.validate()) {
                status = task.status();
                return task.execute();
            }
        }
        return 0;
    }
}
```

Tasks can be a powerful way to combine logic into fully automated scripts for breaking down complex in game actions.

## Continued Reading

As you develop your skills as a scripter you will likely run into issues where tasks need to be executed in some priority order where
certain tasks take precedence over other ones. The Kraken API ships with a `PriorityTask` abstraction to help you implement your task
execution logic using a data structure like an ordered list or a priority queue. 