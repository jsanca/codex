package codex.codex.internal.index;

import codex.codex.api.index.IndexDocument;
import codex.codex.api.index.IndexResourceType;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentItemStatus;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentItemIndexDocumentMapper}.
 */
class ContentItemIndexDocumentMapperTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("welcome-to-codex");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID =
            ContentRevisionId.forRevision(SITE_KEY, CT_KEY, ITEM_KEY, 1);
    private static final Actor ACTOR = Actor.system("test");

    private ContentItemIndexDocumentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ContentItemIndexDocumentMapper();
    }

    // --- test fixture builders ---

    private ContentItem buildItem() {
        return ContentItem.builder()
                .id(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .key(ITEM_KEY)
                .status(ContentItemStatus.PUBLISHED)
                .currentWorkingRevisionId(REVISION_ID)
                .currentPublishedRevisionId(REVISION_ID)
                .owner(ACTOR.id())
                .createdBy(ACTOR.id())
                .updatedBy(ACTOR.id())
                .build();
    }

    private ContentRevision buildRevision(final Map<FieldKey, Object> values) {
        return ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .values(values)
                .createdBy(ACTOR.id())
                .build();
    }

    // --- 1: full mapping ---

    @Test
    void mapsContentItemAndPublishedRevisionToIndexDocument() {
        final ContentItem item = buildItem();
        final ContentRevision revision = buildRevision(
                Map.of(FieldKey.TITLE, "Welcome to Codex", FieldKey.of("summary"), "A short summary"));

        final IndexDocument doc = mapper.toDocument(item, revision);

        assertEquals(IndexResourceType.CONTENT_ITEM, doc.resourceType());
        assertEquals(SITE_KEY, doc.siteKey());
        assertEquals("content-item:acme:blog-post:welcome-to-codex", doc.id().value());
        assertEquals("Welcome to Codex", doc.title());
        assertTrue(doc.body().contains("Welcome to Codex"));
        assertTrue(doc.body().contains("A short summary"));
        assertTrue(doc.fields().containsKey("title"));
        assertTrue(doc.fields().containsKey("summary"));
    }

    @Test
    void documentIdIsDeterministic() {
        final ContentItem item = buildItem();
        final ContentRevision revision = buildRevision(Map.of());

        final IndexDocument first = mapper.toDocument(item, revision);
        final IndexDocument second = mapper.toDocument(item, revision);

        assertEquals(first.id(), second.id());
        assertEquals("content-item:acme:blog-post:welcome-to-codex", first.id().value());
    }

    @Test
    void metadataIncludesExpectedIdentityFields() {
        final ContentItem item = buildItem();
        final ContentRevision revision = buildRevision(Map.of());

        final IndexDocument doc = mapper.toDocument(item, revision);
        final Map<String, String> metadata = doc.metadata();

        assertEquals(ITEM_ID.value(), metadata.get("contentItemId"));
        assertEquals(ITEM_KEY.value(), metadata.get("contentItemKey"));
        assertEquals(CT_KEY.value(), metadata.get("contentTypeKey"));
        assertEquals(CT_VERSION_ID.value(), metadata.get("contentTypeVersionId"));
        assertEquals(REVISION_ID.value(), metadata.get("contentRevisionId"));
        assertEquals("1", metadata.get("revisionNumber"));
        assertEquals("PUBLISHED", metadata.get("revisionStatus"));
    }

    // --- 2: title fallback ---

    @Test
    void usesContentItemKeyAsTitleWhenTitleFieldIsMissing() {
        final ContentItem item = buildItem();
        final ContentRevision revision = buildRevision(Map.of(FieldKey.of("summary"), "A summary only"));

        final IndexDocument doc = mapper.toDocument(item, revision);

        assertEquals(ITEM_KEY.value(), doc.title());
    }

    // --- 3: null rejections ---

    @Test
    void rejectsNullItem() {
        final ContentRevision revision = buildRevision(Map.of());
        assertThrows(NullPointerException.class, () -> mapper.toDocument(null, revision));
    }

    @Test
    void rejectsNullRevision() {
        final ContentItem item = buildItem();
        assertThrows(NullPointerException.class, () -> mapper.toDocument(item, null));
    }

    // --- 4: item/revision mismatch ---

    @Test
    void rejectsItemRevisionContentItemIdMismatch() {
        final ContentItem item = buildItem();
        final ContentRevision revision = ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ContentItemId.of("other-item-id"))  // mismatch
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .createdBy(ACTOR.id())
                .build();

        assertThrows(IllegalArgumentException.class, () -> mapper.toDocument(item, revision));
    }

    @Test
    void rejectsItemRevisionSiteKeyMismatch() {
        final ContentItem item = buildItem();
        final ContentRevision revision = ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SiteKey.of("other-site"))  // mismatch
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .createdBy(ACTOR.id())
                .build();

        assertThrows(IllegalArgumentException.class, () -> mapper.toDocument(item, revision));
    }

    @Test
    void rejectsItemRevisionContentTypeKeyMismatch() {
        final ContentItem item = buildItem();
        final ContentRevision revision = ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(ContentTypeKey.of("other-type"))  // mismatch
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .createdBy(ACTOR.id())
                .build();

        assertThrows(IllegalArgumentException.class, () -> mapper.toDocument(item, revision));
    }

    @Test
    void rejectsItemRevisionContentTypeVersionIdMismatch() {
        final ContentItem item = buildItem();
        final ContentRevision revision = ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(ContentTypeVersionId.of("other-version"))  // mismatch
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .createdBy(ACTOR.id())
                .build();

        assertThrows(IllegalArgumentException.class, () -> mapper.toDocument(item, revision));
    }

    // --- 5: non-published revision ---

    @Test
    void rejectsNonPublishedRevision() {
        final ContentItem item = buildItem();
        final ContentRevision workingRevision = ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.WORKING)  // not published
                .createdBy(ACTOR.id())
                .build();

        assertThrows(IllegalArgumentException.class, () -> mapper.toDocument(item, workingRevision));
    }

    // --- 6: complex values not dumped into metadata ---

    @Test
    void doesNotIncludeComplexValuesInMetadata() {
        final ContentItem item = buildItem();
        final ContentRevision revision = buildRevision(
                Map.of(FieldKey.of("title"), "Hello"));

        final IndexDocument doc = mapper.toDocument(item, revision);

        // metadata must only contain identity strings, not content field values
        assertFalse(doc.metadata().containsKey("title"));
        assertFalse(doc.metadata().containsKey("summary"));
    }

    // --- 7: scalar fields only ---

    @Test
    void includesOnlyScalarValuesInFields() {
        final ContentItem item = buildItem();
        final ContentRevision revision = buildRevision(
                Map.of(FieldKey.of("title"), "Hello", FieldKey.of("count"), 42, FieldKey.of("active"), true));

        final IndexDocument doc = mapper.toDocument(item, revision);

        assertTrue(doc.fields().containsKey("title"));
        assertTrue(doc.fields().containsKey("count"));
        assertTrue(doc.fields().containsKey("active"));
    }
}
