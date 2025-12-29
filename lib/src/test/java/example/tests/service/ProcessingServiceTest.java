package example.tests.service;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.service.ui.processing.ProcessingService;
import com.kraken.api.service.util.SleepService;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

@Slf4j
@Singleton
public class ProcessingServiceTest extends BaseApiTest {

    private static final WorldPoint BARBARIAN_VILLAGE = new WorldPoint(3104, 3430, 0);
    private static final int BARBARIAN_VILLAGE_FIRE = 43475;
    private static final int COOK_WIDGET_ONE = 17694735;
    private static final int COOK_WIDGET_TWO = 17694736;

    @Inject
    private ProcessingService processingService;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        // Setup
        boolean hasFish = (ctx.inventory().hasItem(335) || ctx.inventory().hasItem(331));
        boolean nearFire = ctx.players().local().isInArea(BARBARIAN_VILLAGE, 10);

         if (!hasFish || !nearFire) {
             log.error("No salmon or trout in inventory or not near fire");
             return false;
         }

         boolean interacted = ctx.gameObjects().withId(BARBARIAN_VILLAGE_FIRE).first().interact("Cook");

         if(!interacted) {
           log.error("Failed to interact with fire");
           return false;
         }

         SleepService.sleep(2000, 2500);
         processingService.setAmount(2);
         if(!processingService.process(329)) { // Cooked salmon is what we want to make raw salmon 335 is what we have
             log.error("Failed to process item 329 -> 335");
             return false;
         }

         SleepService.sleep(2000, 2500);
         if(!processingService.process(333)) { // Cooked trout 333 is what we want, 331 raw is what we have
             log.error("Failed to process item 331 -> 333");
             return false;
         }

        return true;
    }

    @Override
    protected String getTestName() {
        return "Processing Service";
    }
}
