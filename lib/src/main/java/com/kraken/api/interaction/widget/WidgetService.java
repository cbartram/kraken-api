package com.kraken.api.interaction.widget;


import com.kraken.api.core.AbstractService;
import com.kraken.api.core.RandomService;
import com.kraken.api.model.NewMenuEntry;
import com.kraken.api.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.MenuAction;
import net.runelite.api.annotations.Component;
import net.runelite.api.widgets.Widget;

import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class WidgetService extends AbstractService {

    public Widget getWidget(int id, int child) {
        return context.runOnClientThreadOptional(() -> client.getWidget(id, child))
                .orElse(null);
    }

    public Widget getWidget(@Component int id) {
        return context.runOnClientThreadOptional(() -> client.getWidget(id)).orElse(null);
    }

    public boolean isWidgetVisible(@Component int id) {
        return context.runOnClientThreadOptional(() -> {
            Widget widget = getWidget(id);
            if (widget == null) return false;
            return !widget.isHidden();
        }).orElse(false);
    }

    public boolean isWidgetVisible(int widgetId, int childId) {
        return  context.runOnClientThreadOptional(() -> {
            Widget widget = getWidget(widgetId, childId);
            if (widget == null) return false;
            return !widget.isHidden();
        }).orElse(false);
    }

    public boolean clickWidget(Widget widget) {
        if (widget != null) {
            context.getMouse().click(RandomService.randomPoint(widget.getBounds(), 0));
            return true;
        }
        return false;
    }

    public boolean clickWidget(String text) {
        return clickWidget(text, Optional.empty(), 0, false);
    }

    public boolean clickWidget(String text, boolean exact) {
        return clickWidget(text, Optional.empty(), 0, exact);
    }

    public boolean clickWidget(int parentId, int childId) {
        Widget widget = getWidget(parentId, childId);
        return clickWidget(widget);
    }

    public boolean clickWidget(int id) {
        Widget widget = context.runOnClientThreadOptional(() -> client.getWidget(id)).orElse(null);;
        if (widget == null) return false;
        if(widget.isHidden()) return false;
        context.getMouse().click(RandomService.randomPoint(widget.getBounds(), 0));
        return true;
    }

    public boolean clickWidget(String text, Optional<Integer> widgetId, int childId, boolean exact) {
        return context.runOnClientThreadOptional(() -> {
            Widget widget;
            if (widgetId.isEmpty()) {
                widget = findWidget(text, null, exact);
            } else {
                Widget rootWidget = getWidget(widgetId.get(), childId);
                List<Widget> rootWidgets = new ArrayList<>();
                rootWidgets.add(rootWidget);
                widget = findWidget(text, rootWidgets, exact);
            }

            if (widget != null) {
                clickWidget(widget);
            }

            return widget != null;

        }).orElse(false);
    }

    public void clickWidgetFast(int packetId, int identifier) {
        Widget widget = getWidget(packetId);
        clickWidgetFast(widget, -1, identifier);
    }

    public void clickWidgetFast(Widget widget, int param0, int identifier) {
        int param1 = widget.getId();
        String target = "";
        MenuAction menuAction = MenuAction.CC_OP;
        context.doInvoke(new NewMenuEntry(param0 != -1 ? param0 : widget.getType(), param1, menuAction.getId(), identifier, widget.getItemId(), target), widget.getBounds());
    }

    public boolean hasWidgetText(String text, int widgetId, int childId, boolean exact) {
        return context.runOnClientThreadOptional(() -> {
            Widget rootWidget = getWidget(widgetId, childId);
            if (rootWidget == null) return false;

            // Use findWidget to perform the search on all child types
            Widget foundWidget = findWidget(text, List.of(rootWidget), exact);
            return foundWidget != null;
        }).orElse(false);
    }

    /**
     * Searches for a widget with text that matches the specified criteria, either in the provided child widgets
     * or across all root widgets if children are not specified.
     *
     * @param text     The text to search for within the widgets.
     * @param children A list of child widgets to search within. If null, searches through all root widgets.
     * @param exact    Whether the search should match the text exactly or allow partial matches.
     * @return The widget containing the specified text, or null if no match is found.
     */
    public Widget findWidget(String text, List<Widget> children, boolean exact) {
        return context.runOnClientThreadOptional(() -> {
            Widget foundWidget = null;
            if (children == null) {
                // Search through root widgets if no specific children are provided
                List<Widget> rootWidgets = Arrays.stream(client.getWidgetRoots())
                        .filter(x -> x != null && !x.isHidden()).collect(Collectors.toList());
                for (Widget rootWidget : rootWidgets) {
                    if (rootWidget == null) continue;
                    if (matchesText(rootWidget, text, exact)) {
                        return rootWidget;
                    }
                    foundWidget = searchChildren(text, rootWidget, exact);
                    if (foundWidget != null) return foundWidget;
                }
            } else {
                // Search within provided child widgets
                for (Widget child : children) {
                    foundWidget = searchChildren(text, child, exact);
                    if (foundWidget != null) break;
                }
            }
            return foundWidget;
        }).orElse(null);
    }

    /**
     * Recursively searches through all child widgets of the specified widget for a match with the given text.
     *
     * @param text  The text to search for within the widget and its children.
     * @param child The widget to search within.
     * @param exact Whether the search should match the text exactly or allow partial matches.
     * @return The widget containing the specified text, or null if no match is found.
     */
    public static Widget searchChildren(String text, Widget child, boolean exact) {
        if (matchesText(child, text, exact)) return child;

        List<Widget[]> childGroups = Stream.of(child.getChildren(), child.getNestedChildren(), child.getDynamicChildren(), child.getStaticChildren())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (Widget[] childGroup : childGroups) {
            if (childGroup != null) {
                for (Widget nestedChild : Arrays.stream(childGroup).filter(w -> w != null && !w.isHidden()).collect(Collectors.toList())) {
                    Widget found = searchChildren(text, nestedChild, exact);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    /**
     * Checks if the text or any action in the widget matches the search criteria.
     *
     * @param widget The widget to check for the specified text or action.
     * @param text   The text to match within the widget’s content.
     * @param exact  Whether the match should be exact or allow partial matches.
     * @return True if the widget's text or any action matches the search criteria, false otherwise.
     */
    private static boolean matchesText(Widget widget, String text, boolean exact) {
        String cleanText = StringUtils.stripColTags(widget.getText());
        String cleanName = StringUtils.stripColTags(widget.getName());

        if (exact) {
            if (cleanText.equalsIgnoreCase(text) || cleanName.equalsIgnoreCase(text)) return true;
        } else {
            if (cleanText.toLowerCase().contains(text.toLowerCase()) || cleanName.toLowerCase().contains(text.toLowerCase()))
                return true;
        }

        if (widget.getActions() != null) {
            for (String action : widget.getActions()) {
                if (action != null) {
                    String cleanAction = StringUtils.stripColTags(action);
                    if (exact ? cleanAction.equalsIgnoreCase(text) : cleanAction.toLowerCase().contains(text.toLowerCase())) {
                        return true;
                    }
                }
            }
        }

        return false;
    }
}
