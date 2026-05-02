package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentTypeVersionStatus;
import codex.codex.api.model.value.FieldType;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ContentTypeVersionTest {

    private final ContentTypeVersionId id = ContentTypeVersionId.generate();
    private final ContentTypeId contentTypeId = ContentTypeId.generate();
    private final SiteKey siteKey = SiteKey.of("acme");
    private final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
    private final ActorId createdBy = ActorId.of("system:test");

    // --- null checks ---

    @Test
    @DisplayName("rejects null id")
    void rejectsNullId() {
        assertThrows(NullPointerException.class, () -> builder().id(null).build());
    }

    @Test
    @DisplayName("rejects null contentTypeId")
    void rejectsNullContentTypeId() {
        assertThrows(NullPointerException.class, () -> builder().contentTypeId(null).build());
    }

    @Test
    @DisplayName("rejects null siteKey")
    void rejectsNullSiteKey() {
        assertThrows(NullPointerException.class, () -> builder().siteKey(null).build());
    }

    @Test
    @DisplayName("rejects null contentTypeKey")
    void rejectsNullContentTypeKey() {
        assertThrows(NullPointerException.class, () -> builder().contentTypeKey(null).build());
    }

    @Test
    @DisplayName("rejects null createdBy")
    void rejectsNullCreatedBy() {
        assertThrows(NullPointerException.class, () -> builder().createdBy(null).build());
    }

    // --- version number ---

    @Test
    @DisplayName("rejects version less than 1")
    void rejectsVersionLessThan1() {
        assertThrows(IllegalArgumentException.class, () -> builder().version(0).build());
    }

    @Test
    @DisplayName("accepts version 1")
    void acceptsVersion1() {
        final ContentTypeVersion v = builder().build();
        assertEquals(1, v.version());
    }

    // --- fields ---

    @Test
    @DisplayName("fields default to empty map when null")
    void fieldsDefaultToEmptyMapWhenNull() {
        final ContentTypeVersion v = builder().fields(null).build();
        assertNotNull(v.fields());
        assertTrue(v.fields().isEmpty());
    }

    @Test
    @DisplayName("fields are defensively copied")
    void fieldsAreDefensivelyCopied() {
        final FieldKey titleKey = FieldKey.of("title");
        final Field titleField = Field.builder().key(titleKey).type(FieldType.TEXT).build();
        final Map<FieldKey, Field> original = new HashMap<>();
        original.put(titleKey, titleField);

        final ContentTypeVersion v = builder().fields(original).build();
        original.put(FieldKey.of("body"), Field.builder().key("body").type(FieldType.TEXT).build());

        assertEquals(1, v.fields().size());
    }

    @Test
    @DisplayName("fields accessor is immutable")
    void fieldsAccessorIsImmutable() {
        final ContentTypeVersion v = builder().build();
        assertThrows(UnsupportedOperationException.class, () ->
                v.fields().put(FieldKey.of("x"), Field.builder().key("x").type(FieldType.TEXT).build()));
    }

    @Test
    @DisplayName("rejects fields where map key does not match field key")
    void rejectsFieldsWhereMapKeyDiffersFromFieldKey() {
        final FieldKey mapKey = FieldKey.of("title");
        final Field bodyField = Field.builder().key("body").type(FieldType.TEXT).build();
        assertThrows(IllegalArgumentException.class, () ->
                builder().fields(Map.of(mapKey, bodyField)).build());
    }

    // --- status ---

    @Test
    @DisplayName("status defaults to PUBLISHED")
    void statusDefaultsToPublished() {
        assertEquals(ContentTypeVersionStatus.PUBLISHED, builder().build().status());
    }

    // --- createdAt ---

    @Test
    @DisplayName("defaults createdAt when null")
    void defaultsCreatedAtWhenNull() {
        assertNotNull(builder().createdAt(null).build().createdAt());
    }

    // --- builder fields ---

    @Test
    @DisplayName("builder supports siteKey")
    void builderSupportsSiteKey() {
        assertEquals(siteKey, builder().build().siteKey());
    }

    @Test
    @DisplayName("builder supports contentTypeKey")
    void builderSupportsContentTypeKey() {
        assertEquals(contentTypeKey, builder().build().contentTypeKey());
    }

    @Test
    @DisplayName("builder supports fields map")
    void builderSupportsFieldsMap() {
        final FieldKey titleKey = FieldKey.of("title");
        final Field field = Field.builder().key(titleKey).type(FieldType.TEXT).build();
        final ContentTypeVersion v = builder().fields(Map.of(titleKey, field)).build();
        assertEquals(1, v.fields().size());
    }

    @Test
    @DisplayName("builder supports createdBy")
    void builderSupportsCreatedBy() {
        assertEquals(createdBy, builder().build().createdBy());
    }

    // --- copyOf ---

    @Test
    @DisplayName("copyOf preserves all fields")
    void copyOfPreservesAllFields() {
        final FieldKey titleKey = FieldKey.of("title");
        final Field field = Field.builder().key(titleKey).type(FieldType.TEXT).build();
        final Instant now = Instant.parse("2026-05-01T00:00:00Z");

        final ContentTypeVersion original = builder()
                .fields(Map.of(titleKey, field))
                .status(ContentTypeVersionStatus.DEPRECATED)
                .createdAt(now)
                .build();

        final ContentTypeVersion copy = ContentTypeVersion.copyOf(original).build();

        assertEquals(original.id(), copy.id());
        assertEquals(original.contentTypeId(), copy.contentTypeId());
        assertEquals(original.siteKey(), copy.siteKey());
        assertEquals(original.contentTypeKey(), copy.contentTypeKey());
        assertEquals(original.version(), copy.version());
        assertEquals(original.fields(), copy.fields());
        assertEquals(original.status(), copy.status());
        assertEquals(original.createdBy(), copy.createdBy());
        assertEquals(original.createdAt(), copy.createdAt());
    }

    // --- status enum coverage ---

    @Test
    @DisplayName("ContentTypeVersionStatus has PUBLISHED, DEPRECATED, ARCHIVED")
    void statusEnumHasCorrectValues() {
        assertNotNull(ContentTypeVersionStatus.PUBLISHED);
        assertNotNull(ContentTypeVersionStatus.DEPRECATED);
        assertNotNull(ContentTypeVersionStatus.ARCHIVED);
    }

    // --- helpers ---

    private ContentTypeVersion.Builder builder() {
        return ContentTypeVersion.builder()
                .id(id)
                .contentTypeId(contentTypeId)
                .siteKey(siteKey)
                .contentTypeKey(contentTypeKey)
                .version(1)
                .createdBy(createdBy);
    }
}
