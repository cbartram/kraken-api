package com.kraken.api.query.container.inventory;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Comparator;

@Getter
@AllArgsConstructor
public enum InventoryOrder {
    /**
     * Standard reading order:
     * Row 1 (Left{@literal ->}Right), Row 2 (Left{@literal ->}Right)...
     */
    TOP_LEFT_BOTTOM_RIGHT(Comparator.comparingInt(e -> e.raw().getSlot())),

    /**
     * Reverse reading order:
     * Last Item {@literal ->} First Item.
     */
    BOTTOM_RIGHT_TOP_LEFT(Comparator.comparingInt(e -> e.raw().getSlot())),

    /**
     * Snake/Zig-Zag pattern:
     * Row 1 (Left{@literal ->}Right), Row 2 (Right{@literal ->}Left), Row 3 (Left{@literal ->}Right)...
     * Reduces mouse travel distance significantly.
     */
    ZIG_ZAG(Comparator.comparingInt(e -> {
        int index = e.raw().getSlot();
        int row = index / 4;
        int col = index % 4;

        // If row is odd (1, 3, 5), invert the column order
        if (row % 2 != 0) {
            col = 3 - col;
        }

        // Reconstruct a "virtual" index for sorting
        return (row * 4) + col;
    })),

    /**
     * Reverse Snake/Zig-Zag pattern starting from the bottom right.
     */
    ZIG_ZAG_REVERSE(ZIG_ZAG.getComparator().reversed()),

    /**
     * Vertical columns:
     * Col 1 (Top{@literal ->}Bottom), Col 2 (Top{@literal ->}Bottom)...
     */
    TOP_DOWN_LEFT_RIGHT(Comparator.comparingInt(e -> {
        int index = e.raw().getSlot();
        int row = index / 4;
        int col = index % 4;

        // Weight column heavily so it takes precedence over row
        return (col * 100) + row;
    }));

    private final Comparator<InventoryEntity> comparator;
}