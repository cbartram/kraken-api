package com.kraken.api.query.world;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.World;
import net.runelite.api.gameval.InterfaceID;

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

            if (client.getWidget(InterfaceID.Worldswitcher.BUTTONS) == null) {
                client.openWorldHopper();
                client.hopToWorld(raw());
                return true;
            }

            return false;
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
