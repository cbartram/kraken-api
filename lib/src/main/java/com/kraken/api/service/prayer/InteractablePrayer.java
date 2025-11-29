package com.kraken.api.service.prayer;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.runelite.api.Prayer;
import net.runelite.api.annotations.Component;
import net.runelite.api.annotations.Varbit;
import net.runelite.api.gameval.VarbitID;

@Getter
@RequiredArgsConstructor
public enum InteractablePrayer {
    THICK_SKIN("Thick Skin", 35454985, 1, 5.0, VarbitID.PRAYER_THICKSKIN, 0),
    BURST_STRENGTH("Burst of Strength", 35454986, 4, 5.0, VarbitID.PRAYER_BURSTOFSTRENGTH, 1),
    CLARITY_THOUGHT("Clarity of Thought", 35454987, 7, 5.0, VarbitID.PRAYER_CLARITYOFTHOUGHT, 2),
    SHARP_EYE("Sharp Eye", 35455003, 8, 5.0, VarbitID.PRAYER_SHARPEYE, 3),
    MYSTIC_WILL("Mystic Will", 35455006, 9, 5.0, VarbitID.PRAYER_MYSTICWILL, 19),
    ROCK_SKIN("Rock Skin", 35454988, 10, 10.0, VarbitID.PRAYER_ROCKSKIN, 3),
    SUPERHUMAN_STRENGTH("Superhuman Strength", 35454989, 13, 10.0, VarbitID.PRAYER_SUPERHUMANSTRENGTH, 4),
    IMPROVED_REFLEXES("Improved Reflexes", 35454990, 16, 10.0, VarbitID.PRAYER_IMPROVEDREFLEXES, 5),
    RAPID_RESTORE("Rapid Restore", 35454991, 19, 60.0 / 36.0, VarbitID.PRAYER_RAPIDRESTORE, 6),
    RAPID_HEAL("Rapid heal", 35454992, 22, 60.0 / 18, VarbitID.PRAYER_RAPIDHEAL, 7),
    PROTECT_ITEM("Protect Item", 35454993, 25, 60.0 / 18,  VarbitID.PRAYER_PROTECTITEM, 8),
    HAWK_EYE("Hawk eye", 35455004, 26, 10.0, VarbitID.PRAYER_HAWKEYE, 20),
    MYSTIC_LORE("Mystic Lore", 35455007, 27, 10.0, VarbitID.PRAYER_MYSTICLORE, 21),
    STEEL_SKIN("Steel Skin", 35454994, 28, 20.0, VarbitID.PRAYER_STEELSKIN, 9),
    ULTIMATE_STRENGTH("Ultimate Strength", 35454995, 31, 20.0, VarbitID.PRAYER_ULTIMATESTRENGTH, 10),
    INCREDIBLE_REFLEXES("Incredible Reflexes", 35454996, 34, 20.0, VarbitID.PRAYER_INCREDIBLEREFLEXES, 11),
    PROTECT_MAGIC("Protect From Magic", 35454997, 37, 20.0, VarbitID.PRAYER_PROTECTFROMMAGIC, 12),
    PROTECT_RANGE("Protect From Missiles", 35454998, 40, 20.0, VarbitID.PRAYER_PROTECTFROMMISSILES, 13),
    PROTECT_MELEE("Protect From Melee", 35454999, 43, 20.0, VarbitID.PRAYER_PROTECTFROMMELEE, 14),
    EAGLE_EYE("Eagle Eye", 35455005, 44, 20.0, VarbitID.PRAYER_EAGLEEYE, 22),
    MYSTIC_MIGHT("Mystic Might", 35455008, 45, 20.0, VarbitID.PRAYER_MYSTICMIGHT, 23),
    RETRIBUTION("Retribution", 35455000, 46, 5.0, VarbitID.PRAYER_RETRIBUTION, 15),
    REDEMPTION("Redemption", 35455001, 49, 10.0, VarbitID.PRAYER_REDEMPTION, 16),
    SMITE("Smite", 35455002, 52, 30.0, VarbitID.PRAYER_SMITE, 17),
    PRESERVE("Preserve", 35455013, 55, 60.0 / 18.0, VarbitID.PRAYER_PRESERVE, 28),
    CHIVALRY("Chivalry", 35455010,60, 40.0, VarbitID.PRAYER_CHIVALRY, 25),
    PIETY("Piety", 35455011, 70, 40.0, VarbitID.PRAYER_PIETY, 26),
    RIGOUR("Rigour", 35455009, 74, 40.0, VarbitID.PRAYER_RIGOUR, 24),
    AUGURY("Augury", 35455012, 77, 40.0, VarbitID.PRAYER_AUGURY, 27);

    private final String name;

    // The Widget ID for the Prayer
    @Component
    private final int index;
    private final int level;
    private final double drainRate;
    @Varbit
    private final int varbit;
    private final int quickPrayerIndex;

    /**
     * Returns the Kraken prayer (from this class) given a RuneLite API Prayer enum or null if the prayer doesn't exist.
     * @param p RuneLite prayer
     * @return Kraken Prayer (Prayers)
     */
    public static InteractablePrayer of(Prayer p) {
        for (InteractablePrayer krakenPrayer : InteractablePrayer.values()) {
            if (p.getVarbit() == krakenPrayer.getVarbit()) {
                return krakenPrayer;
            }
        }
        return null;
    }

    public static Prayer runelitePrayerFor(InteractablePrayer p) {
        for(Prayer runelitePrayer : Prayer.values()) {
            if(runelitePrayer.getVarbit() == p.getVarbit()) {
                return runelitePrayer;
            }
        }
        return null;
    }
}
