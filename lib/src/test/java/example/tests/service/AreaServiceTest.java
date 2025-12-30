package example.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.tile.AreaService;
import com.kraken.api.service.tile.GameArea;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class AreaServiceTest extends BaseApiTest {

    @Inject
    private AreaService areaService;

    // Public fields so SceneOverlay can access them for rendering
    public GameArea bankReachabilityArea;
    public GameArea complexBuildingArea;
    public GameArea radiusArea;

    private final WorldArea BANK_BOUNDS = new WorldArea(3250, 3416, 8, 8, 0);

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        if (!testReachabilityLogic()) return false;
        if (!testComplexPolygonGeneration()) return false;
        return radiusAreaGeneration();
    }

    private boolean radiusAreaGeneration() {
        this.radiusArea = areaService.createAreaFromRadius(new WorldPoint(3238, 3434, 0), 3);
        return radiusArea != null && !radiusArea.getTiles().isEmpty();
    }

    private boolean testReachabilityLogic() {
        int centerX = BANK_BOUNDS.getX() + (BANK_BOUNDS.getWidth() / 2); // 3254
        int centerY = BANK_BOUNDS.getY() + (BANK_BOUNDS.getHeight() / 2); // 3420
        WorldPoint center = new WorldPoint(centerX, centerY, 0);

        this.bankReachabilityArea = areaService.createReachableArea(center, 6, false);

        if (bankReachabilityArea.getTiles().isEmpty()) {
            log.error("Failed: Reachable area is empty.");
            return false;
        }

        if (!bankReachabilityArea.contains(center)) {
            log.error("Failed: Center point {} is not inside the reachable area.", center);
            return false;
        }
        return true;
    }

    private boolean testComplexPolygonGeneration() {
        List<WorldPoint> path = new ArrayList<>();
        path.add(new WorldPoint(3249, 3438, 0));
        path.add(new WorldPoint(3249, 3432, 0));
        path.add(new WorldPoint(3250, 3431, 0));
        path.add(new WorldPoint(3257, 3431, 0));
        path.add(new WorldPoint(3259, 3435, 0));
        path.add(new WorldPoint(3261, 3435, 0));
        path.add(new WorldPoint(3261, 3438, 0));
        path.add(new WorldPoint(3249, 3439, 0));

        WorldPoint[] vertices = path.toArray(new WorldPoint[0]);

        this.complexBuildingArea = areaService.createPolygonArea(vertices);

        if (complexBuildingArea.getTiles().isEmpty()) {
            log.error("Failed: Polygon area generated 0 tiles.");
            return false;
        }

        WorldPoint internalPoint = new WorldPoint(3252, 3435, 0);
        if (!complexBuildingArea.contains(internalPoint)) {
            log.error("Failed: Polygon did not contain internal point {}", internalPoint);
            return false;
        }

        return true;
    }

    @Override
    protected String getTestName() {
        return "Area Service Visual";
    }
}