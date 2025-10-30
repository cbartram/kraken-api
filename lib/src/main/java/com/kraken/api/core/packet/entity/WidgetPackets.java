package com.kraken.api.core.packet.entity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketDefFactory;
import lombok.SneakyThrows;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A high-level utility class for sending widget-related game packets.
 * This class abstracts the complexity of constructing and sending
 * packets related to widget (interface) interactions, such as clicking buttons.
 * It uses a {@link PacketClient} provider to send the low-level packets,
 * which are defined by the {@link PacketDefFactory}.
 */
@Singleton
public class WidgetPackets {

    @Inject
    private Provider<PacketClient> packetSenderProvider;

    @Inject
    private PacketDefFactory packetDefFactory;

    /**
     * Queues a low-level widget action packet (IF_BUTTONX).
     * <p>
     * This method is a direct wrapper for the IF_BUTTONX packet, which is used
     * for most widget interactions. It corresponds to one of the 10 "IF_BUTTON"
     * opcodes (e.g., IF_BUTTON1, IF_BUTTON2, etc.).
     *
     * @param widgetId      The parent widget ID (e.g., WidgetInfo.BANK_CONTAINER.getId()).
     * @param childId       The specific child widget index (slot) within the parent. -1 for no specific child.
     * @param itemId        The item ID associated with the slot, if any. -1 for no item.
     * @param actionFieldNo The action number (1-10) to execute. This maps to the
     * specific packet (e.g., 1 = IF_BUTTON1, 2 = IF_BUTTON2).
     */
    @SneakyThrows
    public void queueWidgetActionPacket(int widgetId, int childId, int itemId, int actionFieldNo) {
        // Sends the IF_BUTTONX packet (generic widget action packet)
        // actionFieldNo & 65535 is a bitmask to ensure the value fits within an unsigned short,
        // which is how the client likely processes it.
        packetSenderProvider.get().sendPacket(packetDefFactory.getIfButtonX(), widgetId, childId, itemId, actionFieldNo & 65535);
    }

    /**
     * Queues a widget action by searching for a specific action string (e.g., "Withdraw-1", "Bank").
     * <p>
     * This is a higher-level convenience method. Instead of needing to know the
     * exact action number (1-10), you can provide the human-readable text of the
     * action. The method will find the corresponding action number and send the correct packet.
     *
     * @param widget     The {@link Widget} object to interact with.
     * @param actionlist A varargs list of action strings to search for. The method will
     * use the *first* match it finds. The search is case-insensitive
     * and ignores color tags.
     */
    @SneakyThrows
    public void queueWidgetAction(Widget widget, String... actionlist) {
        if (widget == null || widget.getActions() == null || widget.getActions().length == 0) {
            return;
        }

        List<String> actions = Arrays.stream(widget.getActions()).collect(Collectors.toList());
        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i) == null)
                continue;
            actions.set(i, actions.get(i).toLowerCase());
        }

        int num = -1;
        for (String action : actions) {
            for (String action2 : actionlist) {
                if (action != null && Text.removeTags(action).equalsIgnoreCase(action2)) {
                    num = actions.indexOf(action.toLowerCase()) + 1;
                    break;
                }
            }
            if (num != -1) break;
        }

        // If no valid action was found (1-10), do nothing.
        if (num < 1 || num > 10) {
            return;
        }

        queueWidgetActionPacket(widget.getId(), widget.getIndex(), widget.getItemId(), num);
    }
}
