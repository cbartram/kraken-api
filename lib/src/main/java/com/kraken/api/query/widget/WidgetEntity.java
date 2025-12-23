package com.kraken.api.query.widget;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.util.StringUtils;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.widgets.Widget;

public class WidgetEntity extends AbstractEntity<Widget> {
    public WidgetEntity(Context ctx, Widget raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        Widget w = raw();
        return w != null ? w.getName() : null;
    }


    @Override
    public int getId() {
        Widget w = raw();
        return w != null ? w.getItemId() : -1;
    }

    /**
     * Checks if the widget text, name, or actions match the input.
     * @param search The search string to match
     * @param exact True if only exact matches of the search string should be accepted
     * @return true if there is a match within the search string for a specific widget and false otherwise
     */
    public boolean matches(String search, boolean exact) {
        Widget raw = raw();
        if (raw == null) return false;

        if (isMatchFound(search, exact)) return true;

        // Check Actions
        if (raw.getActions() != null) {
            for (String action : raw.getActions()) {
                if (action != null) {
                    String cleanAction = StringUtils.stripColTags(action);
                    if (exact) {
                        if (cleanAction.equalsIgnoreCase(search)) return true;
                    } else {
                        if (cleanAction.toLowerCase().contains(search.toLowerCase())) return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isMatchFound(String search, boolean exact) {
        Widget raw = raw();
        String cleanText = StringUtils.stripColTags(raw.getText());
        String cleanName = StringUtils.stripColTags(raw.getName());

        boolean matchFound = false;

        // Check Text and Name
        if (exact) {
            if (cleanText.equalsIgnoreCase(search) || cleanName.equalsIgnoreCase(search)) matchFound = true;
        } else {
            if (cleanText.toLowerCase().contains(search.toLowerCase())
                    || cleanName.toLowerCase().contains(search.toLowerCase())) matchFound = true;
        }
        return matchFound;
    }

    @Override
    public boolean interact(String action) {
        Widget w = raw();
        if (w == null) return false;
        ctx.getInteractionManager().interact(w, action);
        return true;
    }

    /**
     * Checks if the widget is currently visible.
     *
     * <p>A widget is considered visible if it is not marked as hidden in its underlying raw state.</p>
     *
     * @return {@code true} if the widget is visible; {@code false} otherwise.
     */
    public boolean isVisible() {
        if(raw() == null) return false;
        return !raw().isHidden() && !raw().isSelfHidden();
    }

    /**
     * Uses a widget on another widget. (i.e. High Alchemy)
     * @param destinationWidget The destination widget to use this entity on
     * @return True if the action is successful and false otherwise.
     */
    public boolean useOn(Widget destinationWidget) {
        Widget w = raw();
        if(w == null) return false;
        ctx.getInteractionManager().interact(w, destinationWidget);
        return true;
    }

    /**
     * Uses a widget on an NPC (i.e. Crumble Undead Spell on the Undead Spawn from Vorkath)
     * @param npc NPC to use the widget on.
     * @return True if the action was successful and false otherwise.
     */
    public boolean useOn(NPC npc) {
        Widget w = raw();
        if(w == null) return false;
        ctx.getInteractionManager().interact(w, npc);
        return true;
    }

    /**
     * Uses a widget on a Game Object (i.e. Bones on the Chaos Altar)
     * @param gameObject The Game Object to use the widget on.
     * @return True if the action was successful and false otherwise.
     */
    public boolean useOn(GameObject gameObject) {
        Widget w = raw();
        if(w == null) return false;
        ctx.getInteractionManager().interact(w, gameObject);
        return true;
    }
}