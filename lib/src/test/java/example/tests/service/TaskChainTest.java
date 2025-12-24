package example.tests.service;

import com.kraken.api.Context;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.util.TaskChain;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class TaskChainTest extends BaseApiTest {

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        // This expects to run near lumbridge
        boolean success = TaskChain.builder(ctx)
                .walkTo(new WorldPoint(3245, 3235, 0))
                .run(() -> ctx.gameObjects().withName("Canoe Station").nearest().interact("Chop-down"))
                .waitUntil(() -> {
                    GameObjectEntity canoe = ctx.gameObjects().withAction("Shape-Canoe").nearest();
                    if(canoe != null) {
                        return !canoe.isNull();
                    }
                    return false;
                })
                .run(() -> ctx.gameObjects().withAction("Shape-Canoe").nearest().interact("Shape-Canoe"))
                .waitUntil(() -> {
                    WidgetEntity entity = ctx.widgets().get(416, 3);
                    log.info("Canoe widget entity: {}", entity);
                    if(entity == null) return false;
                    return !entity.isNull();
                })
                .run(() -> {
                    log.info("Making canoe...");
                    ctx.widgets().withId(27262996).first().interact("Make");
                })
                .execute();
        return success;
    }

    @Override
    protected String getTestName() {
        return "Task Chain";
    }
}
