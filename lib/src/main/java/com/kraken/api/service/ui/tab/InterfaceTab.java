package com.kraken.api.service.ui.tab;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.gameval.VarbitID;

@AllArgsConstructor
public enum InterfaceTab {
    COMBAT("Combat Options", VarbitID.STONE_COMBAT_KEY, 0),
    SKILLS("Skills", VarbitID.STONE_STATS_KEY, 1),
    QUESTS("Quest List", VarbitID.STONE_JOURNAL_KEY, 2),
    INVENTORY("Inventory", VarbitID.STONE_INV_KEY, 3),
    EQUIPMENT("Worn Equipment", VarbitID.STONE_WORN_KEY, 4),
    PRAYER("Prayer", VarbitID.STONE_PRAYER_KEY, 5),
    MAGIC("Magic", VarbitID.STONE_MAGIC_KEY, 6),
    FRIENDS("Friends List", VarbitID.STONE_FRIENDS_KEY, 9),
    SETTINGS("Settings", VarbitID.STONE_OPTIONS1_KEY, 11),
    MUSIC("Music Player", VarbitID.STONE_MUSIC_KEY, 13),
    LOGOUT("Logout", VarbitID.STONE_LOGOUT_KEY, 10),
    CHAT("Chat Channel", VarbitID.STONE_CLANCHAT_KEY, 7),
    ACC_MAN("Account Management", VarbitID.STONE_ACCOUNT_KEY, 8),
    EMOTES("Emotes", VarbitID.STONE_OPTIONS2_KEY, 12),
    NOTHING_SELECTED("NothingSelected", -1, -1);

    @Getter
    private final String name;

    @Getter
    private final int hotkeyVarbit;

    @Getter
    private final int index;
}
