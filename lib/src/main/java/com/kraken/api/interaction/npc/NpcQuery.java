package com.kraken.api.interaction.npc;

import com.kraken.api.Context;
import com.kraken.api.core.AbstractQuery;

import java.util.Comparator;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class NpcQuery extends AbstractQuery<NpcEntity, NpcQuery> {

    public NpcQuery(Context ctx) {
        super(ctx);
    }

    @Override
    protected Supplier<Stream<NpcEntity>> source() {
        return () -> ctx.getClient().getTopLevelWorldView().npcs().stream()
                .filter(Objects::nonNull)
                .filter(x -> x.getName() != null)
                .sorted(Comparator.comparingInt(value -> value.getLocalLocation().distanceTo(ctx.getClient().getLocalPlayer().getLocalLocation())))
                .map(rawNpc -> new NpcEntity(ctx, rawNpc));
    }
}