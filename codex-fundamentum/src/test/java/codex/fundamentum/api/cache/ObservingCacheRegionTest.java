package codex.fundamentum.api.cache;

import codex.fundamentum.api.observance.InMemoryObservance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ObservingCacheRegion}.
 */
class ObservingCacheRegionTest {

    private static final String REGION = "test";

    private ConcurrentMapCacheRegion<String, String> delegate;
    private InMemoryObservance observance;
    private ObservingCacheRegion<String, String> region;

    @BeforeEach
    void setUp() {
        delegate = new ConcurrentMapCacheRegion<>();
        observance = new InMemoryObservance();
        region = new ObservingCacheRegion<>(delegate, REGION, observance);
    }

    // --- constructor ---

    @Test
    void constructorRejectsNullDelegate() {
        assertThrows(NullPointerException.class,
                () -> new ObservingCacheRegion<>(null, REGION, observance));
    }

    @Test
    void constructorRejectsNullRegionName() {
        assertThrows(NullPointerException.class,
                () -> new ObservingCacheRegion<>(delegate, null, observance));
    }

    @Test
    void constructorRejectsNullObservance() {
        assertThrows(NullPointerException.class,
                () -> new ObservingCacheRegion<>(delegate, REGION, null));
    }

    @Test
    void constructorRejectsEmptyRegionName() {
        assertThrows(IllegalArgumentException.class,
                () -> new ObservingCacheRegion<>(delegate, "", observance));
    }

    @Test
    void constructorRejectsBlankRegionName() {
        assertThrows(IllegalArgumentException.class,
                () -> new ObservingCacheRegion<>(delegate, "   ", observance));
    }

    // --- get: miss ---

    @Test
    void getMissIncreasesMissCounter() {
        region.get("missing");

        assertEquals(1L, observance.counterValue("cache.test.get.miss"));
        assertEquals(0L, observance.counterValue("cache.test.get.hit"));
    }

    @Test
    void getMissReturnsEmpty() {
        assertTrue(region.get("missing").isEmpty());
    }

    @Test
    void getMissDoesNotIncrementHit() {
        region.get("missing");

        assertEquals(0L, observance.counterValue("cache.test.get.hit"));
    }

    // --- get: hit ---

    @Test
    void getHitIncreasesHitCounter() {
        delegate.put("key", CacheEntry.found("value"));

        region.get("key");

        assertEquals(1L, observance.counterValue("cache.test.get.hit"));
        assertEquals(0L, observance.counterValue("cache.test.get.miss"));
    }

    @Test
    void getHitReturnsEntry() {
        delegate.put("key", CacheEntry.found("value"));

        final Optional<CacheEntry<String>> result = region.get("key");

        assertTrue(result.isPresent());
        assertTrue(result.get().isFound());
    }

    @Test
    void getHitWorksForNotFoundEntry() {
        delegate.put("key", CacheEntry.notFound());

        region.get("key");

        assertEquals(1L, observance.counterValue("cache.test.get.hit"));
    }

    @Test
    void multipleMissesCumulate() {
        region.get("a");
        region.get("b");
        region.get("c");

        assertEquals(3L, observance.counterValue("cache.test.get.miss"));
    }

    @Test
    void mixedGetHitsAndMisses() {
        delegate.put("present", CacheEntry.found("v"));

        region.get("missing");
        region.get("present");
        region.get("also-missing");

        assertEquals(2L, observance.counterValue("cache.test.get.miss"));
        assertEquals(1L, observance.counterValue("cache.test.get.hit"));
    }

    // --- getOrLoad: miss (loader invoked) ---

    @Test
    void getOrLoadMissIncreasesMissCounter() {
        region.getOrLoad("key", () -> CacheEntry.found("loaded"));

        assertEquals(1L, observance.counterValue("cache.test.getOrLoad.miss"));
        assertEquals(0L, observance.counterValue("cache.test.getOrLoad.hit"));
    }

    @Test
    void getOrLoadMissReturnsLoadedEntry() {
        final CacheEntry<String> result =
                region.getOrLoad("key", () -> CacheEntry.found("loaded"));

        assertTrue(result.isFound());
        assertEquals("loaded", ((CacheEntry.Found<String>) result).value());
    }

    @Test
    void getOrLoadMissInvokesLoaderExactlyOnce() {
        final AtomicInteger calls = new AtomicInteger(0);

        region.getOrLoad("key", () -> { calls.incrementAndGet(); return CacheEntry.found("v"); });

        assertEquals(1, calls.get());
    }

    @Test
    void getOrLoadMissWorksForNotFoundEntry() {
        region.getOrLoad("key", CacheEntry::notFound);

        assertEquals(1L, observance.counterValue("cache.test.getOrLoad.miss"));
    }

    @Test
    void getOrLoadDoesNotIncrementCounterWhenLoaderThrows() {
        assertThrows(RuntimeException.class, () ->
                region.getOrLoad("key", () -> { throw new RuntimeException("loader failed"); }));

        assertEquals(0L, observance.counterValue("cache.test.getOrLoad.miss"));
        assertEquals(0L, observance.counterValue("cache.test.getOrLoad.hit"));
    }

    @Test
    void getOrLoadExceptionPropagatesUnchanged() {
        final RuntimeException cause = new RuntimeException("loader failed");
        final RuntimeException thrown = assertThrows(RuntimeException.class, () ->
                region.getOrLoad("key", () -> { throw cause; }));

        assertSame(cause, thrown);
    }

    // --- getOrLoad: hit (loader not invoked) ---

    @Test
    void getOrLoadHitIncreasesHitCounter() {
        delegate.put("key", CacheEntry.found("existing"));

        region.getOrLoad("key", () -> CacheEntry.found("should-not-be-called"));

        assertEquals(1L, observance.counterValue("cache.test.getOrLoad.hit"));
        assertEquals(0L, observance.counterValue("cache.test.getOrLoad.miss"));
    }

    @Test
    void getOrLoadHitReturnsCachedEntry() {
        delegate.put("key", CacheEntry.found("existing"));

        final CacheEntry<String> result =
                region.getOrLoad("key", () -> CacheEntry.found("should-not-be-called"));

        assertTrue(result.isFound());
        assertEquals("existing", ((CacheEntry.Found<String>) result).value());
    }

    @Test
    void getOrLoadHitDoesNotInvokeLoader() {
        delegate.put("key", CacheEntry.found("existing"));
        final AtomicInteger calls = new AtomicInteger(0);

        region.getOrLoad("key", () -> { calls.incrementAndGet(); return CacheEntry.found("new"); });

        assertEquals(0, calls.get());
    }

    @Test
    void secondGetOrLoadCallIsHit() {
        region.getOrLoad("key", () -> CacheEntry.found("first-load"));

        region.getOrLoad("key", () -> CacheEntry.found("second-load"));

        assertEquals(1L, observance.counterValue("cache.test.getOrLoad.miss"));
        assertEquals(1L, observance.counterValue("cache.test.getOrLoad.hit"));
    }

    // --- put ---

    @Test
    void putIncreasesPutCounter() {
        region.put("key", CacheEntry.found("value"));

        assertEquals(1L, observance.counterValue("cache.test.put"));
    }

    @Test
    void putStoresEntryInDelegate() {
        region.put("key", CacheEntry.found("value"));

        assertTrue(delegate.get("key").isPresent());
    }

    @Test
    void multiplePutsCumulate() {
        region.put("a", CacheEntry.found("1"));
        region.put("b", CacheEntry.found("2"));

        assertEquals(2L, observance.counterValue("cache.test.put"));
    }

    // --- evict ---

    @Test
    void evictIncreasesEvictCounter() {
        delegate.put("key", CacheEntry.found("value"));

        region.evict("key");

        assertEquals(1L, observance.counterValue("cache.test.evict"));
    }

    @Test
    void evictRemovesEntryFromDelegate() {
        delegate.put("key", CacheEntry.found("value"));

        region.evict("key");

        assertTrue(delegate.get("key").isEmpty());
    }

    @Test
    void evictCountsEvenIfKeyAbsent() {
        region.evict("nonexistent");

        assertEquals(1L, observance.counterValue("cache.test.evict"));
    }

    // --- clear ---

    @Test
    void clearIncreaseslearCounter() {
        delegate.put("a", CacheEntry.found("1"));

        region.clear();

        assertEquals(1L, observance.counterValue("cache.test.clear"));
    }

    @Test
    void clearRemovesAllEntriesFromDelegate() {
        delegate.put("a", CacheEntry.found("1"));
        delegate.put("b", CacheEntry.found("2"));

        region.clear();

        assertTrue(delegate.get("a").isEmpty());
        assertTrue(delegate.get("b").isEmpty());
    }

    @Test
    void multipleClearsCumulate() {
        region.clear();
        region.clear();

        assertEquals(2L, observance.counterValue("cache.test.clear"));
    }

    // --- null guards ---

    @Test
    void getNullKeyRejected() {
        assertThrows(NullPointerException.class, () -> region.get(null));
    }

    @Test
    void getOrLoadNullKeyRejected() {
        assertThrows(NullPointerException.class,
                () -> region.getOrLoad(null, () -> CacheEntry.found("v")));
    }

    @Test
    void getOrLoadNullLoaderRejected() {
        assertThrows(NullPointerException.class, () -> region.getOrLoad("key", null));
    }

    @Test
    void putNullKeyRejected() {
        assertThrows(NullPointerException.class,
                () -> region.put(null, CacheEntry.found("v")));
    }

    @Test
    void putNullEntryRejected() {
        assertThrows(NullPointerException.class, () -> region.put("key", null));
    }

    @Test
    void evictNullKeyRejected() {
        assertThrows(NullPointerException.class, () -> region.evict(null));
    }

    // --- counter isolation: regions do not cross-contaminate ---

    @Test
    void countersAreScopedToRegionName() {
        final ObservingCacheRegion<String, String> other =
                new ObservingCacheRegion<>(new ConcurrentMapCacheRegion<>(), "other", observance);

        region.get("a");
        other.get("b");

        assertEquals(1L, observance.counterValue("cache.test.get.miss"));
        assertEquals(1L, observance.counterValue("cache.other.get.miss"));
    }

    // --- behavior: return values preserved ---

    @Test
    void getReturnsSameResultAsDelegate() {
        delegate.put("key", CacheEntry.found("direct"));

        assertEquals(delegate.get("key"), region.get("key"));
    }
}
