package example.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.movement.MovementService;
import example.ExamplePlugin;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class MovementServiceTest extends BaseApiTest {

    @Inject
    private Client client;

    @Inject
    private MovementService movementService;

    @Inject
    private ExamplePlugin examplePlugin;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        log.info("Test started. Please Shift + Right Click 'Walk here' -> 'Set' on a tile to begin movement.");

        WorldPoint target = waitForTargetSelection();
        if (target == null) {
            log.error("Test timed out waiting for target selection.");
            return false;
        }

        log.info("Target selected: {}. Attempting to move...", target);
        movementService.moveTo(target);
        return waitForArrival(target);
    }

    private WorldPoint waitForTargetSelection() throws InterruptedException {
        int timeout = 0;
        // Wait up to 30 seconds for user input
        while (examplePlugin.getTargetTile() == null && timeout < 300) {
            Thread.sleep(100);
            timeout++;
        }
        return examplePlugin.getTargetTile();
    }

    private boolean waitForArrival(WorldPoint target) throws InterruptedException {
        int timeoutTicks = 0;
        int maxTicks = 25; // Fail if not arrived in ~30 seconds (adjust based on distance)

        // Loop while the test is running
        while (timeoutTicks < maxTicks) {
            WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();

            // Check distance to target
            // We use distanceTo (Chebyshev) to ensure we are exactly on the tile (distance 0)
            if (playerLoc.distanceTo(target) == 0) {
                log.info("Success! Player reached destination: {}", playerLoc);
                return true;
            }

            Thread.sleep(600);
            timeoutTicks++;
        }

        log.error("Test failed: Player did not reach destination within timeout.");
        return false;
    }

    @Override
    protected String getTestName() {
        return "Movement Service";
    }
}
