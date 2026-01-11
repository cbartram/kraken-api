package com.kraken.api.service.ui.grandexchange;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.query.container.inventory.InventoryEntity;
import com.kraken.api.service.ui.UIService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.GrandExchangeOffer;
import net.runelite.api.GrandExchangeOfferState;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.ItemID;
import net.runelite.api.widgets.Widget;

@Slf4j
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
     * Attempts to find the first free GE Offer slot. This returns null if no slot could be found
     * @return GrandExchangeSlot the first free slot or null if it cannot be found.
     */
     public GrandExchangeSlot getFirstFreeSlot() {
         try {
             Client client = ctx.getClient();
             GrandExchangeOffer[] offers = ctx.runOnClientThread(client::getGrandExchangeOffers);
             for (int slot = 0; slot < 8; slot++) {
                 if (offers[slot] == null || offers[slot].getState() == GrandExchangeOfferState.EMPTY) {
                     return GrandExchangeSlot.getBySlot(slot + 1);
                 }
             }
         } catch (Exception e) {
             log.error("Failed to get next free GE slot: ", e);
         }
         return null;
     }

    /**
     * Collects all completed offers in the Grand Exchange.
     */
    public void collectAll() {
        widgetPackets.queueWidgetActionPacket(InterfaceID.GeOffers.COLLECTALL, 0,  -1, 1);
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
     * Queues a sell offer in the first free Grand Exchange slot. When the amount specified is -1 then all of that item
     * will be sold as part of the offer.
     * @param itemId The item id of the item to sell.
     * @param amount The amount to sell. Use -1 to sell all available.
     * @param price The price per item.
     * @return The GrandExchangeSlot object for the GE slot that was used to queue the sell order, or null if no slot is free or an error occurs.
     */
    public GrandExchangeSlot queueSellOrder(int itemId, int amount, int price) {
        GrandExchangeSlot slot = getFirstFreeSlot();
        if(slot == null) return null;
        ctx.runOnClientThread(() -> {
            InventoryEntity item = ctx.inventory().withId(itemId).first();
            if(item == null) {
                log.error("No item with id: {} found in inventory", itemId);
                return;
            }

            int itemSlot = item.raw().getSlot();
            widgetPackets.queueWidgetActionPacket(slot.getId(), slot.getSellChild(), -1, 1);
            widgetPackets.queueWidgetActionPacket(InterfaceID.GeOffersSide.ITEMS, itemSlot, itemId, 1);

            widgetPackets.queueWidgetActionPacket(InterfaceID.GeOffers.SETUP, 12, -1, 1);
            widgetPackets.queueResumeCount(price);
            if(amount != -1) {
                widgetPackets.queueWidgetActionPacket(InterfaceID.GeOffers.SETUP, 7, -1, 1);
                widgetPackets.queueResumeCount(amount);
            }
            widgetPackets.queueWidgetActionPacket(InterfaceID.GeOffers.SETUP_CONFIRM, -1, -1, 1);
            widgetPackets.queueResumeCount(1);
            UIService.closeNumberDialogue();
        });

        return slot;
    }

    /**
     * Queues a sell offer for all of a specific item in the first free Grand Exchange slot.
     * @param itemId The item id to sell
     * @param price The price to sell the item for.
     * @return The GrandExchangeSlot object for the GE slot that was used to queue the sell order, or null if no slot is free or an error occurs.
     */
    public GrandExchangeSlot queueSellOrder(int itemId, int price) {
        return queueSellOrder(itemId, -1, price);
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
