package com.kraken.api.service.magic.rune;

import lombok.Getter;
import net.runelite.api.gameval.ItemID;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
public enum Rune {
    AIR(1, ItemID.AIRRUNE),
    WATER(2, ItemID.WATERRUNE),
    EARTH(3, ItemID.EARTHRUNE),
    FIRE(4, ItemID.FIRERUNE),
    MIND(5, ItemID.MINDRUNE),
    CHAOS(6, ItemID.CHAOSRUNE),
    DEATH(7, ItemID.DEATHRUNE),
    BLOOD(8, ItemID.BLOODRUNE),
    COSMIC(9, ItemID.COSMICRUNE),
    NATURE(10, ItemID.NATURERUNE),
    LAW(11, ItemID.LAWRUNE),
    BODY(12, ItemID.BODYRUNE),
    SOUL(13, ItemID.SOULRUNE),
    ASTRAL(14, ItemID.ASTRALRUNE),
    MIST(15, ItemID.MISTRUNE, AIR, WATER),
    MUD(16, ItemID.MUDRUNE, WATER, EARTH),
    DUST(17, ItemID.DUSTRUNE, AIR, EARTH),
    LAVA(18, ItemID.LAVARUNE, EARTH, FIRE),
    STEAM(19, ItemID.STEAMRUNE, WATER, FIRE),
    SMOKE(20, ItemID.SMOKERUNE, AIR, FIRE),
    WRATH(21, ItemID.WRATHRUNE),
    SUNFIRE(22, ItemID.SUNFIRERUNE),
    AETHER(23, ItemID.AETHERRUNE, COSMIC, SOUL);

    private final int id;
    private final int itemId;
    private final Rune[] baseRunes;

    Rune(int id, int itemId, Rune... baseRunes) {
        this.id = id;
        this.itemId = itemId;
        this.baseRunes = baseRunes;
    }

    /**
     * Determines whether this {@code Rune} provides the specified {@code Rune}.
     * <p>
     * This method checks if the current {@code Rune} is equal to the provided {@code Rune},
     * or if the provided {@code Rune} is included in this {@code Rune}'s base runes.
     * Base runes represent the elemental components of a combo {@code Rune}.
     * </p>
     *
     * @param rune The {@code Rune} to check against this {@code Rune}.
     *             If {@code rune} is {@code null}, the method will return {@code false}.
     * @return {@code true} if the specified {@code Rune} is equal to this {@code Rune},
     *         or if it is contained within this {@code Rune}'s base runes; {@code false} otherwise.
     */
    public boolean providesRune(Rune rune) {
        if (this == rune) return true;
        return Arrays.stream(getBaseRunes()).anyMatch(baseRune -> rune == baseRune);
    }


    /**
     * Determines whether this {@code Rune} is a combo rune.
     * <p>
     * A combo rune is a type of rune that consists of multiple elemental
     * components, represented internally by an array of base runes. This
     * method checks if the {@code Rune} has any associated base runes.
     * </p>
     *
     * @return {@code true} if this {@code Rune} has one or more associated base runes,
     * indicating it is a combo rune; {@code false} otherwise.
     */
    public boolean isComboRune() {
        return getBaseRunes().length > 0;
    }


    /**
     * Retrieves the {@code Rune} associated with the specified item ID.
     * <p>
     * This method searches through all available {@code Rune} values and compares their associated item IDs
     * to the provided {@code itemId}. If a match is found, the corresponding {@code Rune} is returned.
     * Otherwise, {@code null} is returned if no matching {@code Rune} exists.
     * </p>
     *
     * @param itemId The item ID used to identify the corresponding {@code Rune}.
     *
     * @return The {@code Rune} associated with the specified {@code itemId}, or {@code null} if no match is found.
     */
    public static Rune byItemId(int itemId) {
        return Arrays.stream(values()).filter(v -> v.getItemId() == itemId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves an array of combo runes that can be derived from the specified base {@code Rune}.
     * <p>
     * A combo rune is defined as a rune that includes the specified base rune as one of its elemental components.
     * This method will return all applicable combo runes for the given input rune.
     * </p>
     *
     * @param rune The base {@code Rune} for which to find applicable combo runes.
     *             If the specified rune is not part of any combo rune, an empty array will be returned.
     *
     * @return An array of {@code Rune} representing all combo runes that include the provided base rune.
     *         If no combo runes are found for the given rune, an empty array is returned.
     */
    public static Rune[] getComboRunes(Rune rune) {
        return Arrays.stream(values())
                .collect(Collectors.toMap(Function.identity(), entry -> {
                    final Rune[] comboRunes = Arrays.stream(values())
                            .filter(r -> r.providesRune(entry))
                            .filter(r -> r != entry)
                            .toArray(Rune[]::new);
                    if (comboRunes.length == 0) return new Rune[0];
                    return comboRunes;
                })).getOrDefault(rune, new Rune[0]);
    }
}

