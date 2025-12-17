package com.kraken.api.query.player;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.Actor;
import net.runelite.api.Player;
import net.runelite.api.coords.WorldPoint;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerQuery extends AbstractQuery<PlayerEntity, PlayerQuery, Player> {

    public PlayerQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<PlayerEntity>> source() {
        return () -> {
            List<PlayerEntity> players = ctx.getClient().getTopLevelWorldView().players().stream()
                    .filter(Objects::nonNull)
                    // Do not include the local player by default
                    .filter(p -> ctx.runOnClientThread(() -> p.getName() != null && !p.getName().equalsIgnoreCase(ctx.getClient().getLocalPlayer().getName())))
                    .map(player -> new PlayerEntity(ctx, player))
                    .collect(Collectors.toList());
            return players.stream();
        };
    }

    /**
     * Filters the stream for players interacting with a specified actor
     * @param actor The actor to check for interacting with
     * @return PlayerQuery
     */
    public PlayerQuery interactingWith(Actor actor) {
        return filter(p -> p.raw().isInteracting() && p.raw().getInteracting() == actor);
    }

    /**
     * Returns a stream of Players at a given world point.
     * @param location The world point to check
     * @return PlayerQuery
     */
    public PlayerQuery at(WorldPoint location) {
        return filter(p -> p.raw().getWorldLocation().equals(location));
    }

    /**
     * Filters for players within a specified area. The min WorldPoint should be the southwest tile of the area
     * and the max WorldPoint should be the northeast tile of the area.
     * @param minimum The southwest minimum world point of the area to check
     * @param max The northeast maximum world point of the area to check
     * @return NpcQuery
     */
    public PlayerQuery withinArea(WorldPoint minimum, WorldPoint max) {
        int x1 = minimum.getX();
        int x2 = max.getX();
        int y1 = minimum.getY();
        int y2 = max.getY();

        return filter(p -> {
            WorldPoint pt = p.raw().getWorldLocation();
            int x3 = pt.getX();
            int y3 = pt.getY();

            if (x3 > Math.max(x1, x2) || x3 < Math.min(x1, x2)) {
                return false;
            }

            return y3 <= Math.max(y1, y2) && y3 >= Math.min(y1, y2);
        });
    }

    /**
     * Filters the stream for players within a specified distance from the local player.
     *
     * @param distance The maximum distance from the local player.
     * @return PlayerQuery
     */
    public PlayerQuery withinDistance(int distance) {
        return filter(p -> p.raw().getLocalLocation().distanceTo(ctx.getClient().getLocalPlayer().getLocalLocation()) <= distance);
    }

    /**
     * Filters the stream for players within a specified combat level (inclusive).
     * @param low The minimum combat level bound
     * @param high The maximum combat level bound
     * @return PlayerQuery
     */
    public PlayerQuery withinLevel(int low, int high) {
        return filter(p -> p.raw().getCombatLevel() >= low && p.raw().getCombatLevel() <= high);
    }

    /**
     * Finds players with a combat level strictly greater than the given argument
     * @param level Combat level
     * @return PlayerQuery
     */
    public PlayerQuery combatLevelGreaterThan(int level) {
        return filter(player -> player.raw().getCombatLevel() > level);
    }

    /**
     * Finds the nearest player to the local player.
     * @return PlayerQuery
     */
    public PlayerEntity nearest() {
        return sorted(Comparator.comparingInt(p ->
                p.raw().getLocalLocation().distanceTo(ctx.getClient().getLocalPlayer().getLocalLocation())
        )).first();
    }

    /**
     * Directly retrieves the local player wrapper.
     * Does not use the filter list.
     * @return PlayerEntity the {@code LocalPlayerEntity} object.
     */
    public LocalPlayerEntity local() {
        return ctx.getLocalPlayer();
    }
}
