package codex.fundamentum.api.cache;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * A named region of a cache holding entries of type {@link CacheEntry}{@code <V>} keyed by {@code K}.
 *
 * <p>Cache is not canonical storage. Canonical services remain the source of truth. A
 * {@code CacheRegion} accelerates identity reads and repeated misses; it must never be the only
 * place where data lives.
 *
 * <p><b>Three-state semantics:</b>
 * <ul>
 *   <li>{@link #get(Object)} returns {@code Optional.empty()} when the key has never been cached.</li>
 *   <li>{@link #get(Object)} returns {@code Optional.of(CacheEntry.Found)} for a positive hit.</li>
 *   <li>{@link #get(Object)} returns {@code Optional.of(CacheEntry.NotFound)} for a cached 404.</li>
 * </ul>
 *
 * <p><b>Atomic loading:</b> Implementations should avoid executing the loader multiple times for
 * the same key when they can provide atomic loading. {@link ConcurrentMapCacheRegion} uses
 * {@link java.util.concurrent.ConcurrentHashMap#computeIfAbsent} to guarantee this. A future
 * Caffeine implementation will map this to {@code cache.get(key, mappingFunction)}. Distributed
 * Redis implementations may not guarantee single-flight loading without additional locking.
 *
 * <p><b>Future adapters:</b> Caffeine, Redis, and Chronicle Map are planned as future
 * implementations. TTL, refresh-ahead, cache invalidation subscribers, and configuration are
 * not part of this foundation.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public interface CacheRegion<K, V> {

    /**
     * Returns the cached entry for the given key, or {@code Optional.empty()} on a cache miss.
     *
     * @param key the cache key; must not be null
     * @return {@code Optional.empty()} if not cached, otherwise the cached {@link CacheEntry}
     */
    Optional<CacheEntry<V>> get(K key);

    /**
     * Returns the cached entry for the key, loading and storing it if absent.
     *
     * <p>The loader is only called when the key is not present in the cache. If the loader returns
     * {@link CacheEntry.NotFound}, that negative entry is stored like any other entry so that
     * subsequent calls do not invoke the loader again.
     *
     * @param key    the cache key; must not be null
     * @param loader supplies the entry to cache on a miss; must not be null and must not return null
     * @return the cached or freshly loaded entry
     */
    CacheEntry<V> getOrLoad(K key, Supplier<? extends CacheEntry<V>> loader);

    /**
     * Stores an entry in the cache under the given key.
     *
     * @param key   the cache key; must not be null
     * @param entry the entry to store; must not be null
     */
    void put(K key, CacheEntry<V> entry);

    /**
     * Removes the entry for the given key from the cache.
     *
     * @param key the cache key; must not be null
     */
    void evict(K key);

    /**
     * Removes all entries from this cache region.
     */
    void clear();
}
