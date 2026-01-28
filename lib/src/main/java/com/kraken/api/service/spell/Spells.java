package com.kraken.api.service.spell;

import lombok.Getter;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.client.plugins.skillcalculator.skills.MagicAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Getter
public enum Spells {
    CONFUSE(MagicAction.CONFUSE, Map.of(
            Runes.EARTH, 2,
            Runes.WATER, 3,
            Runes.BODY, 1
    ), Spellbook.STANDARD, 14286857),
    WEAKEN(MagicAction.WEAKEN, Map.of(
            Runes.EARTH, 2,
            Runes.WATER, 3,
            Runes.BODY, 1
    ), Spellbook.STANDARD, 14286863),
    CURSE(MagicAction.CURSE, Map.of(
            Runes.EARTH, 2,
            Runes.WATER, 3,
            Runes.BODY, 1
    ), Spellbook.STANDARD, 14286867),
    BIND(MagicAction.BIND, Map.of(
            Runes.EARTH, 3,
            Runes.WATER, 3,
            Runes.NATURE, 2
    ), Spellbook.STANDARD, 14286868),
    SNARE(MagicAction.SNARE, Map.of(
            Runes.EARTH, 4,
            Runes.WATER, 4,
            Runes.NATURE, 3
    ), Spellbook.STANDARD, 14286887),
    VULNERABILITY(MagicAction.VULNERABILITY, Map.of(
            Runes.EARTH, 5,
            Runes.WATER, 5,
            Runes.SOUL, 1
    ), Spellbook.STANDARD, 14286908),
    ENFEEBLE(MagicAction.ENFEEBLE, Map.of(
            Runes.EARTH, 8,
            Runes.WATER, 8,
            Runes.SOUL, 1
    ), Spellbook.STANDARD, 14286911),
    ENTANGLE(MagicAction.ENTANGLE, Map.of(
            Runes.EARTH, 5,
            Runes.WATER, 5,
            Runes.NATURE, 4
    ), Spellbook.STANDARD, 14286914),
    STUN(MagicAction.STUN, Map.of(
            Runes.EARTH, 12,
            Runes.WATER, 12,
            Runes.SOUL, 1
    ), Spellbook.STANDARD, 14286915),
    TELE_BLOCK(MagicAction.TELE_BLOCK, Map.of(
            Runes.CHAOS, 1,
            Runes.DEATH, 1,
            Runes.LAW, 1
    ), Spellbook.STANDARD, 14286920),
    VARROCK_TELEPORT(MagicAction.VARROCK_TELEPORT, Map.of(
            Runes.FIRE, 1,
            Runes.AIR, 3,
            Runes.LAW, 1
    ), Spellbook.STANDARD, 14286871),
    GRAND_EXCHANGE_TELEPORT(MagicAction.VARROCK_TELEPORT, Map.of(
            Runes.FIRE, 1,
            Runes.AIR, 3,
            Runes.LAW, 1
    ), Spellbook.STANDARD, 14286871),
    LUMBRIDGE_TELEPORT(MagicAction.LUMBRIDGE_TELEPORT, Map.of(
            Runes.EARTH, 1,
            Runes.AIR, 3,
            Runes.LAW, 1
    ), Spellbook.STANDARD, 14286874),
    FALADOR_TELEPORT(MagicAction.FALADOR_TELEPORT, Map.of(
            Runes.WATER, 1,
            Runes.AIR, 3,
            Runes.LAW, 1
    ), Spellbook.STANDARD, 14286877),
    TELEPORT_TO_HOUSE(MagicAction.TELEPORT_TO_HOUSE, Map.of(
            Runes.AIR, 1,
            Runes.EARTH, 1,
            Runes.LAW, 1
    ), Spellbook.STANDARD, 14286879),
    CAMELOT_TELEPORT(MagicAction.CAMELOT_TELEPORT, Map.of(
            Runes.AIR, 5,
            Runes.LAW, 1
    ), Spellbook.STANDARD, 14286882),
    SEERS_TELEPORT(MagicAction.CAMELOT_TELEPORT, Map.of(
            Runes.AIR, 5,
            Runes.LAW, 1
    ), Spellbook.STANDARD, 14286882),
    ARDOUGNE_TELEPORT(MagicAction.ARDOUGNE_TELEPORT, Map.of(
            Runes.WATER, 2,
            Runes.LAW, 2
    ), Spellbook.STANDARD, 14286889),
    WATCHTOWER_TELEPORT(MagicAction.WATCHTOWER_TELEPORT, Map.of(
            Runes.EARTH, 2,
            Runes.LAW, 2
    ), Spellbook.STANDARD, 14286895),
    YANILLE_TELEPORT(MagicAction.WATCHTOWER_TELEPORT, Map.of(
            Runes.EARTH, 2,
            Runes.LAW, 2
    ), Spellbook.STANDARD, 14286895),
    TROLLHEIM_TELEPORT(MagicAction.TROLLHEIM_TELEPORT, Map.of(
            Runes.FIRE, 2,
            Runes.LAW, 2
    ), Spellbook.STANDARD, 14286902),
    CIVITAS_ILLA_FORTIS_TELEPORT(MagicAction.CIVITAS_ILLA_FORTIS_TELEPORT, Map.of(
            Runes.LAW, 2,
            Runes.EARTH, 1,
            Runes.FIRE, 1
    ), Spellbook.STANDARD, 14286891),
    KOUREND_CASTLE_TELEPORT(MagicAction.KOUREND_CASTLE_TELEPORT, Map.of(
            Runes.LAW, 2,
            Runes.WATER, 1,
            Runes.FIRE, 1
    ), Spellbook.STANDARD, 14286884),
    TELEKINETIC_GRAB(MagicAction.TELEKINETIC_GRAB, Map.of(
            Runes.AIR, 1,
            Runes.LAW, 1
    ), Spellbook.STANDARD, 14286875),
    BONES_TO_BANANAS(MagicAction.BONES_TO_BANANAS, Map.of(
            Runes.WATER, 2,
            Runes.EARTH, 2,
            Runes.NATURE, 1
    ), Spellbook.STANDARD, 14286865),
    BONES_TO_PEACHES(MagicAction.BONES_TO_PEACHES, Map.of(
            Runes.WATER, 4,
            Runes.EARTH, 4,
            Runes.NATURE, 2
    ), Spellbook.STANDARD, 14286898),
    LOW_LEVEL_ALCHEMY(MagicAction.LOW_LEVEL_ALCHEMY, Map.of(
            Runes.FIRE, 3,
            Runes.NATURE, 1
    ), Spellbook.STANDARD, 14286869),
    SUPERHEAT_ITEM(MagicAction.SUPERHEAT_ITEM, Map.of(
            Runes.FIRE, 4,
            Runes.NATURE, 1
    ), Spellbook.STANDARD, 14286881),
    HIGH_LEVEL_ALCHEMY(MagicAction.HIGH_LEVEL_ALCHEMY, Map.of(
            Runes.FIRE, 5,
            Runes.NATURE, 1
    ), Spellbook.STANDARD, 14286892),

    ENCHANT_SAPPHIRE_JEWELLERY(MagicAction.ENCHANT_SAPPHIRE_JEWELLERY, Map.of(
            Runes.COSMIC, 1,
            Runes.WATER, 1
    ), Spellbook.STANDARD, 14286861),
    ENCHANT_EMERALD_JEWELLERY(MagicAction.ENCHANT_EMERALD_JEWELLERY, Map.of(
            Runes.COSMIC, 1,
            Runes.AIR, 3
    ), Spellbook.STANDARD, 14286872),
    ENCHANT_RUBY_JEWELLERY(MagicAction.ENCHANT_RUBY_JEWELLERY, Map.of(
            Runes.COSMIC, 1,
            Runes.FIRE, 5
    ), Spellbook.STANDARD, 14286885),
    ENCHANT_DIAMOND_JEWELLERY(MagicAction.ENCHANT_DIAMOND_JEWELLERY, Map.of(
            Runes.COSMIC, 1,
            Runes.EARTH, 10
    ), Spellbook.STANDARD, 14286894),
    ENCHANT_DRAGONSTONE_JEWELLERY(MagicAction.ENCHANT_DRAGONSTONE_JEWELLERY, Map.of(
            Runes.COSMIC, 1,
            Runes.WATER, 15
    ), Spellbook.STANDARD, 14286909),
    ENCHANT_ONYX_JEWELLERY(MagicAction.ENCHANT_ONYX_JEWELLERY, Map.of(
            Runes.COSMIC, 1,
            Runes.FIRE, 20
    ), Spellbook.STANDARD, 14286922),
    ENCHANT_ZENYTE_JEWELLERY(MagicAction.ENCHANT_ZENYTE_JEWELLERY, Map.of(
            Runes.COSMIC, 1,
            Runes.SOUL, 20,
            Runes.BLOOD, 20
    ), Spellbook.STANDARD, 14286925),
    CHARGE_WATER_ORB(MagicAction.CHARGE_WATER_ORB, Map.of(
            Runes.WATER, 30,
            Runes.COSMIC, 3
    ), Spellbook.STANDARD, 14286893),
    CHARGE_EARTH_ORB(MagicAction.CHARGE_EARTH_ORB, Map.of(
            Runes.EARTH, 30,
            Runes.COSMIC, 3
    ), Spellbook.STANDARD, 14286897),
    CHARGE_FIRE_ORB(MagicAction.CHARGE_FIRE_ORB, Map.of(
            Runes.FIRE, 30,
            Runes.COSMIC, 3
    ), Spellbook.STANDARD, 14286904),
    CHARGE_AIR_ORB(MagicAction.CHARGE_AIR_ORB, Map.of(
            Runes.AIR, 30,
            Runes.COSMIC, 3
    ), Spellbook.STANDARD, 14286907),

     // ================
     // Ancients
     // ===============
    SMOKE_RUSH(MagicAction.SMOKE_RUSH, Map.of(Runes.AIR, 1, Runes.FIRE, 1, Runes.CHAOS, 2, Runes.DEATH, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.SMOKE_RUSH),
    SHADOW_RUSH(MagicAction.SHADOW_RUSH, Map.of(Runes.AIR, 1, Runes.CHAOS, 2, Runes.DEATH, 2, Runes.SOUL, 1), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.SHADOW_RUSH),
    BLOOD_RUSH(MagicAction.BLOOD_RUSH, Map.of(Runes.CHAOS, 2, Runes.DEATH, 2, Runes.BLOOD, 1), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.BLOOD_RUSH),
    ICE_RUSH(MagicAction.ICE_RUSH, Map.of(Runes.WATER, 2, Runes.CHAOS, 2, Runes.DEATH, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.ICE_RUSH),
    SMOKE_BURST(MagicAction.SMOKE_BURST, Map.of(Runes.AIR, 2, Runes.FIRE, 2, Runes.CHAOS, 4, Runes.DEATH, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.SMOKE_BURST),
    SHADOW_BURST(MagicAction.SHADOW_BURST, Map.of(Runes.AIR, 1, Runes.CHAOS, 4, Runes.DEATH, 2, Runes.SOUL, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.SHADOW_BURST),
    BLOOD_BURST(MagicAction.BLOOD_BURST, Map.of(Runes.CHAOS, 2, Runes.DEATH, 4, Runes.BLOOD, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.BLOOD_BURST),
    ICE_BURST(MagicAction.ICE_BURST, Map.of(Runes.WATER, 4, Runes.CHAOS, 4, Runes.DEATH, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.ICE_BURST),
    SMOKE_BLITZ(MagicAction.SMOKE_BLITZ, Map.of(Runes.AIR, 2, Runes.FIRE, 2, Runes.DEATH, 2, Runes.BLOOD, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.SMOKE_BLITZ),
    SHADOW_BLITZ(MagicAction.SHADOW_BLITZ, Map.of(Runes.AIR, 2, Runes.DEATH, 2, Runes.BLOOD, 2, Runes.SOUL, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.SHADOW_BLITZ),
    BLOOD_BLITZ(MagicAction.BLOOD_BLITZ, Map.of(Runes.DEATH, 2, Runes.BLOOD, 4), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.BLOOD_BLITZ),
    ICE_BLITZ(MagicAction.ICE_BLITZ, Map.of(Runes.WATER, 3, Runes.DEATH, 2, Runes.BLOOD, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.ICE_BLITZ),
    SMOKE_BARRAGE(MagicAction.SMOKE_BARRAGE, Map.of(Runes.AIR, 4, Runes.FIRE, 4, Runes.DEATH, 4, Runes.BLOOD, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.SMOKE_BARRAGE),
    SHADOW_BARRAGE(MagicAction.SHADOW_BARRAGE, Map.of(Runes.AIR, 4, Runes.DEATH, 4, Runes.BLOOD, 2, Runes.SOUL, 3), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.SHADOW_BARRAGE),
    BLOOD_BARRAGE(MagicAction.BLOOD_BARRAGE, Map.of(Runes.DEATH, 4, Runes.BLOOD, 4, Runes.SOUL, 1), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.BLOOD_BARRAGE),
    ICE_BARRAGE(MagicAction.ICE_BARRAGE, Map.of(Runes.WATER, 6, Runes.DEATH, 4, Runes.BLOOD, 2), Spellbook.ANCIENT, InterfaceID.MagicSpellbook.ICE_BARRAGE),
    PADDEWWA_TELEPORT(MagicAction.PADDEWWA_TELEPORT, Map.of(
            Runes.AIR, 1,
            Runes.FIRE, 1,
            Runes.LAW, 2
    ), Spellbook.ANCIENT, 14286943),
    SENNTISTEN_TELEPORT(MagicAction.SENNTISTEN_TELEPORT, Map.of(
            Runes.LAW, 2,
            Runes.SOUL, 1
    ), Spellbook.ANCIENT, 14286944),
    KHARYRLL_TELEPORT(MagicAction.KHARYRLL_TELEPORT, Map.of(
            Runes.BLOOD, 1,
            Runes.LAW, 2
    ), Spellbook.ANCIENT, 14286945),
    LASSAR_TELEPORT(MagicAction.LASSAR_TELEPORT, Map.of(
            Runes.WATER, 4,
            Runes.LAW, 2
    ), Spellbook.ANCIENT, 14286946),
    DAREEYAK_TELEPORT(MagicAction.DAREEYAK_TELEPORT, Map.of(
            Runes.AIR, 2,
            Runes.FIRE, 3,
            Runes.LAW, 2
    ), Spellbook.ANCIENT, 14286947),
    CARRALLANGER_TELEPORT(MagicAction.CARRALLANGER_TELEPORT, Map.of(
            Runes.LAW, 2,
            Runes.SOUL, 2
    ), Spellbook.ANCIENT, 14286948),
    ANNAKARL_TELEPORT(MagicAction.ANNAKARL_TELEPORT, Map.of(
            Runes.BLOOD, 2,
            Runes.LAW, 2
    ), Spellbook.ANCIENT, 14286949),
    GHORROCK_TELEPORT(MagicAction.GHORROCK_TELEPORT, Map.of(
            Runes.WATER, 8,
            Runes.LAW, 2
    ), Spellbook.ANCIENT, 14286950),


    // =======================
    // Lunar Spellbook
    // =======================
    MONSTER_EXAMINE(MagicAction.MONSTER_EXAMINE, Map.of(
            Runes.ASTRAL, 1,
            Runes.COSMIC, 1,
            Runes.MIND, 1
    ), Spellbook.LUNAR, 14286955),
    CURE_OTHER(MagicAction.CURE_OTHER, Map.of(
            Runes.EARTH, 10,
            Runes.ASTRAL, 1,
            Runes.LAW, 1
    ), Spellbook.LUNAR, 14286957),
    CURE_ME(MagicAction.CURE_ME, Map.of(
            Runes.ASTRAL, 2,
            Runes.COSMIC, 2,
            Runes.LAW, 1
    ), Spellbook.LUNAR, 14286961),
    CURE_GROUP(MagicAction.CURE_GROUP, Map.of(
            Runes.ASTRAL, 2,
            Runes.COSMIC, 2,
            Runes.LAW, 2
    ), Spellbook.LUNAR, 14286965),
    STAT_SPY(MagicAction.STAT_SPY, Map.of(
            Runes.ASTRAL, 2,
            Runes.BODY, 5,
            Runes.COSMIC, 1
    ), Spellbook.LUNAR, 14286966),
    DREAM(MagicAction.DREAM, Map.of(
            Runes.ASTRAL, 2,
            Runes.BODY, 5,
            Runes.COSMIC, 1
    ), Spellbook.LUNAR, 14286973),
    STAT_RESTORE_POT_SHARE(MagicAction.STAT_RESTORE_POT_SHARE, Map.of(
            Runes.EARTH, 10,
            Runes.WATER, 10,
            Runes.ASTRAL, 2
    ), Spellbook.LUNAR, 14286975),
    BOOST_POTION_SHARE(MagicAction.BOOST_POTION_SHARE, Map.of(
            Runes.EARTH, 12,
            Runes.WATER, 10,
            Runes.ASTRAL, 3
    ), Spellbook.LUNAR, 14286978),
    ENERGY_TRANSFER(MagicAction.ENERGY_TRANSFER, Map.of(
            Runes.ASTRAL, 3,
            Runes.LAW, 2,
            Runes.NATURE, 1
    ), Spellbook.LUNAR, 14286987),
    HEAL_OTHER(MagicAction.HEAL_OTHER, Map.of(
            Runes.ASTRAL, 3,
            Runes.BLOOD, 1,
            Runes.LAW, 3
    ), Spellbook.LUNAR, 14286988),
    VENGEANCE_OTHER(MagicAction.VENGEANCE_OTHER, Map.of(
            Runes.EARTH, 10,
            Runes.ASTRAL, 3,
            Runes.DEATH, 2
    ), Spellbook.LUNAR, 14286989),
    VENGEANCE(MagicAction.VENGEANCE, Map.of(
            Runes.EARTH, 10,
            Runes.ASTRAL, 4,
            Runes.DEATH, 2
    ), Spellbook.LUNAR, 14286990),
    HEAL_GROUP(MagicAction.HEAL_GROUP, Map.of(
            Runes.ASTRAL, 4,
            Runes.BLOOD, 3,
            Runes.LAW, 6
    ), Spellbook.LUNAR, 14286991),

    MOONCLAN_TELEPORT(MagicAction.MOONCLAN_TELEPORT, Map.of(
            Runes.EARTH, 2,
            Runes.ASTRAL, 2,
            Runes.LAW, 1
    ), Spellbook.LUNAR, 14286959),
    TELE_GROUP_MOONCLAN(MagicAction.TELE_GROUP_MOONCLAN, Map.of(
            Runes.EARTH, 4,
            Runes.ASTRAL, 2,
            Runes.LAW, 1
    ), Spellbook.LUNAR, 14286960),
    OURANIA_TELEPORT(MagicAction.OURANIA_TELEPORT, Map.of(
            Runes.EARTH, 6,
            Runes.ASTRAL, 2,
            Runes.LAW, 1
    ), Spellbook.LUNAR, 14286995),
    WATERBIRTH_TELEPORT(MagicAction.WATERBIRTH_TELEPORT, Map.of(
            Runes.WATER, 1,
            Runes.ASTRAL, 2,
            Runes.LAW, 1
    ), Spellbook.LUNAR, 14286963),
    TELE_GROUP_WATERBIRTH(MagicAction.TELE_GROUP_WATERBIRTH, Map.of(
            Runes.WATER, 5,
            Runes.ASTRAL, 2,
            Runes.LAW, 1
    ), Spellbook.LUNAR, 14286964),
    BARBARIAN_TELEPORT(MagicAction.BARBARIAN_TELEPORT, Map.of(
            Runes.FIRE, 3,
            Runes.ASTRAL, 2,
            Runes.LAW, 2
    ), Spellbook.LUNAR, 14286967),
    TELE_GROUP_BARBARIAN(MagicAction.TELE_GROUP_BARBARIAN, Map.of(
            Runes.FIRE, 6,
            Runes.ASTRAL, 2,
            Runes.LAW, 2
    ), Spellbook.LUNAR, 14286968),
    KHAZARD_TELEPORT(MagicAction.KHAZARD_TELEPORT, Map.of(
            Runes.WATER, 4,
            Runes.ASTRAL, 2,
            Runes.LAW, 2
    ), Spellbook.LUNAR, 14286971),
    TELE_GROUP_KHAZARD(MagicAction.TELE_GROUP_KHAZARD, Map.of(
            Runes.WATER, 8,
            Runes.ASTRAL, 2,
            Runes.LAW, 2
    ), Spellbook.LUNAR, 14286972),
    FISHING_GUILD_TELEPORT(MagicAction.FISHING_GUILD_TELEPORT, Map.of(
            Runes.WATER, 10,
            Runes.ASTRAL, 3,
            Runes.LAW, 3
    ), Spellbook.LUNAR, 14286979),
    TELE_GROUP_FISHING_GUILD(MagicAction.TELE_GROUP_FISHING_GUILD, Map.of(
            Runes.WATER, 15,
            Runes.ASTRAL, 3,
            Runes.LAW, 3
    ), Spellbook.LUNAR, 14286980),
    CATHERBY_TELEPORT(MagicAction.CATHERBY_TELEPORT, Map.of(
            Runes.WATER, 10,
            Runes.ASTRAL, 3,
            Runes.LAW, 3
    ), Spellbook.LUNAR, 14286982),
    TELE_GROUP_CATHERBY(MagicAction.TELE_GROUP_CATHERBY, Map.of(
            Runes.WATER, 15,
            Runes.ASTRAL, 3,
            Runes.LAW, 3
    ), Spellbook.LUNAR, 14286983),
    ICE_PLATEAU_TELEPORT(MagicAction.ICE_PLATEAU_TELEPORT, Map.of(
            Runes.WATER, 8,
            Runes.ASTRAL, 3,
            Runes.LAW, 3
    ), Spellbook.LUNAR, 14286985),
    TELE_GROUP_ICE_PLATEAU(MagicAction.TELE_GROUP_ICE_PLATEAU, Map.of(
            Runes.WATER, 16,
            Runes.ASTRAL, 3,
            Runes.LAW, 3
    ), Spellbook.LUNAR, 14286986),
    BAKE_PIE(MagicAction.BAKE_PIE, Map.of(
            Runes.FIRE, 5,
            Runes.WATER, 4,
            Runes.ASTRAL, 1
    ), Spellbook.LUNAR, 14286953),
    GEOMANCY(MagicAction.GEOMANCY, Map.of(
            Runes.EARTH, 8,
            Runes.ASTRAL, 3,
            Runes.NATURE, 3
    ), Spellbook.LUNAR, 14286993),
    CURE_PLANT(MagicAction.CURE_PLANT, Map.of(
            Runes.EARTH, 8,
            Runes.ASTRAL, 1
    ), Spellbook.LUNAR, 14286954),
    NPC_CONTACT(MagicAction.NPC_CONTACT, Map.of(
            Runes.AIR, 2,
            Runes.ASTRAL, 1,
            Runes.COSMIC, 1
    ), Spellbook.LUNAR, 14286956),
    HUMIDIFY(MagicAction.HUMIDIFY, Map.of(
            Runes.FIRE, 1,
            Runes.WATER, 3,
            Runes.ASTRAL, 1
    ), Spellbook.LUNAR, 14286958),
    HUNTER_KIT(MagicAction.HUNTER_KIT, Map.of(
            Runes.EARTH, 2,
            Runes.ASTRAL, 2
    ), Spellbook.LUNAR, 14286962),
    SPIN_FLAX(MagicAction.SPIN_FLAX, Map.of(
            Runes.AIR, 5,
            Runes.ASTRAL, 1,
            Runes.NATURE, 2
    ), Spellbook.LUNAR, 14286994),
    SUPERGLASS_MAKE(MagicAction.SUPERGLASS_MAKE, Map.of(
            Runes.AIR, 10,
            Runes.FIRE, 6,
            Runes.ASTRAL, 2
    ), Spellbook.LUNAR, 14286969),
    TAN_LEATHER(MagicAction.TAN_LEATHER, Map.of(
            Runes.FIRE, 5,
            Runes.ASTRAL, 2,
            Runes.NATURE, 1
    ), Spellbook.LUNAR, 14286970),
    STRING_JEWELLERY(MagicAction.STRING_JEWELLERY, Map.of(
            Runes.EARTH, 10,
            Runes.WATER, 5,
            Runes.ASTRAL, 2
    ), Spellbook.LUNAR, 14286974),
    MAGIC_IMBUE(MagicAction.MAGIC_IMBUE, Map.of(
            Runes.FIRE, 7,
            Runes.WATER, 7,
            Runes.ASTRAL, 2
    ), Spellbook.LUNAR, 14286976),
    FERTILE_SOIL(MagicAction.FERTILE_SOIL, Map.of(
            Runes.EARTH, 15,
            Runes.ASTRAL, 3,
            Runes.NATURE, 2
    ), Spellbook.LUNAR, 14286977),
    PLANK_MAKE(MagicAction.PLANK_MAKE, Map.of(
            Runes.EARTH, 15,
            Runes.ASTRAL, 2,
            Runes.NATURE, 1
    ), Spellbook.LUNAR, 14286981),
    RECHARGE_DRAGONSTONE(MagicAction.RECHARGE_DRAGONSTONE, Map.of(
            Runes.WATER, 4,
            Runes.ASTRAL, 1,
            Runes.SOUL, 1
    ), Spellbook.LUNAR, 14286984),
    SPELLBOOK_SWAP(MagicAction.SPELLBOOK_SWAP, Map.of(
            Runes.ASTRAL, 3,
            Runes.COSMIC, 2,
            Runes.LAW, 1
    ), Spellbook.LUNAR, 14286992),

     // ===========================
     // ARCEUUS SPELLBOOK
     // ===========================
    RESURRECT_LESSER_GHOST(MagicAction.RESURRECT_LESSER_THRALL, Map.of(
            Runes.MIND, 5,
            Runes.AIR, 10,
            Runes.COSMIC, 1
    ), Spellbook.ARCEUUS, 14287032),
    RESURRECT_LESSER_SKELETON(MagicAction.RESURRECT_LESSER_THRALL, Map.of(
            Runes.MIND, 5,
            Runes.AIR, 10,
            Runes.COSMIC, 1
    ), Spellbook.ARCEUUS, 14287033),
    RESURRECT_LESSER_ZOMBIE(MagicAction.RESURRECT_LESSER_THRALL, Map.of(
            Runes.MIND, 5,
            Runes.AIR, 10,
            Runes.COSMIC, 1
    ), Spellbook.ARCEUUS, 14287034),
    RESURRECT_SUPERIOR_GHOST(MagicAction.RESURRECT_SUPERIOR_THRALL, Map.of(
            Runes.DEATH, 5,
            Runes.EARTH, 10,
            Runes.COSMIC, 1
    ), Spellbook.ARCEUUS, 14287035),
    RESURRECT_SUPERIOR_SKELETON(MagicAction.RESURRECT_SUPERIOR_THRALL, Map.of(
            Runes.DEATH, 5,
            Runes.EARTH, 10,
            Runes.COSMIC, 1
    ), Spellbook.ARCEUUS, 14287036),
    RESURRECT_SUPERIOR_ZOMBIE(MagicAction.RESURRECT_SUPERIOR_THRALL, Map.of(
            Runes.DEATH, 5,
            Runes.EARTH, 10,
            Runes.COSMIC, 1
    ), Spellbook.ARCEUUS, 14287037),
    RESURRECT_GREATER_GHOST(MagicAction.RESURRECT_GREATER_THRALL, Map.of(
            Runes.BLOOD, 5,
            Runes.FIRE, 10,
            Runes.COSMIC, 1
    ), Spellbook.ARCEUUS, 14287038),
    RESURRECT_GREATER_SKELETON(MagicAction.RESURRECT_GREATER_THRALL, Map.of(
            Runes.BLOOD, 5,
            Runes.FIRE, 10,
            Runes.COSMIC, 1
    ), Spellbook.ARCEUUS, 14287039),
    RESURRECT_GREATER_ZOMBIE(MagicAction.RESURRECT_GREATER_THRALL, Map.of(
            Runes.BLOOD, 5,
            Runes.FIRE, 10,
            Runes.COSMIC, 1
    ), Spellbook.ARCEUUS, 14287040),
    ARCEUUS_LIBRARY_TELEPORT(MagicAction.ARCEUUS_LIBRARY_TELEPORT, Map.of(
            Runes.EARTH, 2,
            Runes.LAW, 1
    ), Spellbook.ARCEUUS, 14286998),
    DRAYNOR_MANOR_TELEPORT(MagicAction.DRAYNOR_MANOR_TELEPORT, Map.of(
            Runes.EARTH, 1,
            Runes.WATER, 1,
            Runes.LAW, 1
    ), Spellbook.ARCEUUS, 14287002),
    MIND_ALTAR_TELEPORT(MagicAction.MIND_ALTAR_TELEPORT, Map.of(
            Runes.LAW, 1,
            Runes.MIND, 2
    ), Spellbook.ARCEUUS, 14287004),
    RESPAWN_TELEPORT(MagicAction.RESPAWN_TELEPORT, Map.of(
            Runes.LAW, 1,
            Runes.SOUL, 1
    ), Spellbook.ARCEUUS, 14287005),
    SALVE_GRAVEYARD_TELEPORT(MagicAction.SALVE_GRAVEYARD_TELEPORT, Map.of(
            Runes.LAW, 1,
            Runes.SOUL, 2
    ), Spellbook.ARCEUUS, 14287006),
    FENKENSTRAINS_CASTLE_TELEPORT(MagicAction.FENKENSTRAINS_CASTLE_TELEPORT, Map.of(
            Runes.EARTH, 1,
            Runes.LAW, 1,
            Runes.SOUL, 1
    ), Spellbook.ARCEUUS, 14287007),
    HARMONY_ISLAND_TELEPORT(MagicAction.HARMONY_ISLAND_TELEPORT, Map.of(
            Runes.LAW, 1,
            Runes.NATURE, 1,
            Runes.SOUL, 1
    ), Spellbook.ARCEUUS,14287009),
    CEMETERY_TELEPORT(MagicAction.CEMETERY_TELEPORT, Map.of(
            Runes.BLOOD, 1,
            Runes.LAW, 1,
            Runes.SOUL, 1
    ), Spellbook.ARCEUUS, 14287010),
    BARROWS_TELEPORT(MagicAction.BARROWS_TELEPORT, Map.of(
            Runes.BLOOD, 1,
            Runes.LAW, 2,
            Runes.SOUL, 2
    ), Spellbook.ARCEUUS, 14287012),
    APE_ATOLL_TELEPORT(MagicAction.APE_ATOLL_TELEPORT, Map.of(
            Runes.BLOOD, 2,
            Runes.LAW, 2,
            Runes.SOUL, 2
    ), Spellbook.ARCEUUS, 14287013),
    GHOSTLY_GRASP(MagicAction.GHOSTLY_GRASP, Map.of(
            Runes.AIR, 4,
            Runes.CHAOS, 1
    ), Spellbook.ARCEUUS, 14287019),
    SKELETAL_GRASP(MagicAction.SKELETAL_GRASP, Map.of(
            Runes.EARTH, 8,
            Runes.DEATH, 1
    ), Spellbook.ARCEUUS, 14287020),
    UNDEAD_GRASP(MagicAction.UNDEAD_GRASP, Map.of(
            Runes.FIRE, 12,
            Runes.BLOOD, 1
    ), Spellbook.ARCEUUS, 14287021),
    INFERIOR_DEMONBANE(MagicAction.INFERIOR_DEMONBANE, Map.of(
            Runes.FIRE, 4,
            Runes.CHAOS, 1
    ), Spellbook.ARCEUUS, 14287015),
    SUPERIOR_DEMONBANE(MagicAction.SUPERIOR_DEMONBANE, Map.of(
            Runes.FIRE, 8,
            Runes.SOUL, 1
    ), Spellbook.ARCEUUS, 14287016),
    DARK_DEMONBANE(MagicAction.DARK_DEMONBANE, Map.of(
            Runes.FIRE, 12,
            Runes.SOUL, 2
    ), Spellbook.ARCEUUS, 14287017),
    LESSER_CORRUPTION(MagicAction.LESSER_CORRUPTION, Map.of(
            Runes.DEATH, 1,
            Runes.SOUL, 2
    ), Spellbook.ARCEUUS, 14287023),
    GREATER_CORRUPTION(MagicAction.GREATER_CORRUPTION, Map.of(
            Runes.BLOOD, 1,
            Runes.SOUL, 3
    ), Spellbook.ARCEUUS, 14287024),
    DARK_LURE(MagicAction.DARK_LURE, Map.of(
            Runes.DEATH, 1,
            Runes.NATURE, 1
    ), Spellbook.ARCEUUS, 14287030),
    MARK_OF_DARKNESS(MagicAction.MARK_OF_DARKNESS, Map.of(
            Runes.COSMIC, 1,
            Runes.SOUL, 1
    ), Spellbook.ARCEUUS, 14287018),
    WARD_OF_ARCEUUS(MagicAction.WARD_OF_ARCEUUS, Map.of(
            Runes.COSMIC, 1,
            Runes.NATURE, 2,
            Runes.SOUL, 4
    ), Spellbook.ARCEUUS, 14287022),
    BASIC_REANIMATION(MagicAction.BASIC_REANIMATION, Map.of(
            Runes.BODY, 4,
            Runes.NATURE, 2
    ), Spellbook.ARCEUUS, 14286997),
    ADEPT_REANIMATION(MagicAction.ADEPT_REANIMATION, Map.of(
            Runes.BODY, 4,
            Runes.NATURE, 3,
            Runes.SOUL, 1
    ), Spellbook.ARCEUUS, 14286999),
    EXPERT_REANIMATION(MagicAction.EXPERT_REANIMATION, Map.of(
            Runes.BLOOD, 1,
            Runes.NATURE, 3,
            Runes.SOUL, 2
    ), Spellbook.ARCEUUS, 14287000),
    MASTER_REANIMATION(MagicAction.MASTER_REANIMATION, Map.of(
            Runes.BLOOD, 2,
            Runes.NATURE, 4,
            Runes.SOUL, 4
    ), Spellbook.ARCEUUS, 14287001),
    DEMONIC_OFFERING(MagicAction.DEMONIC_OFFERING, Map.of(
            Runes.SOUL, 1,
            Runes.WRATH, 1
    ), Spellbook.ARCEUUS, 14287025),
    SINISTER_OFFERING(MagicAction.SINISTER_OFFERING, Map.of(
            Runes.BLOOD, 1,
            Runes.WRATH, 1
    ), Spellbook.ARCEUUS, 14287026),
    SHADOW_VEIL(MagicAction.SHADOW_VEIL, Map.of(
            Runes.EARTH, 5,
            Runes.FIRE, 5,
            Runes.COSMIC, 5
    ), Spellbook.ARCEUUS, 14287028),
    VILE_VIGOUR(MagicAction.VILE_VIGOUR, Map.of(
            Runes.AIR, 3,
            Runes.SOUL, 1
    ), Spellbook.ARCEUUS, 14287029),
    DEGRIME(MagicAction.DEGRIME, Map.of(
            Runes.EARTH, 4,
            Runes.NATURE, 2
    ), Spellbook.ARCEUUS, 14287027),
    RESURRECT_CROPS(MagicAction.RESURRECT_CROPS, Map.of(
            Runes.EARTH, 25,
            Runes.BLOOD, 8,
            Runes.NATURE, 12,
            Runes.SOUL, 8
    ), Spellbook.ARCEUUS, 14287011),
    DEATH_CHARGE(MagicAction.DEATH_CHARGE, Map.of(
            Runes.BLOOD, 1,
            Runes.DEATH, 1,
            Runes.SOUL, 1
    ), Spellbook.ARCEUUS, InterfaceID.MagicSpellbook.DEATH_CHARGE);

    private final String name;
    private final MagicAction magicAction;
    private final Map<Runes, Integer> requiredRunes;
    private final Spellbook spellbook;
    private final int requiredLevel;
    private final int widgetId;

    Spells(MagicAction magicAction, Map<Runes, Integer> requiredRunes, Spellbook spellbook, int widgetId) {
        this.magicAction = magicAction;
        this.requiredRunes = requiredRunes;
        this.spellbook = spellbook;
        this.name = magicAction.getName();
        this.requiredLevel = magicAction.getLevel();
        this.widgetId = widgetId;
    }

    /**
     * Returns only the elemental runes (Air, Water, Earth, Fire) required for this spell.
     * This is useful for transports that use elemental staves or combination runes.
     *
     * @return A list of elemental Runes enums used in this spell
     */
    public List<Runes> getElementalRunes() {
        return requiredRunes.keySet().stream()
                .filter(rune -> rune == Runes.AIR || rune == Runes.WATER ||
                        rune == Runes.EARTH || rune == Runes.FIRE)
                .collect(Collectors.toList());
    }

    /**
     * Returns a copy of the required runes for this spell.
     *
     * @return A HashMap containing the required runes and their quantities
     */
    public HashMap<Runes, Integer> getRequiredRunes() {
        return new HashMap<>(requiredRunes);
    }
}