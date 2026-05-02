package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentRevision}.
 */
class ContentRevisionTest {

    private static final SiteKey SITE = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentTypeVersionId VERSION_ID =
            ContentTypeVersionId.of("content-type-version:acme:blog-post:v1");
    private static final ContentItemId ITEM_ID =
            ContentItemId.forItem(SITE, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID =
            ContentRevisionId.forRevision(SITE, CT_KEY, ITEM_KEY, 1);
    private static final ActorId ACTOR = ActorId.of("user-1");

    private ContentRevision.Builder minimalBuilder() {
        return ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .createdBy(ACTOR);
    }

    // --- required fields ---

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().id(null).build());
    }

    @Test
    void rejectsNullContentItemId() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().contentItemId(null).build());
    }

    @Test
    void rejectsNullSiteKey() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().siteKey(null).build());
    }

    @Test
    void rejectsNullContentTypeKey() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().contentTypeKey(null).build());
    }

    @Test
    void rejectsNullContentTypeVersionId() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().contentTypeVersionId(null).build());
    }

    @Test
    void rejectsNullContentItemKey() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().contentItemKey(null).build());
    }

    @Test
    void rejectsRevisionNumberLessThanOne() {
        assertThrows(IllegalArgumentException.class,
                () -> minimalBuilder().revisionNumber(0).build());
    }

    @Test
    void rejectsNullCreatedBy() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().createdBy(null).build());
    }

    // --- defaults ---

    @Test
    void defaultsStatusToWorking() {
        final ContentRevision revision = minimalBuilder().build();
        assertEquals(ContentRevisionStatus.WORKING, revision.status());
    }

    @Test
    void defaultsValuesToEmptyMap() {
        final ContentRevision revision = minimalBuilder().build();
        assertNotNull(revision.values());
        assertTrue(revision.values().isEmpty());
    }

    @Test
    void defaultsCreatedAtWhenNull() {
        final ContentRevision revision = minimalBuilder().build();
        assertNotNull(revision.createdAt());
    }

    // --- deterministic identity ---

    @Test
    void forRevisionIsDeterministic() {
        final ContentRevisionId id1 = ContentRevisionId.forRevision(SITE, CT_KEY, ITEM_KEY, 1);
        final ContentRevisionId id2 = ContentRevisionId.forRevision(SITE, CT_KEY, ITEM_KEY, 1);
        assertEquals(id1, id2);
    }

    @Test
    void forRevisionUsesExpectedFormat() {
        final ContentRevisionId id = ContentRevisionId.forRevision(SITE, CT_KEY, ITEM_KEY, 3);
        assertEquals("content-revision:acme:blog-post:my-post:r3", id.value());
    }

    @Test
    void forRevisionDiffersByNumber() {
        final ContentRevisionId r1 = ContentRevisionId.forRevision(SITE, CT_KEY, ITEM_KEY, 1);
        final ContentRevisionId r2 = ContentRevisionId.forRevision(SITE, CT_KEY, ITEM_KEY, 2);
        assertNotEquals(r1, r2);
    }

    // --- values map ---

    @Test
    void defensivelyCopiesValues() {
        final Map<FieldKey, Object> mutable = new HashMap<>();
        mutable.put(FieldKey.of("title"), "Hello");
        final ContentRevision revision = minimalBuilder().values(mutable).build();

        mutable.put(FieldKey.of("extra"), "mutation");
        assertEquals(1, revision.values().size());
    }

    @Test
    void valuesAccessorIsImmutable() {
        final ContentRevision revision = minimalBuilder()
                .values(Map.of(FieldKey.of("title"), "Hello"))
                .build();
        assertThrows(UnsupportedOperationException.class,
                () -> revision.values().put(FieldKey.of("other"), "x"));
    }

    @Test
    void rejectsNullValueMapKey() {
        final Map<FieldKey, Object> bad = new HashMap<>();
        bad.put(null, "value");
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().values(bad).build());
    }

    @Test
    void rejectsNullValueMapValue() {
        final Map<FieldKey, Object> bad = new HashMap<>();
        bad.put(FieldKey.of("title"), null);
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().values(bad).build());
    }

    // --- copyOf ---

    @Test
    void copyOfPreservesAllFields() {
        final Instant now = Instant.parse("2025-06-01T10:00:00Z");
        final ContentRevision original = minimalBuilder()
                .revisionNumber(3)
                .status(ContentRevisionStatus.PUBLISHED)
                .values(Map.of(FieldKey.of("title"), "Hello World"))
                .createdAt(now)
                .build();

        final ContentRevision copy = ContentRevision.copyOf(original).build();

        assertEquals(original.id(), copy.id());
        assertEquals(original.contentItemId(), copy.contentItemId());
        assertEquals(original.siteKey(), copy.siteKey());
        assertEquals(original.contentTypeKey(), copy.contentTypeKey());
        assertEquals(original.contentTypeVersionId(), copy.contentTypeVersionId());
        assertEquals(original.contentItemKey(), copy.contentItemKey());
        assertEquals(original.revisionNumber(), copy.revisionNumber());
        assertEquals(original.status(), copy.status());
        assertEquals(original.values(), copy.values());
        assertEquals(original.createdBy(), copy.createdBy());
        assertEquals(original.createdAt(), copy.createdAt());
    }

    // --- happy path ---

    @Test
    void validRevisionBuildsSuccessfully() {
        final ContentRevision revision = minimalBuilder()
                .revisionNumber(5)
                .status(ContentRevisionStatus.WORKING)
                .values(Map.of(FieldKey.of("title"), "Hello World", FieldKey.of("body"), "Content here"))
                .createdAt(Instant.parse("2025-06-01T10:00:00Z"))
                .build();

        assertEquals(5, revision.revisionNumber());
        assertEquals(ContentRevisionStatus.WORKING, revision.status());
        assertEquals(2, revision.values().size());
        assertEquals("Hello World", revision.values().get(FieldKey.of("title")));
    }
}
