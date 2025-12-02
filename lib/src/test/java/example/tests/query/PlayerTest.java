package example.tests.query;

import com.kraken.api.Context;
import com.kraken.api.query.player.PlayerEntity;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class PlayerTest extends BaseApiTest {

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        boolean testsPassed = true;

        try {
            // 1. Local Player Retrieval
            // Verify we can grab the special LocalPlayerEntity
            var local = ctx.players().local();
            if (local == null) {
                log.error("Failed to retrieve 'local()' player entity");
                testsPassed = false;
            }

            // 2. Self Exclusion
            // The standard ctx.players() stream should NOT contain the local player
            List<PlayerEntity> allStreamedPlayers = ctx.players().stream().collect(Collectors.toList());
            boolean containsLocal = allStreamedPlayers.stream()
                    .anyMatch(p -> p.getName().equalsIgnoreCase(local.getName()));

            if (containsLocal) {
                log.error("ctx.players() stream incorrectly includes the local player");
                testsPassed = false;
            }

            // If no other players are around, we can't test filters effectively.
            if (allStreamedPlayers.isEmpty()) {
                log.warn("No other players found nearby. Skipping filter tests.");
                return testsPassed;
            }

            // 3. Combat Level Filter (.withinLevel)
            // Grab the first available player and try to find them again using their specific level
            PlayerEntity target = allStreamedPlayers.get(0);
            int lvl = target.raw().getCombatLevel();

            boolean foundByLevel = !ctx.players().withinLevel(lvl, lvl).first().isNull();
            if (!foundByLevel) {
                log.error("Failed to find player via withinLevel(" + lvl + ", " + lvl + ")");
                testsPassed = false;
            }

            // Test Greater Than
            if (lvl > 3) {
                boolean foundByGreater = !ctx.players().combatLevelGreaterThan(lvl - 1).first().isNull();
                if (!foundByGreater) {
                    log.error("Failed to find player via combatLevelGreaterThan(" + (lvl - 1) + ")");
                    testsPassed = false;
                }
            }

            // 4. Location Query (.at)
            WorldPoint targetLoc = target.raw().getWorldLocation();
            boolean foundByLoc = !ctx.players().at(targetLoc).first().isNull();
            if (!foundByLoc) {
                log.error("Failed to find player via .at(" + targetLoc + ")");
                testsPassed = false;
            }

            // 5. Area Query (.withinArea)
            // Create a small box around the target player
            WorldPoint min = new WorldPoint(targetLoc.getX() - 1, targetLoc.getY() - 1, targetLoc.getPlane());
            WorldPoint max = new WorldPoint(targetLoc.getX() + 1, targetLoc.getY() + 1, targetLoc.getPlane());

            boolean foundInArea = !ctx.players().withinArea(min, max).first().isNull();
            if (!foundInArea) {
                log.error("Failed to find player via .withinArea()");
                testsPassed = false;
            }

            // 6. Nearest
            // Your API definition for nearest() returns PlayerEntity directly, not PlayerQuery
            PlayerEntity nearestPlayer = ctx.players().nearest();
            if (nearestPlayer == null || nearestPlayer.isNull()) {
                log.error("nearest() returned null/empty despite players existing in stream");
                testsPassed = false;
            } else {
                // Follow the nearest player
                nearestPlayer.interact("follow");
            }

        } catch (Exception e) {
            log.error("Failed to run Player test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Player";
    }
}