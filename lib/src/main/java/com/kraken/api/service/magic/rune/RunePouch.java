package com.kraken.api.service.magic.rune;

import com.kraken.api.Context;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.EnumComposition;
import net.runelite.api.EnumID;
import net.runelite.api.ItemComposition;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.RuneLite;
import net.runelite.client.game.ItemManager;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
public enum RunePouch {
    RUNE_POUCH(ItemID.BH_RUNE_POUCH),
    DIVINE_RUNE_POUCH(ItemID.DIVINE_RUNE_POUCH, true),
    DIVINE_RUNE_POUCH_L(ItemID.DIVINE_RUNE_POUCH_TROUVER, true),
    RUNE_POUCH_L(ItemID.BH_RUNE_POUCH_TROUVER),
    RUNE_POUCH_LMS(ItemID.BR_RUNE_REPLACEMENT);

    @Getter
    private final int itemId;

    @Getter
    private final boolean has4Slots;

    private static final Context ctx = RuneLite.getInjector().getInstance(Context.class);
    private static final ItemManager itemManager = RuneLite.getInjector().getInstance(ItemManager.class);

    private static final int[] RUNE_VARBITS = {
            VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4,
            VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6
    };

    private static final int[] AMOUNT_VARBITS = {
            VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4,
            VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6
    };

    RunePouch(int itemId) {
        this.itemId = itemId;
        this.has4Slots = false;
    }

    /**
     * Retrieves the {@code RunePouch} instance corresponding to a rune pouch currently present
     * in the player's inventory. This checks all available rune pouch variants and returns the
     * matching instance if any are found.
     *
     * @return The {@code RunePouch} corresponding to a pouch found in the player's inventory,
     *         or {@code null} if no matching pouch exists.
     */
    public static RunePouch getRunePouch() {
        List<Integer> pouchIds = Arrays.stream(RunePouch.values()).map(RunePouch::getItemId).collect(Collectors.toList());

        for(int pouchId : pouchIds) {
            if(ctx.inventory().hasItem(pouchId)) {
                return RunePouch.byItemId(pouchId);
            }
        }

        return null;
    }

    public static boolean hasRunePouch() {
        return RunePouch.getRunePouch() != null;
    }

    /**
     * Retrieves the {@literal @code RunePouch} instance corresponding to the specified item ID.
     * This method searches through all defined {@literal @code RunePouch} values and returns the
     * matching instance if the provided item ID corresponds to a known Rune Pouch.
     *
     * <p>Returns {@literal @code null} if no {@literal @code RunePouch} with the given item ID is found.</p>
     *
     * @param itemId The item ID of the Rune Pouch to locate.
     *               This should be an integer representing a valid item ID of a Rune Pouch.
     *               For example, these may include IDs for standard, divine, or decorative Rune Pouches.
     *
     * @return The {@literal @code RunePouch} instance matching the given item ID,
     *         or {@literal @code null} if no match is found.
     */
    public static RunePouch byItemId(int itemId) {
        return Arrays.stream(values()).filter(v -> v.getItemId() == itemId)
                .findFirst()
                .orElse(null);
    }

    /**
     * Retrieves the base contents of the rune pouch, converting any combination runes into their
     * respective basic elemental runes.
     *
     * <p>This method first obtains the current contents of the rune pouch through
     * {@link RunePouch#getRunePouchContents()}. If any combination runes are present in the pouch,
     * they are decomposed into their corresponding base runes (e.g., a Dust Rune is split into both
     * Air and Earth Runes). The output is a flattened mapping containing all base runes and their
     * counts, ensuring that combination runes are fully represented by their components.</p>
     *
     * <p>Note: The quantity of each base rune will match the total quantity of the combination rune
     * that produces it.</p>
     *
     * <ul>
     * <li>If the rune pouch contains only non-combination runes, the result will be identical to the
     * original mapping from {@link RunePouch#getRunePouchContents()}.</li>
     * <li>If the rune pouch contains a combination rune, all its base components will be added to the
     * returned map with identical counts.</li>
     * <li>If the rune pouch is empty or the player does not have a rune pouch, an empty map is returned.</li>
     * </ul>
     *
     * @return A {@code Map<Rune, Integer>} where:
     * <ul>
     * <li>The key is a {@link Rune} instance representing a base rune included in the pouch contents.</li>
     * <li>The value is an {@code Integer} representing the quantity of that rune.</li>
     * </ul>
     * Combination runes are decomposed into their base components, and the returned map
     * includes the quantities of these base components.
     */
    public static Map<Rune, Integer> getBaseRunePouchContents() {
        Map<Rune, Integer> pouchRunes = RunePouch.getRunePouchContents();
        Map<Rune, Integer> flattenedRunes = new HashMap<>(); // Combo runes flattened into their base variants i.e. dust -> air and earth runes

        for(Map.Entry<Rune, Integer> entry : pouchRunes.entrySet()) {
            Rune potentialCombinationRune = entry.getKey();
            if (potentialCombinationRune != null && potentialCombinationRune.isComboRune()) {
                // If the rune is a combo rune, add all base runes to the combined runes
                for (Rune baseRune : potentialCombinationRune.getBaseRunes()) {
                    flattenedRunes.merge(baseRune, entry.getValue(), Integer::sum);
                }
                continue;
            }
            flattenedRunes.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }

        return flattenedRunes;
    }


    /**
     * Retrieves the current contents of the rune pouch if one is present in the player's inventory.
     * <p>
     * The rune pouch can hold up to three types of runes, each represented by a {@link Rune}
     * object and its corresponding count. This method checks if a rune pouch is available,
     * determines the runes contained within it, and returns a mapping of runes to their quantities.
     * </p>
     *
     * <ul>
     * <li>If no rune pouch is found in the player's inventory, an empty map is returned.</li>
     * <li>If a rune pouch exists, the method queries the item definitions and in-game variables
     * (varbits) to determine the specific runes and their counts.</li>
     * </ul>
     *
     * @return A {@code Map<Rune, Integer>} where:
     * <ul>
     * <li>The key is a {@link Rune} instance representing a type of rune stored in the pouch.</li>
     * <li>The value is an {@code Integer} representing the quantity of that rune.</li>
     * </ul>
     * Returns an empty map if no rune pouch is present or if the pouch contains no runes.
     */
    public static Map<Rune, Integer> getRunePouchContents() {
        Map<Rune, Integer> pouchRunes = new HashMap<>();

        // No rune pouch in inventory and thus no contents in the rune pouch.
        if(!RunePouch.hasRunePouch()) return pouchRunes;

        final EnumComposition runePouchEnum = ctx.getEnum(EnumID.RUNEPOUCH_RUNE);

        for (int i = 0; i < 6; i++) {
            @Varbit
            int amountVarbit = AMOUNT_VARBITS[i];
            int amount = ctx.getVarbitValue(amountVarbit);

            if(amount > 0) {
                @Varbit
                int runeVarbit = RUNE_VARBITS[i];
                int runeId = ctx.getVarbitValue(runeVarbit);

                if(runeId > 0) {
                    ItemComposition rune = ctx.runOnClientThread(() -> itemManager.getItemComposition(runePouchEnum.getIntValue(runeId)));
                    Rune r = Rune.byItemId(rune.getId());
                    if(r != null) pouchRunes.put(r, amount);
                }
            }
        }

        return pouchRunes;
    }
}
