package com.kraken.api.query.gameobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;

public class GameObjectEntity extends AbstractEntity<GameObject> {

    public GameObjectEntity(Context ctx, GameObject raw) {
        super(ctx, raw);
    }

    /**
     * Returns the object composition for a given {@code TileObject}.
     * @return The object composition for the wrapped {@code TileObject}.
     */
    public ObjectComposition getObjectComposition() {
        ObjectComposition def = ctx.runOnClientThread(() -> ctx.getClient().getObjectDefinition(this.raw.getId()));
        if(def.getImpostorIds() != null && def.getImpostor() != null) {
            return ctx.runOnClientThread(def::getImpostor);
        }

        return def;
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