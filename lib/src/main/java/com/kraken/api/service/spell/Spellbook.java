package com.kraken.api.service.spell;

import lombok.Getter;

@Getter
public enum Spellbook {

    /**
     * Standard/Modern Spellbook - Default spellbook available to all players
     */
    STANDARD(0, "Standard spellbook - accessible from any other spellbook altar"),

    /**
     * Ancient Magicks Spellbook - Unlocked after Desert Treasure I quest
     */
    ANCIENT(1, "Ancient Pyramid altar - pray to switch between Ancient Magicks and Standard spellbook"),

    /**
     * Lunar Spellbook - Unlocked after Lunar Diplomacy quest
     */
    LUNAR(2, "Astral Altar on Lunar Isle - pray to switch to Lunar spellbook or back to Standard"),

    /**
     * Arceuus Spellbook - No quest requirement, accessible to all players
     */
    ARCEUUS(3, "Dark Altar north of Arceuus - speak to Tyss to access Arceuus spellbook");

    private final int value;
    private final String description;

    /**
     * Constructor for Rs2Spellbook enum
     */
    Spellbook(int value,  String description) {
        this.value = value;
        this.description = description;
    }

    /**
     * Get the spellbook from its value
     *
     * @param value The numeric value of the spellbook
     * @return The corresponding Rs2Spellbook enum
     */
    public static Spellbook fromValue(int value) {
        for (Spellbook spellbook : Spellbook.values()) {
            if (spellbook.getValue() == value) {
                return spellbook;
            }
        }
        return STANDARD;
    }
}
