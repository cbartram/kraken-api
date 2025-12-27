package com.kraken.api.service.ui.tab;

import com.kraken.api.Context;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.gameval.VarbitID;

import javax.inject.Inject;
import javax.inject.Singleton;

@Slf4j
@Singleton
public class TabService {

    @Inject
    private Context ctx;

    @Inject
    private SleepService sleepService;

    public InterfaceTab getCurrentTab() {
        final int varcIntValue = ctx.getClient().getVarcIntValue(171); // Inventory tab varc int
        switch (VarcIntValues.valueOf(varcIntValue)) {
            case TAB_COMBAT_OPTIONS:
                return InterfaceTab.COMBAT;
            case TAB_SKILLS:
                return InterfaceTab.SKILLS;
            case TAB_QUEST_LIST:
                return InterfaceTab.QUESTS;
            case TAB_INVENTORY:
                return InterfaceTab.INVENTORY;
            case TAB_WORN_EQUIPMENT:
                return InterfaceTab.EQUIPMENT;
            case TAB_PRAYER:
                return InterfaceTab.PRAYER;
            case TAB_SPELLBOOK:
                return InterfaceTab.MAGIC;
            case TAB_FRIEND_LIST:
                return InterfaceTab.FRIENDS;
            case TAB_LOGOUT:
                return InterfaceTab.LOGOUT;
            case TAB_SETTINGS:
                return InterfaceTab.SETTINGS;
            case TAB_MUSIC:
                return InterfaceTab.MUSIC;
            case TAB_CHAT_CHANNEL:
                return InterfaceTab.CHAT;
            case TAB_ACC_MANAGEMENT:
                return InterfaceTab.ACC_MAN;
            case TAB_EMOTES:
                return InterfaceTab.EMOTES;
            case TAB_NOT_SELECTED:
                return InterfaceTab.NOTHING_SELECTED;
            default:
                throw new IllegalStateException("Unexpected value: " + VarcIntValues.valueOf(varcIntValue));
        }
    }


    public boolean switchTo(InterfaceTab tab) {
        if (isCurrentTab(tab)) return true;

        if (tab == InterfaceTab.NOTHING_SELECTED && ctx.getVarbitValue(VarbitID.RESIZABLE_STONE_ARRANGEMENT) == 0)
            return false;

        // 915 is a tab switch script
        ctx.getClientThread().invokeLater(() -> ctx.getClient().runScript(915, tab.getIndex()));
        return sleepService.sleepUntil(() -> isCurrentTab(tab));
    }

    public boolean isCurrentTab(InterfaceTab tab) {
        return getCurrentTab() == tab;
    }
}
