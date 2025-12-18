package example.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.movement.MovementService;
import com.kraken.api.service.movement.Pathfinder;
import com.kraken.api.service.util.RandomService;
import com.kraken.api.service.util.SleepService;
import example.ExamplePlugin;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.List;

@Slf4j
@Singleton
public class PathfinderServiceTest extends BaseApiTest {

    private static final WorldPoint PLAYER_START = new WorldPoint(3253, 3421, 0);
    private static final WorldPoint VARROCK_SQUARE = new WorldPoint(3208, 3422, 0);
    private static final WorldPoint BLUE_MOON_INN_INVALID = new WorldPoint(3220, 3404, 0); // An unreachable tile should find closest point.
    private static final WorldPoint OUT_OF_SCENE_LUMBRIDGE = new WorldPoint(3253, 3251, 0); // A tile far outside the currently loaded region

    @Inject
    private Pathfinder pathfinder;

    @Inject
    private ExamplePlugin plugin;

    @Inject
    private MovementService movementService;

    @Inject
    private SleepService sleepService;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        log.info("Moving player to start position");
        movementService.moveTo(PLAYER_START);
        sleepService.sleepUntil(() -> ctx.players().local().isIdle() && ctx.players().local().raw().getWorldLocation() == PLAYER_START, 10000);

        // Assert we can find a path within the scene, this assumes we are standing in or near Varrock west
        List<WorldPoint> path = pathfinder.findPath(PLAYER_START, VARROCK_SQUARE);
        if(path.isEmpty() || path.size() < 40) {
            log.info("Varrock Sq path size: {}", path.size());
            return false;
        }

        plugin.getCurrentPath().clear();
        plugin.getCurrentPath().addAll(path);
        Thread.sleep(RandomService.between(3000, 5000));

        List<WorldPoint> blueMoonPath = pathfinder.findPath(PLAYER_START, BLUE_MOON_INN_INVALID);
        if(blueMoonPath.isEmpty() || blueMoonPath.size() < 40) {
            log.info("Blue Moon path size: {}", blueMoonPath.size());
            return false;
        }

        plugin.getCurrentPath().clear();
        plugin.getCurrentPath().addAll(blueMoonPath);
        Thread.sleep(RandomService.between(3000, 5000));

        List<WorldPoint> nearestOutOfScene = pathfinder.findPath(PLAYER_START, OUT_OF_SCENE_LUMBRIDGE);
        if(nearestOutOfScene.isEmpty() || nearestOutOfScene.size() < 20) {
            log.info("Nearest out of scene path size: {}", nearestOutOfScene.size());
            return false;
        }

        plugin.getCurrentPath().clear();
        plugin.getCurrentPath().addAll(nearestOutOfScene);
        Thread.sleep(RandomService.between(3000, 5000));
        return true;
    }

    @Override
    protected String getTestName() {
        return "Pathfinder";
    }
}
