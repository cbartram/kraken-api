package com.kraken.api.service.ui.grandexchange;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.service.util.SleepService;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;

@Singleton
public class GrandExchangeService {

    @Inject
    private WidgetPackets widgetPackets;

    @Inject
    private Context ctx;

    /**
     * Checks if the Grand Exchange interface is currently open.
     * @return true if the Grand Exchange interface is open, false otherwise.
     */
    public boolean isOpen() {
        return ctx.runOnClientThread(() -> {
            Widget w = ctx.getClient().getWidget(InterfaceID.GeOffers.UNIVERSE);
            return w != null && !w.isSelfHidden() && !w.isHidden();
        });
    }

    /**
     * Returns true when an offer details interface is open for a specific item
     * @param itemId The item id to check for
     * @return True if the offer details interface is open and false otherwise
     */
    public boolean isOfferDetailsOpen(int itemId) {
        Client client = ctx.getClient();
        return ctx.runOnClientThread(() -> {
            Widget w = client.getWidget(InterfaceID.GeOffers.DETAILS_COLLECT, 2);
            boolean isVisible =  w != null && !w.isHidden() && !w.isSelfHidden();
            return isVisible && w.getItemId() == itemId;
        });
    }

    /**
     * Collects items from a canceled or completed Grand Exchange offer in the specified slot.
     * @param slot The GrandExchangeSlot to collect from.
     * @param noted True to collect noted items, false to collect unnoted items.
     */
    public void collect(GrandExchangeSlot slot, boolean noted) {
        if(slot == null) return;
        int itemId = slot.getItemId();
        ctx.runOnClientThread(() -> {
            if(!isOfferDetailsOpen(itemId)) {
                widgetPackets.queueWidgetActionPacket(slot.getId(), 2,  -1, 1);
            }
            // action, widget, child, item -> widget, child, item, action
            widgetPackets.queueWidgetActionPacket(InterfaceID.GeOffers.DETAILS_COLLECT, 2, itemId, noted ? 1 : 2);
            widgetPackets.queueWidgetActionPacket(InterfaceID.GeOffers.DETAILS_COLLECT, 3, ItemID.COINS, 1);
        });
    }

    /**
     * Cancels an active Grand Exchange offer in the specified slot.
     * @param slot The GrandExchangeSlot to cancel.
     */
    public void cancel(GrandExchangeSlot slot) {
        widgetPackets.queueWidgetActionPacket(2, slot.getId(), 2, -1);
        SleepService.sleepFor(2);
    }
}
