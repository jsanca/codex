package codex.fundamentum.api.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RecordingCacheRegion}.
 */
class RecordingCacheRegionTest {

    private RecordingCacheRegion<String, String> region;

    @BeforeEach
    void setUp() {
        region = new RecordingCacheRegion<>();
    }

    // --- recording ---

    @Test
    void recordsGetKeys() {
        region.get("a");
        region.get("b");
        assertEquals(2, region.getKeys().size());
        assertEquals("a", region.getKeys().get(0));
        assertEquals("b", region.getKeys().get(1));
    }

    @Test
    void recordsGetOrLoadKeys() {
        region.getOrLoad("x", () -> CacheEntry.found("v"));
        region.getOrLoad("y", () -> CacheEntry.found("v"));
        assertEquals(2, region.loadKeys().size());
        assertEquals("x", region.loadKeys().get(0));
        assertEquals("y", region.loadKeys().get(1));
    }

    @Test
    void recordsPutKeys() {
        region.put("p", CacheEntry.found("1"));
        region.put("q", CacheEntry.found("2"));
        assertEquals(2, region.putKeys().size());
        assertEquals("p", region.putKeys().get(0));
        assertEquals("q", region.putKeys().get(1));
    }

    @Test
    void recordsEvictKeys() {
        region.evict("e1");
        region.evict("e2");
        assertEquals(2, region.evictKeys().size());
        assertEquals("e1", region.evictKeys().get(0));
        assertEquals("e2", region.evictKeys().get(1));
    }

    @Test
    void recordsClearCount() {
        assertEquals(0, region.clearCount());
        region.clear();
        region.clear();
        assertEquals(2, region.clearCount());
    }

    @Test
    void snapshotsAreImmutable() {
        region.get("key");
        assertThrows(UnsupportedOperationException.class, () -> region.getKeys().add("extra"));
        assertThrows(UnsupportedOperationException.class, () -> region.loadKeys().add("extra"));
        assertThrows(UnsupportedOperationException.class, () -> region.putKeys().add("extra"));
        assertThrows(UnsupportedOperationException.class, () -> region.evictKeys().add("extra"));
    }

    @Test
    void clearRecordingClearsRecordedOperationsButNotCacheContents() {
        region.put("key", CacheEntry.found("value"));
        region.get("key");
        region.clearRecording();

        assertEquals(0, region.getKeys().size());
        assertEquals(0, region.putKeys().size());
        assertEquals(0, region.clearCount());

        // cached entry still present
        final Optional<CacheEntry<String>> result = region.get("key");
        assertTrue(result.isPresent());
    }

    @Test
    void clearClearsCacheContentsAndRecordsClear() {
        region.put("key", CacheEntry.found("value"));
        region.clear();

        assertEquals(1, region.clearCount());
        assertTrue(region.get("key").isEmpty());
    }

    // --- normal cache behavior ---

    @Test
    void normalCacheBehaviorWorks() {
        region.put("key", CacheEntry.found("value"));
        final Optional<CacheEntry<String>> result = region.get("key");
        assertTrue(result.isPresent());
        assertTrue(result.get().isFound());
        assertEquals("value", ((CacheEntry.Found<String>) result.get()).value());
    }

    @Test
    void getOrLoadStoresAndReturnsEntry() {
        final CacheEntry<String> entry = region.getOrLoad("key", () -> CacheEntry.found("loaded"));
        assertTrue(entry.isFound());
        assertTrue(region.get("key").isPresent());
    }

    // --- negative cache behavior ---

    @Test
    void negativeCacheBehaviorWorks() {
        region.getOrLoad("missing", CacheEntry::notFound);

        final Optional<CacheEntry<String>> result = region.get("missing");
        assertTrue(result.isPresent());
        assertTrue(result.get().isNotFound());
    }

    @Test
    void getOrLoadDoesNotCallLoaderAgainForCachedNotFound() {
        region.getOrLoad("key", CacheEntry::notFound);

        final int loadCountBefore = region.loadKeys().size();
        region.getOrLoad("key", () -> { fail("loader should not be called"); return CacheEntry.found("x"); });
        // loadKeys still records the attempt, but the loader itself is not called
        assertEquals(loadCountBefore + 1, region.loadKeys().size());
        assertTrue(region.get("key").get().isNotFound());
    }

    // --- null validation ---

    @Test
    void getRejectsNullKey() {
        assertThrows(NullPointerException.class, () -> region.get(null));
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
}
