package codex.index.internal;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentItemPublishedIndexingSubscriber}.
 */
class ContentItemPublishedIndexingSubscriberTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID = ContentRevisionId.of("rev-1");
    private static final ActorId ACTOR_ID = ActorId.of("user-1");
    private static final Actor ACTOR = Actor.system("test");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    private RecordingIndexWriter indexWriter;
    private ContentItemPublishedIndexingSubscriber subscriber;

    private ContentItem item;
    private ContentRevision revision;
    private ContentItemPublishedEvent event;

    @BeforeEach
    void setUp() {
        item = ContentItem.builder()
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

        revision = ContentRevision.builder()
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

        event = new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID,
                ITEM_KEY, REVISION_ID, ACTOR, NOW);

        final ContentItemProjectionSource source = new StubProjectionSource(item, revision);
        indexWriter = new RecordingIndexWriter();
        subscriber = new ContentItemPublishedIndexingSubscriber(
                source, indexWriter, new ContentItemIndexDocumentMapper());
    }

    @Test
    void rejectsNullProjectionSource() {
        assertThrows(NullPointerException.class, () ->
                new ContentItemPublishedIndexingSubscriber(null, indexWriter, new ContentItemIndexDocumentMapper()));
    }

    @Test
    void rejectsNullIndexWriter() {
        final ContentItemProjectionSource source = new StubProjectionSource(item, revision);
        assertThrows(NullPointerException.class, () ->
                new ContentItemPublishedIndexingSubscriber(source, null, new ContentItemIndexDocumentMapper()));
    }

    @Test
    void rejectsNullMapper() {
        final ContentItemProjectionSource source = new StubProjectionSource(item, revision);
        assertThrows(NullPointerException.class, () ->
                new ContentItemPublishedIndexingSubscriber(source, indexWriter, null));
    }

    @Test
    void rejectsNullEvent() {
        assertThrows(NullPointerException.class, () -> subscriber.handle(null));
    }

    @Test
    void eventTypeIsContentItemPublishedEvent() {
        assertEquals(ContentItemPublishedEvent.class, subscriber.eventType());
    }

    @Test
    void handleUpsertsDocumentToIndexWriter() {
        subscriber.handle(event);
        assertEquals(1, indexWriter.upserts().size());
    }

    @Test
    void upsertedDocumentHasCorrectId() {
        subscriber.handle(event);
        assertEquals("content-item:acme:blog-post:my-post",
                indexWriter.upserts().get(0).id().value());
    }

    @Test
    void upsertedDocumentHasCorrectSiteKey() {
        subscriber.handle(event);
        assertEquals(SITE_KEY, indexWriter.upserts().get(0).siteKey());
    }

    private static final class StubProjectionSource implements ContentItemProjectionSource {
        private final ContentItem item;
        private final ContentRevision revision;

        StubProjectionSource(final ContentItem item, final ContentRevision revision) {
            this.item = item;
            this.revision = revision;
        }

        @Override
        public ContentItem loadItem(final ContentItemPublishedEvent event) {
            return item;
        }

        @Override
        public ContentRevision loadPublishedRevision(final ContentItemPublishedEvent event) {
            return revision;
        }
    }
}
