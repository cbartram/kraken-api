package com.kraken.api.service.ui.grandexchange;

import com.kraken.api.Context;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;

@Slf4j
@Getter
public enum GrandExchangeSlot {
    SLOT_1(InterfaceID.GeOffers.INDEX_0, 1),
    SLOT_2(InterfaceID.GeOffers.INDEX_1, 2),
    SLOT_3(InterfaceID.GeOffers.INDEX_2, 3),
    SLOT_4(InterfaceID.GeOffers.INDEX_3, 4),
    SLOT_5(InterfaceID.GeOffers.INDEX_4, 5),
    SLOT_6(InterfaceID.GeOffers.INDEX_5, 6),
    SLOT_7(InterfaceID.GeOffers.INDEX_6, 7),
    SLOT_8(InterfaceID.GeOffers.INDEX_7, 8);

    private final int id;
    private final int slot;
    private final int buyChild = 3;
    private final int sellChild = 4;
    private final Context ctx;

    GrandExchangeSlot(int id, int slot) {
        this.id = id;
        this.slot = slot;
        this.ctx = RuneLite.getInjector().getInstance(Context.class);
    }

    /**
     * Returns true when a grand exchange slot has been fulfilled (the item has been bought or sold).
     * @return True when the slot of fulfilled and false otherwise
     */
    public boolean isFulfilled() {
        Client client = ctx.getClient();
        return ctx.runOnClientThread(() ->{
            Widget widget = client.getWidget(id);
            if(widget == null) return false;
            Widget child = widget.getChild(22);
            if(child == null || (widget.isHidden() && widget.isSelfHidden())) return false;
            return Integer.toString(child.getTextColor(), 16).equals("5f00");
        });
    }

    /**
     * Returns the item id for the item in the grand exchange offer slot. This is the item
     * being bought or sold.
     * @return int
     */
    public int getItemId() {
        return ctx.runOnClientThread(() -> {
            Widget widget = ctx.getClient().getWidget(id);
            if(widget == null) return -1;
            Widget child = widget.getChild(18);
            if(child == null) return -1;
            return child.getItemId();
        });
    }

    /**
     * Given an integer for the slot number, returns the grand exchange slot object for that slot.
     * @param slot The slot number between 1 and 8
     * @return GrandExchangeSlot or null if no slot could be found.
     */
    public static GrandExchangeSlot getBySlot(int slot) {
        if(slot < 1 || slot > 8) {
            log.warn("Invalid Grand Exchange slot: {}, must be between 1 and 8", slot);
            return null;
        }
        for(GrandExchangeSlot s : GrandExchangeSlot.values()) {
            if(s.getSlot() == slot) return s;
        }
        return null;
    }
}