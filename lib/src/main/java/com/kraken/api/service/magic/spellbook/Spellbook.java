package com.kraken.api.service.magic.spellbook;

import com.kraken.api.Context;
import com.kraken.api.service.magic.CastableSpell;
import lombok.Getter;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.RuneLite;

@Getter
public enum Spellbook {

    STANDARD(0, 1982),
    ANCIENT(1, 1983),
    LUNAR(2, 1984),
    ARCEUUS(3, 1985);

    private final int value;
    private final int enumCompositionIndex;
    private CastableSpell[] spells;

    Spellbook(int value, int enumCompositionIndex) {
        this.value = value;
        this.enumCompositionIndex = enumCompositionIndex;
    }

    /**
     * Retrieves the array of spells associated with the current spellbook.
     *
     * <p>
     * Depending on the type of spellbook (e.g., {@code LUNAR}, {@code STANDARD},
     * {@code ANCIENT}, or {@code ARCEUUS}), this method initializes and returns
     * an array of spells belonging to that specific spellbook. If the spellbook
     * cannot be determined, an empty array is returned.
     * </p>
     *
     * <p>
     * If this method is called before the spells are initialized, it will populate
     * the corresponding spell array based on the spellbook type and cache the result
     * for subsequent calls.
     * </p>
     *
     * @return An array of {@link CastableSpell} objects representing the spells available
     *         in the current spellbook. If the spellbook is not recognized, an empty array
     *         of {@code CastableSpell} will be returned.
     */
    public CastableSpell[] getSpells() {
        if (spells == null) {
            switch (this) {
                case LUNAR:
                    this.spells = Lunar.values();
                    break;
                case STANDARD:
                    this.spells = Standard.values();
                    break;
                case ANCIENT:
                    this.spells = Ancient.values();
                    break;
                case ARCEUUS:
                    this.spells = Arceuus.values();
                    break;
                default:
                    this.spells = new CastableSpell[0];
            }
        }
        return spells;
    }

    public static boolean isOnStandardSpellbook() {
        return getCurrentSpellbook() == STANDARD;
    }

    public static boolean isOnAncientSpellbook() {
        return getCurrentSpellbook() == ANCIENT;
    }

    public static boolean isOnLunarSpellbook() {
        return getCurrentSpellbook() == LUNAR;
    }

    public static boolean isOnArceuusSpellbook() {
        return getCurrentSpellbook() == ARCEUUS;
    }

    /**
     * Retrieves the current spellbook being used in the game.
     * <p>
     * The method determines the active spellbook by checking the corresponding
     * game variable (varbit) value and matching it with the {@code value} of
     * the enum constants defined in {@code Spellbook}.
     * </p>
     * <p>
     * If no match is found, the {@code STANDARD} spellbook is returned by default.
     * </p>
     *
     * @return The active {@link Spellbook} instance, which represents the
     *         currently selected spellbook in the game. Defaults to {@code STANDARD}
     *         if no match is found.
     */
    public static Spellbook getCurrentSpellbook() {
        Context ctx = RuneLite.getInjector().getInstance(Context.class);
        int varbitValue = ctx.getVarbitValue(VarbitID.SPELLBOOK);

        for (Spellbook spellbook : Spellbook.values()) {
            if (spellbook.getValue() == varbitValue) {
                return spellbook;
            }
        }
        return STANDARD;
    }
}
