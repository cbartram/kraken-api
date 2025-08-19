package com.kraken.api.interaction.ui;

import com.kraken.api.core.AbstractService;
import com.kraken.api.core.RandomService;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Perspective;
import net.runelite.api.Point;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;
import net.runelite.api.coords.LocalPoint;

import javax.inject.Singleton;
import java.awt.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Singleton
public class UIService extends AbstractService {

    public Rectangle getDefaultRectangle() {
        int randomValue = ThreadLocalRandom.current().nextInt(3) - 1;
        return new Rectangle(randomValue, randomValue, client.getCanvasWidth(), client.getCanvasHeight());
    }
    
    public Rectangle getActorClickbox(Actor actor) {
        LocalPoint lp = actor.getLocalLocation();
        if (lp == null) {
            return getDefaultRectangle();
        }

        Shape clickbox = context.runOnClientThreadOptional(() -> Perspective.getClickbox(client, actor.getModel(), actor.getCurrentOrientation(), lp.getX(), lp.getY(),
                        Perspective.getTileHeight(client, lp, actor.getWorldLocation().getPlane())))
                .orElse(null);

        if (clickbox == null) return getDefaultRectangle();
        return new Rectangle(clickbox.getBounds());
    }

    public Rectangle getObjectClickbox(TileObject object) {
        if (object == null) return getDefaultRectangle();
        Shape clickbox = context.runOnClientThreadOptional(object::getClickbox).orElse(null);
        if (clickbox == null) return getDefaultRectangle();
        if (clickbox.getBounds() == null) return getDefaultRectangle();

        return new Rectangle(clickbox.getBounds());
    }

    public Rectangle getTileClickbox(Tile tile) {
        if (tile == null) return getDefaultRectangle();

        LocalPoint localPoint = tile.getLocalLocation();
        if (localPoint == null) return getDefaultRectangle();

        // Get the screen point of the tile center
        Point screenPoint = Perspective.localToCanvas(client, localPoint, client.getTopLevelWorldView().getPlane());

        if (screenPoint == null) return getDefaultRectangle();

        int tileSize = Perspective.LOCAL_TILE_SIZE;
        int halfSize = tileSize / 4;

        return new Rectangle(screenPoint.getX() - halfSize, screenPoint.getY() - halfSize, tileSize / 2, tileSize / 2);
    }

    public static Point getClickingPoint(Rectangle rectangle, boolean randomize) {
        if (rectangle == null) return new Point(1, 1);
        if (rectangle.getX() == 1 && rectangle.getY() == 1) return new Point(1, 1);
        if (rectangle.getX() == 0 && rectangle.getY() == 0) return new Point(1, 1);

        if (!randomize) return new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY());

        return RandomService.randomPointEx(new Point((int) rectangle.getCenterX(), (int) rectangle.getCenterY()), rectangle, 0.78);
    }
}
