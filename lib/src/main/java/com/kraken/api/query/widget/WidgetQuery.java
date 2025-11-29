package com.kraken.api.query.widget;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class WidgetQuery extends AbstractQuery<WidgetEntity, WidgetQuery> {

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
     * Filters for widgets which are currently visible on the canvas.
     */
    public WidgetQuery visible() {
        return filter(w -> !w.raw().isHidden() && w.raw().getParent() != null && !w.raw().getParent().isHidden());
    }

    /**
     * Finds widgets containing specific text (e.g., chat options, dialogs).
     */
    public WidgetQuery textContains(String text) {
        return filter(w -> w.raw().getText() != null && w.raw().getText().contains(text));
    }

    /**
     * Finds widgets by their Sprite ID (images).
     */
    public WidgetQuery withSprite(int spriteId) {
        return filter(w -> w.raw().getSpriteId() == spriteId);
    }

    /**
     * Finds widgets that have a specific listener (e.g., "Continue" buttons).
     */
    public WidgetQuery withListener() {
        return filter(w -> w.raw().getOnOpListener() != null);
    }
}