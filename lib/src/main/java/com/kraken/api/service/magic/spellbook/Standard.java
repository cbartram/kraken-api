package com.kraken.api.service.magic.spellbook;

import com.kraken.api.Context;
import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.rune.Rune;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.client.RuneLite;

import java.util.Map;

@Getter
public enum Standard implements CastableSpell {

    HOME_TELEPORT(
            0,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_STANDARD,
            0,
            Map.of()
    ),
    VARROCK_TELEPORT(
            25,
            InterfaceID.MagicSpellbook.VARROCK_TELEPORT,
            15,
            Map.of(Rune.AIR, 3, Rune.FIRE, 1, Rune.LAW, 1)
    ),
    GRAND_EXCHANGE_TELEPORT(
            25,
            InterfaceID.MagicSpellbook.VARROCK_TELEPORT,
            15,
            Map.of(Rune.AIR, 3, Rune.FIRE, 1, Rune.LAW, 1)
    ),
    LUMBRIDGE_TELEPORT(
            31,
            InterfaceID.MagicSpellbook.LUMBRIDGE_TELEPORT,
            17,
            Map.of(Rune.AIR, 3, Rune.EARTH, 1, Rune.LAW, 1)
    ),
    FALADOR_TELEPORT(
            37,
            InterfaceID.MagicSpellbook.FALADOR_TELEPORT,
            20,
            Map.of(Rune.AIR, 3, Rune.WATER, 1, Rune.LAW, 1)
    ),
    TELEPORT_TO_HOUSE(
            40,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_STANDARD,
            48,
            Map.of(Rune.AIR, 1, Rune.EARTH, 1, Rune.LAW, 1)
    ),
    CAMELOT_TELEPORT(
            45,
            InterfaceID.MagicSpellbook.CAMELOT_TELEPORT,
            25,
            Map.of(Rune.AIR, 5, Rune.LAW, 1)
    ),
    SEERS_TELEPORT(
            45,
            InterfaceID.MagicSpellbook.CAMELOT_TELEPORT,
            25,
            Map.of(Rune.AIR, 5, Rune.LAW, 1)
    ),
    ARDOUGNE_TELEPORT(
            51,
            InterfaceID.MagicSpellbook.ARDOUGNE_TELEPORT,
            30,
            Map.of(Rune.WATER, 2, Rune.LAW, 2)
    ),
    WATCHTOWER_TELEPORT(
            58,
            InterfaceID.MagicSpellbook.WATCHTOWER_TELEPORT,
            34,
            Map.of(Rune.EARTH, 2, Rune.LAW, 2)
    ),
    YANILLE_TELEPORT(
            58,
            InterfaceID.MagicSpellbook.WATCHTOWER_TELEPORT,
            34,
            Map.of(Rune.EARTH, 2, Rune.LAW, 2)
    ),
    TROLLHEIM_TELEPORT(
            61,
            InterfaceID.MagicSpellbook.TROLLHEIM_TELEPORT,
            41,
            Map.of(Rune.FIRE, 2, Rune.LAW, 2)
    ),
    TELEPORT_TO_APE_ATOLL(
            64,
            InterfaceID.MagicSpellbook.APE_TELEPORT,
            44,
            Map.of(Rune.FIRE, 2, Rune.WATER, 2, Rune.LAW, 2)
    ),
    TELEPORT_TO_KOUREND(
            69,
            InterfaceID.MagicSpellbook.KOUREND_TELEPORT,
            49,
            Map.of(Rune.FIRE, 5, Rune.WATER, 4, Rune.SOUL, 2, Rune.LAW, 2)
    ),
    TELEOTHER_LUMBRIDGE(
            74,
            InterfaceID.MagicSpellbook.TELEOTHER_LUMBRIDGE,
            52,
            Map.of(Rune.EARTH, 1, Rune.LAW, 1, Rune.SOUL, 1)
    ),
    TELEOTHER_FALADOR(
            82,
            InterfaceID.MagicSpellbook.TELEOTHER_FALADOR,
            58,
            Map.of(Rune.WATER, 1, Rune.LAW, 1, Rune.SOUL, 1)
    ),
    TELEPORT_TO_BOUNTY_TARGET(
            85,
            InterfaceID.MagicSpellbook.BOUNTY_TARGET,
            61,
            Map.of(Rune.CHAOS, 1, Rune.DEATH, 1, Rune.LAW, 1)
    ),
    TELEOTHER_CAMELOT(
            90,
            InterfaceID.MagicSpellbook.TELEOTHER_CAMELOT,
            62,
            Map.of(Rune.LAW, 1, Rune.SOUL, 2)
    ),

    // Strike Spells
    WIND_STRIKE(
            1,
            InterfaceID.MagicSpellbook.WIND_STRIKE,
            1,
            Map.of(Rune.AIR, 1, Rune.MIND, 1)
    ),
    WATER_STRIKE(
            5,
            InterfaceID.MagicSpellbook.WATER_STRIKE,
            4,
            Map.of(Rune.AIR, 1, Rune.WATER, 1, Rune.MIND, 1)
    ),
    EARTH_STRIKE(
            9,
            InterfaceID.MagicSpellbook.EARTH_STRIKE,
            6,
            Map.of(Rune.AIR, 1, Rune.EARTH, 2, Rune.MIND, 1)
    ),
    FIRE_STRIKE(
            13,
            InterfaceID.MagicSpellbook.FIRE_STRIKE,
            8,
            Map.of(Rune.AIR, 2, Rune.FIRE, 3, Rune.MIND, 1)
    ),

    // Bolt Spells
    WIND_BOLT(
            17,
            InterfaceID.MagicSpellbook.WIND_BOLT,
            10,
            Map.of(Rune.AIR, 2, Rune.CHAOS, 1)
    ),
    WATER_BOLT(
            23,
            InterfaceID.MagicSpellbook.WATER_BOLT,
            14,
            Map.of(Rune.AIR, 2, Rune.WATER, 2, Rune.CHAOS, 1)
    ),
    EARTH_BOLT(
            29,
            InterfaceID.MagicSpellbook.EARTH_BOLT,
            16,
            Map.of(Rune.AIR, 2, Rune.EARTH, 3, Rune.CHAOS, 1)
    ),
    FIRE_BOLT(
            35,
            InterfaceID.MagicSpellbook.FIRE_BOLT,
            19,
            Map.of(Rune.AIR, 3, Rune.FIRE, 4, Rune.CHAOS, 1)
    ),

    // Blast Spells
    WIND_BLAST(
            41,
            InterfaceID.MagicSpellbook.WIND_BLAST,
            22,
            Map.of(Rune.AIR, 3, Rune.DEATH, 1)
    ),
    WATER_BLAST(
            47,
            InterfaceID.MagicSpellbook.WATER_BLAST,
            26,
            Map.of(Rune.AIR, 3, Rune.WATER, 3, Rune.DEATH, 1)
    ),
    EARTH_BLAST(
            53,
            InterfaceID.MagicSpellbook.EARTH_BLAST,
            31,
            Map.of(Rune.AIR, 3, Rune.EARTH, 4, Rune.DEATH, 1)
    ),
    FIRE_BLAST(
            59,
            InterfaceID.MagicSpellbook.FIRE_BLAST,
            35,
            Map.of(Rune.AIR, 4, Rune.FIRE, 5, Rune.DEATH, 1)
    ),

    // Wave Spells
    WIND_WAVE(
            62,
            InterfaceID.MagicSpellbook.WIND_WAVE,
            42,
            Map.of(Rune.AIR, 5, Rune.BLOOD, 1)
    ),
    WATER_WAVE(
            65,
            InterfaceID.MagicSpellbook.WATER_WAVE,
            45,
            Map.of(Rune.AIR, 5, Rune.WATER, 7, Rune.BLOOD, 1)
    ),
    EARTH_WAVE(
            70,
            InterfaceID.MagicSpellbook.EARTH_WAVE,
            50,
            Map.of(Rune.AIR, 5, Rune.EARTH, 7, Rune.BLOOD, 1)
    ),
    FIRE_WAVE(
            75,
            InterfaceID.MagicSpellbook.FIRE_WAVE,
            53,
            Map.of(Rune.AIR, 5, Rune.FIRE, 7, Rune.BLOOD, 1)
    ),

    // Surge Spells
    WIND_SURGE(
            81,
            InterfaceID.MagicSpellbook.WIND_SURGE,
            57,
            Map.of(Rune.AIR, 7, Rune.WRATH, 1)
    ),
    WATER_SURGE(
            85,
            InterfaceID.MagicSpellbook.WATER_SURGE,
            59,
            Map.of(Rune.AIR, 7, Rune.WATER, 10, Rune.WRATH, 1)
    ),
    EARTH_SURGE(
            90,
            InterfaceID.MagicSpellbook.EARTH_SURGE,
            63,
            Map.of(Rune.AIR, 7, Rune.EARTH, 10, Rune.WRATH, 1)
    ),
    FIRE_SURGE(
            95,
            InterfaceID.MagicSpellbook.FIRE_SURGE,
            64,
            Map.of(Rune.AIR, 7, Rune.FIRE, 10, Rune.WRATH, 1)
    ),

    // God Spells
    SARADOMIN_STRIKE(
            60,
            InterfaceID.MagicSpellbook.SARADOMIN_STRIKE,
            38,
            Map.of(Rune.AIR, 4, Rune.FIRE, 2, Rune.BLOOD, 2)
    ),
    CLAWS_OF_GUTHIX(
            60,
            InterfaceID.MagicSpellbook.CLAWS_OF_GUTHIX,
            39,
            Map.of(Rune.AIR, 4, Rune.FIRE, 1, Rune.BLOOD, 2)
    ),
    FLAMES_OF_ZAMORAK(
            60,
            InterfaceID.MagicSpellbook.FLAMES_OF_ZAMORAK,
            40,
            Map.of(Rune.AIR, 1, Rune.FIRE, 4, Rune.BLOOD, 2)
    ),

    // Other Combat Spells
    CRUMBLE_UNDEAD(
            39,
            InterfaceID.MagicSpellbook.CRUMBLE_UNDEAD,
            21,
            Map.of(Rune.AIR, 2, Rune.EARTH, 2, Rune.CHAOS, 1)
    ),
    IBAN_BLAST(
            50,
            InterfaceID.MagicSpellbook.IBAN_BLAST,
            27,
            Map.of(Rune.FIRE, 5, Rune.DEATH, 1)
    ),
    MAGIC_DART(
            50,
            InterfaceID.MagicSpellbook.MAGIC_DART,
            29,
            Map.of(Rune.DEATH, 1, Rune.MIND, 4)
    ),

    // Curse/Debuff Spells
    CONFUSE(
            3,
            InterfaceID.MagicSpellbook.CONFUSE,
            2,
            Map.of(Rune.EARTH, 2, Rune.WATER, 3, Rune.BODY, 1)
    ),
    WEAKEN(
            11,
            InterfaceID.MagicSpellbook.WEAKEN,
            7,
            Map.of(Rune.EARTH, 2, Rune.WATER, 3, Rune.BODY, 1)
    ),
    CURSE(
            19,
            InterfaceID.MagicSpellbook.CURSE,
            11,
            Map.of(Rune.EARTH, 3, Rune.WATER, 2, Rune.BODY, 1)
    ),
    BIND(
            20,
            InterfaceID.MagicSpellbook.BIND,
            12,
            Map.of(Rune.EARTH, 3, Rune.WATER, 3, Rune.NATURE, 2)
    ),
    SNARE(
            50,
            InterfaceID.MagicSpellbook.SNARE,
            28,
            Map.of(Rune.EARTH, 4, Rune.WATER, 4, Rune.NATURE, 3)
    ),
    VULNERABILITY(
            66,
            InterfaceID.MagicSpellbook.VULNERABILITY,
            47,
            Map.of(Rune.EARTH, 5, Rune.WATER, 5, Rune.SOUL, 1)
    ),
    ENFEEBLE(
            73,
            InterfaceID.MagicSpellbook.ENFEEBLE,
            51,
            Map.of(Rune.EARTH, 8, Rune.WATER, 8, Rune.SOUL, 1)
    ),
    ENTANGLE(
            79,
            InterfaceID.MagicSpellbook.ENTANGLE,
            54,
            Map.of(Rune.EARTH, 5, Rune.WATER, 5, Rune.NATURE, 4)
    ),
    STUN(
            80,
            InterfaceID.MagicSpellbook.STUN,
            55,
            Map.of(Rune.EARTH, 12, Rune.WATER, 12, Rune.SOUL, 1)
    ),
    TELE_BLOCK(
            85,
            InterfaceID.MagicSpellbook.TELEPORT_BLOCK,
            60,
            Map.of(Rune.CHAOS, 1, Rune.DEATH, 1, Rune.LAW, 1)
    ),

    // Utility Spells
    CHARGE(
            80,
            InterfaceID.MagicSpellbook.CHARGE,
            56,
            Map.of(Rune.AIR, 3, Rune.FIRE, 3, Rune.BLOOD, 3)
    ),
    BONES_TO_BANANAS(
            15,
            InterfaceID.MagicSpellbook.BONES_BANANAS,
            9,
            Map.of(Rune.EARTH, 2, Rune.WATER, 2, Rune.NATURE, 1)
    ),
    LOW_LEVEL_ALCHEMY(
            21,
            InterfaceID.MagicSpellbook.LOW_ALCHEMY,
            13,
            Map.of(Rune.FIRE, 3, Rune.NATURE, 1)
    ),
    SUPERHEAT_ITEM(
            43,
            InterfaceID.MagicSpellbook.SUPERHEAT,
            24,
            Map.of(Rune.FIRE, 4, Rune.NATURE, 1)
    ),
    HIGH_LEVEL_ALCHEMY(
            55,
            InterfaceID.MagicSpellbook.HIGH_ALCHEMY,
            32,
            Map.of(Rune.FIRE, 5, Rune.NATURE, 1)
    ),
    BONES_TO_PEACHES(
            60,
            InterfaceID.MagicSpellbook.BONES_PEACHES,
            37,
            Map.of(Rune.EARTH, 2, Rune.WATER, 4, Rune.NATURE, 2)
    ),

    // Enchantment Spells
    LVL_1_ENCHANT(
            7,
            InterfaceID.MagicSpellbook.ENCHANT_1,
            5,
            Map.of(Rune.WATER, 1, Rune.COSMIC, 1)
    ),
    LVL_2_ENCHANT(
            27,
            InterfaceID.MagicSpellbook.ENCHANT_2,
            5,
            Map.of(Rune.AIR, 3, Rune.COSMIC, 1)
    ),
    LVL_3_ENCHANT(
            49,
            InterfaceID.MagicSpellbook.ENCHANT_3,
            5,
            Map.of(Rune.FIRE, 5, Rune.COSMIC, 1)
    ),
    CHARGE_WATER_ORB(
            56,
            InterfaceID.MagicSpellbook.CHARGE_WATER_ORB,
            33,
            Map.of(Rune.WATER, 30, Rune.COSMIC, 3)
    ),
    LVL_4_ENCHANT(
            57,
            InterfaceID.MagicSpellbook.ENCHANT_4,
            5,
            Map.of(Rune.EARTH, 10, Rune.COSMIC, 1)
    ),
    CHARGE_EARTH_ORB(
            60,
            InterfaceID.MagicSpellbook.CHARGE_EARTH_ORB,
            36,
            Map.of(Rune.EARTH, 30, Rune.COSMIC, 3)
    ),
    CHARGE_FIRE_ORB(
            63,
            InterfaceID.MagicSpellbook.CHARGE_FIRE_ORB,
            43,
            Map.of(Rune.FIRE, 30, Rune.COSMIC, 3)
    ),
    CHARGE_AIR_ORB(
            66,
            InterfaceID.MagicSpellbook.CHARGE_AIR_ORB,
            46,
            Map.of(Rune.AIR, 30, Rune.COSMIC, 3)
    ),
    LVL_5_ENCHANT(
            68,
            InterfaceID.MagicSpellbook.ENCHANT_5,
            5,
            Map.of(Rune.EARTH, 15, Rune.WATER, 15, Rune.COSMIC, 1)
    ),
    LVL_6_ENCHANT(
            87,
            InterfaceID.MagicSpellbook.ENCHANT_6,
            5,
            Map.of(Rune.EARTH, 20, Rune.FIRE, 20, Rune.COSMIC, 1)
    ),
    LVL_7_ENCHANT(
            93,
            InterfaceID.MagicSpellbook.ENCHANT_7,
            5,
            Map.of(Rune.BLOOD, 20, Rune.SOUL, 20, Rune.COSMIC, 1)
    ),
    TELEKINETIC_GRAB(
            31,
            InterfaceID.MagicSpellbook.TELEGRAB,
            18,
            Map.of(Rune.AIR, 1, Rune.LAW, 1)
    );

    private final int level;
    private final int widget;
    private final int index;
    private final Map<Rune, Integer> runeRequirement;
    private final Spellbook spellbook = Spellbook.STANDARD;
    private final String name;

    Standard(int level, int widget, int index, Map<Rune, Integer> runeRequirement) {
        this.level = level;
        this.widget = widget;
        this.index = index;
        this.runeRequirement = runeRequirement;
        this.name = this.name();
    }

    @Override
    public int getAction() {
        int action = 1;
        if (this != VARROCK_TELEPORT && this != CAMELOT_TELEPORT && this != WATCHTOWER_TELEPORT && this != GRAND_EXCHANGE_TELEPORT && this != SEERS_TELEPORT && this != YANILLE_TELEPORT) {
            return action;
        }

        if (this == VARROCK_TELEPORT || this == GRAND_EXCHANGE_TELEPORT) {
            action = getVariantAction(VarbitID.VARROCK_GE_TELEPORT, this, VARROCK_TELEPORT, GRAND_EXCHANGE_TELEPORT);
        } else if (this == CAMELOT_TELEPORT || this == SEERS_TELEPORT) {
            action = getVariantAction(VarbitID.SEERS_CAMELOT_TELEPORT, this, CAMELOT_TELEPORT, SEERS_TELEPORT);
        } else if (this == WATCHTOWER_TELEPORT || this == YANILLE_TELEPORT) {
            action = getVariantAction(VarbitID.YANILLE_TELEPORT_LOCATION, this, WATCHTOWER_TELEPORT, YANILLE_TELEPORT);
        }

        return action;
    }

    /**
     * Returns the action for spells which have double teleport actions after certain
     * unlocks. i.e. Medium diaries unlock the ability to teleport to the GE with the Varrock teleport.
     * When the spell cast is GE, then this must return the correct action for the GE teleport, not the Varrock Teleport.
     * @param varbit The varbit denoting the action configuration
     * @param spell The spell being cast by the developer/player
     * @param baseSpell The base spell i.e. Varrock
     * @param variantSpell The variant spell i.e. Grand Exchange
     * @return The action number to send in the widget packet
     */
    private int getVariantAction(int varbit, CastableSpell spell, CastableSpell baseSpell, CastableSpell variantSpell) {
        Context ctx = RuneLite.getInjector().getInstance(Context.class);
        int config = ctx.getVarbitValue(varbit);
        if (config == 0) {
            return spell == baseSpell ? 1 : 2;
        }
        return spell == variantSpell ? 3 : 2;
    }
}
