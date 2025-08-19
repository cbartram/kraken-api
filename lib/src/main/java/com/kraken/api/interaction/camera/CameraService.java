package com.kraken.api.interaction.camera;


import com.kraken.api.core.AbstractService;
import com.kraken.api.core.SleepService;
import com.kraken.api.input.KeyboardService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginManager;
import net.runelite.client.plugins.camera.CameraPlugin;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Singleton
public class CameraService extends AbstractService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> trackingTask;

    @Inject
    private SleepService sleepService;

    @Inject
    private ConfigManager configManager;

    @Inject
    private PluginManager pluginManager;

    @Inject
    private KeyboardService keyboardService;

    @Inject
    private ClientThread runeliteClientThread;

    public int angleToTile(Actor t) {
        int angle = (int) Math.toDegrees(Math.atan2(t.getWorldLocation().getY() - client.getLocalPlayer().getWorldLocation().getY(),
                t.getWorldLocation().getX() - client.getLocalPlayer().getWorldLocation().getX()));
        return angle >= 0 ? angle : 360 + angle;
    }

    public int angleToTile(TileObject t) {
        int angle = (int) Math.toDegrees(Math.atan2(t.getWorldLocation().getY() - client.getLocalPlayer().getWorldLocation().getY(),
                t.getWorldLocation().getX() - client.getLocalPlayer().getWorldLocation().getX()));
        return angle >= 0 ? angle : 360 + angle;
    }

    public int angleToTile(LocalPoint localPoint) {
        int angle = (int) Math.toDegrees(Math.atan2(localPoint.getY() - client.getLocalPlayer().getLocalLocation().getY(),
                localPoint.getX() - client.getLocalPlayer().getLocalLocation().getX()));
        return angle >= 0 ? angle : 360 + angle;
    }

    public int angleToTile(WorldPoint worldPoint) {
        int angle = (int) Math.toDegrees(Math.atan2(worldPoint.getY() - client.getLocalPlayer().getWorldLocation().getY(),
                worldPoint.getX() - client.getLocalPlayer().getWorldLocation().getX()));
        return angle >= 0 ? angle : 360 + angle;
    }

    public void turnTo(final Actor actor) {
        int angle = getCharacterAngle(actor);
        setAngle(angle, 40);
    }

    public void turnTo(final Actor actor, int maxAngle) {
        int angle = getCharacterAngle(actor);
        setAngle(angle, maxAngle);
    }

    public void turnTo(final TileObject tileObject) {
        int angle = getObjectAngle(tileObject);
        setAngle(angle, 40);
    }

    public void turnTo(final TileObject tileObject, int maxAngle) {
        int angle = getObjectAngle(tileObject);
        setAngle(angle, maxAngle);
    }

    public void turnTo(final LocalPoint localPoint) {
        int angle = (angleToTile(localPoint) - 90) % 360;
        setAngle(angle, 40);
    }

    public void turnTo(final LocalPoint localPoint, int maxAngle) {
        int angle = (angleToTile(localPoint) - 90) % 360;
        setAngle(angle, maxAngle);
    }

    public int getCharacterAngle(Actor actor) {
        return getTileAngle(actor);
    }

    public int getObjectAngle(TileObject tileObject) {
        return getTileAngle(tileObject);
    }

    public int getTileAngle(Actor actor) {
        int a = (angleToTile(actor) - 90) % 360;
        return a < 0 ? a + 360 : a;
    }

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
        client.setCameraSpeed(3f);

        if (getAngleTo(targetDegrees) > maxAngle) {
            keyboardService.keyHold(KeyEvent.VK_LEFT);
            sleepService.sleepUntilTrue(() -> Math.abs(getAngleTo(targetDegrees)) <= maxAngle, 50, 5000);
            keyboardService.keyRelease(KeyEvent.VK_LEFT);
        } else if (getAngleTo(targetDegrees) < -maxAngle) {
            keyboardService.keyHold(KeyEvent.VK_RIGHT);
            sleepService.sleepUntilTrue(() -> Math.abs(getAngleTo(targetDegrees)) <= maxAngle, 50, 5000);
            keyboardService.keyRelease(KeyEvent.VK_RIGHT);
        }
        client.setCameraSpeed((float) defaultCameraSpeed);
    }

    public void adjustPitch(float percentage) {
        float currentPitchPercentage = cameraPitchPercentage();

        if (currentPitchPercentage < percentage) {
            keyboardService.keyHold(KeyEvent.VK_UP);
            sleepService.sleepUntilTrue(() -> cameraPitchPercentage() >= percentage, 50, 5000);
            keyboardService.keyRelease(KeyEvent.VK_UP);
        } else {
            keyboardService.keyHold(KeyEvent.VK_DOWN);
            sleepService.sleepUntilTrue(() -> cameraPitchPercentage() <= percentage, 50, 5000);
            keyboardService.keyRelease(KeyEvent.VK_DOWN);
        }
    }

    public int getPitch() {
        return client.getCameraPitch();
    }

    // set camera pitch
    public void setPitch(int pitch) {
        int minPitch = 128;
        int maxPitch = 383;
        // clamp pitch to avoid out of bounds
        pitch = Math.max(minPitch, Math.min(maxPitch, pitch));
        client.setCameraPitchTarget(pitch);
    }

    public float cameraPitchPercentage() {
        int minPitch = 128;
        int maxPitch = 383;
        int currentPitch = client.getCameraPitch();

        int adjustedPitch = currentPitch - minPitch;
        int adjustedMaxPitch = maxPitch - minPitch;

        return (float) adjustedPitch / (float) adjustedMaxPitch;
    }

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

    public int getAngle() {
        // the client uses fixed point radians 0 - 2^14
        // degrees = yaw * 360 / 2^14 = yaw / 45.5111...
        // This leaves it on a scale of 45 versus a scale of 360 so we multiply it by 8 to fix that.
        return (int) Math.abs(client.getCameraYaw() / 45.51 * 8);
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
        if (!(client.getGameState() == GameState.LOGGED_IN)) {
            return;
        }

        NPC npc = client.getTopLevelWorldView().npcs().stream().filter(Objects::nonNull).filter(n -> n.getId() == id).findFirst().orElse(null);

        if(npc == null) {
            return;
        }

        client.setCameraYawTarget(calculateCameraYaw(angleToTile(npc)));
    }

    public boolean isTileOnScreen(TileObject tileObject) {
        int viewportHeight = client.getViewportHeight();
        int viewportWidth = client.getViewportWidth();


        Polygon poly = Perspective.getCanvasTilePoly(client, tileObject.getLocalLocation());

        if (poly == null) return false;

        return poly.getBounds2D().getX() <= viewportWidth && poly.getBounds2D().getY() <= viewportHeight;
    }

    public boolean isTileOnScreen(LocalPoint localPoint) {
        int viewportHeight = client.getViewportHeight();
        int viewportWidth = client.getViewportWidth();

        Polygon poly = Perspective.getCanvasTilePoly(client, localPoint);
        if (poly == null) return false;

        // Check if any part of the polygon intersects with the screen bounds
        Rectangle viewportBounds = new Rectangle(0, 0, viewportWidth, viewportHeight);
        if (!poly.intersects(viewportBounds)) return false;

        // Optionally, check if the tile is in front of the camera
        Point canvasPoint = Perspective.localToCanvas(client, localPoint, client.getTopLevelWorldView().getPlane());
        return canvasPoint != null;
    }

    // get the camera zoom
    public int getZoom() {
        return client.getVarcIntValue(VarClientInt.CAMERA_ZOOM_RESIZABLE_VIEWPORT);
    }

    public void setZoom(int zoom) {
        runeliteClientThread.invokeLater(() -> {
            client.runScript(ScriptID.CAMERA_DO_ZOOM, zoom, zoom);
        });
    }
    // Get camera/compass facing
    public int getYaw() {
        return client.getCameraYaw();
    }

    // Set camera/compass facing
    // North = 0, 2048
    // East = 1536
    // South = 1024
    // West = 512

    public void setYaw(int yaw) {
        if ( yaw >= 0 && yaw < 2048 ) {
            client.setCameraYawTarget(yaw);
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
     * @return {@code true} if the tile’s screen bounds lie entirely within the centered margin box;
     * {@code false} if the tile cannot be projected or lies outside that box
     */
    public boolean isTileCenteredOnScreen(LocalPoint tile, double marginPercentage) {
        Polygon poly = Perspective.getCanvasTilePoly(client, tile);
        if (poly == null) return false;

        Rectangle tileBounds = poly.getBounds();
        int viewportWidth = client.getViewportWidth();
        int viewportHeight = client.getViewportHeight();
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
     * @return {@code true} if the tile’s screen bounds lie entirely within the centered
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
