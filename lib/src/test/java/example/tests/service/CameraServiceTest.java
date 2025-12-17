package example.tests.service;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.service.RandomService;
import com.kraken.api.service.camera.CameraService;
import example.ExamplePlugin;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class CameraServiceTest extends BaseApiTest {

    @Inject
    private Client client;

    @Inject
    private CameraService camera;

    @Inject
    private ExamplePlugin examplePlugin;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        log.info("Starting Camera Service Test.");

        WorldPoint target = waitForTargetSelection();
        if (target == null) {
            log.error("Test timed out waiting for target selection.");
            return false;
        }

        LocalPoint targetLp = LocalPoint.fromWorld(client, target);

        if(targetLp == null) {
            log.info("Target tile could not be converted to local point");
            return false;
        }

        log.info("Target selected: {}. Running camera service tests...", target);

        // 2. Test Pitch
        log.info("--- Testing Pitch ---");
        int originalPitch = camera.getPitch();

        camera.setPitch(383); // Max up
        Thread.sleep(RandomService.between(2000, 3500));
        if (camera.getPitch() < 370) return fail("Failed to set Pitch MAX (Up)");

        camera.setPitch(128); // Max down
        Thread.sleep(RandomService.between(2000, 3500));
        if (camera.getPitch() > 140) return fail("Failed to set Pitch MIN (Down)");

        // Reset Pitch
        camera.setPitch(originalPitch);

        // 3. Test Zoom
        log.info("--- Testing Zoom ---");
        int originalZoom = camera.getZoom();

        camera.setZoom(800); // Zoom way out
        Thread.sleep(RandomService.between(2000, 3500));

        if (camera.getZoom() < 400) return fail("Failed to Zoom OUT");

        camera.setZoom(100);
        Thread.sleep(RandomService.between(2000, 3500));
        if (camera.getZoom() > 200) return fail("Failed to Zoom IN");

        // Reset Zoom
        camera.setZoom(originalZoom);

        // 4. Test Rotation (Turn To)
        log.info("--- Testing Turn To ---");
        // Randomly offset camera first so we know we actually moved
        int startAngle = camera.angleToTile(target) + 100;
        camera.setAngle(startAngle, 10);
        camera.turnTo(targetLp);


        // Calculate expected angle
//        int expectedAngle = camera.angleToTile(targetLp);
        // Check if we are within the default 80 degree tolerance of turnTo
//        log.info("Expected angle: {}", expectedAngle);
//        if (!camera.isAngleGood(expectedAngle, 80)) {
//            return fail("Failed to Turn To target. Angle difference too high.");
//        }

        log.info("--- Testing Centering ---");
        camera.setPitch(128);
        camera.setAngle(camera.getAngle() + 90, 10);
        Thread.sleep(RandomService.between(2000, 3500));

        camera.centerTileOnScreen(targetLp);

        if (!camera.isTileCenteredOnScreen(targetLp)) {
            camera.centerTileOnScreen(targetLp);
            if (!camera.isTileCenteredOnScreen(targetLp)) {
                return fail("Failed to center tile on screen.");
            }
        }

        log.info("Camera Service Test Passed Successfully.");
        return true;
    }

    private WorldPoint waitForTargetSelection() throws InterruptedException {
        int timeout = 0;
        while (examplePlugin.getTargetTile() == null && timeout < 300) {
            Thread.sleep(100);
            timeout++;
        }
        return examplePlugin.getTargetTile();
    }

    private boolean fail(String reason) {
        log.error("TEST FAILED: {}", reason);
        return false;
    }

    @Override
    protected String getTestName() {
        return "Camera Service";
    }
}
