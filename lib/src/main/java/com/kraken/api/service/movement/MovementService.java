package com.kraken.api.service.movement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.core.packet.entity.MousePackets;
import com.kraken.api.core.packet.entity.MovementPackets;
import com.kraken.api.service.tile.TileService;
import com.kraken.api.service.ui.UIService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
     * Moves the player to the specified {@literal WorldPoint}, handling instanced areas conversion when necessary.
     * This method ensures accurate movement by checking if the player is within an instance and applying the
     * respective {@literal LocalPoint} to {@literal WorldPoint} conversion.
     *
     * <p>The operation involves:
     * <ul>
     *   <li>Converting the given {@literal WorldPoint} to handle instanced logic if the player is in an instance.</li>
     *   <li>Obtaining a click position on the game canvas for the target point.</li>
     *   <li>Sending interaction packets to queue both mouse clicks and movement commands.</li>
     * </ul>
     *
     * @param point The {@literal WorldPoint} representing the destination to move towards.
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
     * Moves the player to a specified {@literal LocalPoint}. This method accounts for
     * whether the player is within an instance and converts the {@literal LocalPoint}
     * to a {@literal WorldPoint} to enable movement. If the current top-level world view
     * is an instance, additional conversion logic is applied to ensure accuracy.
     *
     * <p>The method performs the following actions:
     * <ul>
     *   <li>Converts the provided {@literal LocalPoint} to a {@literal WorldPoint}, handling instance logic if needed.</li>
     *   <li>Determines the clickbox for the target {@literal WorldPoint} to simulate a mouse click.</li>
     *   <li>Queues packets to notify the game client of the movement and corresponding mouse interaction.</li>
     * </ul>
     *
     * @param point The {@literal LocalPoint} representing the target destination to move towards.
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

    /**
     * Converts a dense path (every tile) into a strided path.
     * Logic: Start with a stride of 5, increase by 1 every step, cap at 12.
     */
    public List<WorldPoint> applyVariableStride(List<WorldPoint> densePath) {
        if (densePath.size() <= 5) {
            return Collections.singletonList(densePath.get(densePath.size() - 1));
        }

        List<WorldPoint> waypoints = new ArrayList<>();
        int currentStride = 5;
        int maxStride = 12;
        int currentIndex = 0;

        // densePath[0] is usually the player's current tile, so we start looking ahead
        while (currentIndex < densePath.size() - 1) {
            int nextIndex = currentIndex + currentStride;

            if (nextIndex >= densePath.size() - 1) {
                // If the next stride overshoots or hits the end, just add the final destination
                waypoints.add(densePath.get(densePath.size() - 1));
                break;
            }

            waypoints.add(densePath.get(nextIndex));
            currentIndex = nextIndex;

            // Increase stride for next step, up to the cap
            if (currentStride < maxStride) {
                currentStride++;
            }
        }

        return waypoints;
    }

    /**
     * Traverses a given path made up of waypoints, attempting to successfully move the player
     * to each {@literal WorldPoint} in the sequence. This method invokes movement commands and uses
     * retries if a waypoint fails to be reached. It aborts if a waypoint cannot be reached after
     * multiple attempts.
     *
     * <p>The method performs the following tasks for each waypoint in the path:
     * <ul>
     *   <li>Sends a movement command to the client to move towards the target waypoint.</li>
     *   <li>Calculates a dynamic timeout based on distance and walking speed, with a buffer for path variance.</li>
     *   <li>Waits for the player to reach the waypoint within the allowed timeout.</li>
     *   <li>Retries the movement command up to two times if the waypoint is not reached within the timeout.</li>
     *   <li>Aborts and returns failure if retries are exhausted for any waypoint.</li>
     * </ul>
     *
     * @param client The client instance used to interact with the game world and retrieve the player's location.
     * @param movement The {@literal MovementService} instance responsible for managing player movement commands.
     * @param path A list of {@literal WorldPoint} objects representing the sequence of waypoints to traverse.
     * @return {@code true} if the path was successfully traversed to the end, {@code false} if any waypoint
     *         could not be reached after retries.
     * @throws InterruptedException If the thread running the method is interrupted during execution.
     */
    public boolean traversePath(Client client, MovementService movement, List<WorldPoint> path) throws InterruptedException {
        for (WorldPoint step : path) {
            boolean stepSuccess = false;

            // Try to move to this waypoint up to 2 times
            for (int attempt = 0; attempt < 2; attempt++) {

                // 1. Send the click
                ctx.runOnClientThread(() -> movement.moveTo(step));

                // 2. Calculate dynamic timeout
                // Walking = 1 tile/0.6s. Running = 2 tiles/0.6s.
                // We assume walking speed (worst case) + 2 seconds buffer.
                WorldPoint playerLoc = client.getLocalPlayer().getWorldLocation();
                int dist = playerLoc.distanceTo(step);

                // (Distance * 600ms) is exact walking time. We multiply by 1.3 for path variance, plus 2000ms base buffer.
                long timeoutDuration = (long) ((dist * 600 * 1.3) + 2000);
                long timeout = System.currentTimeMillis() + timeoutDuration;

                boolean reached = false;
                while (System.currentTimeMillis() < timeout) {
                    playerLoc = client.getLocalPlayer().getWorldLocation();

                    // Success condition: Distance <= 2
                    if (playerLoc.distanceTo(step) <= 2) {
                        reached = true;
                        break;
                    }
                    Thread.sleep(50);
                }

                if (reached) {
                    stepSuccess = true;
                    break; // Move to next step in the path
                } else {
                    log.warn("TaskChain: Timeout moving to {}, retrying... (Attempt {}/2)", step, attempt + 1);
                }
            }

            // If we failed all attempts for this specific step, abort the whole chain
            if (!stepSuccess) {
                log.error("TaskChain: Failed to reach waypoint {} after retries. Aborting.", step);
                return false;
            }
        }
        return true;
    }
}