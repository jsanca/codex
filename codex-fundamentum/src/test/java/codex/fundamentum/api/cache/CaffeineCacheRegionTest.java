package codex.fundamentum.api.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CaffeineCacheRegion}.
 */
class CaffeineCacheRegionTest {

    private CaffeineCacheRegion<String, String> region;

    @BeforeEach
    void setUp() {
        region = CaffeineCacheRegion.unbounded();
    }

    // --- constructor / factory ---

    @Test
    void constructorRejectsNullCache() {
        assertThrows(NullPointerException.class, () -> new CaffeineCacheRegion<>(null));
    }

    @Test
    void unboundedCreatesUsableCache() {
        final CaffeineCacheRegion<String, String> r = CaffeineCacheRegion.unbounded();
        assertNotNull(r);
        r.put("k", CacheEntry.found("v"));
        assertTrue(r.get("k").isPresent());
    }

    @Test
    void maximumSizeOneCreatesUsableCache() {
        final CaffeineCacheRegion<String, String> r = CaffeineCacheRegion.maximumSize(1);
        assertNotNull(r);
        r.put("k", CacheEntry.found("v"));
        assertTrue(r.get("k").isPresent());
    }

    @Test
    void maximumSizeZeroRejects() {
        assertThrows(IllegalArgumentException.class, () -> CaffeineCacheRegion.maximumSize(0));
    }

    @Test
    void maximumSizeNegativeRejects() {
        assertThrows(IllegalArgumentException.class, () -> CaffeineCacheRegion.maximumSize(-1));
    }

    // --- get ---

    @Test
    void getReturnsEmptyWhenKeyMissing() {
        assertTrue(region.get("missing").isEmpty());
    }

    @Test
    void getRejectsNullKey() {
        assertThrows(NullPointerException.class, () -> region.get(null));
    }

    // --- put / get ---

    @Test
    void putThenGetReturnsFoundEntry() {
        region.put("key", CacheEntry.found("value"));
        final Optional<CacheEntry<String>> result = region.get("key");
        assertTrue(result.isPresent());
        assertTrue(result.get().isFound());
        assertEquals("value", ((CacheEntry.Found<String>) result.get()).value());
    }

    @Test
    void putSupportsNotFound() {
        region.put("key", CacheEntry.notFound());
        final Optional<CacheEntry<String>> result = region.get("key");
        assertTrue(result.isPresent());
        assertTrue(result.get().isNotFound());
    }

    @Test
    void putRejectsNullKey() {
        assertThrows(NullPointerException.class,
                () -> region.put(null, CacheEntry.found("v")));
    }

    @Test
    void putRejectsNullEntry() {
        assertThrows(NullPointerException.class, () -> region.put("key", null));
    }

    // --- evict ---

    @Test
    void evictRemovesEntry() {
        region.put("key", CacheEntry.found("value"));
        region.evict("key");
        assertTrue(region.get("key").isEmpty());
    }

    @Test
    void evictRejectsNullKey() {
        assertThrows(NullPointerException.class, () -> region.evict(null));
    }

    // --- clear ---

    @Test
    void clearRemovesAllEntries() {
        region.put("a", CacheEntry.found("1"));
        region.put("b", CacheEntry.found("2"));
        region.clear();
        assertTrue(region.get("a").isEmpty());
        assertTrue(region.get("b").isEmpty());
    }

    // --- getOrLoad ---

    @Test
    void getOrLoadLoadsAndStoresFoundEntry() {
        final CacheEntry<String> entry = region.getOrLoad("key", () -> CacheEntry.found("loaded"));
        assertTrue(entry.isFound());
        assertTrue(region.get("key").isPresent());
    }

    @Test
    void getOrLoadLoadsAndStoresNotFoundEntry() {
        final CacheEntry<String> entry = region.getOrLoad("key", CacheEntry::notFound);
        assertTrue(entry.isNotFound());
        final Optional<CacheEntry<String>> cached = region.get("key");
        assertTrue(cached.isPresent());
        assertTrue(cached.get().isNotFound());
    }

    @Test
    void getOrLoadDoesNotCallLoaderAgainWhenFoundEntryExists() {
        region.put("key", CacheEntry.found("existing"));
        final AtomicInteger calls = new AtomicInteger(0);
        region.getOrLoad("key", () -> { calls.incrementAndGet(); return CacheEntry.found("new"); });
        assertEquals(0, calls.get());
    }

    @Test
    void getOrLoadDoesNotCallLoaderAgainWhenNotFoundEntryExists() {
        region.put("key", CacheEntry.notFound());
        final AtomicInteger calls = new AtomicInteger(0);
        region.getOrLoad("key", () -> { calls.incrementAndGet(); return CacheEntry.found("new"); });
        assertEquals(0, calls.get());
    }

    @Test
    void getOrLoadRejectsNullKey() {
        assertThrows(NullPointerException.class,
                () -> region.getOrLoad(null, () -> CacheEntry.found("v")));
    }

    @Test
    void getOrLoadRejectsNullLoader() {
        assertThrows(NullPointerException.class, () -> region.getOrLoad("key", null));
    }

    @Test
    void getOrLoadRejectsNullLoaderResult() {
        assertThrows(NullPointerException.class, () -> region.getOrLoad("key", () -> null));
    }

    // --- negative cache behavior ---

    @Test
    void getOrLoadStoresNotFoundThenSubsequentGetReturnsNotFound() {
        region.getOrLoad("key", CacheEntry::notFound);
        final Optional<CacheEntry<String>> result = region.get("key");
        assertTrue(result.isPresent());
        assertTrue(result.get().isNotFound());
    }

    @Test
    void afterEvictingNotFoundNextGetReturnsMiss() {
        region.getOrLoad("key", CacheEntry::notFound);
        region.evict("key");
        assertTrue(region.get("key").isEmpty());
    }

    // --- bounded cache ---

    @Test
    void maximumSizeCacheWorksForBasicPutAndGet() {
        final CaffeineCacheRegion<String, String> bounded = CaffeineCacheRegion.maximumSize(10);
        bounded.put("k", CacheEntry.found("v"));
        assertTrue(bounded.get("k").isPresent());
    }

    // --- constructor with explicit Caffeine cache ---

    @Test
    void constructorWithExplicitCacheWorks() {
        final CaffeineCacheRegion<String, String> r =
                new CaffeineCacheRegion<>(Caffeine.newBuilder().maximumSize(5).build());
        r.put("k", CacheEntry.found("v"));
        assertTrue(r.get("k").isPresent());
    }

    // --- concurrency ---

    @Test
    void concurrentGetOrLoadCallsLoaderAtMostOnce() throws InterruptedException {
        final int threadCount = 20;
        final AtomicInteger loaderCallCount = new AtomicInteger(0);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final List<CacheEntry<String>> results = new ArrayList<>(threadCount);
        for (int i = 0; i < threadCount; i++) {
            results.add(null);
        }

        try (final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            final List<java.util.concurrent.Future<?>> futures = new ArrayList<>(threadCount);
            for (int index = 0; index < threadCount; index++) {
                final int idx = index;
                futures.add(executor.submit(() -> {
                    try {
                        startLatch.await();
                    } catch (final InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                    results.set(idx, region.getOrLoad("shared-key", () -> {
                        loaderCallCount.incrementAndGet();
                        return CacheEntry.found("computed");
                    }));
                }));
            }
            startLatch.countDown();
            for (final var future : futures) {
                try { future.get(); } catch (final Exception ex) { fail(ex); }
            }
        }

        assertTrue(loaderCallCount.get() >= 1, "loader must be called at least once");
        assertEquals(1, loaderCallCount.get(), "loader should be called exactly once");
        results.forEach(entry -> assertTrue(entry.isFound()));
    }
}
