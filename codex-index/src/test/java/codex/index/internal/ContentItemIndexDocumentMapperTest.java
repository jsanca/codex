package codex.index.internal;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.fundamentum.api.model.ActorId;
import codex.index.api.IndexDocument;
import codex.index.api.IndexResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentItemIndexDocumentMapper}.
 */
class ContentItemIndexDocumentMapperTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID = ContentRevisionId.of("rev-1");
    private static final ActorId ACTOR = ActorId.of("user-1");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    private ContentItemIndexDocumentMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new ContentItemIndexDocumentMapper();
    }

    private ContentItem.Builder itemBuilder() {
        return ContentItem.builder()
                .id(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .key(ITEM_KEY)
                .currentWorkingRevisionId(REVISION_ID)
                .owner(ACTOR)
                .createdBy(ACTOR)
                .updatedBy(ACTOR)
                .updatedAt(NOW);
    }

    private ContentRevision.Builder revisionBuilder() {
        return ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .createdBy(ACTOR);
    }

    @Test
    void rejectsNullItem() {
        final ContentRevision revision = revisionBuilder().build();
        assertThrows(NullPointerException.class, () -> mapper.toDocument(null, revision));
    }

    @Test
    void rejectsNullRevision() {
        final ContentItem item = itemBuilder().build();
        assertThrows(NullPointerException.class, () -> mapper.toDocument(item, null));
    }

    @Test
    void rejectsUnpublishedRevision() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision working = revisionBuilder()
                .status(ContentRevisionStatus.WORKING)
                .build();
        assertThrows(IllegalArgumentException.class, () -> mapper.toDocument(item, working));
    }

    @Test
    void rejectsMismatchedContentItemId() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder()
                .contentItemId(ContentItemId.of("different-id"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> mapper.toDocument(item, revision));
    }

    @Test
    void rejectsMismatchedSiteKey() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder()
                .siteKey(SiteKey.of("other"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> mapper.toDocument(item, revision));
    }

    @Test
    void rejectsMismatchedContentTypeKey() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder()
                .contentTypeKey(ContentTypeKey.of("other-type"))
                .build();
        assertThrows(IllegalArgumentException.class, () -> mapper.toDocument(item, revision));
    }

    @Test
    void producesCorrectDocumentId() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder().build();
        final IndexDocument doc = mapper.toDocument(item, revision);
        assertEquals("content-item:acme:blog-post:my-post", doc.id().value());
    }

    @Test
    void setsCorrectResourceType() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder().build();
        final IndexDocument doc = mapper.toDocument(item, revision);
        assertEquals(IndexResourceType.CONTENT_ITEM, doc.resourceType());
    }

    @Test
    void setsSiteKey() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder().build();
        final IndexDocument doc = mapper.toDocument(item, revision);
        assertEquals(SITE_KEY, doc.siteKey());
    }

    @Test
    void extractsTitleFromTitleField() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder()
                .values(Map.of(FieldKey.TITLE, "Hello World"))
                .build();
        final IndexDocument doc = mapper.toDocument(item, revision);
        assertEquals("Hello World", doc.title());
    }

    @Test
    void fallsBackToItemKeyWhenNoTitleField() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder().build();
        final IndexDocument doc = mapper.toDocument(item, revision);
        assertEquals(ITEM_KEY.value(), doc.title());
    }

    @Test
    void buildsBodyFromScalarValues() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder()
                .values(Map.of(FieldKey.of("summary"), "Short summary"))
                .build();
        final IndexDocument doc = mapper.toDocument(item, revision);
        assertTrue(doc.body().contains("Short summary"));
    }

    @Test
    void includesMetadataFields() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder().build();
        final IndexDocument doc = mapper.toDocument(item, revision);
        assertEquals(ITEM_ID.value(), doc.metadata().get("contentItemId"));
        assertEquals(ITEM_KEY.value(), doc.metadata().get("contentItemKey"));
        assertEquals(CT_KEY.value(), doc.metadata().get("contentTypeKey"));
        assertEquals(CT_VERSION_ID.value(), doc.metadata().get("contentTypeVersionId"));
        assertEquals(REVISION_ID.value(), doc.metadata().get("contentRevisionId"));
        assertEquals("1", doc.metadata().get("revisionNumber"));
        assertEquals("PUBLISHED", doc.metadata().get("revisionStatus"));
    }

    @Test
    void setsUpdatedAtFromItem() {
        final ContentItem item = itemBuilder().updatedAt(NOW).build();
        final ContentRevision revision = revisionBuilder().build();
        final IndexDocument doc = mapper.toDocument(item, revision);
        assertEquals(NOW, doc.updatedAt());
    }

    @Test
    void scalarFieldsIncludedInFields() {
        final ContentItem item = itemBuilder().build();
        final ContentRevision revision = revisionBuilder()
                .values(Map.of(FieldKey.of("published"), true, FieldKey.of("score"), 42))
                .build();
        final IndexDocument doc = mapper.toDocument(item, revision);
        assertEquals(true, doc.fields().get("published"));
        assertEquals(42, doc.fields().get("score"));
    }
}
