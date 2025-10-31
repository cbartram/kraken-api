package com.kraken.api.core.packet.entity;

import com.google.inject.Provider;
import com.kraken.api.core.packet.PacketClient;
import com.kraken.api.core.packet.model.PacketDefFactory;
import net.runelite.api.Client;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import javax.inject.Inject;

public class MovementPackets {

    @Inject
    private PacketDefFactory packetDefFactory;

    @Inject
    private Provider<PacketClient> packetClient;

    @Inject
    private Client client;

    /**
     * Queues a movement packet for a specific world point x and y location.
     * @param worldPointX The X location of the world point
     * @param worldPointY The Y location of the world point
     * @param ctrlDown True if control should be pressed (walks when run is toggled on and runs when walk is toggled on).
     */
    public void queueMovement(int worldPointX, int worldPointY, boolean ctrlDown) {
        int ctrl = ctrlDown ? 1 : 0;
        packetClient.get().sendPacket(packetDefFactory.getMoveGameClick(), worldPointX, worldPointY, ctrl, 5);
    }

    /**
     * Queues a movement packet for a specific world point.
     * @param location The world point to queue a packet to move to.
     */
    public void queueMovement(WorldPoint location) {
        queueMovement(location.getX(), location.getY(), false);
    }

    /**
     * Queues a movement packet for a specific local point.
     * @param location The local point to queue a packet to move to.
     */
    public void queueMovement(LocalPoint location) {
        queueMovement(WorldPoint.fromLocal(client, location));
    }
}
