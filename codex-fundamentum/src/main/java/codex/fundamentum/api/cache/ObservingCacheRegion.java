package codex.fundamentum.api.cache;

import codex.fundamentum.api.observance.Observance;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * A {@link CacheRegion} decorator that records operation counters via {@link Observance}.
 *
 * <p>Counters recorded for a region named {@code {region}}:</p>
 * <ul>
 *   <li>{@code cache.{region}.get.hit} — {@link #get} found a cached entry</li>
 *   <li>{@code cache.{region}.get.miss} — {@link #get} found no cached entry</li>
 *   <li>{@code cache.{region}.getOrLoad.hit} — {@link #getOrLoad} returned a cached entry
 *       without invoking the loader</li>
 *   <li>{@code cache.{region}.getOrLoad.miss} — {@link #getOrLoad} invoked the loader</li>
 *   <li>{@code cache.{region}.put} — incremented on each {@link #put}</li>
 *   <li>{@code cache.{region}.evict} — incremented on each {@link #evict}</li>
 *   <li>{@code cache.{region}.clear} — incremented on each {@link #clear}</li>
 * </ul>
 *
 * <p>All cache semantics, return values, and thread-safety guarantees of the delegate are
 * preserved. Counters are only incremented after the delegate operation succeeds; a delegate
 * that throws will not increment its counter.</p>
 *
 * <p>The {@code regionName} must be a stable, low-cardinality string — never a cache key, id,
 * or any value derived from user input.</p>
 *
 * @param <K> the key type
 * @param <V> the value type
 * @author jsanca &amp; clio
 */
public final class ObservingCacheRegion<K, V> implements CacheRegion<K, V> {

    private final CacheRegion<K, V> delegate;
    private final Observance observance;
    private final String getHitKey;
    private final String getMissKey;
    private final String getOrLoadHitKey;
    private final String getOrLoadMissKey;
    private final String putKey;
    private final String evictKey;
    private final String clearKey;

    /**
     * Creates an observing cache region wrapping the given delegate.
     *
     * @param delegate   the cache region to delegate all operations to; must not be null
     * @param regionName a stable, low-cardinality name used to scope metric names
     *                   (e.g. {@code "contentItem"}); must not be null or blank
     * @param observance the observance for counter recording; must not be null
     */
    public ObservingCacheRegion(
            final CacheRegion<K, V> delegate,
            final String regionName,
            final Observance observance) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        Objects.requireNonNull(regionName, "regionName must not be null");
        if (regionName.isBlank()) {
            throw new IllegalArgumentException("regionName must not be blank");
        }
        this.observance = Objects.requireNonNull(observance, "observance must not be null");

        final String prefix = "cache." + regionName + ".";
        this.getHitKey       = prefix + "get.hit";
        this.getMissKey      = prefix + "get.miss";
        this.getOrLoadHitKey = prefix + "getOrLoad.hit";
        this.getOrLoadMissKey = prefix + "getOrLoad.miss";
        this.putKey          = prefix + "put";
        this.evictKey        = prefix + "evict";
        this.clearKey        = prefix + "clear";
    }

    /**
     * {@inheritDoc}
     *
     * <p>Increments {@code cache.{region}.get.hit} on a cache hit,
     * {@code cache.{region}.get.miss} on a miss.</p>
     */
    @Override
    public Optional<CacheEntry<V>> get(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        final Optional<CacheEntry<V>> result = delegate.get(key);
        if (result.isPresent()) {
            observance.counter(getHitKey).increment();
        } else {
            observance.counter(getMissKey).increment();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Increments {@code cache.{region}.getOrLoad.miss} when the loader is invoked (cache miss),
     * {@code cache.{region}.getOrLoad.hit} when a cached entry is returned without loading.</p>
     */
    @Override
    public CacheEntry<V> getOrLoad(final K key, final Supplier<? extends CacheEntry<V>> loader) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(loader, "loader must not be null");

        final AtomicBoolean loaderInvoked = new AtomicBoolean(false);
        final CacheEntry<V> result = delegate.getOrLoad(key, () -> {
            loaderInvoked.set(true);
            return loader.get();
        });

        if (loaderInvoked.get()) {
            observance.counter(getOrLoadMissKey).increment();
        } else {
            observance.counter(getOrLoadHitKey).increment();
        }
        return result;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Increments {@code cache.{region}.put} after storing the entry.</p>
     */
    @Override
    public void put(final K key, final CacheEntry<V> entry) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(entry, "entry must not be null");
        delegate.put(key, entry);
        observance.counter(putKey).increment();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Increments {@code cache.{region}.evict} after removing the entry.</p>
     */
    @Override
    public void evict(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        delegate.evict(key);
        observance.counter(evictKey).increment();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Increments {@code cache.{region}.clear} after clearing all entries.</p>
     */
    @Override
    public void clear() {
        delegate.clear();
        observance.counter(clearKey).increment();
    }
}
