package com.kraken.api.interaction.widget;


import com.kraken.api.core.AbstractService;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.interaction.ui.UIService;
import com.kraken.api.util.StringUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.annotations.Component;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Singleton
public class WidgetService extends AbstractService {

    @Inject
    private UIService uiService;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private WidgetPackets widgetPackets;

    private int lastSearchIdleTicks = -10;

    @Getter
    private HashSet<Widget> cachedWidgets = new HashSet<>();

    /**
     * Interacts with a widget using the specified action.
     * @param widget the widget to interact with
     * @param action the action to perform
     * @return true if the interaction was successful, false otherwise
     */
    public boolean interact(Widget widget, String action) {
        if(!context.isPacketsLoaded()) return false;
        if(widget == null) return false;

        return context.runOnClientThread(() -> {
            Point pt = uiService.getClickbox(widget);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetAction(widget, action);
            return Optional.of(true);
        }).orElse(false);
    }

    /**
     * Interacts with a widget identified by its ID using the specified action.
     * @param id the widget ID
     * @param action the action to perform
     * @return true if the interaction was successful, false otherwise
     */
    public boolean interact(int id, String action) {
        Widget widget = getWidget(id);
        if (widget == null) {
            return false;
        }

        return interact(widget, action);
    }

    /**
     * Interacts with a widget identified by its ID and child index using the specified action.
     * @param id the widget ID
     * @param child the child index
     * @param action the action to perform
     * @return true if the interaction was successful, false otherwise
     */
    public boolean interact(int id, int child, String action) {
        Widget widget = getWidget(id, child);
        if (widget == null) {
            return false;
        }

        return interact(widget, action);
    }

    /**
     * Interacts with a widget identified by its text using the specified action.
     * @param text the widget text
     * @param action the action to perform
     * @return true if the interaction was successful, false otherwise
     */
    public boolean interact(String text, String action) {
        Widget widget = findWidget(text);
        if (widget == null) {
            return false;
        }

        return interact(widget, action);
    }

    /**
     * Uses a source widget on a destination widget (i.e. High Alchemy)
     * @param srcWidget The source widget to use on the destination widget
     * @param destWidget The destination widget
     * @return true if the operation is successful and false otherwise
     */
    public boolean widgetOnWidget(Widget srcWidget, Widget destWidget) {
        return context.runOnClientThreadOptional(() -> {
            Point pt = uiService.getClickbox(srcWidget);
            mousePackets.queueClickPacket(pt.getX(), pt.getY());
            widgetPackets.queueWidgetOnWidget(srcWidget, destWidget);
            return true;
        }).orElse(false);
    }

    /**
     * Returns a widget by its ID and child index.
     * @param id the widget ID
     * @param child the child index
     * @return the widget, or null if not found
     */
    public Widget getWidget(int id, int child) {
        return context.runOnClientThreadOptional(() -> client.getWidget(id, child))
                .orElse(null);
    }

    /**
     * Returns a widget by its ID
     * @param id the widget ID
     * @return the widget, or null if not found
     */
    public Widget getWidget(@Component int id) {
        return context.runOnClientThreadOptional(() -> client.getWidget(id)).orElse(null);
    }

    /**
     * Checks if a widget with the specified ID is visible (not hidden).
     * @param id the widget ID
     * @return true if the widget is visible, false otherwise
     */
    public boolean isWidgetVisible(@Component int id) {
        return context.runOnClientThreadOptional(() -> {
            Widget widget = getWidget(id);
            if (widget == null) return false;
            return !widget.isHidden();
        }).orElse(false);
    }

    /**
     * Checks if a widget with the specified ID is visible (not hidden).
     * @param id the widget ID
     * @param child the child index
     * @return true if the widget is visible, false otherwise
     */
    public boolean isWidgetVisible(int id, int child) {
        return  context.runOnClientThreadOptional(() -> {
            Widget widget = getWidget(id, child);
            if (widget == null) return false;
            return !widget.isHidden();
        }).orElse(false);
    }

    /**
     * Checks if a widget with the specified text exists within the widget identified by widgetId and childId.
     *
     * @param text the text to search for
     * @param widgetId the widget ID
     * @param childId the child index
     * @param exact whether to match the text exactly or partially
     * @return true if a matching widget is found, false otherwise
     */
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
     * Finds a widget with the specified text among the provided child widgets.
     * @param text the text to search for
     * @param children the list of child widgets to search within
     * @return the widget containing the specified text, or null if not found
     */
    public Widget findWidget(String text, List<Widget> children) {
        return findWidget(text, children, false);
    }

    /**
     * Finds a widget with the specified text among the provided child widgets.
     * @param text the text to search for
     * @return the widget containing the specified text, or null if not found
     */
    public Widget findWidget(String text) {
        return findWidget(text, null, false);
    }

    /**
     * Finds a widget with the specified text among the provided child widgets.
     * @param text the text to search for
     * @param exact whether to match the text exactly or partially
     * @return the widget containing the specified text, or null if not found
     */
    public Widget findWidget(String text, boolean exact) {
        return findWidget(text, null, exact);
    }

    /**
     * Checks if a widget with the specified text exists among the provided child widgets or across all root widgets if children are not specified.
     * @param text the text to search for
     * @return true if a matching widget is found, false otherwise
     */
    public boolean hasWidget(String text) {
        return findWidget(text, null, false) != null;
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
     * Searches for a widget with the specified sprite ID among root widgets or the specified child widgets.
     *
     * @param spriteId The sprite ID to search for.
     * @param children A list of child widgets to search within. If null, searches root widgets.
     * @return The widget with the specified sprite ID, or null if not found.
     */
    public Widget findWidget(int spriteId, List<Widget> children) {
        return context.runOnClientThreadOptional(() -> {
            Widget foundWidget = null;

            if (children == null) {
                // Search through root widgets if no specific children are provided
                List<Widget> rootWidgets = Arrays.stream(context.getClient().getWidgetRoots())
                        .filter(widget -> widget != null && !widget.isHidden())
                        .collect(Collectors.toList());
                for (Widget rootWidget : rootWidgets) {
                    if (rootWidget == null) continue;
                    if (matchesSpriteId(rootWidget, spriteId)) {
                        return rootWidget;
                    }
                    foundWidget = searchChildren(spriteId, rootWidget);
                    if (foundWidget != null) return foundWidget;
                }
            } else {
                // Search within provided child widgets
                for (Widget child : children) {
                    foundWidget = searchChildren(spriteId, child);
                    if (foundWidget != null) break;
                }
            }
            return foundWidget;
        }).orElse(null);
    }

    /**
     * Checks if a widget's sprite ID matches the specified sprite ID.
     *
     * @param widget   The widget to check.
     * @param spriteId The sprite ID to match.
     * @return True if the widget's sprite ID matches the specified sprite ID, false otherwise.
     */
    private boolean matchesSpriteId(Widget widget, int spriteId) {
        return widget != null && widget.getSpriteId() == spriteId;
    }

    /**
     * Recursively searches through the child widgets of the given widget for a match with the specified sprite ID.
     *
     * @param spriteId The sprite ID to search for.
     * @param child    The widget to search within.
     * @return The widget with the specified sprite ID, or null if not found.
     */
    public Widget searchChildren(int spriteId, Widget child) {
        if (matchesSpriteId(child, spriteId)) return child;

        List<Widget[]> childGroups = Stream.of(child.getChildren(), child.getNestedChildren(), child.getDynamicChildren(), child.getStaticChildren())
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        for (Widget[] childGroup : childGroups) {
            if (childGroup != null) {
                for (Widget nestedChild : Arrays.stream(childGroup).filter(w -> w != null && !w.isHidden()).collect(Collectors.toList())) {
                    Widget found = searchChildren(spriteId, nestedChild);
                    if (found != null) return found;
                }
            }
        }
        return null;
    }

    /**
     * Recursively searches through all child widgets of the specified widget for a match with the given text.
     *
     * @param text  The text to search for within the widget and its children.
     * @param child The widget to search within.
     * @param exact Whether the search should match the text exactly or allow partial matches.
     * @return The widget containing the specified text, or null if no match is found.
     */
    public Widget searchChildren(String text, Widget child, boolean exact) {
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
    private boolean matchesText(Widget widget, String text, boolean exact) {
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

    public WidgetQuery query() {
        if(lastSearchIdleTicks == client.getKeyboardIdleTicks()) {
            return new WidgetQuery(cachedWidgets);
        }

        HashSet<Widget> returnList = new HashSet<>();
        Widget[] currentQueue;
        ArrayList<Widget> buffer = new ArrayList<>();

        currentQueue = client.getWidgetRoots();

        while(currentQueue.length != 0) {
            for (Widget widget : currentQueue) {
                if (widget == null) {
                    continue;
                }

                returnList.add(widget);

                if (widget.getDynamicChildren() != null) {
                    for (Widget dynamicChild : widget.getDynamicChildren()) {
                        if (dynamicChild == null) {
                            continue;
                        }
                        buffer.add(dynamicChild);
                        returnList.add(dynamicChild);
                    }
                }

                if (widget.getNestedChildren() != null) {
                    for (Widget nestedChild : widget.getNestedChildren()) {
                        if (nestedChild == null) {
                            continue;
                        }
                        buffer.add(nestedChild);
                        returnList.add(nestedChild);
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
                        returnList.add(staticChild);
                    }
                }
            }

            currentQueue = buffer.toArray(new Widget[]{});
            buffer.clear();
        }
        lastSearchIdleTicks = client.getKeyboardIdleTicks();
        cachedWidgets = returnList;
        return new WidgetQuery(cachedWidgets);
    }
}
