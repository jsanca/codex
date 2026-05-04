package codex.fundamentum.api.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NoOpCacheRegion}.
 */
class NoOpCacheRegionTest {

    private NoOpCacheRegion<String, String> region;

    @BeforeEach
    void setUp() {
        region = new NoOpCacheRegion<>();
    }

    @Test
    void getReturnsEmpty() {
        assertTrue(region.get("any-key").isEmpty());
    }

    @Test
    void getOrLoadCallsLoaderEveryTime() {
        final AtomicInteger callCount = new AtomicInteger(0);
        region.getOrLoad("key", () -> { callCount.incrementAndGet(); return CacheEntry.found("v"); });
        region.getOrLoad("key", () -> { callCount.incrementAndGet(); return CacheEntry.found("v"); });
        assertEquals(2, callCount.get());
    }

    @Test
    void getOrLoadReturnsLoaderResult() {
        final CacheEntry<String> result = region.getOrLoad("key", () -> CacheEntry.found("value"));
        assertTrue(result.isFound());
        assertEquals("value", ((CacheEntry.Found<String>) result).value());
    }

    @Test
    void getOrLoadReturnsNotFoundFromLoader() {
        final CacheEntry<String> result = region.getOrLoad("key", CacheEntry::notFound);
        assertTrue(result.isNotFound());
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

    @Test
    void putRejectsNullKey() {
        assertThrows(NullPointerException.class,
                () -> region.put(null, CacheEntry.found("v")));
    }

    @Test
    void putRejectsNullEntry() {
        assertThrows(NullPointerException.class, () -> region.put("key", null));
    }

    @Test
    void evictRejectsNullKey() {
        assertThrows(NullPointerException.class, () -> region.evict(null));
    }

    @Test
    void clearDoesNotFail() {
        assertDoesNotThrow(() -> region.clear());
    }
}
