package com.kraken.api.core;

import com.kraken.api.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// T = The type of object being queried (e.g., NPC, Widget, TileItem)
// Q = The concrete query class (e.g., NpcQuery), used for method chaining
public abstract class AbstractQuery<T extends Interactable, Q extends AbstractQuery<T, Q>> {

    protected final Context ctx;
    private final List<Predicate<T>> filters = new ArrayList<>();

    public AbstractQuery(Context ctx) {
        this.ctx = ctx;
    }

    protected abstract Supplier<Stream<T>> source();

    @SuppressWarnings("unchecked")
    public Q filter(Predicate<T> predicate) {
        if (predicate != null) {
            filters.add(predicate);
        }
        return (Q) this;
    }

    // Standard filter implementations that apply to all Interactables
    public Q withName(String name) {
        return filter(t -> t.getName() != null && t.getName().equalsIgnoreCase(name));
    }

    public List<T> list() {
        return ctx.runOnClientThread(() -> {
            Stream<T> stream = source().get();
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }
            return stream.collect(Collectors.toList());
        });
    }

    public T first() {
        return ctx.runOnClientThread(() -> {
            Stream<T> stream = source().get();
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }
            return stream.findFirst().orElse(null);
        });
    }
}