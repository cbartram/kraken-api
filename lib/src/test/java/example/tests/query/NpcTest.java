package example.tests.query;

import com.kraken.api.Context;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.stream.Collectors;

@Slf4j
public class NpcTest extends BaseApiTest {

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        boolean testsPassed = true;

        try {
            // 2. Attackable Filter: Verify logic on Guards
            // Guards should be attackable
            boolean guardsFound = !ctx.npcs().withName("Guard").first().isNull();
            if (guardsFound) {
                boolean guardIsAttackable = !ctx.npcs().withName("Guard").attackable().first().isNull();
                if (!guardIsAttackable) {
                    log.error("Found 'Guard' but attackable() filter excluded them.");
                    testsPassed = false;
                }
            } else {
                log.warn("Skipping Guard attackable test (No Guards nearby)");
            }

            boolean aliveCheck = !ctx.npcs().alive().first().isNull();
            if (!aliveCheck) {
                log.error("Failed to find any 'alive' NPCs");
                testsPassed = false;
            }

            // Nearest Sorting: Verify distance order
            var nearestNpcs = ctx.npcs().nearest().stream().limit(2).collect(Collectors.toList());
            if (nearestNpcs.size() >= 2) {
                int dist1 = nearestNpcs.get(0).raw().getWorldLocation().distanceTo(ctx.players().local().raw().getWorldLocation());
                int dist2 = nearestNpcs.get(1).raw().getWorldLocation().distanceTo(ctx.players().local().raw().getWorldLocation());

                if (dist1 > dist2) {
                    log.error("nearest() sort failed: 1st NPC dist (" + dist1 + ") > 2nd NPC dist (" + dist2 + ")");
                    testsPassed = false;
                }
            }

            // Area Query (.withinArea)
            // Define a box around the player and ensure we find NPCs inside it
            WorldPoint playerLoc = ctx.players().local().raw().getWorldLocation();
            WorldPoint min = new WorldPoint(playerLoc.getX() - 15, playerLoc.getY() - 15, playerLoc.getPlane());
            WorldPoint max = new WorldPoint(playerLoc.getX() + 15, playerLoc.getY() + 15, playerLoc.getPlane());

            boolean npcsInArea = !ctx.npcs().withinArea(min, max).first().isNull();
            if (!npcsInArea) {
                log.error("withinArea() returned no NPCs despite loose bounds (15 tiles)");
                testsPassed = false;
            }

            // Reachability
            // Ensure at least some NPCs are reachable (e.g., other players' pets, guards, or men)
            // Note: Bankers behind booths might return false for reachability depending on exact tile logic
            boolean anyReachable = !ctx.npcs().reachable().first().isNull();
            if (!anyReachable) {
                log.error("No NPCs marked as reachable found");
                testsPassed = false;
            }

            // Interaction Chain (Optional Smoke Test)
            // Only run if we found a guard, try to hover or check interaction logic (without clicking)
            if (guardsFound) {
                var guard = ctx.npcs().withName("Guard").nearest().first();
                if (guard.isNull()) {
                    log.error("Guard found previously but failed to retrieve in Interaction test");
                    testsPassed = false;
                }
                guard.attack();
            }

        } catch (Exception e) {
            log.error("Failed to run NPC test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "NPC";
    }
}