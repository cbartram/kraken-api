package com.kraken.api.service;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.SailingPackets;
import com.kraken.api.core.packet.entity.WidgetPackets;
import com.kraken.api.service.ui.UIService;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarbitID;

import java.awt.*;

@Slf4j
@Singleton
public class SailingService {

    @Inject
    private Context ctx;

    @Inject
    private MousePackets mousePackets;

    @Inject
    private SailingPackets sailingPackets;

    @Inject
    private WidgetPackets widgetPackets;

    public static final int SAILING_CONTROLS_ID = InterfaceID.SailingSidepanel.FACILITIES_CONTENT_CLICKLAYER;
    public static final int NAVIGATING_VARBIT = VarbitID.SAILING_BOAT_FACILITY_LOCKEDIN; // 0 = not navigating, 3 = navigating (? maybe != 0 == navigating)
    public static final int SPEED_VARBIT = VarbitID.SAILING_SIDEPANEL_BOAT_MOVE_MODE; // 0 = stopped, 1 = first, 2 = second, 3 = reverse

    /**
     * Sets the sailing direction of the boat in the game.
     * <p>
     * This method first ensures that the necessary game packets are loaded. If not, it
     * immediately returns {@literal false}. If packets are loaded, a mouse click packet
     * is queued, followed by a request to update the boat direction using the provided
     * {@link Direction} value.
     *
     * <p>The direction is set by passing the direction's code to the sailing packet system.
     * Directions are represented using predefined constants in the {@link Direction} enumeration.
     * </p>
     *
     * @param direction The desired {@link Direction} to set the boat's heading.
     *                  Directions are enumerated values ranging from {@literal SOUTH(0)} to
     *                  {@literal SOUTH_SOUTH_EAST(15)}.
     *
     * @return {@literal true} if the direction was successfully set;
     *         {@literal false} if the necessary game packets were not loaded.
     */
    public boolean setDirection(Direction direction) {
        if(!ctx.isPacketsLoaded()) return false;
        mousePackets.queueClickPacket(1, 1);
        sailingPackets.queueSetDirection(direction.getCode());
        return true;
    }

    /**
     * Increases the speed of the boat in the game.
     *
     * <p>This method interacts with the in-game sailing controls to increment the
     * boat's speed by one level, provided it is not already at the maximum speed setting.</p>
     */
    public void increaseSpeed() {
        if (!isMoving()) {
            setSails();
            return;
        }

        int speed = ctx.getVarbitValue(SPEED_VARBIT);

        if (speed == 2) {
            return;
        }

        Point pt = UIService.getClickbox(ctx.widgets().fromClient(SAILING_CONTROLS_ID).raw());
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetActionPacket(SAILING_CONTROLS_ID, 2, -1, 1);
    }

    /**
     * Decreases the speed of the boat in the game.
     *
     * <p>This method interacts with the in-game sailing controls to reduce the boat's speed
     * by one level, unless the current speed is already at the lowest setting. The speed is
     * determined by querying the value of a specific varbit used to represent the boat's
     * speed state.</p>
     *
     * <p>If the boat's speed is set to the minimum value (3 in the current configuration),
     * the method exits immediately without issuing any further actions.</p>
     */
    public void decreaseSpeed() {
        int speed = ctx.getVarbitValue(SPEED_VARBIT);

        if (speed == 3) {
            return;
        }

        Point pt = UIService.getClickbox(ctx.widgets().fromClient(SAILING_CONTROLS_ID).raw());
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetActionPacket(SAILING_CONTROLS_ID, 1, -1, 1);
    }

    /**
     * Retrieves the current direction of the boat based on its angle in the game.
     * <p>
     * This method utilizes the {@link Direction#fromAngle(int)} method to derive
     * the boat's direction from the angle value provided by the {@code VarbitID.SAILING_BOAT_SPAWNED_ANGLE} varbit.
     * The angle is divided into predefined directional constants representing the boat's heading.
     * </p>
     *
     * @return The {@link Direction} the boat is currently facing.
     *         If no valid direction can be derived, {@code null} is returned.
     */
    public Direction getDirection() {
        int angle = ctx.getVarbitValue(VarbitID.SAILING_BOAT_SPAWNED_ANGLE);
        return Direction.fromAngle(angle);
    }

    /**
     * Disables the boat's sails to bring the vessel to a halt.
     *
     * <p>This method initiates the in-game action to unset the boat's sails, effectively stopping
     * the boat's movement. It checks the current movement status of the boat by invoking
     * {@link #isMoving()}. If the boat is stationary, the method exits immediately without
     * issuing any commands. This safeguards against unnecessary actions when the boat is not
     * in motion.</p>
     */
    public void unsetSails() {
        if (!isMoving()) {
            return;
        }

        Point pt = UIService.getClickbox(ctx.widgets().fromClient(SAILING_CONTROLS_ID).raw());
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetActionPacket(SAILING_CONTROLS_ID, 0, -1, 1);
    }

    /**
     * Deploys the boat's sails to enable sailing control.
     * <p>
     * This method triggers the in-game action to set the boat's sails, allowing the player
     * to control the vessel. If the boat is currently in motion, the method exits immediately
     * without performing any action to avoid conflicting states.
     * </p>
     */
    public void setSails() {
        if (isMoving()) {
            return;
        }

        Point pt = UIService.getClickbox(ctx.widgets().fromClient(SAILING_CONTROLS_ID).raw());
        mousePackets.queueClickPacket(pt.getX(), pt.getY());
        widgetPackets.queueWidgetActionPacket(SAILING_CONTROLS_ID, 0, -1, 1);
    }

    /**
     * Determines whether the player is at the helm of the boat and navigating
     * <p>
     * This method checks the value of a specific varbit to identify if the navigation
     * system is active. A non-zero value indicates that the boat is navigating,
     * while a value of zero suggests it is not.
     * </p>
     *
     * @return {@literal true} if the boat's navigation system is active;
     *         {@literal false} otherwise.
     */
    public boolean isNavigating() {
        return ctx.getVarbitValue(NAVIGATING_VARBIT) != 0;
    }

    /**
     * Checks if the boat is currently moving based on the speed state.
     * <p>
     * This method determines the movement state by examining the value of a specific varbit
     * linked to the boat's speed. A non-zero value indicates that the boat is in motion,
     * while a zero value means it is stationary.
     * </p>
     *
     * @return {@literal true} if the boat is moving; {@literal false} otherwise.
     */
    public boolean isMoving() {
        return ctx.getVarbitValue(SPEED_VARBIT) != 0;
    }

    @Getter
    @AllArgsConstructor
    public enum Direction {
        SOUTH(0),
        SOUTH_SOUTH_WEST(1),
        SOUTH_WEST(2),
        WEST_SOUTH_WEST(3),
        WEST(4),
        WEST_NORTH_WEST(5),
        NORTH_WEST(6),
        NORTH_NORTH_WEST(7),
        NORTH(8),
        NORTH_NORTH_EAST(9),
        NORTH_EAST(10),
        EAST_NORTH_EAST(11),
        EAST(12),
        EAST_SOUTH_EAST(13),
        SOUTH_EAST(14),
        SOUTH_SOUTH_EAST(15);

        private final int code;

        public static Direction fromCode(int code) {
            for (Direction d : values()) {
                if (d.code == code) {
                    return d;
                }
            }
            return null;
        }

        public static Direction fromAngle(int angle) {
            int code = angle / 128;
            return Direction.fromCode(code);
        }
    }
}
