package com.kraken.api.core.packet.entity;


import com.example.PacketUtils.PacketDef;
import com.example.PacketUtils.PacketReflection;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketDefFactory;
import com.kraken.api.interaction.tile.TileService;
import lombok.SneakyThrows;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.Player;
import net.runelite.api.widgets.Widget;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A high-level utility class for sending packets related to {@link Player} interactions
 * to the game server.
 * <p>
 * This class handles actions like right-clicking other players (e.g., Follow, Trade)
 * and "use-on" actions (e.g., using an item on a player). It abstracts the low-level
 * packet construction and uses client data to determine the correct action index.
 */
public class PlayerPackets {

    @Inject
    private Provider<PacketClient> packetClientProvider;

    @Inject
    private PacketDefFactory packetDefFactory;

    @Inject
    private Client client;

    /**
     * Queues the low-level packet to perform a generic action click on another player.
     * <p>
     * This method sends one of the {@code OPPLAYER} packets (e.g., {@code OPPLAYER1} through {@code OPPLAYER10}),
     * where the action is determined by the {@code actionFieldNo} (which corresponds to a Player Option).
     *
     * @param actionFieldNo The 1-based index of the action to execute (1-10).
     * @param playerIndex The server index/ID of the target player.
     * @param ctrlDown If true, indicates the control key was held down.
     */
    @SneakyThrows
    public void queuePlayerAction(int actionFieldNo, int playerIndex, boolean ctrlDown) {
        int ctrl = ctrlDown ? 1 : 0;
        packetClientProvider.get().sendPacket(packetDefFactory.getOpPlayer(actionFieldNo), playerIndex, ctrl);
    }

    /**
     * Queues a player action by matching a human-readable action string (e.g., "Attack", "Trade", "Follow").
     * <p>
     * This is a high-level convenience method that checks the client's current
     * **Player Options** (the text that appears on the right-click menu) for a
     * matching action, finds the corresponding action number (1-10), and sends the correct packet.
     *
     * @param player The target {@link Player} object to interact with.
     * @param actionlist A varargs list of action strings to search for (case-insensitive).
     */
    @SneakyThrows
    public void queuePlayerAction(Player player, String... actionlist) {
        // Retrieve the current client player options (e.g., ["Attack", "Trade", "Follow", ...])
        List<String> actions = Arrays.stream(client.getPlayerOptions()).collect(Collectors.toList());

        for (int i = 0; i < actions.size(); i++) {
            if (actions.get(i) == null)
                continue;
            actions.set(i, actions.get(i).toLowerCase());
        }

        int num = -1;

        for (String action : actions) {
            for (String action2 : actionlist) {
                if (action != null && action.equalsIgnoreCase(action2)) {
                    num = actions.indexOf(action.toLowerCase()) + 1;
                    break;
                }
            }
            if (num != -1) break;
        }

        if (num < 1 || num > 10) {
            return;
        }

        queuePlayerAction(num, player.getId(), false);
    }

    /**
     * Queues the raw packet for using a widget (typically an item) on another player.
     * <p>
     * This method sends the {@code OPPLAYERT} (Use Widget on Player) packet, which
     * contains the details of the source item/widget and the target player.
     *
     * @param playerIndex The server index/ID of the target player.
     * @param sourceItemId The ID of the item being used.
     * @param sourceSlot The slot index of the item being used (e.g., inventory slot).
     * @param sourceWidgetId The ID of the parent widget containing the item (e.g., inventory widget ID).
     * @param ctrlDown If true, indicates the control key was held down.
     */
    public void queueWidgetOnPlayer(int playerIndex, int sourceItemId, int sourceSlot, int sourceWidgetId, boolean ctrlDown) {
        int ctrl = ctrlDown ? 1 : 0;
        packetClientProvider.get().sendPacket(packetDefFactory.getOpPlayerT(), playerIndex, sourceItemId, sourceSlot, sourceWidgetId, ctrl);
    }

    /**
     * Queues the packet for using a specific {@link Widget} (or item it represents) on a target {@link Player}.
     * <p>
     * This is a convenience method that extracts the necessary item and widget details
     * from the provided {@link Widget} object and calls the raw {@code queueWidgetOnPlayer} method.
     *
     * @param player The target {@link Player} object.
     * @param widget The source {@link Widget} containing the item or action to be used on the player.
     */
    public void queueWidgetOnPlayer(Player player, Widget widget) {
        queueWidgetOnPlayer(player.getId(), widget.getItemId(), widget.getIndex(), widget.getId(), false);
    }
}
