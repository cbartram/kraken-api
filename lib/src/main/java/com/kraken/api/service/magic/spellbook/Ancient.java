package com.kraken.api.service.magic.spellbook;

import com.kraken.api.service.magic.CastableSpell;
import com.kraken.api.service.magic.rune.Rune;
import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;

import java.util.Map;

@Getter
public enum Ancient implements CastableSpell {
    EDGEVILLE_HOME_TELEPORT(
            0,
            InterfaceID.MagicSpellbook.TELEPORT_HOME_ZAROS,
            0,
            Map.of()
    ),

    // Rush Spells
    SMOKE_RUSH(
            50,
            InterfaceID.MagicSpellbook.SMOKE_RUSH,
            1,
            Map.of(Rune.AIR, 1, Rune.FIRE, 1, Rune.CHAOS, 2, Rune.DEATH, 2)
    ),
    SHADOW_RUSH(
            52,
            InterfaceID.MagicSpellbook.SHADOW_RUSH,
            2,
            Map.of(Rune.AIR, 1, Rune.CHAOS, 2, Rune.DEATH, 2, Rune.SOUL, 1)
    ),
    BLOOD_RUSH(
            56,
            InterfaceID.MagicSpellbook.BLOOD_RUSH,
            4,
            Map.of(Rune.CHAOS, 2, Rune.DEATH, 2, Rune.BLOOD, 1)
    ),
    ICE_RUSH(
            58,
            InterfaceID.MagicSpellbook.ICE_RUSH,
            5,
            Map.of(Rune.WATER, 2, Rune.CHAOS, 2, Rune.DEATH, 2)
    ),

    // Burst Spells
    SMOKE_BURST(
            62,
            InterfaceID.MagicSpellbook.SMOKE_BURST,
            7,
            Map.of(Rune.AIR, 2, Rune.FIRE, 2, Rune.CHAOS, 4, Rune.DEATH, 2)
    ),
    SHADOW_BURST(
            64,
            InterfaceID.MagicSpellbook.SHADOW_BURST,
            8,
            Map.of(Rune.AIR, 1, Rune.CHAOS, 4, Rune.DEATH, 2, Rune.SOUL, 2)
    ),
    BLOOD_BURST(
            68,
            InterfaceID.MagicSpellbook.BLOOD_BURST,
            10,
            Map.of(Rune.CHAOS, 2, Rune.DEATH, 4, Rune.BLOOD, 2)
    ),
    ICE_BURST(
            70,
            InterfaceID.MagicSpellbook.ICE_BURST,
            11,
            Map.of(Rune.WATER, 4, Rune.CHAOS, 4, Rune.DEATH, 2)
    ),

    // Blitz Spells
    SMOKE_BLITZ(
            74,
            InterfaceID.MagicSpellbook.SMOKE_BLITZ,
            13,
            Map.of(Rune.AIR, 2, Rune.FIRE, 2, Rune.DEATH, 2, Rune.BLOOD, 2)
    ),
    SHADOW_BLITZ(
            76,
            InterfaceID.MagicSpellbook.SHADOW_BLITZ,
            14,
            Map.of(Rune.AIR, 2, Rune.DEATH, 2, Rune.BLOOD, 2, Rune.SOUL, 2)
    ),
    BLOOD_BLITZ(
            80,
            InterfaceID.MagicSpellbook.BLOOD_BLITZ,
            16,
            Map.of(Rune.DEATH, 2, Rune.BLOOD, 4)
    ),
    ICE_BLITZ(
            82,
            InterfaceID.MagicSpellbook.ICE_BLITZ,
            17,
            Map.of(Rune.WATER, 3, Rune.DEATH, 2, Rune.BLOOD, 2)
    ),

    // Barrage Spells
    SMOKE_BARRAGE(
            86,
            InterfaceID.MagicSpellbook.SMOKE_BARRAGE,
            20,
            Map.of(Rune.AIR, 4, Rune.FIRE, 4, Rune.DEATH, 4, Rune.BLOOD, 2)
    ),
    SHADOW_BARRAGE(
            88,
            InterfaceID.MagicSpellbook.SHADOW_BARRAGE,
            21,
            Map.of(Rune.AIR, 4, Rune.DEATH, 4, Rune.BLOOD, 2, Rune.SOUL, 3)
    ),
    BLOOD_BARRAGE(
            92,
            InterfaceID.MagicSpellbook.BLOOD_BARRAGE,
            23,
            Map.of(Rune.DEATH, 4, Rune.BLOOD, 4, Rune.SOUL, 1)
    ),
    ICE_BARRAGE(
            94,
            InterfaceID.MagicSpellbook.ICE_BARRAGE,
            24,
            Map.of(Rune.WATER, 6, Rune.DEATH, 4, Rune.BLOOD, 2)
    ),

    // Teleport Spells
    PADDEWWA_TELEPORT(
            54,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT1,
            3,
            Map.of(Rune.AIR, 1, Rune.FIRE, 1, Rune.LAW, 2)
    ),
    SENNTISTEN_TELEPORT(
            60,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT2,
            6,
            Map.of(Rune.LAW, 2, Rune.SOUL, 1)
    ),
    GHORROCK_TELEPORT(
            96,
            InterfaceID.MagicSpellbook.ZAROSTELEPORT8,
            25,
            Map.of(Rune.WATER, 8, Rune.LAW, 2)
    );

    private final int level;
    private final int widget;
    private final int index;
    private final Map<Rune, Integer> runeRequirement;
    private final Spellbook spellbook = Spellbook.ANCIENT;
    private final String name;

    Ancient(int level, int widget, int index, Map<Rune, Integer> runeRequirement) {
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
