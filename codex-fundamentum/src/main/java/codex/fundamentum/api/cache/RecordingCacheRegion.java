package codex.fundamentum.api.cache;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * A {@link CacheRegion} that records all cache operations, delegating actual storage to a
 * {@link ConcurrentMapCacheRegion}.
 *
 * <p>Intended for use in tests and integration assertions. Call {@link #getKeys()},
 * {@link #loadKeys()}, {@link #putKeys()}, {@link #evictKeys()}, and {@link #clearCount()} to
 * inspect which operations were performed. Call {@link #clearRecording()} to reset the recorded
 * operations without disturbing the cached entries.
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public final class RecordingCacheRegion<K, V> implements CacheRegion<K, V> {

    private final ConcurrentMapCacheRegion<K, V> delegate = new ConcurrentMapCacheRegion<>();

    private final List<K> recordedGetKeys = Collections.synchronizedList(new ArrayList<>());
    private final List<K> recordedLoadKeys = Collections.synchronizedList(new ArrayList<>());
    private final List<K> recordedPutKeys = Collections.synchronizedList(new ArrayList<>());
    private final List<K> recordedEvictKeys = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger recordedClearCount = new AtomicInteger(0);

    @Override
    public Optional<CacheEntry<V>> get(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        recordedGetKeys.add(key);
        return delegate.get(key);
    }

    @Override
    public CacheEntry<V> getOrLoad(final K key, final Supplier<? extends CacheEntry<V>> loader) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(loader, "loader must not be null");
        recordedLoadKeys.add(key);
        return delegate.getOrLoad(key, loader);
    }

    @Override
    public void put(final K key, final CacheEntry<V> entry) {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(entry, "entry must not be null");
        recordedPutKeys.add(key);
        delegate.put(key, entry);
    }

    @Override
    public void evict(final K key) {
        Objects.requireNonNull(key, "key must not be null");
        recordedEvictKeys.add(key);
        delegate.evict(key);
    }

    @Override
    public void clear() {
        recordedClearCount.incrementAndGet();
        delegate.clear();
    }

    /**
     * Returns an immutable snapshot of all keys passed to {@link #get(Object)}, in insertion order.
     */
    public List<K> getKeys() {
        synchronized (recordedGetKeys) {
            return List.copyOf(recordedGetKeys);
        }
    }

    /**
     * Returns an immutable snapshot of all keys passed to {@link #getOrLoad(Object, Supplier)},
     * in insertion order.
     */
    public List<K> loadKeys() {
        synchronized (recordedLoadKeys) {
            return List.copyOf(recordedLoadKeys);
        }
    }

    /**
     * Returns an immutable snapshot of all keys passed to {@link #put(Object, CacheEntry)},
     * in insertion order.
     */
    public List<K> putKeys() {
        synchronized (recordedPutKeys) {
            return List.copyOf(recordedPutKeys);
        }
    }

    /**
     * Returns an immutable snapshot of all keys passed to {@link #evict(Object)},
     * in insertion order.
     */
    public List<K> evictKeys() {
        synchronized (recordedEvictKeys) {
            return List.copyOf(recordedEvictKeys);
        }
    }

    /**
     * Returns the number of times {@link #clear()} has been called.
     */
    public int clearCount() {
        return recordedClearCount.get();
    }

    /**
     * Clears all recorded operation lists and resets the clear counter, without disturbing cached
     * entries.
     */
    public void clearRecording() {
        synchronized (recordedGetKeys) { recordedGetKeys.clear(); }
        synchronized (recordedLoadKeys) { recordedLoadKeys.clear(); }
        synchronized (recordedPutKeys) { recordedPutKeys.clear(); }
        synchronized (recordedEvictKeys) { recordedEvictKeys.clear(); }
        recordedClearCount.set(0);
    }
}
