package com.kraken.api.interaction.gameobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;

public class GameObjectEntity extends AbstractEntity<TileObject> {

    public GameObjectEntity(Context ctx, TileObject raw) {
        super(ctx, raw);
    }

    /**
     * Returns the object composition for a given TileObject.
     * @param tileObject The tile object to retrieve the composition for
     * @return The object composition for a given tile object
     */
    private ObjectComposition getObjectComposition(TileObject tileObject) {
        if(ctx.getClient().getObjectDefinition(tileObject.getId()).getImpostorIds() != null && ctx.getClient().getObjectDefinition(tileObject.getId()).getImpostor() != null) {
            return ctx.runOnClientThread(() -> ctx.getClient().getObjectDefinition(tileObject.getId()).getImpostor());
        }

        return ctx.runOnClientThread(() -> ctx.getClient().getObjectDefinition(tileObject.getId()));
    }

    @Override
    public String getName() {
        ObjectComposition composition = getObjectComposition(this.raw);
        if(composition != null) {
            return composition.getName();
        }
        return "Unknown (no composition)";
    }

    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }
}