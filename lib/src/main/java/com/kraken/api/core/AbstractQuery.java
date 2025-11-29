package com.kraken.api.core;

import com.kraken.api.Context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// T = The type of object being queried (e.g., NPC, Widget, TileItem)
// Q = The concrete query class (e.g., NpcQuery), used for method chaining
public abstract class AbstractQuery<T extends Interactable, Q extends AbstractQuery<T, Q>> {

    protected final Context ctx;
    private final List<Predicate<T>> filters = new ArrayList<>();
    private Comparator<T> comparator = null;

    public AbstractQuery(Context ctx) {
        this.ctx = ctx;
    }

    protected abstract Supplier<Stream<T>> source();

    /**
     * Applies a predicate to the stream to filter elements of the stream.
     * @param predicate Filter to add
     * @return Q
     */
    @SuppressWarnings("unchecked")
    public Q filter(Predicate<T> predicate) {
        if (predicate != null) {
            filters.add(predicate);
        }
        return (Q) this;
    }

    public Q withName(String name) {
        return filter(t -> t.getName() != null && t.getName().equalsIgnoreCase(name));
    }

    /**
     * Filters out elements that match the given predicate.
     * Effectively: filter(!predicate)
     */
    @SuppressWarnings("unchecked")
    public Q except(Predicate<T> predicate) {
        if (predicate != null) {
            // Predicate.negate() flips true to false and vice versa
            filters.add(predicate.negate());
        }
        return (Q) this;
    }

    /**
     * Returns a random element from the filtered list.
     * Excellent for anti-ban (e.g., picking a random cow to attack).
     */
    public T random() {
        List<T> all = list();
        if (all.isEmpty()) return null;
        return all.get(new Random().nextInt(all.size()));
    }

    /**
     * Filters the stream to only include elements that are distinct based on a property.
     * Usage: ctx.npcs().distinct(NpcEntity::getName).list();
     * (Returns one of each type of NPC nearby)
     */
    public Q distinct(Function<T, Object> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return filter(t -> seen.add(keyExtractor.apply(t)));
    }

    /**
     * Applies a comparator to the stream for sorting elements within the stream.
     * @param comparator Comparator to add
     * @return Q
     */
    @SuppressWarnings("unchecked")
    public Q sorted(Comparator<T> comparator) {
        this.comparator = comparator;
        return (Q) this;
    }

    public List<T> list() {
        return ctx.runOnClientThread(() -> {
            Stream<T> stream = source().get();
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }

            if (comparator != null) {
                stream = stream.sorted(comparator);
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

            if (comparator != null) {
                stream = stream.sorted(comparator);
            }

            return stream.findFirst().orElse(null);
        });
    }
}