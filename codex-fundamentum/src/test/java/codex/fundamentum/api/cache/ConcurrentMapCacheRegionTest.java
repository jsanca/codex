package codex.fundamentum.api.cache;

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
 * Tests for {@link ConcurrentMapCacheRegion}.
 */
class ConcurrentMapCacheRegionTest {

    private ConcurrentMapCacheRegion<String, String> region;

    @BeforeEach
    void setUp() {
        region = new ConcurrentMapCacheRegion<>();
    }

    // --- get ---

    @Test
    void getReturnsEmptyWhenKeyMissing() {
        assertTrue(region.get("missing").isEmpty());
    }

    @Test
    void nullKeyRejectedForGet() {
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
    void putSupportsNotFoundEntry() {
        region.put("key", CacheEntry.notFound());
        final Optional<CacheEntry<String>> result = region.get("key");
        assertTrue(result.isPresent());
        assertTrue(result.get().isNotFound());
    }

    @Test
    void nullKeyRejectedForPut() {
        assertThrows(NullPointerException.class,
                () -> region.put(null, CacheEntry.found("v")));
    }

    @Test
    void nullEntryRejectedForPut() {
        assertThrows(NullPointerException.class, () -> region.put("key", null));
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
    void nullKeyRejectedForGetOrLoad() {
        assertThrows(NullPointerException.class,
                () -> region.getOrLoad(null, () -> CacheEntry.found("v")));
    }

    @Test
    void nullLoaderRejectedForGetOrLoad() {
        assertThrows(NullPointerException.class, () -> region.getOrLoad("key", null));
    }

    @Test
    void nullLoaderResultRejectedForGetOrLoad() {
        assertThrows(NullPointerException.class, () -> region.getOrLoad("key", () -> null));
    }

    // --- evict ---

    @Test
    void evictRemovesEntry() {
        region.put("key", CacheEntry.found("value"));
        region.evict("key");
        assertTrue(region.get("key").isEmpty());
    }

    @Test
    void nullKeyRejectedForEvict() {
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
        // computeIfAbsent guarantees at most one call in ConcurrentHashMap
        assertEquals(1, loaderCallCount.get(), "loader should be called exactly once");
        results.forEach(entry -> assertTrue(entry.isFound()));
    }
}
