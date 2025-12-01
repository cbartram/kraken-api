package com.kraken.api.query.gameobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.TileObject;

public class GameObjectEntity extends AbstractEntity<GameObject> {

    public GameObjectEntity(Context ctx, GameObject raw) {
        super(ctx, raw);
    }

    /**
     * Returns the object composition for a given {@code TileObject}.
     * @return The object composition for the wrapped {@code TileObject}.
     */
    public ObjectComposition getObjectComposition() {
        TileObject tileObject = this.raw;
        if(ctx.getClient().getObjectDefinition(tileObject.getId()).getImpostorIds() != null && ctx.getClient().getObjectDefinition(tileObject.getId()).getImpostor() != null) {
            return ctx.runOnClientThread(() -> ctx.getClient().getObjectDefinition(tileObject.getId()).getImpostor());
        }

        return ctx.runOnClientThread(() -> ctx.getClient().getObjectDefinition(tileObject.getId()));
    }

    @Override
    public String getName() {
        // TODO Sometimes this returns some really weird things for NPC's that are being considered game objects?
        ObjectComposition composition = getObjectComposition();
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