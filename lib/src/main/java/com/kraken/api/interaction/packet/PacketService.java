package com.kraken.api.interaction.packet;

import com.example.Packets.MousePackets;
import com.example.Packets.MovementPackets;
import com.google.inject.Singleton;
import com.kraken.api.core.AbstractService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.coords.WorldPoint;

import java.util.Random;
import net.runelite.api.Point;

@Slf4j
@Singleton
public class PacketService extends AbstractService {
    private static final Random coordRandom = new Random();
    private static final int CHAT_X = 47, CHAT_Y = 863;
    private static final int CHAT_WIDTH = 746, CHAT_HEIGHT = 143;
    private static final int MAIN_X = 42, MAIN_Y = 46;
    private static final int MAIN_WIDTH = 1396, MAIN_HEIGHT = 747;
    private static final int INVENTORY_X = 1570, INVENTORY_Y = 630;
    private static final int INVENTORY_WIDTH = 300, INVENTORY_HEIGHT = 400;

    public Point queueClickPacketCoordinateArea() {
        if(!context.isPacketsLoaded()) return new Point(-1, -1);
        return queueClickPacketCoordinateArea(CoordinateArea.MAIN_MODAL);
    }

    public Point queueClickPacketCoordinateArea(CoordinateArea area) {
        if(!context.isPacketsLoaded()) return new Point(-1, -1);
        if (area == null) {
            return new Point(-1, -1);
        }

        Point random = pointFromCoordinateArea(area);
        MousePackets.queueClickPacket(random.getX(), random.getY());
        return random;
    }

    public static Point pointFromCoordinateArea(CoordinateArea area) {
        int clickX = -1;
        int clickY = -1;

        switch (area) {
            case CHAT:
                clickX = CHAT_X + coordRandom.nextInt(CHAT_WIDTH);
                clickY = CHAT_Y + coordRandom.nextInt(CHAT_HEIGHT);
                break;
            case INVENTORY:
                clickX = INVENTORY_X + coordRandom.nextInt(INVENTORY_WIDTH);
                clickY = INVENTORY_Y + coordRandom.nextInt(INVENTORY_HEIGHT);
                break;
            case MAIN_MODAL:
                clickX = MAIN_X + coordRandom.nextInt(MAIN_WIDTH);
                clickY = MAIN_Y + coordRandom.nextInt(MAIN_HEIGHT);
                break;
        }

        if (coordRandom.nextInt(10) == 0 && clickX % 2 == 0) {
            clickX -= coordRandom.nextInt(7);
        }

        else if (coordRandom.nextInt(10) == 0 && clickY % 2 == 0) {
            clickY -= coordRandom.nextInt(7);
        }

        return new Point(clickX, clickY);
    }

    public void walk(WorldPoint point) {
        if(!context.isPacketsLoaded()) return;
        queueClickPacketCoordinateArea();
        MovementPackets.queueMovement(point);
    }
}
