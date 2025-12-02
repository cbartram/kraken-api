package com.kraken.api.service.movement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.MovementPackets;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.ui.UIService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

@Slf4j
@Singleton
public class MovementService {

    @Inject
    private Context ctx;

    @Inject
    private TileService tileService;

    @Inject
    private UIService uiService;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private MovementPackets movementPackets;

    /**
     * Moves the player to a specific world point. This takes into account when it is called
     * from within an instance and will convert the world point from an instance world point into a
     * normal world point which is moveable.
     * @param point The world point to move towards
     */
    public void moveTo(WorldPoint point) {
        WorldPoint convertedPoint;
        if (ctx.getClient().getTopLevelWorldView().isInstance()) {
            // multiple conversions here: 1 which takes WP and creates instanced LP and
            // 2 which converts a LP to WP
            convertedPoint = WorldPoint.fromLocal(ctx.getClient(), tileService.fromWorldInstance(point));
        } else {
            convertedPoint = point;
        }

        Point clickingPoint = uiService.getClickbox(convertedPoint);
        mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
        movementPackets.queueMovement(convertedPoint);
    }

    /**
     * Moves the player to a given local point using packets. This method assumes that if the local point passed
     * is in an instance it
     * @param point The local point to move to
     */
    public void moveTo(LocalPoint point) {
        WorldPoint converted;
        if(ctx.getClient().getTopLevelWorldView().isInstance()) {
            // TODO May not work right
            converted = WorldPoint.fromLocalInstance(ctx.getClient(), point);
            LocalPoint lp = tileService.fromWorldInstance(converted);
            converted = WorldPoint.fromLocal(ctx.getClient(), lp);
        } else {
            converted = WorldPoint.fromLocal(ctx.getClient(), point);
        }


        Point clickingPoint = uiService.getClickbox(converted);
        mousePackets.queueClickPacket(clickingPoint.getX(), clickingPoint.getY());
        movementPackets.queueMovement(converted);
    }
}