package com.kraken.api.core.packet.entity;

import com.example.PacketUtils.PacketDef;
import com.example.PacketUtils.PacketReflection;
import com.google.inject.Singleton;
import lombok.SneakyThrows;
import net.runelite.api.widgets.Widget;
import net.runelite.client.util.Text;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class WidgetPackets {

    @SneakyThrows
    public static void queueWidgetActionPacket(int actionFieldNo, int widgetId, int itemId, int childId) {
        PacketReflection.sendPacket(PacketDef.getIfButtonX(), widgetId, childId, itemId, actionFieldNo & 65535);
    }

    @SneakyThrows
    public static void queueWidgetAction(Widget widget, String... actionlist) {
        if (widget == null) {
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
