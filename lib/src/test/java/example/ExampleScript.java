package example;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.script.Script;
import com.kraken.api.core.script.breakhandler.BreakConditions;
import com.kraken.api.core.script.breakhandler.BreakManager;
import com.kraken.api.core.script.breakhandler.BreakProfile;
import com.kraken.api.service.bank.BankService;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.pathfinding.LocalPathfinder;
import com.kraken.api.service.util.RandomService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;

import java.util.Arrays;
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

    @Inject
    private BreakManager breakManager;

    @Inject
    private BankService bankService;

    @Override
    public void onStart() {
        // Create a custom break profile
        BreakProfile profile = BreakProfile.builder()
                .name("Jewelry Profile")
                .minRuntime(java.time.Duration.ofMinutes(2))
                .maxRuntime(java.time.Duration.ofMinutes(4))
                .minBreakDuration(java.time.Duration.ofMinutes(2))
                .maxBreakDuration(java.time.Duration.ofMinutes(5))
                .logoutDuringBreak(true)
                .randomizeTimings(true)
                .build();

        profile.getCustomConditions().add(
            BreakConditions.onLevelReached(ctx.getClient(), Skill.CRAFTING, 51)
        );

        profile.getCustomConditions().add(
            BreakConditions.onBankEmpty(bankService, ctx, 1603)
        );

        // Attach this script to the break handler
        breakManager.attachScript(this, profile);
    }

    @Override
    public int loop() {
        if (config.startPathfinding()) {
            String waypointLocation = config.waypointLocation();
            String[] coords = waypointLocation.split(",");
            if (coords.length == 3) {
                log.info("Starting pathfinding...");
                log.info("Coords: {}", Arrays.asList(coords));
                int x = Integer.parseInt(coords[0]);
                int y = Integer.parseInt(coords[1]);
                int z = Integer.parseInt(coords[2]);
                WorldPoint target = new WorldPoint(x, y, z);

                List<WorldPoint> path = pathfinder.findPath(ctx.getClient().getLocalPlayer().getWorldLocation(), target);
                plugin.getCurrentPath().clear();
                plugin.getCurrentPath().addAll(path);
                // TODO Clicks every single tick, instead we need a better way to determine waypoints.
                if (!path.isEmpty()) {
                    try {
                        movementService.traversePath(ctx.getClient(), movementService, path);
                    } catch (InterruptedException e) {
                        log.error("Failed to traverse path", e);
                    }
                }
            }
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
