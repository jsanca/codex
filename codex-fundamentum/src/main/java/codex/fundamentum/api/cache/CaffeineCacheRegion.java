package codex.fundamentum.api.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A {@link CacheRegion} backed by a Caffeine {@link Cache}.
 *
 * <p>Caffeine provides high-performance local in-memory caching with bounded size,
 * compute-if-absent loading semantics, and eviction policies. This adapter wraps a
 * {@code Cache<K, CacheEntry<V>>} and maps the Codex three-state cache contract
 * onto Caffeine's native API.</p>
 *
 * <p>{@link #getOrLoad(Object, Supplier)} uses {@link Cache#get(Object, java.util.function.Function)}
 * for atomic loading — Caffeine guarantees at most one loader invocation per key under concurrent
 * access for a given cache entry. Both {@link CacheEntry.Found} and {@link CacheEntry.NotFound}
 * entries are stored identically, enabling the negative-cache pattern.</p>
 *
 * <p>Usage:</p>
 * <pre>{@code
 * CacheRegion<SiteKey, Site> region = CaffeineCacheRegion.maximumSize(1_000);
 * CacheEntry<Site> entry = region.getOrLoad(key, () -> siteRepository.findByKey(key)
 *         .map(CacheEntry::found)
 *         .orElse(CacheEntry.notFound()));
 * }</pre>
 *
 * <p><b>No TTL yet.</b> Future versions may support different TTL policies for
 * {@link CacheEntry.Found} and {@link CacheEntry.NotFound} entries.</p>
 *
 * <p><b>No refresh-ahead yet.</b> Future versions may use Caffeine {@code refreshAfterWrite}
 * or scheduled warmers for eager cache regions.</p>
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class CaffeineCacheRegion<K, V> implements CacheRegion<K, V> {

    private final Cache<K, CacheEntry<V>> cache;

    /**
     * Creates a {@code CaffeineCacheRegion} wrapping the provided Caffeine cache.
     *
     * @param cache the Caffeine cache to delegate to; must not be null
     */
    public CaffeineCacheRegion(final Cache<K, CacheEntry<V>> cache) {
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
    }

    // --- factories ---

    /**
     * Creates an unbounded {@code CaffeineCacheRegion}.
     *
     * <p>No maximum size is set. Suitable for caches where the key space is small and
     * well-bounded by design (e.g. site registry). Use {@link #maximumSize(long)} for
     * content-item or high-cardinality caches.</p>
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return a new, unbounded cache region
     */
    public static <K, V> CaffeineCacheRegion<K, V> unbounded() {
        return new CaffeineCacheRegion<>(Caffeine.newBuilder().build());
    }

    /**
     * Creates a bounded {@code CaffeineCacheRegion} with the given maximum size.
     *
     * <p>When the cache exceeds this size Caffeine evicts entries based on its internal
     * frequency-sketch eviction policy (W-TinyLFU). Eviction may be slightly deferred
     * relative to put operations.</p>
     *
     * @param maximumSize the maximum number of entries to hold; must be at least 1
     * @param <K>         the key type
     * @param <V>         the value type
     * @return a new, bounded cache region
     * @throws IllegalArgumentException if {@code maximumSize} is less than 1
     */
    public static <K, V> CaffeineCacheRegion<K, V> maximumSize(final long maximumSize) {
        if (maximumSize < 1) {
            throw new IllegalArgumentException("maximumSize must be at least 1, was: " + maximumSize);
        }
        return new CaffeineCacheRegion<>(Caffeine.newBuilder().maximumSize(maximumSize).build());
    }

    // --- CacheRegion ---

    /**
     * {@inheritDoc}
     *
     * @return {@code Optional.empty()} on a cache miss; otherwise the cached {@link CacheEntry}
     */
    @Override
    public Optional<CacheEntry<V>> get(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(cache.getIfPresent(key));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses {@link Cache#get(Object, java.util.function.Function)} for atomic loading.
     * Caffeine guarantees that the loader is called at most once per key under concurrent access.</p>
     */
    @Override
    public CacheEntry<V> getOrLoad(final K key, final Supplier<? extends CacheEntry<V>> loader) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(loader, "loader must not be null");
        return cache.get(key, ignored -> {
            final CacheEntry<V> entry = loader.get();
            return Objects.requireNonNull(entry, "loader must not return null");
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void put(final K key, final CacheEntry<V> entry) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(entry, "entry must not be null");
        cache.put(key, entry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void evict(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        cache.invalidate(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear() {
        cache.invalidateAll();
    }
}
