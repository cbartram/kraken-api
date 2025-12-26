# Scripting

This doc covers some basic principles and examples to start writing
scripts with the Kraken API.

### Script Structure

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
