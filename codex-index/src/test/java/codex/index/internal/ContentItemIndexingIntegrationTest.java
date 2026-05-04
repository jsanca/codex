package codex.index.internal;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.codex.api.projection.ContentItemProjectionReader;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import codex.index.api.IndexDocument;
import codex.index.api.IndexResourceType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-end test for the content item indexing pipeline:
 * {@link ContentItemPublishedEvent} → {@link ContentItemPublishedIndexingSubscriber}
 * → {@link ReaderContentItemProjectionSource}
 * → {@link ContentItemIndexDocumentMapper}
 * → {@link RecordingIndexWriter}.
 * <p>
 * Wires all components manually without Spring or CodexRuntime.
 */
class ContentItemIndexingIntegrationTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID = ContentRevisionId.of("rev-1");
    private static final ActorId ACTOR_ID = ActorId.of("user-1");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    private RecordingIndexWriter indexWriter;
    private ContentItemPublishedIndexingSubscriber subscriber;
    private StubContentItemProjectionReader stubReader;

    @BeforeEach
    void setUp() {
        stubReader = new StubContentItemProjectionReader();
        final ReaderContentItemProjectionSource source = new ReaderContentItemProjectionSource(stubReader);
        indexWriter = new RecordingIndexWriter();
        subscriber = new ContentItemPublishedIndexingSubscriber(
                source, indexWriter, new ContentItemIndexDocumentMapper());
    }

    @Test
    void fullPipelineIndexesPublishedItem() {
        final ContentItem item = ContentItem.builder()
                .id(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .key(ITEM_KEY)
                .currentWorkingRevisionId(REVISION_ID)
                .owner(ACTOR_ID)
                .createdBy(ACTOR_ID)
                .updatedBy(ACTOR_ID)
                .updatedAt(NOW)
                .build();

        final ContentRevision revision = ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .values(Map.of(FieldKey.TITLE, "Hello World", FieldKey.of("summary"), "A great post"))
                .createdBy(ACTOR_ID)
                .build();

        stubReader.saveItem(item);
        stubReader.saveRevision(revision);

        final ContentItemPublishedEvent event = new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID,
                ITEM_KEY, REVISION_ID, Actor.system("test"), NOW);

        subscriber.handle(event);

        assertEquals(1, indexWriter.upserts().size());
        final IndexDocument doc = indexWriter.upserts().get(0);
        assertEquals("content-item:acme:blog-post:my-post", doc.id().value());
        assertEquals(IndexResourceType.CONTENT_ITEM, doc.resourceType());
        assertEquals(SITE_KEY, doc.siteKey());
        assertEquals("Hello World", doc.title());
        assertTrue(doc.body().contains("Hello World"));
        assertTrue(doc.body().contains("A great post"));
        assertEquals(NOW, doc.updatedAt());
    }

    @Test
    void secondPublishUpsertsToSameDocumentId() {
        final ContentItem item = ContentItem.builder()
                .id(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .key(ITEM_KEY)
                .currentWorkingRevisionId(REVISION_ID)
                .owner(ACTOR_ID)
                .createdBy(ACTOR_ID)
                .updatedBy(ACTOR_ID)
                .updatedAt(NOW)
                .build();

        final ContentRevision revision = ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .createdBy(ACTOR_ID)
                .build();

        stubReader.saveItem(item);
        stubReader.saveRevision(revision);

        final ContentItemPublishedEvent event = new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID,
                ITEM_KEY, REVISION_ID, Actor.system("test"), NOW);

        subscriber.handle(event);
        subscriber.handle(event);

        assertEquals(2, indexWriter.upserts().size());
        assertEquals(indexWriter.upserts().get(0).id(), indexWriter.upserts().get(1).id());
    }

    private static final class StubContentItemProjectionReader implements ContentItemProjectionReader {
        private final Map<String, ContentItem> items = new HashMap<>();
        private final Map<String, ContentRevision> revisions = new HashMap<>();

        void saveItem(final ContentItem item) {
            items.put(item.siteKey().value() + ":" + item.contentTypeKey().value() + ":" + item.key().value(), item);
        }

        void saveRevision(final ContentRevision revision) {
            revisions.put(revision.id().value(), revision);
        }

        @Override
        public Optional<ContentItem> findContentItem(
                final SiteKey siteKey, final ContentTypeKey contentTypeKey, final ContentItemKey key) {
            return Optional.ofNullable(
                    items.get(siteKey.value() + ":" + contentTypeKey.value() + ":" + key.value()));
        }

        @Override
        public Optional<ContentRevision> findContentRevision(final ContentRevisionId revisionId) {
            return Optional.ofNullable(revisions.get(revisionId.value()));
        }
    }
}
