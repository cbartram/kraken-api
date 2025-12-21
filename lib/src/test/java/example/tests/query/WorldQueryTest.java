package example.tests.query;

import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.query.world.WorldEntity;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.WorldType;

import java.util.List;

@Slf4j
@Singleton
public class WorldQueryTest extends BaseApiTest {
    @Override
    protected boolean runTest(Context ctx) throws Exception {

        // 1. Test fetching all worlds
        List<WorldEntity> allWorlds = ctx.worlds().list();
        if (allWorlds.isEmpty()) {
            log.warn("No worlds returned from WorldQuery. Skipping detailed checks.");
            return true;
        }
        
        assertThat(true, "Successfully retrieved world list");

        // 2. Test F2P filter
        List<WorldEntity> f2pWorlds = ctx.worlds().freeToPlay().list();
        for (WorldEntity world : f2pWorlds) {
            if (!assertThat(!world.raw().getTypes().contains(WorldType.MEMBERS),
                    "F2P world " + world.getId() + " should not be MEMBERS")) {
                return false;
            }
        }

        // 3. Test Members filter
        List<WorldEntity> membersWorlds = ctx.worlds().members().list();
        for (WorldEntity world : membersWorlds) {
            if (!assertThat(world.raw().getTypes().contains(WorldType.MEMBERS),
                    "Members world " + world.getId() + " should be MEMBERS")) {
                return false;
            }
        }

        // 4. Test Sorting by World Number
        List<WorldEntity> sortedWorlds = ctx.worlds().sortByWorldNumberAsc().list();
        int lastId = -1;
        for (WorldEntity world : sortedWorlds) {
            if (!assertThat(world.getId() >= lastId, "Worlds should be sorted by ID ascending")) {
                return false;
            }
            lastId = world.getId();
        }

        // 5. Test specific world lookup
        WorldEntity first = allWorlds.get(0);
        List<WorldEntity> specific = ctx.worlds().withName(String.valueOf(first.getId())).list();
        if (!assertThat(specific.size() == 1, "Should find exactly one world by ID")) {
            return false;
        }
        if (!assertEquals(first.getId(), specific.get(0).getId(), "Found world ID should match")) {
            return false;
        }

        // 6. Test next/previous (ensure no exceptions)
        try {
            ctx.worlds().next();
            ctx.worlds().previous();
        } catch (Exception e) {
            return assertThat(false, "next() or previous() threw exception: " + e.getMessage());
        }

        // 7. Test standard world filter
        List<WorldEntity> standardWorlds = ctx.worlds().standard().list();
        for (WorldEntity world : standardWorlds) {
             if (!assertThat(!world.raw().getTypes().contains(WorldType.PVP), "Standard world should not be PVP")) {
                 return false;
             }
        }

        // 8. Test WorldEntity methods
        WorldEntity w = allWorlds.get(0);
        if (!assertNotNull(w.getName(), "World name should not be null")) return false;
        if (!assertThat(w.getId() > 0, "World ID should be positive")) return false;
        
        // 9. Test withTypes
        List<WorldEntity> pvpWorlds = ctx.worlds().withTypes(WorldType.PVP).list();
        for (WorldEntity world : pvpWorlds) {
            if (!assertThat(world.raw().getTypes().contains(WorldType.PVP), "World should be PVP")) {
                return false;
            }
        }
        
        // 10. Test nameContains
        // Find a world with 3 digits
        WorldEntity target = allWorlds.stream().filter(world -> world.getId() > 300 && world.getId() < 400).findFirst().orElse(null);
        if (target != null) {
             String search = String.valueOf(target.getId()).substring(0, 1); // e.g. "3"
             List<WorldEntity> matches = ctx.worlds().nameContains(search).list();
             if (!assertThat(!matches.isEmpty(), "Should find worlds containing " + search)) {
                 return false;
             }
             // Verify at least one match is correct (the logic in nameContains is complex, matching 3xx for "3")
             boolean foundTarget = matches.stream().anyMatch(world -> world.getId() == target.getId());
             if (!assertThat(foundTarget, "Should contain the target world " + target.getId())) {
                 return false;
             }
        }

        // 11. Test withActivity
        // Find a world with activity
        WorldEntity activityWorld = allWorlds.stream().filter(world -> world.raw().getActivity() != null && !world.raw().getActivity().isEmpty()).findFirst().orElse(null);
        if (activityWorld != null) {
            String activity = activityWorld.raw().getActivity();
            // take a part of it
            String part = activity.substring(0, Math.min(activity.length(), 4));
            List<WorldEntity> activityMatches = ctx.worlds().withActivity(part).list();
            if (!assertThat(!activityMatches.isEmpty(), "Should find worlds with activity " + part)) {
                return false;
            }
             if (!assertThat(activityMatches.stream().anyMatch(world -> world.getId() == activityWorld.getId()), "Should contain the target world")) {
                 return false;
             }
        }

        ctx.worlds().freeToPlay().next().hop();
        return true;
    }

    @Override
    protected String getTestName() {
        return "WorldQuery Test";
    }
}
