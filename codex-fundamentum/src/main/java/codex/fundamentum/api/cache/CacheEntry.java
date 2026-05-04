package codex.fundamentum.api.cache;

import java.util.Objects;

/**
 * Represents a cache entry that is either a positive hit ({@link Found}) carrying a value, or a
 * negative hit ({@link NotFound}) representing a cached "not found" / 404 response.
 *
 * <p>The three possible cache states when using {@link CacheRegion#get(Object)}:
 * <ul>
 *   <li>{@code Optional.empty()} — cache miss; the canonical source has not yet been consulted.</li>
 *   <li>{@code Optional.of(CacheEntry.Found(value))} — cache hit; the value is available.</li>
 *   <li>{@code Optional.of(CacheEntry.NotFound)} — negative cache hit; the canonical source
 *       previously confirmed this key does not exist.</li>
 * </ul>
 *
 * <p>Storing {@link NotFound} prevents repeated canonical-source lookups for absent resources.
 * When a resource is later created, an event-driven invalidation subscriber should evict the
 * negative entry so the next request loads the real value.
 *
 * <p><b>Future TTL direction:</b> {@link Found} entries may eventually carry a longer TTL than
 * {@link NotFound} entries (e.g., 5 min vs 30 sec). TTL is not modeled here; it belongs in a
 * future {@code CachePolicy}.
 *
 * @param <V> the type of the cached value
 */
public sealed interface CacheEntry<V> permits CacheEntry.Found, CacheEntry.NotFound {

    /**
     * Returns {@code true} if this is a positive cache hit carrying a value.
     */
    default boolean isFound() {
        return this instanceof Found<?>;
    }

    /**
     * Returns {@code true} if this is a negative cache hit (cached 404).
     */
    default boolean isNotFound() {
        return this instanceof NotFound<?>;
    }

    /**
     * Creates a positive cache entry wrapping the given value.
     *
     * @param value the value to cache; must not be null
     * @param <V>   the value type
     * @return a {@link Found} entry
     */
    static <V> CacheEntry<V> found(final V value) {
        return new Found<>(value);
    }

    /**
     * Creates a negative cache entry representing a cached "not found" / 404 response.
     *
     * @param <V> the value type
     * @return a {@link NotFound} entry
     */
    static <V> CacheEntry<V> notFound() {
        return new NotFound<>();
    }

    /**
     * A positive cache entry carrying the cached value.
     *
     * @param <V> the value type
     */
    record Found<V>(V value) implements CacheEntry<V> {

        public Found {
            Objects.requireNonNull(value, "cached value must not be null");
        }
    }

    /**
     * A negative cache entry representing a cached "not found" / 404 response.
     *
     * <p>No value is carried. All {@code NotFound} instances are equal.
     *
     * @param <V> the value type (unused; present for type safety with {@link CacheRegion})
     */
    record NotFound<V>() implements CacheEntry<V> {}
}
