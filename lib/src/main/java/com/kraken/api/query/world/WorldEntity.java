package com.kraken.api.query.world;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.query.widget.WidgetEntity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.World;
import net.runelite.api.gameval.InterfaceID;

@Slf4j
public class WorldEntity extends AbstractEntity<World> {

    @Getter
    private final net.runelite.http.api.worlds.World httpPackageWorld;

    public WorldEntity(Context ctx, World raw, net.runelite.http.api.worlds.World httpPackageWorld) {
        super(ctx, raw);
        this.httpPackageWorld = httpPackageWorld;
    }

    @Override
    public String getName() {
        World w = raw();
        return w != null ? "w" + w.getId() : null;
    }

    @Override
    public int getId() {
        World w = raw();
        return w != null ? w.getId() : -1;
    }

    // Note: I believe this requires the world hopper plugin to be enabled.
    @Override
    public boolean interact(String action) {
        return ctx.runOnClientThread(() -> {
             Client client = ctx.getClient();
            if(client == null) return false;

            if(client.getGameState() == GameState.LOGIN_SCREEN) {
                client.changeWorld(raw());
                client.hopToWorld(raw());
                return true;
            }

            if (ctx.widgets().get(InterfaceID.Worldswitcher.BUTTONS) == null) {
                // TODO This takes time to complete widgets aren't visible yet. Need to run in client thread separately as to not block before
                // we actually
                client.openWorldHopper();

                WidgetEntity widget = ctx.widgets()
                        .withId(InterfaceID.Worldswitcher.BUTTONS)
                        .nameContains(String.valueOf(this.getId())).first();

                if(widget == null) {
                    log.error("world: {} widget is null", this.getId());
                    return false;
                }

                return widget.interact("Switch");
            } else {
                WidgetEntity widget = ctx.widgets()
                        .withId(InterfaceID.Worldswitcher.BUTTONS)
                        .nameContains(String.valueOf(this.getId())).first();

                if(widget == null) {
                    log.error("world widget: {} is null", this.getId());
                    return false;
                }
                return widget.interact("Switch");
            }
        });
    }

    /**
     * Attempts to perform a world hop for the current {@code WorldEntity}.
     * <p>
     * This method interacts with the RuneLite client to hop to the target world associated
     * with this {@code WorldEntity}. Depending on the client's state, it may handle login
     * screen transitions or directly use the world hopper interface to complete the action.
     * This operation may require the world hopper plugin to be enabled.
     * </p>
     *
     * @return {@code true} if the world hop was successfully performed; {@code false} otherwise.
     */
    public boolean hop() {
        return this.interact("");
    }
}
