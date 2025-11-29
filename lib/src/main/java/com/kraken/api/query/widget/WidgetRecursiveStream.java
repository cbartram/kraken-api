package com.kraken.api.query.widget;

import net.runelite.api.widgets.Widget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class WidgetRecursiveStream {

    public static Stream<Widget> flatten(Widget root) {
        return Stream.concat(
                Stream.of(root),
                getChildren(root).flatMap(WidgetRecursiveStream::flatten)
        );
    }

    private static Stream<Widget> getChildren(Widget parent) {
        if (parent == null) return Stream.empty();

        List<Widget[]> allChildren = new ArrayList<>();
        if (parent.getChildren() != null) allChildren.add(parent.getChildren());
        if (parent.getNestedChildren() != null) allChildren.add(parent.getNestedChildren());
        if (parent.getDynamicChildren() != null) allChildren.add(parent.getDynamicChildren());
        if (parent.getStaticChildren() != null) allChildren.add(parent.getStaticChildren());

        return allChildren.stream()
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull);
    }
}