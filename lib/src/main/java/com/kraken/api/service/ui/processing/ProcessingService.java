package com.kraken.api.service.ui.processing;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.query.container.ContainerItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarClientID;
import net.runelite.api.widgets.Widget;
import org.apache.commons.lang3.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class ProcessingService {

    @Inject
    private Context ctx;

    @Inject
    private WidgetPackets widgetPackets;

    /**
     * Confirms the selection of one of the specified item IDs by resuming the appropriate widget
     * interaction based on the current multi-quantity value. This method expects the item id of the item
     * to create, not necessarily the item the player has. i.e. for cooking Salmon it expects the item id
     * of a cooked salmon, not the raw salmon that the player may have in their inventory.
     *
     * <p>This method iterates over a map of processable item IDs and their associated slot indices,
     * checking if any of the provided {@code itemIds} match the available items. If a match is found,
     * it sends a "resume/pause" action packet for the corresponding widget slot with the current
     * quantity value.</p>
     *
     * @param itemIds A variable-length list of item IDs to compare against the processable
     *                items currently available. These represent the items the user
     *                wants to confirm.
     *
     * @return {@code true} if at least one of the provided {@code itemIds} matches the processable
     *         items and an interaction is successfully queued; {@code false} otherwise.
     */
    public boolean process(int... itemIds) {
        List<ExtendedItem> items = getProcessableItems();
        for (ExtendedItem item : items) {
            if (ArrayUtils.contains(itemIds, item.getId())) {
                widgetPackets.queueResumePause(item.getSlot(), getAmount());
                return true;
            }
        }

        return false;
    }

      /**
     * Confirms the selection of one of the specified container items by resuming the appropriate widget
     * interaction based on the current multi-quantity value. This method expects the item id of the item
     * to create, not necessarily the item the player has. i.e. for cooking Salmon it expects the item id
     * of a cooked salmon, not the raw salmon that the player may have in their inventory.
     *
     * <p>This method iterates over a map of processable item IDs and their associated slot indices,
     * checking if any of the provided {@code itemIds} match the available items. If a match is found,
     * it sends a "resume/pause" action packet for the corresponding widget slot with the current
     * quantity value.</p>
     *
     * @param containerItem A non null container item to compare against the processable
     *                items currently available. These represent the items the user
     *                wants to confirm.
     *
     * @return {@code true} if at least one of the provided {@code itemIds} matches the processable
     *         items and an interaction is successfully queued; {@code false} otherwise.
     */
    public boolean process(ContainerItem containerItem) {
        List<ExtendedItem> items = getProcessableItems();
        for (ExtendedItem item : items) {
            if (containerItem.getId() == item.getId()) {
                widgetPackets.queueResumePause(item.getSlot(), getAmount());
                return true;
            }
        }

        return false;
    }

    /**
     * Confirms the selection of one of the specified item names by resuming the appropriate widget
     * interaction based on the current multi-quantity value.
     *
     * <p>This method iterates over a map of processable item IDs and their associated slot indices,
     * checking if any of the provided {@code itemNames} match the available items. If a match is found,
     * it sends a "resume/pause" action packet for the corresponding widget slot with the current
     * quantity value.</p>
     *
     * @param itemNames A variable-length list of item names to compare against the processable
     *                  items currently available. These represent the items the user
     *                  wants to confirm.
     *
     * @return {@code true} if at least one of the provided {@code itemNames} matches the processable
     *         items and an interaction is successfully queued; {@code false} otherwise.
     */
    public boolean process(String... itemNames) {
        List<ExtendedItem> items = getProcessableItems();
        for (ExtendedItem item : items) {
            for (String itemName : itemNames) {
                if (itemName.equalsIgnoreCase(item.getName())) {
                    widgetPackets.queueResumePause(item.getSlot(), getAmount());
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Confirms the selected index by resuming the specific widget and child interface
     * associated with the provided index and the current multi-quantity value.
     *
     * <p>This method executes on the client thread to ensure thread safety. It resolves
     * the widget ID using a base interface ID and the provided {@code index}, and queues
     * the corresponding "resume/pause" action packet using the multi-quantity value
     * retrieved by {@code getAmount()}.</p>
     *
     * @param index The index to confirm in the interface. This value determines the child
     *              component of the base widget that the action will target.
     */
    public void processByIndex(int index) {
        if(index > 16 || index < 0) {
           log.error("Index cannot be greater than 16 or less than 0, got: {}", index);
           return;
        }

        ctx.runOnClientThread(() -> widgetPackets.queueResumePause(InterfaceID.Skillmulti.A + index, getAmount()));
    }

    /**
     * Determines whether the widget corresponding to the specified interface ID is currently open and visible.
     *
     * <p>This method retrieves the widget associated with the {@code InterfaceID.Skillmulti.UNIVERSE}.
     * If the widget is {@code null}, it returns {@code false}, indicating that it is not open.
     * Otherwise, it checks the visibility of the widget and returns {@code true} if the widget is visible.</p>
     *
     * @return {@code true} if the widget is open and currently visible; {@code false} otherwise.
     */
    public boolean isOpen() {
        Widget widget = ctx.getClient().getWidget(InterfaceID.Skillmulti.UNIVERSE);
        if(widget == null) {
            return false;
        }

        return !widget.isSelfHidden();
    }

    /**
     * Retrieves the current value of the skill multi-quantity variable.
     *
     * <p>This method executes on the game client's thread. It reads the value
     * associated with {@code VarClientID.SKILLMULTI_QUANTITY}, which represents the
     * current quantity in the make-X interface or other similar functionality.</p>
     *
     * <p>The value retrieval ensures thread-safety by invoking the method on the client's
     * thread.</p>
     *
     * @return The integer value of the {@literal @}VarClientID.SKILLMULTI_QUANTITY variable,
     *         or {@code 0} if no value is set or the retrieval fails.
     */
    public int getAmount() {
        return ctx.runOnClientThread(() -> ctx.getClient().getVarcIntValue(VarClientID.SKILLMULTI_QUANTITY));
    }

    /**
     * Sets the skill multi-quantity value to the specified amount.
     *
     * <p>This method first compares the current value of the skill multi-quantity variable
     * with the provided {@code amount}. If they are the same, the method exits early without
     * making any changes.</p>
     *
     * <p>If the values differ, the method updates the {@code VarClientID.SKILLMULTI_QUANTITY}
     * variable to the specified {@code amount} by executing the update logic on the game
     * client's thread. This ensures thread safety and avoids potential concurrency issues.</p>
     *
     * @param amount The integer value to set as the new skill multi-quantity. It represents
     *               the selected quantity in the "Make-X" interface or similar functionality.
     */
    public void setAmount(int amount) {
        int selected = getAmount();
        if (selected == amount) {
            return;
        }

        ctx.runOnClientThread(() -> ctx.getClient().setVarcIntValue(VarClientID.SKILLMULTI_QUANTITY, amount));
    }

    /**
     * Retrieves a list of processable items available in the context of the client's current state.
     *
     * <p>This method identifies items within a specified range of interface IDs, filtering out
     * invalid or hidden items. For each valid item found in the corresponding interface widgets,
     * the method creates an {@code ExtendedItem} object containing the item's ID, quantity,
     * interface slot, and name, and then adds it to the resulting list.</p>
     *
     * <p>To ensure thread safety, the method executes on the client thread.</p>
     *
     * <p>The filtering criteria for processable items include the following:</p>
     * <ul>
     *   <li>Widgets that are null or self-hidden are skipped.</li>
     *   <li>Items with an ID of -1 or 6512 are ignored.</li>
     *   <li>Only children widgets within a widget are evaluated for items.</li>
     * </ul>
     *
     * @return A {@code List} of {@code ExtendedItem} objects representing the processable items
     *         found in the specified range of interface IDs. If no items are found or an error occurs,
     *         the list will be empty.
     */
    private List<ExtendedItem> getProcessableItems() {
        return ctx.runOnClientThread(() -> {
            List<ExtendedItem> items = new ArrayList<>();
            Client client = ctx.getClient();
            for (int i = InterfaceID.Skillmulti.A; i < InterfaceID.Skillmulti.R; i++) {
                Widget button = client.getWidget(i);
                if (button == null) continue;
                if(button.isSelfHidden()) continue;

                Widget[] children = button.getChildren();
                if (children == null) continue;

                for (Widget child : children) {
                    int itemId = child.getItemId();
                    if (itemId == -1 || itemId == 6512) {
                        continue;
                    }
                    String name = client.getItemDefinition(itemId).getName();
                    items.add(new ExtendedItem(itemId, child.getItemQuantity(), i, name));
                }
            }
            return items;
        });
    }

    @Getter
    @AllArgsConstructor
    private static class ExtendedItem {
        private int id;
        private int quantity;
        private int slot;
        private String name;
    }
}
