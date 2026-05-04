package codex.fundamentum.api.cache;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A {@link CacheRegion} that never caches anything.
 *
 * <p>Useful when caching is disabled but callers still depend on the {@link CacheRegion}
 * abstraction. Every call to {@link #get(Object)} is a miss. Every call to
 * {@link #getOrLoad(Object, Supplier)} invokes the loader and returns its result without storing
 * it.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class NoOpCacheRegion<K, V> implements CacheRegion<K, V> {

    @Override
    public Optional<CacheEntry<V>> get(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.empty();
    }

    @Override
    public CacheEntry<V> getOrLoad(final K key, final Supplier<? extends CacheEntry<V>> loader) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(loader, "loader must not be null");
        final CacheEntry<V> entry = loader.get();
        Objects.requireNonNull(entry, "loader must not return null");
        return entry;
    }

    @Override
    public void put(final K key, final CacheEntry<V> entry) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(entry, "entry must not be null");
    }

    @Override
    public void evict(final K key) {
        Objects.requireNonNull(key, "key must not be null");
    }

    @Override
    public void clear() {
        // no-op
    }
}
