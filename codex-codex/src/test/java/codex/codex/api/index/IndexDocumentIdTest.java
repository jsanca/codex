package codex.codex.api.index;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IndexDocumentId}.
 */
class IndexDocumentIdTest {

    @Test
    void rejectsNullValue() {
        assertThrows(NullPointerException.class, () -> IndexDocumentId.of(null));
    }

    @Test
    void rejectsBlankValue() {
        assertThrows(IllegalArgumentException.class, () -> IndexDocumentId.of("   "));
    }

    @Test
    void trimsValue() {
        final IndexDocumentId id = IndexDocumentId.of("  content-item:acme:blog-post:my-post  ");
        assertEquals("content-item:acme:blog-post:my-post", id.value());
    }

    @Test
    void ofFactoryWorks() {
        final IndexDocumentId id = IndexDocumentId.of("site:acme");
        assertNotNull(id);
        assertEquals("site:acme", id.value());
    }

    @Test
    void equalityByValue() {
        final IndexDocumentId a = IndexDocumentId.of("content-item:acme:blog-post:my-post");
        final IndexDocumentId b = IndexDocumentId.of("content-item:acme:blog-post:my-post");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void differentValueNotEqual() {
        final IndexDocumentId a = IndexDocumentId.of("site:acme");
        final IndexDocumentId b = IndexDocumentId.of("site:beta");
        assertNotEquals(a, b);
    }

    @Test
    void toStringReturnsValue() {
        assertEquals("site:acme", IndexDocumentId.of("site:acme").toString());
    }
}
