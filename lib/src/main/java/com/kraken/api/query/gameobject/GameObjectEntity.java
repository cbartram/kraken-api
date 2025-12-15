package com.kraken.api.query.gameobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import net.runelite.api.GameObject;
import net.runelite.api.ObjectComposition;
import net.runelite.api.widgets.Widget;

public class GameObjectEntity extends AbstractEntity<GameObject> {

    public GameObjectEntity(Context ctx, GameObject raw) {
        super(ctx, raw);
    }

    @Override
    public int getId() {
        GameObject raw = raw();
        return raw != null ? raw.getId() : -1;
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


    /**
     * Returns the object composition for a given {@code TileObject}.
     * @return The object composition for the wrapped {@code TileObject}.
     */
    public ObjectComposition getObjectComposition() {
        GameObject raw = raw();
        ObjectComposition def = ctx.runOnClientThread(() -> ctx.getClient().getObjectDefinition(raw.getId()));
        if(def.getImpostorIds() != null && def.getImpostor() != null) {
            return ctx.runOnClientThread(def::getImpostor);
        }

        return def;
    }

    @Override
    public boolean interact(String action) {
        GameObject raw = raw();
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    /**
     * Uses a specified widget on the Game Object (i.e. "Bones" on the "Chaos Altar")
     * @param widget The widget to use on the Game Object
     * @return True if the interaction was successful and false otherwise
     */
    public boolean useWidget(Widget widget) {
        GameObject raw = raw();
        if (raw == null) return false;
        ctx.getInteractionManager().interact(widget, raw);
        return true;
    }
}