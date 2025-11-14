package com.kraken.api.interaction.spells;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import com.kraken.api.interaction.container.inventory.ContainerItem;
import com.kraken.api.interaction.container.inventory.InventoryService;
import com.kraken.api.interaction.reflect.ReflectionService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.game.ItemManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Singleton
public class SpellService extends AbstractService {

    @Inject
    private InventoryService inventoryService;

    @Inject
    private ItemManager itemManager;

    @Inject
    private ReflectionService reflectionService;

    private static final List<Integer> SPELLS_REQUIRING_PRAYER = List.of(
            14287032,
            14287033,
            14287034,
            14287035,
            14287036,
            14287037,
            14287038,
            14287039,
            14287040
    );

    private static final int[] RUNE_VARBITS = {
            VarbitID.RUNE_POUCH_TYPE_1, VarbitID.RUNE_POUCH_TYPE_2, VarbitID.RUNE_POUCH_TYPE_3, VarbitID.RUNE_POUCH_TYPE_4,
            VarbitID.RUNE_POUCH_TYPE_5, VarbitID.RUNE_POUCH_TYPE_6
    };

    private static final int[] AMOUNT_VARBITS = {
            VarbitID.RUNE_POUCH_QUANTITY_1, VarbitID.RUNE_POUCH_QUANTITY_2, VarbitID.RUNE_POUCH_QUANTITY_3, VarbitID.RUNE_POUCH_QUANTITY_4,
            VarbitID.RUNE_POUCH_QUANTITY_5, VarbitID.RUNE_POUCH_QUANTITY_6
    };

    /**
     * Clicks on a spell in the spellbook. This method checks the following conditions before casting a spell:
     * 1. Player is on the right spellbook
     * 2. Player has the magic level required to cast the spell
     * 3. Player has the required runes to cast the spell
     * 4. Player has the required prayer to cast the spell (if applicable)
     *
     * @param spell The spell to be clicked.
     * @return True if the cast was successful and false otherwise
     */
    public boolean cast(Spells spell) {
        if(!context.isHooksLoaded()) return false;
        if(spell == null) {
            return false;
        }

        if(getCurrentSpellbook() != spell.getSpellbook()) {
            log.warn("Cannot cast spell {}. Wrong spellbook: {}", spell, getCurrentSpellbook());
            return false;
        }

        if (client.getBoostedSkillLevel(Skill.MAGIC) < spell.getRequiredLevel() || client.getRealSkillLevel(Skill.MAGIC) < spell.getRequiredLevel()) {
            log.warn("Cannot cast spell {}. Required magic level: {}, current level: {}", spell.getName(), spell.getRequiredLevel(), client.getBoostedSkillLevel(Skill.MAGIC));
            return false;
        }

        if (!hasRequiredRunes(spell)) {
            log.warn("Cannot cast spell {}. Missing required runes: {}", spell.getName(), spell.getRequiredRunes());
            return false;
        }

        if(SPELLS_REQUIRING_PRAYER.contains(spell.getWidgetId()) && client.getBoostedSkillLevel(Skill.PRAYER) < 6) {
            log.warn("Cannot cast spell {}. Required prayer points: 6, current level: {}", spell.getName(), client.getBoostedSkillLevel(Skill.PRAYER));
            return false;
        }

        reflectionService.invokeMenuAction(-1, spell.getWidgetId(), MenuAction.CC_OP.getId(), 1, -1);
        return true;
    }

    /**
     * Returns true if the player has the required runes to cast the given spell.
     * This method checks both the inventory and the rune pouch for the required runes.
     * @param spell The spell to check for required runes.
     * @return true if the player has all required runes, false otherwise.
     */
    public boolean hasRequiredRunes(Spells spell) {
        boolean hasRunePouch = inventoryService.all().stream().anyMatch(item -> item.getId() == ItemID.DIVINE_RUNE_POUCH
                || item.getId() == ItemID.BH_RUNE_POUCH
                || item.getId() == ItemID.BH_RUNE_POUCH_TROUVER
                || item.getId() == ItemID.DIVINE_RUNE_POUCH_TROUVER);

        // Get combined runes from inventory and rune pouch
        Map<Integer, Integer> availableRunes = getCombinedRuneInventory(hasRunePouch);

        // Check if we have enough of each required rune
        for(Map.Entry<Runes, Integer> entry : spell.getRequiredRunes().entrySet()) {
            int runeItemId = entry.getKey().getItemId();
            int requiredAmount = entry.getValue();
            int availableAmount = availableRunes.getOrDefault(runeItemId, 0);

            if(availableAmount < requiredAmount) {
                log.info("Insufficient runes for spell {}. Rune: {}, Required: {}, Available: {}",
                        spell.getName(), entry.getKey(), requiredAmount, availableAmount);
                return false;
            }
        }

        return true;
    }

    /**
     * Gets a combined map of all available runes from both inventory and rune pouch
     * @param hasRunePouch whether the player has a rune pouch
     * @return Map of rune item ID to total quantity available
     */
    private Map<Integer, Integer> getCombinedRuneInventory(boolean hasRunePouch) {
        Map<Integer, Integer> combinedRunes = new HashMap<>();

        // Add runes from inventory
        for(ContainerItem item : inventoryService.all()) {
            Runes rune = Runes.byItemId(item.getId());
            if(rune != null) {
                // If the rune is a combo rune add all base runes to the combined runes because it functions as any of the base runes.
                if(rune.isComboRune()) {
                    for(Runes baseRune : rune.getBaseRunes()) {
                        combinedRunes.merge(baseRune.getItemId(), item.getQuantity(), Integer::sum);
                    }
                }

                combinedRunes.merge(item.getId(), item.getQuantity(), Integer::sum);
            }
        }

        // Add runes from rune pouch if available
        if(hasRunePouch) {
            Map<Integer, Integer> pouchRunes = getRunePouchContents();
            for(Map.Entry<Integer, Integer> entry : pouchRunes.entrySet()) {
                Runes potentialCombinationRune = Runes.byItemId(entry.getKey());
                if(potentialCombinationRune != null && potentialCombinationRune.isComboRune()) {
                    // If the rune is a combo rune, add all base runes to the combined runes
                    for(Runes baseRune : potentialCombinationRune.getBaseRunes()) {
                        combinedRunes.merge(baseRune.getItemId(), entry.getValue(), Integer::sum);
                    }
                    continue;
                }

                combinedRunes.merge(entry.getKey(), entry.getValue(), Integer::sum);
            }
        }

        return combinedRunes;
    }

    /**
     * Gets the contents of the rune pouch
     * @return Map of rune item ID to quantity in the pouch
     */
    private Map<Integer, Integer> getRunePouchContents() {
        Map<Integer, Integer> pouchRunes = new HashMap<>();

        final EnumComposition runepouchEnum = client.getEnum(EnumID.RUNEPOUCH_RUNE);

        for (int i = 0; i < 6; i++) {
            @Varbit
            int amountVarbit = AMOUNT_VARBITS[i];
            int amount = client.getVarbitValue(amountVarbit);

            if(amount > 0) {
                @Varbit
                int runeVarbit = RUNE_VARBITS[i];
                int runeId = client.getVarbitValue(runeVarbit);

                if(runeId > 0) {
                    // Get the actual item ID for this rune type
                    ItemComposition rune = itemManager.getItemComposition(runepouchEnum.getIntValue(runeId));
                    pouchRunes.put(rune.getId(), amount);
                }
            }
        }

        return pouchRunes;
    }


    /**
     * Get the current active spellbook
     *
     * @return The currently active Spellbook
     */
    public Spellbook getCurrentSpellbook() {
        return Spellbook.fromValue(context.getVarbitValue(VarbitID.SPELLBOOK));
    }
}
