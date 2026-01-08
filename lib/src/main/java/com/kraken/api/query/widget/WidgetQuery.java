package com.kraken.api.query.widget;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.util.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
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
     * Returns a widget directly from the client. This can return widgets which may not be visible but
     * are known to the client. i.e. a logout button widget without being on the logout tab.
     * <p>
     * This method wraps the widget in a {@link WidgetEntity} class allowing the widget to be interacted with.
     * @param packedId The packed widget component id to find.
     * @return WidgetEntity or null if no widget is found.
     */
    public WidgetEntity fromClient(int packedId) {
        return ctx.runOnClientThread(() -> {
            Widget w = ctx.getClient().getWidget(packedId);
            if(w == null) return null;
            return new WidgetEntity(ctx, w);
        });
    }

    /**
     * Returns a widget directly from the client. This can return widgets which may not be visible but
     * are known to the client. i.e. a logout button widget without being on the logout tab.
     * <p>
     * This method wraps the widget in a {@link WidgetEntity} class allowing the widget to be interacted with.
     * @param groupId The widgets group id
     * @param childId The widgets child id
     * @return WidgetEntity or null if no widget is found.
     */
    public WidgetEntity fromClient(int groupId, int childId) {
        return ctx.runOnClientThread(() -> {
            Widget w = ctx.getClient().getWidget(groupId, childId);
            if(w == null) return null;
            return new WidgetEntity(ctx, w);
        });
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
     * Retrieves the first {@link WidgetEntity} matching the specified packed ID.
     *
     * <p>This method filters widgets by their packed ID, which is a composite of the
     * group ID and child ID. It is useful when the exact packed ID of a widget is known.</p>
     *
     * @param packedId The packed ID of the widget to retrieve. This ID encapsulates both
     *                 the group and child ID into a single integer value.
     * @return The {@link WidgetEntity} corresponding to the specified packed ID, or
     *         {@code null} if no matching widget is found.
     */
    public WidgetEntity get(int packedId) {
        return withId(packedId).first();
    }


    /**
     * Retrieves the first {@link WidgetEntity} that matches the specified {@link WidgetInfo}.
     *
     * <p>This method filters widgets based on the packed ID retrieved from the {@link WidgetInfo} instance.</p>
     *
     * @param widgetInfo The {@link WidgetInfo} instance containing the packed ID to search for.
     *                   The packed ID incorporates both the group and child ID.
     * @return The corresponding {@link WidgetEntity}, or {@code null} if no match is found.
     */
    public WidgetEntity get(WidgetInfo widgetInfo) {
        return withId(widgetInfo.getId()).first();
    }


    /**
     * Retrieves a {@link WidgetEntity} based on the specified group ID and child ID.
     *
     * <p>This method accesses a widget using its hierarchical identifiers:
     * <ul>
     *   <li><b>groupId:</b> Represents the interface group to which the widget belongs.</li>
     *   <li><b>childId:</b> Identifies the specific widget within the group.</li>
     * </ul>
     *
     * <p>The widget is created and returned as a {@link WidgetEntity} instance, allowing for further
     * interaction or queries.</p>
     *
     * @param groupId The group ID of the widget to retrieve. Represents the top-level container or interface.
     * @param childId The child ID of the widget to retrieve. Represents the specific item within the group.
     * @return A {@link WidgetEntity} representing the widget with the provided group ID and child ID,
     *         or {@code null} if no matching widget is found.
     */
    public WidgetEntity get(int groupId, int childId) {
        return withGroupChild(groupId, childId).first();
    }

    /**
     * Filters for a widget with a specific group ID and child ID.
     *
     * <p>This method evaluates the group and child ID of each widget by decomposing
     * its packed ID. The packed ID is a combination where the group ID occupies the
     * higher 16 bits, and the child ID occupies the lower 16 bits.</p>
     *
     * <p>For example, in the packed id format:<br>
     * {@code packedId = (groupId << 16) | childId}<br>
     * The group ID and child ID are derived as follows:</p>
     * <ul>
     *   <li>{@code groupId = packedId >>> 16}</li>
     *   <li>{@code childId = packedId & 0xFFFF}</li>
     * </ul>
     *
     * <p>Only the widgets that match the specified group and child ID will be included
     * in the resulting query.</p>
     *
     * @param group The group identifier of the widget.
     * @param child The child identifier of the widget.
     * @return {@literal WidgetQuery} A new query filtered for widgets with the specified
     *         group and child IDs.
     */
    public WidgetQuery withGroupChild(int group, int child) {
        return filter(w -> {
            int widgetChild = w.raw().getId() & 0xFFFF;
            int widgetGroup = w.raw().getId() >>> 16;
            return widgetGroup == group && widgetChild == child;
        });
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
     * Filters for widgets with a specific action.
     * @param action The action string to filter widgets for
     * @return WidgetQuery
     */
    public WidgetQuery withAction(String action) {
        return filter(Objects::nonNull)
            .filter(widget -> {
                String[] actions = widget.raw().getActions();
                return actions != null &&
                        Arrays.stream(actions)
                                .filter(Objects::nonNull)
                                .map(Text::removeTags)
                                .anyMatch(s -> s.equalsIgnoreCase(action));
            });
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