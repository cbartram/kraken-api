package com.kraken.api.service.magic.spellbook;

import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.rune.Rune;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

import java.util.Map;

@Getter
public enum Lunar implements CastableSpell {
    // Home Teleport
    LUNAR_HOME_TELEPORT(
            0,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_LUNAR,
            0,
            Map.of()
    ),

    // Utility Spells
    BAKE_PIE(
            66,
            InterfaceID.MagicSpellbook.BAKE_PIE,
            1,
            Map.of(Rune.WATER, 4, Rune.FIRE, 5, Rune.ASTRAL, 1)
    ),
    CURE_PLANT(
            66,
            InterfaceID.MagicSpellbook.CURE_PLANT,
            2,
            Map.of(Rune.EARTH, 8, Rune.ASTRAL, 1)
    ),
    NPC_CONTACT(
            66,
            InterfaceID.MagicSpellbook.NPC_CONTACT,
            3,
            Map.of(Rune.AIR, 2, Rune.COSMIC, 1, Rune.ASTRAL, 1)
    ),
    CURE_OTHER(
            66,
            InterfaceID.MagicSpellbook.CURE_OTHER,
            4,
            Map.of(Rune.EARTH, 10, Rune.ASTRAL, 1, Rune.LAW, 1)
    ),
    CURE_ME(
            66,
            InterfaceID.MagicSpellbook.CURE_ME,
            7,
            Map.of(Rune.COSMIC, 2, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    CURE_GROUP(
            66,
            InterfaceID.MagicSpellbook.CURE_GROUP,
            10,
            Map.of(Rune.COSMIC, 2, Rune.ASTRAL, 2, Rune.LAW, 2)
    ),
    STAT_SPY(
            66,
            InterfaceID.MagicSpellbook.STAT_SPY,
            11,
            Map.of(Rune.BODY, 5, Rune.COSMIC, 1, Rune.ASTRAL, 2)
    ),
    SPIN_FLAX(
            66,
            InterfaceID.MagicSpellbook.SPIN_FLAX,
            14,
            Map.of(Rune.AIR, 5, Rune.ASTRAL, 1, Rune.NATURE, 2)
    ),
    SUPERGLASS_MAKE(
            66,
            InterfaceID.MagicSpellbook.SUPERGLASS,
            15,
            Map.of(Rune.AIR, 10, Rune.FIRE, 6, Rune.ASTRAL, 2)
    ),
    TAN_LEATHER(
            66,
            InterfaceID.MagicSpellbook.TAN_LEATHER,
            16,
            Map.of(Rune.FIRE, 5, Rune.ASTRAL, 2, Rune.NATURE, 1)
    ),
    STRING_JEWELLERY(
            66,
            InterfaceID.MagicSpellbook.STRING_JEWEL,
            19,
            Map.of(Rune.EARTH, 10, Rune.WATER, 5, Rune.ASTRAL, 2)
    ),
    STAT_RESTORE_POT_SHARE(
            66,
            InterfaceID.MagicSpellbook.REST_POT_SHARE,
            20,
            Map.of(Rune.WATER, 10, Rune.EARTH, 10, Rune.ASTRAL, 2)
    ),
    MAGIC_IMBUE(
            66,
            InterfaceID.MagicSpellbook.MAGIC_IMBUE,
            21,
            Map.of(Rune.WATER, 7, Rune.FIRE, 7, Rune.ASTRAL, 2)
    ),
    FERTILE_SOIL(
            66,
            InterfaceID.MagicSpellbook.FERTILE_SOIL,
            22,
            Map.of(Rune.EARTH, 15, Rune.ASTRAL, 3, Rune.NATURE, 2)
    ),
    BOOST_POTION_SHARE(
            66,
            InterfaceID.MagicSpellbook.STREN_POT_SHARE,
            23,
            Map.of(Rune.WATER, 10, Rune.EARTH, 12, Rune.ASTRAL, 3)
    ),
    RECHARGE_DRAGONSTONE(
            66,
            InterfaceID.MagicSpellbook.RECHARGE_DRAGONSTONE,
            28,
            Map.of(Rune.WATER, 4, Rune.ASTRAL, 1, Rune.SOUL, 1)
    ),
    ENERGY_TRANSFER(
            66,
            InterfaceID.MagicSpellbook.ENERGY_TRANS,
            31,
            Map.of(Rune.ASTRAL, 3, Rune.NATURE, 1, Rune.LAW, 2)
    ),
    HEAL_OTHER(
            66,
            InterfaceID.MagicSpellbook.HEAL_OTHER,
            32,
            Map.of(Rune.ASTRAL, 3, Rune.LAW, 3, Rune.BLOOD, 1)
    ),
    VENGEANCE_OTHER(
            66,
            InterfaceID.MagicSpellbook.VENGEANCE_OTHER,
            33,
            Map.of(Rune.EARTH, 10, Rune.ASTRAL, 3, Rune.DEATH, 2)
    ),
    VENGEANCE(
            66,
            InterfaceID.MagicSpellbook.VENGEANCE,
            34,
            Map.of(Rune.EARTH, 10, Rune.ASTRAL, 4, Rune.DEATH, 2)
    ),
    HEAL_GROUP(
            66,
            InterfaceID.MagicSpellbook.HEAL_GROUP,
            35,
            Map.of(Rune.ASTRAL, 4, Rune.LAW, 6, Rune.BLOOD, 3)
    ),
    MONSTER_EXAMINE(
            66,
            InterfaceID.MagicSpellbook.MONSTER_EXAMINE,
            36,
            Map.of(Rune.MIND, 1, Rune.COSMIC, 1, Rune.ASTRAL, 1)
    ),
    HUMIDIFY(
            66,
            InterfaceID.MagicSpellbook.HUMIDIFY,
            37,
            Map.of(Rune.WATER, 3, Rune.FIRE, 1, Rune.ASTRAL, 1)
    ),
    HUNTER_KIT(
            66,
            InterfaceID.MagicSpellbook.HUNTER_KIT,
            39,
            Map.of(Rune.EARTH, 2, Rune.ASTRAL, 2)
    ),
    DREAM(
            66,
            InterfaceID.MagicSpellbook.DREAM,
            40,
            Map.of(Rune.BODY, 5, Rune.COSMIC, 1, Rune.ASTRAL, 2)
    ),
    PLANK_MAKE(
            66,
            InterfaceID.MagicSpellbook.PLANK_MAKE,
            41,
            Map.of(Rune.EARTH, 15, Rune.ASTRAL, 2, Rune.NATURE, 1)
    ),
    SPELLBOOK_SWAP(
            66,
            InterfaceID.MagicSpellbook.SPELLBOOK_SWAP,
            42,
            Map.of(Rune.COSMIC, 2, Rune.ASTRAL, 3, Rune.LAW, 1)
    ),
    GEOMANCY(
            66,
            InterfaceID.MagicSpellbook.GEOMANCY,
            43,
            Map.of(Rune.EARTH, 8, Rune.ASTRAL, 3, Rune.NATURE, 3)
    ),

    // Teleport Spells
    MOONCLAN_TELEPORT(
            69,
            InterfaceID.MagicSpellbook.TELE_MOONCLAN,
            5,
            Map.of(Rune.EARTH, 2, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    TELE_GROUP_MOONCLAN(
            70,
            InterfaceID.MagicSpellbook.TELE_GROUP_MOONCLAN,
            6,
            Map.of(Rune.EARTH, 4, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    WATERBIRTH_TELEPORT(
            72,
            InterfaceID.MagicSpellbook.TELE_WATERBIRTH,
            8,
            Map.of(Rune.WATER, 1, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    TELE_GROUP_WATERBIRTH(
            73,
            InterfaceID.MagicSpellbook.TELE_GROUP_WATERBIRTH,
            9,
            Map.of(Rune.WATER, 5, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    BARBARIAN_TELEPORT(
            75,
            InterfaceID.MagicSpellbook.TELE_BARB_OUT,
            12,
            Map.of(Rune.FIRE, 3, Rune.ASTRAL, 2, Rune.LAW, 2)
    ),
    TELE_GROUP_BARBARIAN(
            76,
            InterfaceID.MagicSpellbook.TELE_GROUP_BARBARIAN,
            13,
            Map.of(Rune.FIRE, 6, Rune.ASTRAL, 2, Rune.LAW, 2)
    ),
    KHAZARD_TELEPORT(
            78,
            InterfaceID.MagicSpellbook.TELE_KHAZARD,
            17,
            Map.of(Rune.WATER, 4, Rune.ASTRAL, 2, Rune.LAW, 2)
    ),
    TELE_GROUP_KHAZARD(
            79,
            InterfaceID.MagicSpellbook.TELE_GROUP_KHAZARD,
            18,
            Map.of(Rune.WATER, 8, Rune.ASTRAL, 2, Rune.LAW, 2)
    ),
    FISHING_GUILD_TELEPORT(
            85,
            InterfaceID.MagicSpellbook.TELE_FISH,
            24,
            Map.of(Rune.WATER, 10, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    TELE_GROUP_FISHING_GUILD(
            86,
            InterfaceID.MagicSpellbook.TELE_GROUP_FISHING_GUILD,
            25,
            Map.of(Rune.WATER, 14, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    CATHERBY_TELEPORT(
            87,
            InterfaceID.MagicSpellbook.TELE_CATHER,
            26,
            Map.of(Rune.WATER, 10, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    TELE_GROUP_CATHERBY(
            88,
            InterfaceID.MagicSpellbook.TELE_GROUP_CATHERBY,
            27,
            Map.of(Rune.WATER, 15, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    ICE_PLATEAU_TELEPORT(
            89,
            InterfaceID.MagicSpellbook.TELE_GHORROCK,
            29,
            Map.of(Rune.WATER, 8, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    TELE_GROUP_ICE_PLATEAU(
            90,
            InterfaceID.MagicSpellbook.TELE_GROUP_GHORROCK,
            30,
            Map.of(Rune.WATER, 16, Rune.ASTRAL, 3, Rune.LAW, 3)
    ),
    OURANIA_TELEPORT(
            71,
            InterfaceID.MagicSpellbook.OURANIA_TELEPORT,
            38,
            Map.of(Rune.EARTH, 6, Rune.ASTRAL, 2, Rune.LAW, 1)
    ),
    TELEPORT_TO_TARGET(
            85,
            InterfaceID.MagicSpellbook.BOUNTY_TARGET,
            44,
            Map.of(Rune.CHAOS, 1, Rune.DEATH, 1, Rune.LAW, 1)
    );

    private final int level;
    private final int widget;
    private final int index;
    private final Map<Rune, Integer> runeRequirement;
    private final Spellbook spellbook = Spellbook.LUNAR;
    private final String name;

    Lunar(int level, int widget, int index, Map<Rune, Integer> runeRequirement) {
        this.level = level;
        this.widget = widget;
        this.index = index;
        this.runeRequirement = runeRequirement;
        this.name = this.name();
    }

    @Override
    public int getAction() {
        return 1;
    }
}

