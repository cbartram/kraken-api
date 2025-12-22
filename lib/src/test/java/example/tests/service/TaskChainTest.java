package example.tests.service;

import com.kraken.api.Context;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.service.util.TaskChain;
import example.tests.BaseApiTest;
import net.runelite.api.coords.WorldPoint;

public class TaskChainTest extends BaseApiTest {

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        // Setup
        boolean success = TaskChain.builder(ctx)
                .walkTo(new WorldPoint(3242, 3238, 0))
                .run(() -> ctx.gameObjects().withName("Canoe Station").nearest().interact("Chop-down"))
                .waitUntil(() -> {
                    GameObjectEntity canoe = ctx.gameObjects().withAction("Shape-Canoe").nearest();
                    return canoe != null && !canoe.isNull();
                })
                .run(() -> ctx.gameObjects().withAction("Shape-Canoe").nearest().interact("Shape-Canoe"))
                .waitUntil(() -> ctx.widgets().inGroup(416).withChildId(3).first() != null)
                .run(() -> ctx.widgets().withId(27262996).first().interact("Float")) // IDs are examples
                .execute();
        return success;
    }

    @Override
    protected String getTestName() {
        return "Task Chain";
    }
}
