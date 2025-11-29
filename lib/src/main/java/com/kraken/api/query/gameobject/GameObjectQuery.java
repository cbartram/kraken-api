package com.kraken.api.query.gameobject;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.GameObject;
import net.runelite.api.Tile;
import net.runelite.api.TileObject;

import java.util.HashSet;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class GameObjectQuery extends AbstractQuery<GameObjectEntity, GameObjectQuery> {

    public GameObjectQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<GameObjectEntity>> source() {
        return () -> {
            HashSet<TileObject> tileObjectHashSet = new HashSet<>();
            for (Tile[] tiles : ctx.getClient().getTopLevelWorldView().getScene().getTiles()[ctx.getClient().getTopLevelWorldView().getPlane()]) {
                if (tiles == null) {
                    continue;
                }
                for (Tile tile : tiles) {
                    if (tile == null) {
                        continue;
                    }
                    for (GameObject gameObject : tile.getGameObjects()) {
                        if (gameObject == null) {
                            continue;
                        }
                        if (gameObject.getId() == -1) {
                            continue;
                        }
                        tileObjectHashSet.add(gameObject);
                    }
                    if (tile.getGroundObject() != null) {
                        if (tile.getGroundObject().getId() == -1) {
                            continue;
                        }
                        tileObjectHashSet.add(tile.getGroundObject());
                    }
                    if (tile.getWallObject() != null) {
                        if (tile.getWallObject().getId() == -1) {
                            continue;
                        }
                        tileObjectHashSet.add(tile.getWallObject());
                    }
                    if (tile.getDecorativeObject() != null) {
                        if (tile.getDecorativeObject().getId() == -1) {
                            continue;
                        }
                        tileObjectHashSet.add(tile.getDecorativeObject());
                    }
                }
            }

            return tileObjectHashSet.stream().map(t -> new GameObjectEntity(ctx, t));
        };
    }
}
