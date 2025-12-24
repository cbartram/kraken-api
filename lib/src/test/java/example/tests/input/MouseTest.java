package example.tests.input;

import com.google.inject.Inject;
import com.kraken.api.Context;
import com.kraken.api.input.mouse.VirtualMouse;
import com.kraken.api.query.gameobject.GameObjectEntity;
import com.kraken.api.query.npc.NpcEntity;
import com.kraken.api.query.player.PlayerEntity;
import com.kraken.api.service.ui.UIService;
import com.kraken.api.service.util.RandomService;
import example.tests.BaseApiTest;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

@Slf4j
public class MouseTest extends BaseApiTest {

    @Inject
    private VirtualMouse mouse;

    @Inject
    private UIService uiService;

    @Override
    protected boolean runTest(Context ctx) throws Exception {
        mouse.load("test");

        // Find a random game object, move mouse to it
        GameObjectEntity gameObject = ctx.gameObjects().within(7).random();
        log.info("Moving to random game object: {}", gameObject.getName());
        mouse.move(uiService.getClickbox(gameObject.raw()));

        Thread.sleep(RandomService.between(3000, 5000));

        // Find a random NPC, move mouse to it
        NpcEntity npc = ctx.npcs().within(10).random();
        log.info("Moving to random NPC: {}", npc.getName());
        mouse.move(uiService.getClickbox(npc.raw()));

        Thread.sleep(RandomService.between(3000, 5000));


        // Find a random player (if nearby)
        PlayerEntity player = ctx.players().nearest();
        if(player != null) {
            log.info("Moving to random player: {}", player.getName());
            mouse.move(uiService.getClickbox(player.raw()));
        } else {
            log.info("No nearby players found.");
        }

        Thread.sleep(RandomService.between(3000, 5000));

        // Find a random world point
        int x = ctx.players().local().raw().getWorldLocation().getX() + RandomService.between(1, 10);
        int y = ctx.players().local().raw().getWorldLocation().getY() + RandomService.between(1, 10);
        log.info("Moving to random world point: ({}, {})", x, y);
        mouse.move(uiService.getClickbox(new WorldPoint(x, y, 0)));

        Thread.sleep(RandomService.between(3000, 5000));
        return true;
    }

    @Override
    protected String getTestName() {
        return "Mouse";
    }
}
