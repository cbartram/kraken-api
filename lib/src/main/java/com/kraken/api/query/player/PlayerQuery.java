package com.kraken.api.query.player;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PlayerQuery extends AbstractQuery<PlayerEntity, PlayerQuery> {

    public PlayerQuery(Context ctx) {
        super(ctx);
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

            players.add(new PlayerEntity(ctx, ctx.getClient().getLocalPlayer()));
            return players.stream();
        };
    }
}
