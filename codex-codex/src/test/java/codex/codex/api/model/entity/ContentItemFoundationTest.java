package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentItemStatus;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentItemKey}, {@link ContentItemId}, and {@link ContentItem}.
 */
class ContentItemFoundationTest {

    // --- ContentItemKey ---

    @Test
    void contentItemKey_rejectsNull() {
        assertThrows(NullPointerException.class, () -> ContentItemKey.of(null));
    }

    @Test
    void contentItemKey_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> ContentItemKey.of("   "));
    }

    @Test
    void contentItemKey_trimsThenValidates() {
        assertThrows(IllegalArgumentException.class, () -> ContentItemKey.of("  a  "));
    }

    @Test
    void contentItemKey_normalizesToLowercase() {
        final ContentItemKey key = ContentItemKey.of("Welcome-To-Codex");
        assertEquals("welcome-to-codex", key.value());
    }

    @Test
    void contentItemKey_acceptsSafeKey() {
        final ContentItemKey key = ContentItemKey.of("home-page");
        assertEquals("home-page", key.value());
    }

    @Test
    void contentItemKey_acceptsKeyWithNumbers() {
        final ContentItemKey key = ContentItemKey.of("product-123");
        assertEquals("product-123", key.value());
    }

    @Test
    void contentItemKey_rejectsKeyStartingWithHyphen() {
        assertThrows(IllegalArgumentException.class, () -> ContentItemKey.of("-invalid"));
    }

    @Test
    void contentItemKey_rejectsKeyEndingWithHyphen() {
        assertThrows(IllegalArgumentException.class, () -> ContentItemKey.of("invalid-"));
    }

    @Test
    void contentItemKey_rejectsTooShortKey() {
        assertThrows(IllegalArgumentException.class, () -> ContentItemKey.of("a"));
    }

    @Test
    void contentItemKey_ofFactoryWorks() {
        final ContentItemKey key = ContentItemKey.of("about-us");
        assertNotNull(key);
        assertEquals("about-us", key.value());
    }

    @Test
    void contentItemKey_toStringReturnsValue() {
        assertEquals("home-page", ContentItemKey.of("home-page").toString());
    }

    // --- ContentItemId ---

    @Test
    void contentItemId_rejectsNull() {
        assertThrows(NullPointerException.class, () -> ContentItemId.of(null));
    }

    @Test
    void contentItemId_rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> ContentItemId.of("   "));
    }

    @Test
    void contentItemId_ofFactoryWorks() {
        final ContentItemId id = ContentItemId.of("some-id");
        assertNotNull(id);
        assertEquals("some-id", id.value());
    }

    @Test
    void contentItemId_forItemIsDeterministic() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");
        final ContentItemKey itemKey = ContentItemKey.of("welcome-to-codex");

        final ContentItemId id1 = ContentItemId.forItem(siteKey, contentTypeKey, itemKey);
        final ContentItemId id2 = ContentItemId.forItem(siteKey, contentTypeKey, itemKey);

        assertEquals(id1, id2);
    }

    @Test
    void contentItemId_forItemUsesExpectedFormat() {
        final ContentItemId id = ContentItemId.forItem(
                SiteKey.of("acme"),
                ContentTypeKey.of("blog-post"),
                ContentItemKey.of("welcome-to-codex"));

        assertEquals("content-item:acme:blog-post:welcome-to-codex", id.value());
    }

    @Test
    void contentItemId_forItemDiffersByKey() {
        final SiteKey siteKey = SiteKey.of("acme");
        final ContentTypeKey contentTypeKey = ContentTypeKey.of("blog-post");

        final ContentItemId id1 = ContentItemId.forItem(siteKey, contentTypeKey, ContentItemKey.of("post-one"));
        final ContentItemId id2 = ContentItemId.forItem(siteKey, contentTypeKey, ContentItemKey.of("post-two"));

        assertNotEquals(id1, id2);
    }

    @Test
    void contentItemId_forItemRejectsNullSiteKey() {
        assertThrows(NullPointerException.class,
                () -> ContentItemId.forItem(null, ContentTypeKey.of("blog-post"), ContentItemKey.of("post-one")));
    }

    @Test
    void contentItemId_forItemRejectsNullContentTypeKey() {
        assertThrows(NullPointerException.class,
                () -> ContentItemId.forItem(SiteKey.of("acme"), null, ContentItemKey.of("post-one")));
    }

    @Test
    void contentItemId_forItemRejectsNullItemKey() {
        assertThrows(NullPointerException.class,
                () -> ContentItemId.forItem(SiteKey.of("acme"), ContentTypeKey.of("blog-post"), null));
    }

    // --- ContentItem ---

    private ContentItemId sampleId() {
        return ContentItemId.forItem(SiteKey.of("acme"), ContentTypeKey.of("blog-post"), ContentItemKey.of("my-post"));
    }

    private ContentItem.Builder minimalBuilder() {
        final ActorId actorId = ActorId.of("user-1");
        return ContentItem.builder()
                .id(sampleId())
                .siteKey(SiteKey.of("acme"))
                .contentTypeKey(ContentTypeKey.of("blog-post"))
                .contentTypeVersionId(ContentTypeVersionId.of("content-type-version:acme:blog-post:v1"))
                .key(ContentItemKey.of("my-post"))
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId);
    }

    @Test
    void contentItem_rejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().id(null).build());
    }

    @Test
    void contentItem_rejectsNullSiteKey() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().siteKey(null).build());
    }

    @Test
    void contentItem_rejectsNullContentTypeKey() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().contentTypeKey(null).build());
    }

    @Test
    void contentItem_rejectsNullContentTypeVersionId() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().contentTypeVersionId(null).build());
    }

    @Test
    void contentItem_rejectsNullKey() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().key(null).build());
    }

    @Test
    void contentItem_defaultsStatusToDraft() {
        final ContentItem item = minimalBuilder().build();
        assertEquals(ContentItemStatus.DRAFT, item.status());
    }

    @Test
    void contentItem_defaultsValuesToEmptyMap() {
        final ContentItem item = minimalBuilder().build();
        assertNotNull(item.values());
        assertTrue(item.values().isEmpty());
    }

    @Test
    void contentItem_defensivelyCopiesValues() {
        final Map<FieldKey, Object> mutable = new HashMap<>();
        mutable.put(FieldKey.of("title"), "Hello");
        final ContentItem item = minimalBuilder().values(mutable).build();

        mutable.put(FieldKey.of("summary"), "Extra");
        assertEquals(1, item.values().size());
    }

    @Test
    void contentItem_valuesAccessorIsImmutable() {
        final ContentItem item = minimalBuilder()
                .values(Map.of(FieldKey.of("title"), "Hello"))
                .build();
        assertThrows(UnsupportedOperationException.class,
                () -> item.values().put(FieldKey.of("other"), "x"));
    }

    @Test
    void contentItem_rejectsNullValueMapKey() {
        final Map<FieldKey, Object> badValues = new HashMap<>();
        badValues.put(null, "some-value");
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().values(badValues).build());
    }

    @Test
    void contentItem_rejectsNullValueMapValue() {
        final Map<FieldKey, Object> badValues = new HashMap<>();
        badValues.put(FieldKey.of("title"), null);
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().values(badValues).build());
    }

    @Test
    void contentItem_rejectsNullOwner() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().owner(null).build());
    }

    @Test
    void contentItem_rejectsNullCreatedBy() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().createdBy(null).build());
    }

    @Test
    void contentItem_rejectsNullUpdatedBy() {
        assertThrows(NullPointerException.class,
                () -> minimalBuilder().updatedBy(null).build());
    }

    @Test
    void contentItem_defaultsCreatedAtWhenNull() {
        final ContentItem item = minimalBuilder().build();
        assertNotNull(item.createdAt());
    }

    @Test
    void contentItem_defaultsUpdatedAtToCreatedAt() {
        final Instant createdAt = Instant.parse("2025-01-01T00:00:00Z");
        final ContentItem item = minimalBuilder().createdAt(createdAt).build();
        assertEquals(createdAt, item.updatedAt());
    }

    @Test
    void contentItem_builderSupportsAllFields() {
        final ActorId actorId = ActorId.of("user-1");
        final Instant now = Instant.now();
        final ContentItem item = ContentItem.builder()
                .id(sampleId())
                .siteKey(SiteKey.of("acme"))
                .contentTypeKey(ContentTypeKey.of("blog-post"))
                .contentTypeVersionId(ContentTypeVersionId.of("v1"))
                .key(ContentItemKey.of("my-post"))
                .status(ContentItemStatus.PUBLISHED)
                .values(Map.of(FieldKey.of("title"), "Hello"))
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        assertEquals(ContentItemStatus.PUBLISHED, item.status());
        assertEquals(1, item.values().size());
    }

    @Test
    void contentItem_copyOfPreservesAllFields() {
        final ActorId actorId = ActorId.of("user-1");
        final Instant now = Instant.parse("2025-06-01T10:00:00Z");
        final ContentItem original = ContentItem.builder()
                .id(sampleId())
                .siteKey(SiteKey.of("acme"))
                .contentTypeKey(ContentTypeKey.of("blog-post"))
                .contentTypeVersionId(ContentTypeVersionId.of("v1"))
                .key(ContentItemKey.of("my-post"))
                .status(ContentItemStatus.DRAFT)
                .values(Map.of(FieldKey.of("title"), "Hello World"))
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .createdAt(now)
                .updatedAt(now)
                .build();

        final ContentItem copy = ContentItem.copyOf(original).build();

        assertEquals(original.id(), copy.id());
        assertEquals(original.siteKey(), copy.siteKey());
        assertEquals(original.contentTypeKey(), copy.contentTypeKey());
        assertEquals(original.contentTypeVersionId(), copy.contentTypeVersionId());
        assertEquals(original.key(), copy.key());
        assertEquals(original.status(), copy.status());
        assertEquals(original.values(), copy.values());
        assertEquals(original.owner(), copy.owner());
        assertEquals(original.createdBy(), copy.createdBy());
        assertEquals(original.updatedBy(), copy.updatedBy());
        assertEquals(original.createdAt(), copy.createdAt());
        assertEquals(original.updatedAt(), copy.updatedAt());
    }
}
