package example;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.script.Script;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import com.kraken.api.service.util.RandomService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.Collections;
import java.util.List;

@Slf4j
@Singleton
public class ExampleScript extends Script {

    @Inject
    private Context ctx;

    @Inject
    private ExampleConfig config;

    @Inject
    private LocalPathfinder pathfinder;

    @Inject
    private MovementService movementService;

    @Inject
    private ExamplePlugin plugin;

    private boolean isTraversing = false;

    @Override
    public int loop() {
        if (config.startPathfinding() && !isTraversing) {
            String waypointLocation = config.waypointLocation();
            String[] coords = waypointLocation.split(",");
            if (coords.length == 3) {
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int z = Integer.parseInt(coords[2]);
                WorldPoint target = new WorldPoint(x, y, z);
                List<WorldPoint> path = pathfinder.findPathWithBackoff(ctx.players().local().raw().getWorldLocation(), target);

                if(path == null) {
                   log.info("Null path");
                   path = Collections.emptyList();
                }

                log.info("Computed path of size: {}", path.size());
                plugin.getScriptPath().clear();
                plugin.getScriptPath().addAll(path);
                isTraversing = true;
            }

//                log.info("Attempting to find path...");
//                List<WorldPoint> path = pathfinder.findPath(ctx.getClient().getLocalPlayer().getWorldLocation(), target);
//                plugin.getCurrentPath().clear();
//                plugin.getCurrentPath().addAll(path);
//                if (!path.isEmpty()) {
//                    movementService.traversePath(ctx.getClient(), path, log::info, (dest) -> {
//                        log.info("{} setting is traversing to false.", dest);
//                        isTraversing = false;
//                    });
//
//                    isTraversing = true;
//                } else {
//                    log.info("No path found.");
//                }
        }

        log.debug("Looping on tick: {}", ctx.getClient().getTickCount());

        if(ctx.getClient().getTickCount() % 100 == 0) {
            int sleepTicks = RandomService.between(5, 10);
            log.debug("Sleeping for: {}", sleepTicks);
            SleepService.sleepFor(sleepTicks);
            return 100;
        }


        if(ctx.getClient().getTickCount() % 50 == 0) {
            log.debug("Sleeping for 3 game ticks (with return) starting on: {}", ctx.getClient().getTickCount());
            return 1800;
        }

        return 0;
    }
}
