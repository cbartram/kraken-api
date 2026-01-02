package com.kraken.api.core;

import com.kraken.api.Context;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Forms the base class for all game client queries. This class defines generic actions which can be taken
 * on streams of game objects like NPC's, Ground Items, Tile Objects, Players and Widgets.
 * @param <T> The type of object being queried (e.g., NpcEntity, WidgetEntity)
 * @param <Q> The concrete query class (e.g., NpcQuery)
 * @param <R> The raw RuneLite type (NPC, Widget, TileObject, etc.)
 */
public abstract class AbstractQuery<T extends Interactable<R>, Q extends AbstractQuery<T, Q, R>, R> {
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

    /**
     * Filters for only entities whose name matches the provided name. This is case-insensitive.
     * @param name The name of the object to filter for
     * @return Q entities whose name matches
     */
    public Q withName(String name) {
        return filter(t -> t.getName() != null && t.getName().equalsIgnoreCase(name));
    }

    /**
     * Filters the stream of game entities for ones where the ID matches a provided id
     * @param id Int the item ID to match against.
     * @return Q entities whose item id matches the provided ID.
     */
    public Q withId(int id) {
        return filter(t -> t.getId() == id);
    }

    /**
     * Filters for entities whose name contains the substring or a portion of the name parameter.
     * @param name The name to match against
     * @return Entities whose name contains the prefix
     */
    public Q nameContains(String name) {
        return filter(t -> t.getName() != null && t.getName().toLowerCase().contains(name.toLowerCase()));
    }

    /**
     * Randomizes the order of elements in the stream and returns a new stream with the shuffled elements.
     * <p>
     * This method collects all elements in the stream into a list, shuffles the list using
     * {@code Collections.shuffle(List)}, and then converts the shuffled list back into a stream for further use.
     * </p>
     *
     * <p>
     * Note: This operation consumes the original stream, making it unsuitable for re-use after calling this method.
     * Additionally, the shuffling process may impact performance for very large datasets due to memory usage
     * (as it fully materializes the stream into a list) and the shuffling algorithm.
     * </p>
     *
     * @return A new {@code Stream<T>} containing the same elements as the original stream, but in a randomized order.
     */
    public Stream<T> shuffle() {
        List<T> list = stream().collect(Collectors.toList());
        Collections.shuffle(list);
        return list.stream();
    }

    /**
     * Reverses the order of elements in the stream and returns a new stream with the reversed order.
     * <p>
     * This method collects all elements in the stream into a list, reverses the list using
     * {@code Collections.reverse(List)}, and then converts the reversed list back into a stream for further use.
     * </p>
     *
     * <p>
     * Note: This operation consumes the original stream, making it unsuitable for re-use
     * after calling this method. Additionally, the reversal process may impact performance
     * for very large datasets due to memory usage (as it fully materializes the stream into a list).
     * </p>
     *
     * @return A new {@code Stream<T>} containing the same elements as the original stream, but in reversed order.
     */
    public Stream<T> reverse() {
        List<T> list = stream().collect(Collectors.toList());
        Collections.reverse(list);
        return list.stream();
    }


    /**
     * Returns the raw stream of elements in the query so filters and matching can be
     * manually applied.
     * @return Stream of entities
     */
    public Stream<T> stream() {
        return ctx.runOnClientThread(() -> {
            Stream<T> stream = source().get();

            if(stream == null) {
                return Stream.empty();
            }

            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }

            if (comparator != null) {
                stream = stream.sorted(comparator);
            }

            return stream;
        });
    }

    /**
     * Returns the underlying RuneLite entities that have been wrapped by the API. For example:
     * {@code ctx.gameObjects().toRuneLite()} returns a list of {@code TileObjects}. You will not be
     * able to perform any interactions on these objects after calling {@code toRuneLite} as they lose
     * their {@code Interactable} wrapping.
     * @return Stream of RuneLite API objects
     */
    public Stream<R> toRuneLite() {
        return ctx.runOnClientThread(() -> stream().map(T::raw));
    }

    /**
     * Returns a count of objects in the stream
     * @return long count of objects.
     */
    public long count() {
        return (Long) ctx.runOnClientThread(() -> {
            Stream<T> stream = source().get();

            if(stream == null) {
                return Collections.emptyList();
            }

            // Apply filters but do not waste time sorting for a basic count op
            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }

            return stream.count();
        });
    }

    /**
     * Filters out elements that match the given predicate.
     * Effectively: filter(!predicate)
     * @param predicate The predicate to apply
     * @return Q All entities except for the ones that match the given predicate
     */
    @SuppressWarnings("unchecked")
    public Q except(Predicate<T> predicate) {
        if (predicate != null) {
            filters.add(predicate.negate());
        }
        return (Q) this;
    }

    /**
     * Returns a random element from the filtered list.
     * Useful for anti-ban measures (e.g., picking a random cow to attack).
     * @return T A random entity from the stream
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
     * @param keyExtractor The function to use to determine uniqueness keys between entities
     * @return Q The distinct entities
     */
    public Q distinct(Function<T, Object> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return filter(t -> seen.add(keyExtractor.apply(t)));
    }

    /**
     * Applies a comparator to the stream for sorting elements within the stream.
     * @param comparator Comparator to add
     * @return Q A sorted stream of entities
     */
    @SuppressWarnings("unchecked")
    public Q sorted(Comparator<T> comparator) {
        this.comparator = comparator;
        return (Q) this;
    }

    /**
     * Returns the stream of entities as a list of objects
     * @return A list of objects that have been queried (e.g., NpcEntity, WidgetEntity)
     */
    public List<T> list() {
        return ctx.runOnClientThread(() -> {
            Stream<T> stream = source().get();

            if(stream == null) {
                return Collections.emptyList();
            }

            for (Predicate<T> filter : filters) {
                stream = stream.filter(filter);
            }

            if (comparator != null) {
                stream = stream.sorted(comparator);
            }

            return stream.collect(Collectors.toList());
        });
    }

    /**
     * An alias for {@code list()}.
     * @return The stream of entities as a list of objects.
     */
    public List<T> result() {
        return list();
    }

    /**
     * Collects the stream of entities into a map keyed by the id of the element in the map. Generally this will
     * be the item id for objects like {@code ContainerItem}, {@code EquipmentEntity}, and {@code GroundObjectEntity} but
     * can take on other ids for things like Game objects, NPC's and widgets.
     * @return Map of entities keyed by their id.
     */
    public Map<Integer, T> map() {
        return stream().collect(Collectors.toMap(T::getId, Function.identity()));
    }

    /**
     * Returns the first type of object being queried (e.g., NpcEntity, WidgetEntity) from the stream.
     * If the stream contains no objects then this will return null.
     * @return T The type of object being queried (e.g., NpcEntity, WidgetEntity)
     */
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

    /**
     * Takes the first N elements from the stream and returns them as a list.
     * @param n The number of elements to take from the stream.
     * @return List of entities
     */
    public List<T> take(int n) {
        return stream().limit(n).collect(Collectors.toList());
    }
}