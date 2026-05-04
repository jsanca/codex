package codex.index.api;

import codex.codex.api.model.identity.SiteKey;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link IndexDocument}.
 */
class IndexDocumentTest {

    private static final IndexDocumentId ID = IndexDocumentId.of("content-item:acme:blog-post:my-post");
    private static final IndexResourceType TYPE = IndexResourceType.CONTENT_ITEM;
    private static final SiteKey SITE = SiteKey.of("acme");

    private IndexDocument.Builder minimal() {
        return IndexDocument.builder()
                .id(ID)
                .resourceType(TYPE)
                .siteKey(SITE);
    }

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class, () -> minimal().id(null).build());
    }

    @Test
    void rejectsNullResourceType() {
        assertThrows(NullPointerException.class, () -> minimal().resourceType(null).build());
    }

    @Test
    void rejectsNullSiteKey() {
        assertThrows(NullPointerException.class, () -> minimal().siteKey(null).build());
    }

    @Test
    void defaultsNullTitleToEmptyString() {
        assertEquals("", minimal().title(null).build().title());
    }

    @Test
    void defaultsNullBodyToEmptyString() {
        assertEquals("", minimal().body(null).build().body());
    }

    @Test
    void trimsTitle() {
        assertEquals("Hello World", minimal().title("  Hello World  ").build().title());
    }

    @Test
    void trimsBody() {
        assertEquals("Full text", minimal().body("  Full text  ").build().body());
    }

    @Test
    void defaultsNullFieldsToEmptyMap() {
        assertTrue(minimal().fields(null).build().fields().isEmpty());
    }

    @Test
    void defaultsNullMetadataToEmptyMap() {
        assertTrue(minimal().metadata(null).build().metadata().isEmpty());
    }

    @Test
    void defaultsUpdatedAtWhenNull() {
        assertNotNull(minimal().updatedAt(null).build().updatedAt());
    }

    @Test
    void defensivelyCopiesFields() {
        final Map<String, Object> mutable = new HashMap<>();
        mutable.put("title", "Hello");
        final IndexDocument doc = minimal().fields(mutable).build();
        mutable.put("extra", "mutation");
        assertEquals(1, doc.fields().size());
    }

    @Test
    void defensivelyCopiesMetadata() {
        final Map<String, String> mutable = new HashMap<>();
        mutable.put("contentTypeKey", "blog-post");
        final IndexDocument doc = minimal().metadata(mutable).build();
        mutable.put("extra", "mutation");
        assertEquals(1, doc.metadata().size());
    }

    @Test
    void fieldsAccessorIsImmutable() {
        final IndexDocument doc = minimal().fields(Map.of("k", "v")).build();
        assertThrows(UnsupportedOperationException.class, () -> doc.fields().put("x", "y"));
    }

    @Test
    void metadataAccessorIsImmutable() {
        final IndexDocument doc = minimal().metadata(Map.of("k", "v")).build();
        assertThrows(UnsupportedOperationException.class, () -> doc.metadata().put("x", "y"));
    }

    @Test
    void rejectsNullFieldKey() {
        final Map<String, Object> bad = new HashMap<>();
        bad.put(null, "value");
        assertThrows(NullPointerException.class, () -> minimal().fields(bad).build());
    }

    @Test
    void rejectsNullFieldValue() {
        final Map<String, Object> bad = new HashMap<>();
        bad.put("key", null);
        assertThrows(NullPointerException.class, () -> minimal().fields(bad).build());
    }

    @Test
    void rejectsNullMetadataKey() {
        final Map<String, String> bad = new HashMap<>();
        bad.put(null, "value");
        assertThrows(NullPointerException.class, () -> minimal().metadata(bad).build());
    }

    @Test
    void rejectsNullMetadataValue() {
        final Map<String, String> bad = new HashMap<>();
        bad.put("key", null);
        assertThrows(NullPointerException.class, () -> minimal().metadata(bad).build());
    }

    @Test
    void builderSupportsAllFields() {
        final Instant now = Instant.parse("2026-05-01T00:00:00Z");
        final IndexDocument doc = IndexDocument.builder()
                .id(ID).resourceType(TYPE).siteKey(SITE)
                .title("Hello World").body("Full searchable text")
                .fields(Map.of("published", true))
                .metadata(Map.of("contentTypeKey", "blog-post"))
                .updatedAt(now)
                .build();
        assertEquals(ID, doc.id());
        assertEquals(TYPE, doc.resourceType());
        assertEquals(SITE, doc.siteKey());
        assertEquals("Hello World", doc.title());
        assertEquals("Full searchable text", doc.body());
        assertEquals(1, doc.fields().size());
        assertEquals(1, doc.metadata().size());
        assertEquals(now, doc.updatedAt());
    }

    @Test
    void copyOfPreservesAllFields() {
        final Instant now = Instant.parse("2026-05-01T00:00:00Z");
        final IndexDocument original = IndexDocument.builder()
                .id(ID).resourceType(TYPE).siteKey(SITE)
                .title("Original").body("Body text")
                .fields(Map.of("x", 1)).metadata(Map.of("k", "v"))
                .updatedAt(now).build();
        final IndexDocument copy = IndexDocument.copyOf(original).build();
        assertEquals(original.id(), copy.id());
        assertEquals(original.title(), copy.title());
        assertEquals(original.fields(), copy.fields());
        assertEquals(original.updatedAt(), copy.updatedAt());
    }

    @Test
    void toStringDoesNotDumpFullBody() {
        final IndexDocument doc = minimal()
                .body("This is a very long body text that should not appear in toString")
                .build();
        assertFalse(doc.toString().contains("This is a very long body text"));
    }

    @Test
    void toStringIncludesId() {
        assertTrue(minimal().build().toString().contains(ID.value()));
    }
}
