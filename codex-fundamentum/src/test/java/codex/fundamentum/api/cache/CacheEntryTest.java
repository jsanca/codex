package codex.fundamentum.api.cache;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CacheEntry}.
 */
class CacheEntryTest {

    // --- Found ---

    @Test
    void foundRejectsNullValue() {
        assertThrows(NullPointerException.class, () -> CacheEntry.found(null));
    }

    @Test
    void foundStoresValue() {
        final CacheEntry<String> entry = CacheEntry.found("hello");
        assertInstanceOf(CacheEntry.Found.class, entry);
        assertEquals("hello", ((CacheEntry.Found<String>) entry).value());
    }

    @Test
    void foundIsFound() {
        final CacheEntry<String> entry = CacheEntry.found("x");
        assertTrue(entry.isFound());
        assertFalse(entry.isNotFound());
    }

    @Test
    void foundFactoryEqualsDirectConstruction() {
        assertEquals(new CacheEntry.Found<>("abc"), CacheEntry.found("abc"));
    }

    // --- NotFound ---

    @Test
    void notFoundCanBeCreated() {
        final CacheEntry<String> entry = CacheEntry.notFound();
        assertNotNull(entry);
        assertInstanceOf(CacheEntry.NotFound.class, entry);
    }

    @Test
    void notFoundIsNotFound() {
        final CacheEntry<String> entry = CacheEntry.notFound();
        assertTrue(entry.isNotFound());
        assertFalse(entry.isFound());
    }

    @Test
    void notFoundFactoryEqualsDirectConstruction() {
        assertEquals(new CacheEntry.NotFound<>(), CacheEntry.notFound());
    }

    @Test
    void twoNotFoundInstancesAreEqual() {
        assertEquals(CacheEntry.notFound(), CacheEntry.notFound());
    }
}
