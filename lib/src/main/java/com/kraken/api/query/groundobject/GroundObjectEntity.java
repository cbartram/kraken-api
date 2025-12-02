package com.kraken.api.query.groundobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;

public class GroundObjectEntity extends AbstractEntity<GroundItem> {

    public GroundObjectEntity(Context ctx, GroundItem raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        return this.raw.getName();
    }

    /**
     * Interact interacts with the ground object. This interact is slightly different from other interactions because the underlying
     * packet object does not accept an action to take on the ground object. It is always "Take" because that is the only action
     * you can perform to an item on the ground.
     * <p>
     * To conform to the interface for an {@code AbstractEntity} we still accept an action parameter although it will do
     * nothing in this particular instance.
     * @param action The menu action to trigger (e.g. "Take")
     * @return True if the interaction is successful and false otherwise
     */
    @Override
    public boolean interact(String action) {
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw);
        return true;
    }

    /**
     * Takes an item from the ground to be placed in the players inventory.
     * @return True if the action was successful and false otherwise.
     */
    public boolean take() {
        return interact("take"); // Can be any string it doesn't matter, for readability its "take"
    }

    @Override
    public int getId() {
        return raw.getItemId();
    }
}