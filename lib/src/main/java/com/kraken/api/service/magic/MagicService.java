package com.kraken.api.service.magic;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.query.container.ContainerItem;
import com.kraken.api.query.widget.WidgetEntity;
import com.kraken.api.service.magic.rune.Rune;
import com.kraken.api.service.magic.rune.RunePouch;
import com.kraken.api.service.magic.spellbook.Spellbook;
import com.kraken.api.service.ui.UIService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.Skill;
import net.runelite.api.widgets.Widget;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class MagicService {

    @Inject
    private Context ctx;

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

    /**
     * Determines whether the specified spell can be cast by the player.
     *
     * <p>This method verifies various conditions required to cast a spell, including:
     * <ul>
     *   <li>Whether the client packets are properly loaded</li>
     *   <li>Whether the spell is valid and belongs to the player's current spellbook</li>
     *   <li>Whether the player's boosted Magic level meets or exceeds the spell's required level</li>
     *   <li>Whether the player possesses the necessary runes to cast the spell</li>
     *   <li>For specific CastableSpell requiring prayer, whether the player has sufficient Prayer points</li>
     * </ul>
     * If any of these conditions fail, the method logs a warning and returns {@literal @false}.
     *
     * @param spell The {@literal @CastableSpell} instance representing the spell to check.
     *              <ul>
     *                <li>Must not be {@literal @null}.</li>
     *                <li>The spell must belong to the player's active spellbook to be castable.</li>
     *              </ul>
     * @return {@literal @true} if all conditions for casting the spell are met, {@literal @false} otherwise.
     *         <p>Returns {@literal @false} for invalid CastableSpell, mismatched spellbooks, insufficient Magic level,
     *         missing runes, or insufficient Prayer points for certain CastableSpell.</p>
     */
    public boolean canCast(CastableSpell spell) {
        if (!ctx.isPacketsLoaded()) return false;
        if (spell == null) return false;

        if (Spellbook.getCurrentSpellbook() != spell.getSpellbook()) {
            log.warn("Cannot cast spell {}. Wrong spellbook: {}", spell, Spellbook.getCurrentSpellbook());
            return false;
        }

        int boostedLevel = ctx.getClient().getBoostedSkillLevel(Skill.MAGIC);
        if (boostedLevel < spell.getLevel()) {
            log.warn("Cannot cast spell {}. Required magic level: {}, current level: {}",
                    spell.getName(), spell.getLevel(), boostedLevel);
            return false;
        }

        if (!hasRequiredRunes(spell)) {
            log.warn("Cannot cast spell {}. Missing required runes: {}", spell.getName(), spell.getRuneRequirement());
            return false;
        }

        if (SPELLS_REQUIRING_PRAYER.contains(spell.getWidget()) && ctx.getClient().getBoostedSkillLevel(Skill.PRAYER) < 6) {
            log.warn("Cannot cast spell {}. Required prayer points: 6, current prayer: {}", spell.getName(), ctx.getClient().getBoostedSkillLevel(Skill.PRAYER));
            return false;
        }

        return true;
    }

    /**
     * Helper to get the spell widget if it exists.
     */
    private WidgetEntity getSpellWidget(CastableSpell spell) {
        WidgetEntity w = ctx.widgets().fromClient(spell.getWidget());
        if (w == null) {
            log.error("Cannot cast spell {}. Missing widget: {}", spell.getName(), spell.getWidget());
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
     * Note that this method handles teleport CastableSpell with multiple variants (e.g.,
     * Varrock Teleport vs. Grand Exchange Teleport) and calculates the correct action
     * based on the player's configuration.
     *
     * @param spell The {@literal @CastableSpell} instance representing the spell to be cast.
     *              Must not be null.
     *              <ul>
     *                 <li>For teleport CastableSpell with variants, the variant action will
     *                     be determined dynamically.</li>
     *                 <li>Ensure the correct spell is passed to avoid unintended behavior.</li>
     *              </ul>
     * @return {@literal @true} if the spell was successfully cast, {@literal @false} otherwise.
     *         <p>Returns {@literal @false} if the spell is invalid, cannot be cast, or
     *         if required conditions (e.g., runes) are not met.</p>
     */
    public boolean cast(CastableSpell spell) {
        if (!canCast(spell)) return false;

        WidgetEntity w = getSpellWidget(spell);
        if (w == null) return false;

        Point pt = UIService.getClickingPoint(w.raw().getBounds(), true);
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetActionPacket(spell.getWidget(), -1, -1, spell.getAction());
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
     * @param spell The {@literal @CastableSpell} instance representing the spell to cast.
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
    public boolean castOn(CastableSpell spell, Widget target) {
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
     * @param spell The {@literal @CastableSpell} instance representing the spell to cast.
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
    public boolean castOn(CastableSpell spell, NPC target) {
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
     * @param spell The {@literal @}CastableSpell object representing the spell to be cast.
     * @param target The {@literal @}GameObject on which the spell will be cast.
     * @return {@code true} if the spell was successfully cast on the target,
     *         {@code false} otherwise.
     */
    public boolean castOn(CastableSpell spell, GameObject target) {
        if (!canCast(spell)) return false;

        WidgetEntity w = getSpellWidget(spell);
        if (w == null) return false;

        w.useOn(target);
        return true;
    }

    /**
     * Checks if the player has the required runes to cast a given spell.
     * <p>
     * This method verifies the rune requirements for the provided spell by considering:
     * <ul>
     * <li>Runes available in the base rune pouch.</li>
     * <li>Runes available in the player's inventory, accounting for combination runes that can act
     * as substitutes for their base elemental runes.</li>
     * </ul>
     * If the rune requirements are met and the spell is deemed castable by the client, the method
     * will return {@literal true}.
     *
     * @param spell The {@code CastableSpell} representing the spell to check. Contains information
     *              about the rune requirements and castability.
     * @return {@code true} if the player has the necessary runes and the spell is castable; {@code false} otherwise.
     */
    public boolean hasRequiredRunes(CastableSpell spell) {
        Map<Rune, Integer> runes = RunePouch.getBaseRunePouchContents();

        for(ContainerItem item : ctx.inventory().toRuneLite().collect(Collectors.toList())) {
            Rune rune = Rune.byItemId(item.getId());
            if(rune != null) {
                // If the rune is a combo rune add all base runes to the combined runes because it functions as any of the base runes.
                if(rune.isComboRune()) {
                    for(Rune baseRune : rune.getBaseRunes()) {
                        runes.merge(baseRune, item.getQuantity(), Integer::sum);
                    }
                }

                runes.merge(rune, item.getQuantity(), Integer::sum);
            }
        }

        // Check if we have enough of each required rune
        for(Map.Entry<Rune, Integer> entry : spell.getRuneRequirement().entrySet()) {
            Rune runeRequired = entry.getKey();
            int requiredAmount = entry.getValue();

            int availableAmount = runes.getOrDefault(runeRequired, 0);
            if(availableAmount < requiredAmount) {
                log.info("Insufficient runes for spell {}. Rune: {}, Required: {}, Available: {}", spell.getName(), entry.getKey(), requiredAmount, availableAmount);
                return false;
            }
        }

        // Finally, check that the client also thinks that the spell is castable
        return spell.isCastable();
    }
}
