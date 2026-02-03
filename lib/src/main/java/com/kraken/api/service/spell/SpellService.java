package com.kraken.api.service.spell;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.ui.UIService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.game.ItemManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class SpellService {

    @Inject
    private Context ctx;

    @Inject
    private ItemManager itemManager;

    @Inject
    private WidgetPackets widgetPackets;

    @Inject
    private MousePackets mousePackets;

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
     * Determines whether the specified spell can be cast by the player.
     *
     * <p>This method verifies various conditions required to cast a spell, including:
     * <ul>
     *   <li>Whether the client packets are properly loaded</li>
     *   <li>Whether the spell is valid and belongs to the player's current spellbook</li>
     *   <li>Whether the player's boosted Magic level meets or exceeds the spell's required level</li>
     *   <li>Whether the player possesses the necessary runes to cast the spell</li>
     *   <li>For specific spells requiring prayer, whether the player has sufficient Prayer points</li>
     * </ul>
     * If any of these conditions fail, the method logs a warning and returns {@literal @false}.
     *
     * @param spell The {@literal @Spells} instance representing the spell to check.
     *              <ul>
     *                <li>Must not be {@literal @null}.</li>
     *                <li>The spell must belong to the player's active spellbook to be castable.</li>
     *              </ul>
     * @return {@literal @true} if all conditions for casting the spell are met, {@literal @false} otherwise.
     *         <p>Returns {@literal @false} for invalid spells, mismatched spellbooks, insufficient Magic level,
     *         missing runes, or insufficient Prayer points for certain spells.</p>
     */
    private boolean canCast(Spells spell) {
        if (!ctx.isPacketsLoaded()) return false;
        if (spell == null) return false;

        if (getCurrentSpellbook() != spell.getSpellbook()) {
            log.warn("Cannot cast spell {}. Wrong spellbook: {}", spell, getCurrentSpellbook());
            return false;
        }

        int boostedLevel = ctx.getClient().getBoostedSkillLevel(Skill.MAGIC);
        if (boostedLevel < spell.getRequiredLevel()) {
            log.warn("Cannot cast spell {}. Required magic level: {}, current level: {}",
                    spell.getName(), spell.getRequiredLevel(), boostedLevel);
            return false;
        }

        if (!hasRequiredRunes(spell)) {
            log.warn("Cannot cast spell {}. Missing required runes: {}", spell.getName(), spell.getRequiredRunes());
            return false;
        }

        if (SPELLS_REQUIRING_PRAYER.contains(spell.getWidgetId()) && ctx.getClient().getBoostedSkillLevel(Skill.PRAYER) < 6) {
            log.warn("Cannot cast spell {}. Required prayer points: 6, current level: {}",
                    spell.getName(), ctx.getClient().getBoostedSkillLevel(Skill.PRAYER));
            return false;
        }

        return true;
    }

    /**
     * Helper to get the spell widget if it exists.
     */
    private WidgetEntity getSpellWidget(Spells spell) {
        WidgetEntity w = ctx.widgets().fromClient(spell.getWidgetId());
        if (w == null) {
            log.info("Cannot cast spell {}. Missing widget: {}", spell.getName(), spell.getWidgetId());
            return null;
        }
        return w;
    }

    /**
     * Casts the specified spell, if it is valid and the necessary conditions are met.
     * <p>
     * This method validates whether the spell can be cast, determines the appropriate
     * action (especially for teleport spell variants), and enqueues the required packets
     * to perform the spell cast.
     *
     * <p>
     * Note that this method handles teleport spells with multiple variants (e.g.,
     * Varrock Teleport vs. Grand Exchange Teleport) and calculates the correct action
     * based on the player's configuration.
     *
     * @param spell The {@literal @Spells} instance representing the spell to be cast.
     *              Must not be null.
     *              <ul>
     *                 <li>For teleport spells with variants, the variant action will
     *                     be determined dynamically.</li>
     *                 <li>Ensure the correct spell is passed to avoid unintended behavior.</li>
     *              </ul>
     * @return {@literal @true} if the spell was successfully cast, {@literal @false} otherwise.
     *         <p>Returns {@literal @false} if the spell is invalid, cannot be cast, or
     *         if required conditions (e.g., runes) are not met.</p>
     */
    public boolean cast(Spells spell) {
        if (!canCast(spell)) return false;

        WidgetEntity w = getSpellWidget(spell);
        if (w == null) return false;

        // Queue Mouse Click
        Point pt = UIService.getClickingPoint(w.raw().getBounds(), true);
        mousePackets.queueClickPacket(pt.getX(), pt.getY());

        // Calculate Action (Handling Teleport variants)
        int action = 1;
        if (spell == Spells.VARROCK_TELEPORT || spell == Spells.GRAND_EXCHANGE_TELEPORT) {
            action = getVariantAction(VarbitID.VARROCK_GE_TELEPORT, spell, Spells.VARROCK_TELEPORT, Spells.GRAND_EXCHANGE_TELEPORT);
        } else if (spell == Spells.CAMELOT_TELEPORT || spell == Spells.SEERS_TELEPORT) {
            action = getVariantAction(VarbitID.SEERS_CAMELOT_TELEPORT, spell, Spells.CAMELOT_TELEPORT, Spells.SEERS_TELEPORT);
        } else if (spell == Spells.WATCHTOWER_TELEPORT || spell == Spells.YANILLE_TELEPORT) {
            action = getVariantAction(VarbitID.YANILLE_TELEPORT_LOCATION, spell, Spells.WATCHTOWER_TELEPORT, Spells.YANILLE_TELEPORT);
        }

        widgetPackets.queueWidgetActionPacket(spell.getWidgetId(), -1, -1, action);
        return true;
    }

    /**
     * Attempts to cast the given spell on a specified widget target.
     * <p>
     * This method verifies if the spell can be cast by invoking the {@code canCast} method.
     * If the spell is valid and the required conditions are met (e.g., the spell exists in the active spellbook, the player has the necessary items, levels, etc.),
     * it retrieves the spell's widget representation and performs the "use on" action to cast the spell on the target.
     * </p>
     *
     * @param spell The {@literal @Spells} instance representing the spell to cast.
     *              <ul>
     *                <li>Must not be {@literal @null}.</li>
     *                <li>Should belong to the player's current spellbook and satisfy all requirements for casting.</li>
     *              </ul>
     * @param target The {@literal @Widget} instance representing the target of the spell.
     *               <ul>
     *                 <li>Must not be {@literal @null}.</li>
     *                 <li>The widget should correspond to a valid in-game target for the selected spell.</li>
     *               </ul>
     * @return {@literal @true} if the spell was successfully cast on the target, {@literal @false} otherwise.
     *         <p>Returns {@literal @false} in cases where the spell is invalid, conditions necessary for casting are not met,
     *         or the target widget is not valid or accessible.</p>
     */
    public boolean castOn(Spells spell, Widget target) {
        if (!canCast(spell)) return false;

        WidgetEntity w = getSpellWidget(spell);
        if (w == null) return false;

        w.useOn(target);
        return true;
    }

    /**
     * Attempts to cast the given spell on a specified NPC target.
     *
     * <p>This method checks whether the spell can be cast by invoking the {@code canCast} method.
     * If the spell is valid and all necessary conditions for casting (e.g., current spellbook, required runes, etc.)
     * are satisfied, it retrieves the spell's corresponding widget and performs the "use-on" action to cast the
     * spell on the NPC target.</p>
     *
     * @param spell The {@literal @Spells} instance representing the spell to cast.
     *              <ul>
     *                <li>Must not be {@literal @null}.</li>
     *                <li>The spell should exist in the player's current spellbook.</li>
     *                <li>The spell must meet all prerequisites for casting, including level and resource requirements.</li>
     *              </ul>
     * @param target The {@literal @NPC} instance representing the target of the spell.
     *               <ul>
     *                 <li>Must not be {@literal @null}.</li>
     *                 <li>The NPC must be a valid target for the selected spell.</li>
     *               </ul>
     *
     * @return {@literal @true} if the spell was successfully cast on the NPC target, {@literal @false} otherwise.
     *         <p>Returns {@literal @false} if the spell is invalid, the conditions for casting are not met, the spell's widget
     *         cannot be retrieved, or the NPC is not a valid target.</p>
     */
    public boolean castOn(Spells spell, NPC target) {
        if (!canCast(spell)) return false;

        WidgetEntity w = getSpellWidget(spell);
        if (w == null) return false;

        w.useOn(target);
        return true;
    }

    /**
     * Casts a spell on a specified target object.
     * <p>
     * This method checks if the specified spell can be cast, retrieves the widget
     * associated with the spell, and then uses it on the provided target object.
     *
     * @param spell The {@literal @}Spells object representing the spell to be cast.
     * @param target The {@literal @}GameObject on which the spell will be cast.
     * @return {@code true} if the spell was successfully cast on the target,
     *         {@code false} otherwise.
     */
    public boolean castOn(Spells spell, GameObject target) {
        if (!canCast(spell)) return false;

        WidgetEntity w = getSpellWidget(spell);
        if (w == null) return false;

        w.useOn(target);
        return true;
    }

    /**
     * Returns the action for spells which have double teleport actions after certain
     * unlocks. i.e. Medium diaries unlock the ability to teleport to the GE with the Varrock teleport.
     * When the spell cast is GE then this must return the correct action for the GE teleport not the Varrock Teleport.
     * @param varbit The varbit denoting the action configuration
     * @param spell The spell being cast by the developer/player
     * @param baseSpell The base spell i.e. Varrock
     * @param variantSpell The variant spell i.e. Grand Exchange
     * @return The action number to send in the widget packet
     */
    private int getVariantAction(int varbit, Spells spell, Spells baseSpell, Spells variantSpell) {
        var config = ctx.getVarbitValue(varbit);
        if (config == 0) {
            return spell == baseSpell ? 1 : 2;
        }
        return spell == variantSpell ? 3 : 2;
    }

    /**
     * Returns true if the player has the required runes to cast the given spell.
     * This method checks both the inventory and the rune pouch for the required runes.
     * @param spell The spell to check for required runes.
     * @return true if the player has all required runes, false otherwise.
     */
    public boolean hasRequiredRunes(Spells spell) {
        boolean hasRunePouch = ctx.inventory().stream().filter(Objects::nonNull).anyMatch(item -> item.raw().getId() == ItemID.DIVINE_RUNE_POUCH
                || item.raw().getId() == ItemID.BH_RUNE_POUCH
                || item.raw().getId() == ItemID.BH_RUNE_POUCH_TROUVER
                || item.raw().getId() == ItemID.DIVINE_RUNE_POUCH_TROUVER);

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
        for(ContainerItem item : ctx.inventory().toRuneLite().collect(Collectors.toList())) {
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

        final EnumComposition runepouchEnum = ctx.getEnum(EnumID.RUNEPOUCH_RUNE);

        for (int i = 0; i < 6; i++) {
            @Varbit
            int amountVarbit = AMOUNT_VARBITS[i];
            int amount = ctx.getVarbitValue(amountVarbit);

            if(amount > 0) {
                @Varbit
                int runeVarbit = RUNE_VARBITS[i];
                int runeId = ctx.getVarbitValue(runeVarbit);

                if(runeId > 0) {
                    // Get the actual item ID for this rune type
                    ItemComposition rune = ctx.runOnClientThread(() -> itemManager.getItemComposition(runepouchEnum.getIntValue(runeId)));
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
        return Spellbook.fromValue(ctx.getVarbitValue(VarbitID.SPELLBOOK));
    }
}
