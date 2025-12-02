package com.kraken.api.query.widget;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractEntity;
import com.kraken.api.util.StringUtils;
import net.runelite.api.widgets.Widget;

public class WidgetEntity extends AbstractEntity<Widget> {
    public WidgetEntity(Context ctx, Widget raw) {
        super(ctx, raw);
    }

    @Override
    public String getName() {
        return raw.getName();
    }


    @Override
    public int getId() {
        return raw.getItemId();
    }

    /**
     * Checks if the widget text, name, or actions match the input.
     * @param search The search string to match
     * @param exact True if only exact matches of the search string should be accepted
     * @return true if there is a match within the search string for a specific widget and false otherwise
     */
    public boolean matches(String search, boolean exact) {
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
        if (raw == null) return false;
        ctx.getInteractionManager().interact(raw, action);
        return true;
    }

    /**
     * Uses a widget on another widget. (i.e. High Alchemy)
     * @param destinationWidget The destination widget to use this entity on
     * @return True if the action is successful and false otherwise.
     */
    public boolean useOn(Widget destinationWidget) {
        if(raw == null) return false;
        ctx.getInteractionManager().interact(raw, destinationWidget);
        return true;
    }
}