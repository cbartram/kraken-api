package com.kraken.api.query.widget;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class WidgetQuery extends AbstractQuery<WidgetEntity, WidgetQuery, Widget> {

    public WidgetQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<WidgetEntity>> source() {
        return () -> {
            HashSet<Widget> widgets = new HashSet<>();
            ArrayList<Widget> buffer = new ArrayList<>();
            Widget[] currentQueue = ctx.getClient().getWidgetRoots();
            while(currentQueue.length != 0) {
                for (Widget widget : currentQueue) {
                    if (widget == null) {
                        continue;
                    }

                    widgets.add(widget);
                    if (widget.getDynamicChildren() != null) {
                        for (Widget dynamicChild : widget.getDynamicChildren()) {
                            if (dynamicChild == null) {
                                continue;
                            }
                            buffer.add(dynamicChild);
                            widgets.add(dynamicChild);
                        }
                    }

                    if (widget.getNestedChildren() != null) {
                        for (Widget nestedChild : widget.getNestedChildren()) {
                            if (nestedChild == null) {
                                continue;
                            }
                            buffer.add(nestedChild);
                            widgets.add(nestedChild);
                        }
                    }

                    Widget[] staticChildren;
                    try {
                        staticChildren = widget.getStaticChildren();
                    } catch (NullPointerException e) {
                        continue;
                    }

                    if (staticChildren != null) {
                        for (Widget staticChild : staticChildren) {
                            if (staticChild == null) {
                                continue;
                            }
                            buffer.add(staticChild);
                            widgets.add(staticChild);
                        }
                    }
                }
                currentQueue = buffer.toArray(new Widget[]{});
                buffer.clear();
            }

            return widgets.stream().map(w -> new WidgetEntity(ctx, w));
        };
    }

    /**
     * Filters for a widget with the exact packed ID.
     * Useful if you know the full ID (e.g., WidgetInfo.INVENTORY.getId()).
     * @param packedId The packed widget id to search for. Packed ID's encapsulate both the group and child id into a single
     *                 integer value.
     * @return WidgetQuery
     */
    public WidgetQuery withId(int packedId) {
        return filter(w -> w.raw().getId() == packedId);
    }

    /**
     * Filters for widgets belonging to a specific Interface Group.
     * Example: 149 is the Inventory group.
     * @param groupId The group id of the widget to search for
     * @return WidgetQuery
     */
    public WidgetQuery inGroup(int groupId) {
        return filter(w -> {
            int widgetGroup = w.raw().getId() >> 16;
            return widgetGroup == groupId;
        });
    }

    /**
     * Filters for a specific child component ID within a group.
     * Note: This checks the lower bits of the packed ID.
     * @param childId The child widget id to search for
     * @return WidgetQuery
     */
    public WidgetQuery withChildId(int childId) {
        return filter(w -> {
            int widgetChild = w.raw().getId() & 0xFFFF;
            return widgetChild == childId;
        });
    }

    /**
     * Filters by the widget's index in its parent's array.
     * Crucial for dynamic lists (chatbox messages, bank slots) where
     * every item has the same ID but a different index.
     * @param index The index in the widgets parent array to search for
     * @return WidgetQuery
     */
    public WidgetQuery withIndex(int index) {
        return filter(w -> w.raw().getIndex() == index);
    }

    /**
     * Filters for widgets which are currently visible on the canvas.
     * @return WidgetQuery
     */
    public WidgetQuery visible() {
        return filter(w -> !w.raw().isHidden() && w.raw().getParent() != null && !w.raw().getParent().isHidden());
    }

    /**
     * Finds widgets by their Sprite ID (images).
     * @param spriteId The integer id of the sprite
     * @return WidgetQuery
     */
    public WidgetQuery withSprite(int spriteId) {
        return filter(w -> w.raw().getSpriteId() == spriteId);
    }

    /**
     * Finds widgets that have a specific listener (e.g., "Continue" buttons).
     * @return WidgetQuery
     */
    public WidgetQuery withListener() {
        return filter(w -> w.raw().getOnOpListener() != null);
    }

    /**
     * Filters for widgets containing the specified text in name, text, or actions.
     * @param text The text to search for
     * @return WidgetQuery
     */
    public WidgetQuery withText(String text) {
        return withText(text, false);
    }

    /**
     * Filters for widgets containing the specified text in name, text, or actions.
     * @param text The text to search for
     * @param exact True if the text should match exactly otherwise a substring is used.
     * @return WidgetQuery
     */
    public WidgetQuery withText(String text, boolean exact) {
        return filter(widget -> widget.matches(text, exact));
    }
}