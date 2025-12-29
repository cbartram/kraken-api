package com.kraken.api.service.camera;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kraken.api.Context;
import com.kraken.api.input.KeyboardService;
import com.kraken.api.service.util.SleepService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.camera.CameraPlugin;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class CameraService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> trackingTask;

    @Inject
    private Context ctx;

    @Inject
    private ConfigManager configManager;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private KeyboardService keyboardService;

    @Inject
    private ClientThread runeliteClientThread;

    /**
     * Calculates the angle in degrees from the local player to the specified actor.
     *
     * @param t the target actor
     * @return the angle in degrees (0-359), where 0 is east and increases counter-clockwise
     */
    public int angleToTile(Actor t) {
        int angle = (int) Math.toDegrees(Math.atan2(t.getWorldLocation().getY() - ctx.getClient().getLocalPlayer().getWorldLocation().getY(),
                t.getWorldLocation().getX() - ctx.getClient().getLocalPlayer().getWorldLocation().getX()));
        return angle >= 0 ? angle : 360 + angle;
    }

    /**
     * Calculates the angle in degrees from the local player to the specified tile object.
     *
     * @param t the target tile object
     * @return the angle in degrees (0-359), where 0 is east and increases counter-clockwise
     */
    public int angleToTile(TileObject t) {
        int angle = (int) Math.toDegrees(Math.atan2(t.getWorldLocation().getY() - ctx.getClient().getLocalPlayer().getWorldLocation().getY(),
                t.getWorldLocation().getX() - ctx.getClient().getLocalPlayer().getWorldLocation().getX()));
        return angle >= 0 ? angle : 360 + angle;
    }

    /**
     * Calculates the angle in degrees from the local player to the specified local point.
     *
     * @param localPoint the target local point
     * @return the angle in degrees (0-359), where 0 is east and increases counter-clockwise
     */
    public int angleToTile(LocalPoint localPoint) {
        int angle = (int) Math.toDegrees(Math.atan2(localPoint.getY() - ctx.getClient().getLocalPlayer().getLocalLocation().getY(),
                localPoint.getX() - ctx.getClient().getLocalPlayer().getLocalLocation().getX()));
        return angle >= 0 ? angle : 360 + angle;
    }

    /**
     * Calculates the angle in degrees from the local player to the specified world point.
     *
     * @param worldPoint the target world point
     * @return the angle in degrees (0-359), where 0 is east and increases counter-clockwise
     */
    public int angleToTile(WorldPoint worldPoint) {
        int angle = (int) Math.toDegrees(Math.atan2(worldPoint.getY() - ctx.getClient().getLocalPlayer().getWorldLocation().getY(),
                worldPoint.getX() - ctx.getClient().getLocalPlayer().getWorldLocation().getX()));
        return angle >= 0 ? angle : 360 + angle;
    }

    /**
     * Rotates the camera to face the specified actor with a default maximum angle of 40 degrees.
     *
     * @param actor the actor to turn towards
     */
    public void turnTo(final Actor actor) {
        int angle = getCharacterAngle(actor);
        setAngle(angle, 40);
    }

    /**
     * Rotates the camera to face the specified actor with a custom maximum angle.
     *
     * @param actor the actor to turn towards
     * @param maxAngle the maximum angle to rotate in a single adjustment
     */
    public void turnTo(final Actor actor, int maxAngle) {
        int angle = getCharacterAngle(actor);
        setAngle(angle, maxAngle);
    }

    /**
     * Rotates the camera to face the specified tile object with a default maximum angle of 40 degrees.
     *
     * @param tileObject the tile object to turn towards
     */
    public void turnTo(final TileObject tileObject) {
        int angle = getObjectAngle(tileObject);
        setAngle(angle, 40);
    }

    /**
     * Rotates the camera to face the specified tile object with a custom maximum angle.
     *
     * @param tileObject the tile object to turn towards
     * @param maxAngle the maximum angle to rotate in a single adjustment
     */
    public void turnTo(final TileObject tileObject, int maxAngle) {
        int angle = getObjectAngle(tileObject);
        setAngle(angle, maxAngle);
    }

    /**
     * Rotates the camera to face the specified local point with a default maximum angle of 40 degrees.
     *
     * @param localPoint the local point to turn towards
     */
    public void turnTo(final LocalPoint localPoint) {
        int angle = (angleToTile(localPoint) - 90) % 360;
        setAngle(angle, 40);
    }

    /**
     * Rotates the camera to face the specified local point with a custom maximum angle.
     *
     * @param localPoint the local point to turn towards
     * @param maxAngle the maximum angle to rotate in a single adjustment
     */
    public void turnTo(final LocalPoint localPoint, int maxAngle) {
        int angle = (angleToTile(localPoint) - 90) % 360;
        setAngle(angle, maxAngle);
    }

    /**
     * Gets the camera angle needed to face the specified actor.
     *
     * @param actor the target actor
     * @return the camera angle in degrees (0-359)
     */
    public int getCharacterAngle(Actor actor) {
        return getTileAngle(actor);
    }

    /**
     * Gets the camera angle needed to face the specified tile object.
     *
     * @param tileObject the target tile object
     * @return the camera angle in degrees (0-359)
     */
    public int getObjectAngle(TileObject tileObject) {
        return getTileAngle(tileObject);
    }

    /**
     * Calculates the camera angle needed to face the specified actor.
     * Adjusts the mathematical angle to camera coordinates.
     *
     * @param actor the target actor
     * @return the camera angle in degrees (0-359)
     */
    public int getTileAngle(Actor actor) {
        int a = (angleToTile(actor) - 90) % 360;
        return a < 0 ? a + 360 : a;
    }

    /**
     * Calculates the camera angle needed to face the specified tile object.
     * Adjusts the mathematical angle to camera coordinates.
     *
     * @param tileObject the target tile object
     * @return the camera angle in degrees (0-359)
     */
    public int getTileAngle(TileObject tileObject) {
        int a = (angleToTile(tileObject) - 90) % 360;
        return a < 0 ? a + 360 : a;
    }

    /**
     * <h1> Checks if the angle to the target is within the desired max angle </h1>
     * <p>
     * The desired max angle should not go over 80-90 degrees as the target will be out of view
     *
     * @param targetAngle     the angle to the target
     * @param desiredMaxAngle the maximum angle to the target (Should be a positive number)
     * @return true if the angle to the target is within the desired max angle
     */
    public boolean isAngleGood(int targetAngle, int desiredMaxAngle) {
        return Math.abs(getAngleTo(targetAngle)) <= desiredMaxAngle;
    }

    /**
     * Sets the angle of the camera to the target degrees limited by the max angle.
     * @param targetDegrees    the angle to the target
     * @param maxAngle the maximum angle to the target (Should be a positive number)
     */
    public void setAngle(int targetDegrees, int maxAngle) {
        double defaultCameraSpeed = 1f;

        Plugin cameraPlugin =  pluginManager.getPlugins().stream()
                .filter(plugin -> plugin.getClass() == CameraPlugin.class)
                .map(CameraPlugin.class::cast).findFirst().orElse(null);

        // If the camera plugin is enabled, get the camera speed from the config in case it has been changed
        if (cameraPlugin != null && pluginManager.isPluginEnabled(cameraPlugin)) {
            String configGroup = "zoom";
            String configKey = "cameraSpeed";
            defaultCameraSpeed = configManager.getConfiguration(configGroup, configKey, double.class);
        }

        // Set the camera speed to 3 to make the camera move faster
        ctx.getClient().setCameraSpeed(3f);

        if (getAngleTo(targetDegrees) > maxAngle) {
            keyboardService.keyHold(KeyEvent.VK_LEFT);
            SleepService.sleepUntilTrue(() -> Math.abs(getAngleTo(targetDegrees)) <= maxAngle, 50, 5000);
            keyboardService.keyRelease(KeyEvent.VK_LEFT);
        } else if (getAngleTo(targetDegrees) < -maxAngle) {
            keyboardService.keyHold(KeyEvent.VK_RIGHT);
            SleepService.sleepUntilTrue(() -> Math.abs(getAngleTo(targetDegrees)) <= maxAngle, 50, 5000);
            keyboardService.keyRelease(KeyEvent.VK_RIGHT);
        }

        ctx.getClient().setCameraSpeed((float) defaultCameraSpeed);
    }

    /**
     * Adjusts the camera pitch to the specified percentage.
     * Uses keyboard input to smoothly transition to the target pitch.
     *
     * @param percentage the target pitch as a percentage (0.0 to 1.0, where 0 is looking down and 1 is looking up)
     */
    public void adjustPitch(float percentage) {
        float currentPitchPercentage = cameraPitchPercentage();

        if (currentPitchPercentage < percentage) {
            keyboardService.keyHold(KeyEvent.VK_UP);
            SleepService.sleepUntilTrue(() -> cameraPitchPercentage() >= percentage, 50, 5000);
            keyboardService.keyRelease(KeyEvent.VK_UP);
        } else {
            keyboardService.keyHold(KeyEvent.VK_DOWN);
            SleepService.sleepUntilTrue(() -> cameraPitchPercentage() <= percentage, 50, 5000);
            keyboardService.keyRelease(KeyEvent.VK_DOWN);
        }
    }

    /**
     * Gets the current camera pitch value.
     *
     * @return the current camera pitch (128-383, where 128 is looking down and 383 is looking up)
     */
    public int getPitch() {
        return ctx.getClient().getCameraPitch();
    }

    /**
     * Sets the camera pitch to the specified value.
     * The pitch is clamped between the minimum (128) and maximum (383) values.
     *
     * @param pitch the target pitch value (will be clamped to 128-383 range)
     */
    public void setPitch(int pitch) {
        int minPitch = 128;
        int maxPitch = 383;
        // clamp pitch to avoid out of bounds
        pitch = Math.max(minPitch, Math.min(maxPitch, pitch));
        ctx.getClient().setCameraPitchTarget(pitch);
    }

    /**
     * Calculates the current camera pitch as a percentage of the total pitch range.
     *
     * @return the pitch percentage (0.0 to 1.0, where 0 is looking down and 1 is looking up)
     */
    public float cameraPitchPercentage() {
        int minPitch = 128;
        int maxPitch = 383;
        int currentPitch = ctx.getClient().getCameraPitch();

        int adjustedPitch = currentPitch - minPitch;
        int adjustedMaxPitch = maxPitch - minPitch;

        return (float) adjustedPitch / (float) adjustedMaxPitch;
    }

    /**
     * Calculates the angular difference between the current camera angle and the target angle.
     * Returns a positive value if the target is to the left, negative if to the right.
     *
     * @param degrees the target angle in degrees
     * @return the angular difference (-180 to 180 degrees)
     */
    public int getAngleTo(int degrees) {
        int ca = getAngle();
        if (ca < degrees) {
            ca += 360;
        }
        int da = ca - degrees;
        if (da > 180) {
            da -= 360;
        }
        return da;
    }

    /**
     * Gets the current camera yaw angle.
     * Converts from the client's fixed-point radians to degrees.
     *
     * @return the current camera angle in degrees (0-359)
     */
    public int getAngle() {
        // the client uses fixed point radians 0 - 2^14
        // degrees = yaw * 360 / 2^14 = yaw / 45.5111...
        // This leaves it on a scale of 45 versus a scale of 360 so we multiply it by 8 to fix that.
        return (int) Math.abs(ctx.getClient().getCameraYaw() / 45.51 * 8);
    }

    /**
     * Calculates the CameraYaw based on the given NPC or object angle.
     *
     * @param npcAngle the angle of the NPC or object relative to the player (0-359 degrees)
     * @return the calculated CameraYaw (0-2047)
     */
    public int calculateCameraYaw(int npcAngle) {
        // Convert the NPC angle to CameraYaw using the derived formula
        return (1536 + (int) Math.round(npcAngle * (2048.0 / 360.0))) % 2048;
    }

    /**
     * Track the NPC with the camera
     *
     * @param npcId the ID of the NPC to track
     */
    public void trackNpc(int npcId) {
        if (trackingTask != null && !trackingTask.isCancelled()) {
            log.error("Already tracking an NPC, cannot track another one.");
            return;
        }

        trackingTask = scheduler.scheduleAtFixedRate(() -> trackingJob(npcId), 0, 200, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop tracking the NPC with the camera
     */
    public void stopTrackingNpc() {
        if (trackingTask != null) {
            trackingTask.cancel(true);
            trackingTask = null;
        }
    }

    /**
     * Checks if a NPC is being tracked
     *
     * @return true if a NPC is being tracked, false otherwise
     */
    public boolean isTrackingNpc() {
        return trackingTask != null;
    }

    /**
     * Job which runs every 200ms to track an NPC with the camera.
     * @param id The npc id to track.
     */
    private void trackingJob(int id) {
        if (!(ctx.getClient().getGameState() == GameState.LOGGED_IN)) {
            return;
        }

        NPC npc = ctx.getClient().getTopLevelWorldView().npcs().stream().filter(Objects::nonNull).filter(n -> n.getId() == id).findFirst().orElse(null);

        if(npc == null) {
            return;
        }

        ctx.getClient().setCameraYawTarget(calculateCameraYaw(angleToTile(npc)));
    }

    /**
     * Checks if the specified tile object is visible on the screen.
     *
     * @param tileObject the tile object to check
     * @return true if the tile object is within the viewport bounds, false otherwise
     */
    public boolean isTileOnScreen(TileObject tileObject) {
        int viewportHeight = ctx.getClient().getViewportHeight();
        int viewportWidth = ctx.getClient().getViewportWidth();


        Polygon poly = Perspective.getCanvasTilePoly(ctx.getClient(), tileObject.getLocalLocation());

        if (poly == null) return false;

        return poly.getBounds2D().getX() <= viewportWidth && poly.getBounds2D().getY() <= viewportHeight;
    }

    /**
     * Checks if the specified local point is visible on the screen.
     * Verifies that the tile polygon intersects with the viewport and that the tile is in front of the camera.
     *
     * @param localPoint the local point to check
     * @return true if the tile is within the viewport bounds and in front of the camera, false otherwise
     */
    public boolean isTileOnScreen(LocalPoint localPoint) {
        int viewportHeight = ctx.getClient().getViewportHeight();
        int viewportWidth = ctx.getClient().getViewportWidth();

        Polygon poly = Perspective.getCanvasTilePoly(ctx.getClient(), localPoint);
        if (poly == null) return false;

        // Check if any part of the polygon intersects with the screen bounds
        Rectangle viewportBounds = new Rectangle(0, 0, viewportWidth, viewportHeight);
        if (!poly.intersects(viewportBounds)) return false;

        // Optionally, check if the tile is in front of the camera
        net.runelite.api.Point canvasPoint = Perspective.localToCanvas(ctx.getClient(), localPoint, ctx.getClient().getTopLevelWorldView().getPlane());
        return canvasPoint != null;
    }

    /**
     * Gets the current camera zoom level.
     *
     * @return the current zoom value from VarClientInt.CAMERA_ZOOM_RESIZABLE_VIEWPORT
     */
    public int getZoom() {
        // VarClientInt.CAMERA_ZOOM_RESIZABLE_VIEWPORT
        return ctx.getClient().getVarcIntValue(74);
    }

    /**
     * Sets the camera zoom to the specified level.
     * Invokes the CAMERA_DO_ZOOM script on the client thread.
     *
     * @param zoom the target zoom level
     */
    public void setZoom(int zoom) {
        runeliteClientThread.invokeLater(() -> {
            ctx.getClient().runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom);
        });
    }

    /**
     * Gets the current camera yaw (compass facing direction).
     *
     * @return the current yaw value (0-2047, where 0/2048=North, 512=West, 1024=South, 1536=East)
     */
    public int getYaw() {
        return ctx.getClient().getCameraYaw();
    }

    /**
     * Sets the camera yaw (compass facing direction).
     * <p>
     * Yaw values:
     * <ul>
     * <li>North = 0, 2048</li>
     * <li>East = 1536</li>
     * <li>South = 1024</li>
     * <li>West = 512</li>
     * </ul>
     *
     * @param yaw the target yaw value (must be between 0 and 2047)
     */
    public void setYaw(int yaw) {
        if ( yaw >= 0 && yaw < 2048 ) {
            ctx.getClient().setCameraYawTarget(yaw);
        }
    }

    /**
     * Resets the camera pitch to 280 if it is currently less than 280.
     */
    public void resetPitch() {
        // Set the camera pitch to 280
        if (getPitch() < 280)
            setPitch(280);
    }

    /**
     * Resets the camera zoom to 200 if it is currently greater than 200.
     */
    public void resetZoom() {
        // Set the camera zoom to 200
        if (getZoom() > 200)
            setZoom(200);
    }

    /**
     * Determines whether the specified tile is centered on the screen within a given tolerance.
     * <p>
     * Projects the tile to screen space, computes its bounding rectangle, and then checks
     * whether that rectangle lies entirely inside a centered "box" whose width and height
     * are the given percentage of the viewport dimensions.
     * </p>
     *
     * @param tile             the local tile coordinate to test (may not be null)
     * @param marginPercentage the size of the centered tolerance box, expressed as a percentage
     *                         of the viewport (e.g. 10.0 for 10%)
     * @return {@code true} if the tile's screen bounds lie entirely within the centered margin box;
     * {@code false} if the tile cannot be projected or lies outside that box
     */
    public boolean isTileCenteredOnScreen(LocalPoint tile, double marginPercentage) {
        Polygon poly = Perspective.getCanvasTilePoly(ctx.getClient(), tile);
        if (poly == null) return false;

        Rectangle tileBounds = poly.getBounds();
        int viewportWidth = ctx.getClient().getViewportWidth();
        int viewportHeight = ctx.getClient().getViewportHeight();
        int centerX = viewportWidth / 2;
        int centerY = viewportHeight / 2;

        int marginX = (int) (viewportWidth * (marginPercentage / 100.0));
        int marginY = (int) (viewportHeight * (marginPercentage / 100.0));

        Rectangle centerBox = new Rectangle(
                centerX - marginX / 2,
                centerY - marginY / 2,
                marginX,
                marginY
        );

        return centerBox.contains(tileBounds);
    }

    /**
     * Determines whether the specified tile is centered on the screen, using a default
     * margin tolerance of 10%.
     *
     * @param tile the local tile coordinate to test (may not be null)
     * @return {@code true} if the tile's screen bounds lie entirely within the centered
     * 10% margin box; {@code false} otherwise
     * @see #isTileCenteredOnScreen(LocalPoint, double)
     */
    public boolean isTileCenteredOnScreen(LocalPoint tile) {
        return isTileCenteredOnScreen(tile, 10);
    }

    /**
     * Rotates the camera to center on the specified tile, if it is not already within
     * the given margin tolerance.
     * <p>
     * Computes the bearing from the camera to the tile, adjusts it into a [0–360) range,
     * and then issues a small-angle camera turn if {@link #isTileCenteredOnScreen(LocalPoint, double)}
     * returns {@code false}.
     * </p>
     *
     * @param tile             the local tile coordinate to center on (may not be null)
     * @param marginPercentage the size of the centered tolerance box, expressed as a percentage
     *                         of the viewport (e.g. 10.0 for 10%)
     * @see #angleToTile(LocalPoint)
     * @see #setAngle(int, int)
     */
    public void centerTileOnScreen(LocalPoint tile, double marginPercentage) {
        // Calculate the desired camera angle for the tile
        int rawAngle = angleToTile(tile) - 90;
        int angle = rawAngle < 0 ? rawAngle + 360 : rawAngle;
        // Center if not already within margin
        if (!isTileCenteredOnScreen(tile, marginPercentage)) {
            setAngle(angle, 5); // Use small max angle for precision
        }
    }

    /**
     * Rotates the camera to center on the specified tile, using a default
     * margin tolerance of 10%.
     *
     * @param tile the local tile coordinate to center on (may not be null)
     * @see #centerTileOnScreen(LocalPoint, double)
     */
    public void centerTileOnScreen(LocalPoint tile) {
        centerTileOnScreen(tile, 10.0);
    }
}