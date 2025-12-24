package com.kraken.api.core.packet.entity;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketDefFactory;
import lombok.SneakyThrows;
import net.runelite.api.ItemComposition;
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

    /**
     * Queues a widget sub-action packet by identifying the specific sub-action
     * and menu options associated with a given widget.
     * <p>
     * This method identifies the indices of both a sub-action (from item definitions)
     * and a specific menu option (from the widget's actions). If matches for both
     * the sub-action and menu option are found, it sends a low-level packet to
     * perform the action.
     * <p>
     * Only executes if the widget and its associated item ID are valid, while the
     * sub-actions and menu options must contain the desired action and menu option.
     *
     * @param widget The {@link Widget} instance on which the action is to be performed.
     *               This is the target widget for the queued action.
     * @param menu   A case-insensitive {@literal @<String>} representing the menu action
     *               text to search for (e.g., "Use", "Examine").
     * @param action A case-insensitive {@literal @<String>} representing the sub-action text
     *               to search for (e.g., "Clean", "Equip").
     */
    @SneakyThrows
    public void queueWidgetSubAction(Widget widget, String menu, String action) {
        if (widget == null || widget.getItemId() == -1) {
            return;
        }

        ItemComposition composition = packetSenderProvider.get().getClient().getItemDefinition(widget.getItemId());
        String[][] subOps = composition.getSubops();
        List<String> actions = Arrays.stream(widget.getActions()).collect(Collectors.toList());

        int menuIndex = -1;
        int actionIndex = -1;

        if (subOps == null) {
            return;
        }

        for (String[] subOp : subOps) {
            if (actionIndex != -1) {
                break;
            }
            if (subOp != null) {
                for (int i = 0; i < subOp.length; i++) {
                    String op = subOp[i];
                    if (op != null && op.equalsIgnoreCase(action)) {
                        actionIndex = i;
                        break;
                    }
                }
            }
        }

        for (int i = 0; i < actions.size(); i++) {
            String a = actions.get(i);
            if (a != null && a.equalsIgnoreCase(menu)) {
                menuIndex = i + 1;
                break;
            }
        }

        if (menuIndex == -1 || actionIndex == -1) {
            return;
        }

        packetSenderProvider.get()
                .sendPacket(packetDefFactory.getIfSubOp(), widget.getId(), widget.getIndex(), widget.getItemId(), menuIndex, actionIndex);
    }


    /**
     * Queues a packet simulating the use of one widget item (source) on another
     * widget item (destination). This is typically used for "Use" menu actions
     * like using a potion on a bank slot or an item on a piece of equipment.
     * <p>
     * Delegates to the overloaded method using the raw IDs and indices.
     *
     * @param srcWidget The source widget (e.g., the item being "used").
     * @param destWidget The destination widget (e.g., the item or slot being used "on").
     */
    public void queueWidgetOnWidget(Widget srcWidget, Widget destWidget) {
        queueWidgetOnWidget(srcWidget.getId(), srcWidget.getIndex(), srcWidget.getItemId(), destWidget.getId(), destWidget.getIndex(), destWidget.getItemId());
    }

    /**
     * Queues the raw IF_BUTTONT packet, simulating using an item/slot from a
     * source widget on an item/slot of a destination widget.
     *
     * @param sourceWidgetId The ID of the source widget (interface).
     * @param sourceSlot The slot/index within the source widget.
     * @param sourceItemId The item ID within the source slot.
     * @param destinationWidgetId The ID of the destination widget (interface).
     * @param destinationSlot The slot/index within the destination widget.
     * @param destinationItemId The item ID within the destination slot.
     */
    public void queueWidgetOnWidget(int sourceWidgetId, int sourceSlot, int sourceItemId, int destinationWidgetId, int destinationSlot, int destinationItemId) {
        packetSenderProvider.get().sendPacket(packetDefFactory.getIfButtonT(), sourceWidgetId, sourceSlot, sourceItemId, destinationWidgetId,
                destinationSlot, destinationItemId);
    }

    /**
     * Queues the RESUME_PAUSEBUTTON packet, typically sent when the player
     * clicks a "Click here to continue" or "Close" button on a standard,
     * non-interactable dialog, such as a completion message or a pause screen.
     *
     * @param widgetId The ID of the top-level widget.
     * @param childId The ID of the child component that was clicked.
     */
    public void queueResumePause(int widgetId, int childId) {
        packetSenderProvider.get().sendPacket(packetDefFactory.getResumePausebutton(), widgetId, childId);
    }

    /**
     * Queues the RESUME_COUNTDIALOG packet, sent in response to a numerical
     * input dialog (e.g., "How many?" or "Enter amount").
     *
     * @param id The integer value entered by the player.
     */
    public void queueResumeCount(int id) {
        packetSenderProvider.get().sendPacket(packetDefFactory.getResumeCountDialog(), id);
    }

    /**
     * Queues the RESUME_OBJDIALOG packet, typically sent as a continuation
     * packet after selecting an option in a multi-choice dialog, where the
     * value represents an item ID or object ID relevant to the dialog option.
     *
     * @param value The numerical value associated with the dialog option.
     */
    public void queueResumeObj(int value) {
        packetSenderProvider.get().sendPacket(packetDefFactory.getResumeObjDialog(), value);
    }

    /**
     * Queues the OPHELDD packet, which simulates a drag-and-drop action
     * between two slots within the same or different widgets (e.g., moving
     * an item in the inventory or bank).
     *
     * @param src The source widget/slot from which the item is dragged.
     * @param dest The destination widget/slot onto which the item is dropped.
     */
    public void queueDragAndDrop(Widget src, Widget dest) {
        packetSenderProvider.get().sendPacket(packetDefFactory.getOpHeldd(), src.getId(), src.getIndex(),
                src.getItemId(), dest.getId(), dest.getIndex(), dest.getItemId());
    }

    /**
     * Queues the RESUME_NAMEDIALOG packet, sent in response to a chat dialog
     * asking the player to enter a name (e.g., setting a clan name).
     * <p>
     * Note: The packet data includes the length of the string plus one for the null terminator.
     *
     * @param name The string name entered by the player.
     */
    public void queueResumeName(String name) {
        int length = name.length() + 1;
        packetSenderProvider.get().sendPacket(packetDefFactory.getResumeNameDialog(), length, name);
    }

    /**
     * Queues the RESUME_STRINGDIALOG packet, sent in response to a chat dialog
     * asking the player to enter a generic string (e.g., a search query).
     * <p>
     * Note: The packet data includes the length of the string plus one for the null terminator.
     *
     * @param string The string input entered by the player.
     */
    public void queueResumeString(String string) {
        int length = string.length() + 1;
        packetSenderProvider.get().sendPacket(packetDefFactory.getResumeStringDialog(), length, string);
    }
}
