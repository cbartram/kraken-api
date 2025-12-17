package example.tests.query;

import com.kraken.api.Context;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WidgetTest extends BaseApiTest {

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        boolean testsPassed = true;

        try {
            // 1. Basic Text Search & Visibility
            // The "Public" chat filter button is almost always visible in the chatbox.
            var publicChatButton = ctx.widgets().withText("Public").visible().first();

            if (publicChatButton.isNull()) {
                log.error("Failed to find 'Public' chat button via text query");
                testsPassed = false;
            } else {
                // 2. Test ID Filters (Round Trip)
                // Now that we have a valid widget, let's verify we can find it by its IDs
                int packedId = publicChatButton.raw().getId();
                int groupId = packedId >> 16;
                int childId = packedId & 0xFFFF;
                int index = publicChatButton.raw().getIndex();

                // 2a. Test withId
                boolean foundById = !ctx.widgets().withId(packedId).withIndex(index).first().isNull();
                if (!foundById) {
                    log.error("Failed to find widget by packed ID: " + packedId);
                    testsPassed = false;
                }

                // 2b. Test inGroup
                // There should be many widgets in the chatbox group
                long groupCount = ctx.widgets().inGroup(groupId).stream().count();
                if (groupCount == 0) {
                    log.error("inGroup(" + groupId + ") returned 0 results");
                    testsPassed = false;
                }

                // 2c. Test withChildId
                boolean foundByChild = !ctx.widgets().inGroup(groupId).withChildId(childId).withIndex(index).first().isNull();
                if (!foundByChild) {
                    log.error("Failed to find widget by Child ID: " + childId);
                    testsPassed = false;
                }
            }

            // 3. Test Action Filter
            // We search for something with a generic action usually available.
            // "Report" is a common button text/action on the chatbox frame.
            boolean abuse = !ctx.widgets().withAction("Report abuse").first().isNull();
            if (!abuse) {
                // Fallback: Try "Activate" (Quick prayer) or "Walk here" (Minimap)
                boolean quickPrayers = !ctx.widgets().withAction("Activate Quick-prayers").first().isNull();
                if (!quickPrayers) {
                    log.warn("Could not find any widgets with 'Report abuse' or 'Activate Quick-prayers' actions (Unusual but possible if interfaces hidden)");
                }
            }

            // 4. Test Sprite Filter
            // Find *any* widget with a valid sprite ID, then try to find it back.
            var spriteWidget = ctx.widgets().filter(w -> w.raw().getSpriteId() > -1).first();
            if (!spriteWidget.isNull()) {
                int spriteId = spriteWidget.raw().getSpriteId();
                boolean foundSprite = !ctx.widgets().withSprite(spriteId).first().isNull();
                if (!foundSprite) {
                    log.error("Failed to retrieve widget via withSprite(" + spriteId + ")");
                    testsPassed = false;
                }
            }

            // 5. Test Listener
            // Find a widget that has a listener (buttons usually have these)
            boolean hasListener = !ctx.widgets().withListener().first().isNull();
            if (!hasListener) {
                log.error("Failed to find any widget with a listener (withListener)");
                testsPassed = false;
            }

        } catch (Exception e) {
            log.error("Failed to run Widget test", e);
            return false;
        }

        return testsPassed;
    }

    @Override
    protected String getTestName() {
        return "Widget";
    }
}