package codex.fundamentum.api.cache;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * A {@link CacheRegion} backed by a {@link ConcurrentHashMap}.
 *
 * <p>{@link #getOrLoad(Object, Supplier)} uses {@link ConcurrentHashMap#computeIfAbsent} to
 * guarantee that the loader is called at most once per key under concurrent access. If the loader
 * returns {@link CacheEntry.NotFound}, that negative entry is stored like any other entry, caching
 * the "not found" result and preventing repeated canonical-source lookups.
 *
 * <p>This implementation has no maximum size, no eviction policy, and no TTL. It is suitable for
 * test environments and low-volume in-process caching. A future Caffeine adapter will add bounded
 * size, TTL, and refresh-ahead behavior.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class ConcurrentMapCacheRegion<K, V> implements CacheRegion<K, V> {

    private final ConcurrentHashMap<K, CacheEntry<V>> store = new ConcurrentHashMap<>();

    @Override
    public Optional<CacheEntry<V>> get(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public CacheEntry<V> getOrLoad(final K key, final Supplier<? extends CacheEntry<V>> loader) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(loader, "loader must not be null");
        return store.computeIfAbsent(key, k -> {
            final CacheEntry<V> entry = loader.get();
            return Objects.requireNonNull(entry, "loader must not return null");
        });
    }

    @Override
    public void put(final K key, final CacheEntry<V> entry) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(entry, "entry must not be null");
        store.put(key, entry);
    }

    @Override
    public void evict(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        store.remove(key);
    }

    @Override
    public void clear() {
        store.clear();
    }
}
