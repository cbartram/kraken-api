package com.kraken.api.service.magic.spellbook;

import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.rune.Rune;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

import java.util.Map;

@Getter
public enum Arceuus implements CastableSpell {
    // Home Teleport
    ARCEUUS_HOME_TELEPORT(
            1,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_ARCEUUS,
            0,
            Map.of()
    ),

    // Reanimation Spells
    BASIC_REANIMATION(
            16,
            InterfaceID.MagicSpellbook.REANIMATION_BASIC,
            1,
            Map.of(Rune.BODY, 4, Rune.NATURE, 2)
    ),
    ADEPT_REANIMATION(
            41,
            InterfaceID.MagicSpellbook.REANIMATION_ADEPT,
            2,
            Map.of(Rune.BODY, 4, Rune.NATURE, 3, Rune.SOUL, 1)
    ),
    EXPERT_REANIMATION(
            72,
            InterfaceID.MagicSpellbook.REANIMATION_EXPERT,
            3,
            Map.of(Rune.BLOOD, 1, Rune.NATURE, 3, Rune.SOUL, 2)
    ),
    MASTER_REANIMATION(
            90,
            InterfaceID.MagicSpellbook.REANIMATION_MASTER,
            4,
            Map.of(Rune.BLOOD, 2, Rune.NATURE, 4, Rune.SOUL, 4)
    ),

    // Teleport Spells
    ARCEUUS_LIBRARY_TELEPORT(
            6,
            InterfaceID.MagicSpellbook.TELEPORT_ARCEUUS_LIBRARY,
            5,
            Map.of(Rune.EARTH, 2, Rune.LAW, 1)
    ),
    DRAYNOR_MANOR_TELEPORT(
            17,
            InterfaceID.MagicSpellbook.TELEPORT_DRAYNOR_MANOR,
            6,
            Map.of(Rune.EARTH, 1, Rune.WATER, 1, Rune.LAW, 1)
    ),
    BATTLEFRONT_TELEPORT(
            23,
            InterfaceID.MagicSpellbook.TELEPORT_BATTLEFRONT,
            17,
            Map.of(Rune.EARTH, 1, Rune.FIRE, 1, Rune.LAW, 1)
    ),
    MIND_ALTAR_TELEPORT(
            28,
            InterfaceID.MagicSpellbook.TELEPORT_MIND_ALTAR,
            7,
            Map.of(Rune.MIND, 2, Rune.LAW, 1)
    ),
    RESPAWN_TELEPORT(
            34,
            InterfaceID.MagicSpellbook.TELEPORT_RESPAWN,
            15,
            Map.of(Rune.SOUL, 1, Rune.LAW, 1)
    ),
    SALVE_GRAVEYARD_TELEPORT(
            40,
            InterfaceID.MagicSpellbook.TELEPORT_SALVE_GRAVEYARD,
            8,
            Map.of(Rune.SOUL, 2, Rune.LAW, 1)
    ),
    FENKENSTRAINS_CASTLE_TELEPORT(
            48,
            InterfaceID.MagicSpellbook.TELEPORT_FENKENSTRAIN_CASTLE,
            9,
            Map.of(Rune.EARTH, 1, Rune.SOUL, 1, Rune.LAW, 1)
    ),
    WEST_ARDOUGNE_TELEPORT(
            61,
            InterfaceID.MagicSpellbook.TELEPORT_WEST_ARDOUGNE,
            10,
            Map.of(Rune.SOUL, 2, Rune.LAW, 2)
    ),
    HARMONY_ISLAND_TELEPORT(
            65,
            InterfaceID.MagicSpellbook.TELEPORT_HARMONY_ISLAND,
            11,
            Map.of(Rune.NATURE, 1, Rune.SOUL, 1, Rune.LAW, 1)
    ),
    CEMETERY_TELEPORT(
            71,
            InterfaceID.MagicSpellbook.TELEPORT_CEMETERY,
            12,
            Map.of(Rune.BLOOD, 1, Rune.SOUL, 1, Rune.LAW, 1)
    ),
    BARROWS_TELEPORT(
            83,
            InterfaceID.MagicSpellbook.TELEPORT_BARROWS,
            13,
            Map.of(Rune.BLOOD, 1, Rune.SOUL, 2, Rune.LAW, 2)
    ),
    APE_ATOLL_TELEPORT(
            90,
            InterfaceID.MagicSpellbook.TELEPORT_APE_ATOLL_DUNGEON,
            14,
            Map.of(Rune.BLOOD, 2, Rune.SOUL, 2, Rune.LAW, 2)
    ),

    // Combat Spells - Grasp
    GHOSTLY_GRASP(
            35,
            InterfaceID.MagicSpellbook.GHOSTLY_GRASP,
            22,
            Map.of(Rune.AIR, 4, Rune.CHAOS, 1)
    ),
    SKELETAL_GRASP(
            56,
            InterfaceID.MagicSpellbook.SKELETAL_GRASP,
            23,
            Map.of(Rune.EARTH, 8, Rune.DEATH, 1)
    ),
    UNDEAD_GRASP(
            79,
            InterfaceID.MagicSpellbook.UNDEAD_GRASP,
            24,
            Map.of(Rune.FIRE, 12, Rune.BLOOD, 1)
    ),

    // Combat Spells - Demonbane
    INFERIOR_DEMONBANE(
            44,
            InterfaceID.MagicSpellbook.INFERIOR_DEMONBANE,
            18,
            Map.of(Rune.FIRE, 4, Rune.CHAOS, 1)
    ),
    SUPERIOR_DEMONBANE(
            62,
            InterfaceID.MagicSpellbook.SUPERIOR_DEMONBANE,
            19,
            Map.of(Rune.FIRE, 8, Rune.SOUL, 1)
    ),
    DARK_DEMONBANE(
            82,
            InterfaceID.MagicSpellbook.DARK_DEMONBANE,
            20,
            Map.of(Rune.FIRE, 12, Rune.SOUL, 2)
    ),

    // Combat Spells - Corruption
    LESSER_CORRUPTION(
            64,
            InterfaceID.MagicSpellbook.LESSER_CORRUPTION,
            26,
            Map.of(Rune.DEATH, 1, Rune.SOUL, 2)
    ),
    GREATER_CORRUPTION(
            85,
            InterfaceID.MagicSpellbook.GREATER_CORRUPTION,
            27,
            Map.of(Rune.BLOOD, 1, Rune.SOUL, 3)
    ),

    // Resurrect Spells - Lesser
    RESURRECT_LESSER_GHOST(
            38,
            InterfaceID.MagicSpellbook.RESURRECT_LESSER_GHOST,
            35,
            Map.of(Rune.AIR, 10, Rune.COSMIC, 1, Rune.MIND, 5)
    ),
    RESURRECT_LESSER_SKELETON(
            38,
            InterfaceID.MagicSpellbook.RESURRECT_LESSER_SKELETON,
            36,
            Map.of(Rune.AIR, 10, Rune.COSMIC, 1, Rune.MIND, 5)
    ),
    RESURRECT_LESSER_ZOMBIE(
            38,
            InterfaceID.MagicSpellbook.RESURRECT_LESSER_ZOMBIE,
            37,
            Map.of(Rune.AIR, 10, Rune.COSMIC, 1, Rune.MIND, 5)
    ),

    // Resurrect Spells - Superior
    RESURRECT_SUPERIOR_GHOST(
            57,
            InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_GHOST,
            38,
            Map.of(Rune.EARTH, 10, Rune.COSMIC, 1, Rune.DEATH, 5)
    ),
    RESURRECT_SUPERIOR_SKELETON(
            57,
            InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_SKELETON,
            39,
            Map.of(Rune.EARTH, 10, Rune.COSMIC, 1, Rune.DEATH, 5)
    ),
    RESURRECT_SUPERIOR_ZOMBIE(
            57,
            InterfaceID.MagicSpellbook.RESURRECT_SUPERIOR_ZOMBIE,
            40,
            Map.of(Rune.EARTH, 10, Rune.COSMIC, 1, Rune.DEATH, 5)
    ),

    // Resurrect Spells - Greater
    RESURRECT_GREATER_GHOST(
            76,
            InterfaceID.MagicSpellbook.RESURRECT_GREATER_GHOST,
            41,
            Map.of(Rune.FIRE, 10, Rune.COSMIC, 1, Rune.BLOOD, 5)
    ),
    RESURRECT_GREATER_SKELETON(
            76,
            InterfaceID.MagicSpellbook.RESURRECT_GREATER_SKELETON,
            42,
            Map.of(Rune.FIRE, 10, Rune.COSMIC, 1, Rune.BLOOD, 5)
    ),
    RESURRECT_GREATER_ZOMBIE(
            76,
            InterfaceID.MagicSpellbook.RESURRECT_GREATER_ZOMBIE,
            43,
            Map.of(Rune.FIRE, 10, Rune.COSMIC, 1, Rune.BLOOD, 5)
    ),

    // Utility Spells
    DARK_LURE(
            50,
            InterfaceID.MagicSpellbook.DARK_LURE,
            33,
            Map.of(Rune.DEATH, 1, Rune.NATURE, 1)
    ),
    MARK_OF_DARKNESS(
            59,
            InterfaceID.MagicSpellbook.MARK_OF_DARKNESS,
            21,
            Map.of(Rune.COSMIC, 1, Rune.SOUL, 1)
    ),
    WARD_OF_ARCEUUS(
            73,
            InterfaceID.MagicSpellbook.WARD_OF_ARCEUUS,
            25,
            Map.of(Rune.COSMIC, 1, Rune.NATURE, 2, Rune.SOUL, 4)
    ),
    DEMONIC_OFFERING(
            84,
            InterfaceID.MagicSpellbook.DEMONIC_OFFERING,
            28,
            Map.of(Rune.SOUL, 1, Rune.WRATH, 1)
    ),
    SINISTER_OFFERING(
            92,
            InterfaceID.MagicSpellbook.SINISTER_OFFERING,
            29,
            Map.of(Rune.BLOOD, 1, Rune.WRATH, 1)
    ),
    SHADOW_VEIL(
            47,
            InterfaceID.MagicSpellbook.SHADOW_VEIL,
            31,
            Map.of(Rune.EARTH, 5, Rune.FIRE, 5, Rune.COSMIC, 5)
    ),
    VILE_VIGOUR(
            66,
            InterfaceID.MagicSpellbook.VILE_VIGOUR,
            32,
            Map.of(Rune.AIR, 3, Rune.SOUL, 1)
    ),
    DEGRIME(
            70,
            InterfaceID.MagicSpellbook.DEGRIME,
            30,
            Map.of(Rune.EARTH, 4, Rune.NATURE, 2)
    ),
    RESURRECT_CROPS(
            78,
            InterfaceID.MagicSpellbook.RESURRECT_CROPS,
            16,
            Map.of(Rune.EARTH, 25, Rune.BLOOD, 8, Rune.NATURE, 12, Rune.SOUL, 8)
    ),
    DEATH_CHARGE(
            80,
            InterfaceID.MagicSpellbook.DEATH_CHARGE,
            34,
            Map.of(Rune.BLOOD, 1, Rune.DEATH, 1, Rune.SOUL, 1)
    );

    private final int level;
    private final int widget;
    private final int index;
    private final Map<Rune, Integer> runeRequirement;
    private final Spellbook spellbook = Spellbook.ARCEUUS;
    private final String name;

    Arceuus(int level, int widget, int index, Map<Rune, Integer> runeRequirement) {
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
