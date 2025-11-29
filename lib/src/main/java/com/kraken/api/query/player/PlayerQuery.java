package com.kraken.api.query.player;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;
import net.runelite.api.Player;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerQuery extends AbstractQuery<PlayerEntity, PlayerQuery> {

    private final ScheduledExecutorService executor;

    public PlayerQuery(Context ctx, ScheduledExecutorService executor) {
        super(ctx);
        this.executor = executor;
    }

    @Override
    protected Supplier<Stream<PlayerEntity>> source() {
        return () -> {
            List<PlayerEntity> players = ctx.getClient().getTopLevelWorldView().players().stream()
                    .filter(Objects::nonNull)
                    .filter(x -> x.getName() != null)
                    .sorted(Comparator.comparingInt(value -> value.getLocalLocation().distanceTo(ctx.getClient().getLocalPlayer().getLocalLocation())))
                    .map(player -> new PlayerEntity(ctx, player))
                    .collect(Collectors.toList());
            return players.stream();
        };
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
     * Finds a player by their name
     * @param name The players name
     * @return PlayerQuery
     */
    public PlayerQuery withName(String name) {
        return filter(player -> player.getName() != null && player.getName().equalsIgnoreCase(name));
    }

    /**
     * Finds the nearest player to the local player.
     * @return PlayerQuery
     */
    public PlayerQuery nearest() {
        return sorted(Comparator.comparingInt(p ->
                p.raw().getLocalLocation().distanceTo(ctx.getClient().getLocalPlayer().getLocalLocation())
        ));
    }

    /**
     * Directly retrieves the local player wrapper.
     * Does not use the filter list.
     * @return PlayerEntity the {@code LocalPlayerEntity} object.
     */
    public LocalPlayerEntity local() {
        return ctx.runOnClientThread(() -> {
            Player local = ctx.getClient().getLocalPlayer();
            if(local == null) return null;
            LocalPlayerEntity localEntity = new LocalPlayerEntity(ctx, local, executor);
            ctx.getEventBus().register(localEntity);
            return localEntity;
        });
    }
}
