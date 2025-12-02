package example.tests.query;

import com.kraken.api.Context;
import com.kraken.api.query.groundobject.GroundObjectEntity;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class GroundObjectTest extends BaseApiTest {

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        boolean testsPassed = true;

        // Setup: We need an item to test with. Try to find the first droppable item in inventory.
        var itemToDrop = ctx.inventory().withAction("Drop").first();

        if (itemToDrop.isNull()) {
            log.error("Inventory is empty. Cannot run Ground Object test without an item to drop.");
            return false;
        }

        String itemName = itemToDrop.getName();
        int itemId = itemToDrop.getId();
        long initialCount = ctx.groundItems().withId(itemId).count();
        log.info("Testing with item: {}", itemName);

        try {
            // 2. Drop the item
            itemToDrop.interact("Drop");

            // Wait for item to appear (Server latency ~600-1200ms)
            boolean appeared = ctx.runOnClientThread(() ->
                    ctx.groundItems().withId(itemId).count() > initialCount
            );

            int retries = 0;
            while (!appeared && retries < 5) {
                Thread.sleep(600);
                appeared = ctx.groundItems().withId(itemId).count() > initialCount;
                retries++;
            }

            if (!appeared) {
                log.error("Item was dropped but did not appear in GroundObjectQuery");
                return false;
            }

            // Existence & Name
            // Note: withName matches loose string, so strict ID check is safer,
            // but we test name here for query coverage.
            if (ctx.groundItems().withName(itemName).first().isNull()) {
                log.error("Failed to find ground item by name: {}", itemName);
                testsPassed = false;
            }

            // 4. Test: Distance (within)
            // Since we dropped it, it should be within 1 tile (under us) or 2 tiles maximum.
            if (ctx.groundItems().withId(itemId).within(2).count() == 0) {
                log.error("Dropped item was not found within 2 tiles");
                testsPassed = false;
            }

            // 5. Test: Reachable
            // An item dropped at our feet MUST be reachable.
            if (ctx.groundItems().withId(itemId).reachable().count() == 0) {
                log.error("Dropped item was not marked as reachable");
                testsPassed = false;
            }

            // 6. Test: Nearest
            GroundObjectEntity nearestItem = ctx.groundItems().withId(itemId).nearest();
            if (nearestItem.isNull()) {
                log.error("nearest() returned null for the dropped item");
                testsPassed = false;
            } else {
                // Validate it's actually close
                int dist = nearestItem.raw().getLocation().distanceTo(ctx.players().local().raw().getWorldLocation());
                if (dist > 2) {
                    log.error("nearest() returned an item that is {} tiles away, expected < 2", dist);
                    testsPassed = false;
                }
            }

            // 7. Test: Value (Prices)
            // Even if the item is worth 0, the query shouldn't crash.
            // We check if valueAbove(-1) returns our item.
            if (ctx.groundItems().withId(itemId).valueAbove(-1).count() == 0) {
                log.error("valueAbove() query failed to find the item");
                testsPassed = false;
            }

            // 8. Test: At (Location)
            WorldPoint itemLoc = nearestItem.raw().getLocation();
            if (ctx.groundItems().at(itemLoc).withId(itemId).count() == 0) {
                log.error("at(WorldPoint) query failed");
                testsPassed = false;
            }

            // 9. Cleanup: Pick it back up
            if (testsPassed) {
                log.info("Taking ground item");
                nearestItem.interact("Take");
                Thread.sleep(1200); // Wait for pickup
            }

        } catch (Exception e) {
            log.error("Exception during Ground Object test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Ground Items";
    }
}