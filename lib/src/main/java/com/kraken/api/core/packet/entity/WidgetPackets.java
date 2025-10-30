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

@Singleton
public class WidgetPackets {

    @Inject
    private Provider<PacketClient> packetSenderProvider;

    @Inject
    private  PacketDefFactory packetDefFactory;

    @SneakyThrows
    public void queueWidgetActionPacket(int widgetId, int childId, int itemId, int actionFieldNo) {
        packetSenderProvider.get().sendPacket(packetDefFactory.getIfButtonX(), widgetId, childId, itemId, actionFieldNo & 65535);
    }

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
                }
            }
        }

        if (num < 1 || num > 10) {
            return;
        }

        queueWidgetActionPacket(num, widget.getId(), widget.getItemId(), widget.getIndex());
    }

}
