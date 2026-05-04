package codex.codex.internal.index;

import codex.codex.api.index.IndexDocument;
import codex.codex.api.index.IndexResourceType;
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
import codex.codex.api.model.value.ContentItemStatus;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.codex.internal.repository.MemoryContentItemRepository;
import codex.codex.internal.repository.MemoryContentRevisionRepository;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentItemPublishedIndexingSubscriber}.
 */
class ContentItemPublishedIndexingSubscriberTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("welcome-to-codex");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID =
            ContentRevisionId.forRevision(SITE_KEY, CT_KEY, ITEM_KEY, 1);
    private static final Actor ACTOR = Actor.system("test");

    private MemoryContentItemRepository itemRepository;
    private MemoryContentRevisionRepository revisionRepository;
    private RepositoryContentItemProjectionSource projectionSource;
    private RecordingIndexWriter indexWriter;
    private ContentItemPublishedIndexingSubscriber subscriber;

    @BeforeEach
    void setUp() {
        itemRepository = new MemoryContentItemRepository();
        revisionRepository = new MemoryContentRevisionRepository();
        projectionSource = new RepositoryContentItemProjectionSource(itemRepository, revisionRepository);
        indexWriter = new RecordingIndexWriter();
        subscriber = new ContentItemPublishedIndexingSubscriber(
                projectionSource, indexWriter, new ContentItemIndexDocumentMapper());
    }

    private ContentItem buildPublishedItem() {
        return ContentItem.builder()
                .id(ITEM_ID).siteKey(SITE_KEY).contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID).key(ITEM_KEY)
                .status(ContentItemStatus.PUBLISHED)
                .currentWorkingRevisionId(REVISION_ID)
                .currentPublishedRevisionId(REVISION_ID)
                .owner(ACTOR.id()).createdBy(ACTOR.id()).updatedBy(ACTOR.id())
                .build();
    }

    private ContentRevision buildPublishedRevision() {
        return ContentRevision.builder()
                .id(REVISION_ID).contentItemId(ITEM_ID).siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY).contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY).revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .values(Map.of(FieldKey.TITLE, "Welcome to Codex"))
                .createdBy(ACTOR.id())
                .build();
    }

    private ContentItemPublishedEvent buildEvent() {
        return new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY,
                REVISION_ID, ACTOR, Instant.now());
    }

    // --- constructor ---

    @Test
    void constructorRejectsNullProjectionSource() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemPublishedIndexingSubscriber(
                        null, indexWriter, new ContentItemIndexDocumentMapper()));
    }

    @Test
    void constructorRejectsNullIndexWriter() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemPublishedIndexingSubscriber(
                        projectionSource, null, new ContentItemIndexDocumentMapper()));
    }

    @Test
    void constructorRejectsNullMapper() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemPublishedIndexingSubscriber(
                        projectionSource, indexWriter, null));
    }

    // --- handle ---

    @Test
    void handleRejectsNullEvent() {
        assertThrows(NullPointerException.class, () -> subscriber.handle(null));
    }

    @Test
    void handleUsesProjectionSourceToLoadItemAndRevision() {
        itemRepository.save(buildPublishedItem());
        revisionRepository.save(buildPublishedRevision());

        subscriber.handle(buildEvent());

        assertEquals(1, indexWriter.upserts().size());
    }

    @Test
    void handleMapsItemAndRevisionToDocument() {
        itemRepository.save(buildPublishedItem());
        revisionRepository.save(buildPublishedRevision());

        subscriber.handle(buildEvent());

        final IndexDocument doc = indexWriter.upserts().getFirst();
        assertEquals("Welcome to Codex", doc.title());
        assertEquals(SITE_KEY, doc.siteKey());
    }

    @Test
    void handleWritesOneUpsertToIndexWriter() {
        itemRepository.save(buildPublishedItem());
        revisionRepository.save(buildPublishedRevision());

        subscriber.handle(buildEvent());

        assertEquals(1, indexWriter.upserts().size());
    }

    @Test
    void missingItemStillThrows() {
        revisionRepository.save(buildPublishedRevision());
        assertThrows(IllegalStateException.class, () -> subscriber.handle(buildEvent()));
    }

    @Test
    void missingRevisionStillThrows() {
        itemRepository.save(buildPublishedItem());
        assertThrows(IllegalStateException.class, () -> subscriber.handle(buildEvent()));
    }

    @Test
    void whenSourceThrowsNoDocumentIsIndexed() {
        // neither item nor revision seeded — source will throw
        assertThrows(IllegalStateException.class, () -> subscriber.handle(buildEvent()));
        assertTrue(indexWriter.upserts().isEmpty());
    }

    // --- CodexEventSubscriber contract ---

    @Test
    void subscriberImplementsCodexEventSubscriber() {
        assertInstanceOf(CodexEventSubscriber.class, subscriber);
    }

    @Test
    void eventTypeReturnsContentItemPublishedEventClass() {
        assertEquals(ContentItemPublishedEvent.class, subscriber.eventType());
    }

    @Test
    void dispatcherCanInvokeSubscriberThroughLocalDispatcher() {
        itemRepository.save(buildPublishedItem());
        revisionRepository.save(buildPublishedRevision());

        final LocalCodexEventDispatcher dispatcher = new LocalCodexEventDispatcher(List.of(subscriber));
        dispatcher.dispatch(buildEvent());

        assertEquals(1, indexWriter.upserts().size());
    }

    @Test
    void whenDispatchedThroughLocalDispatcherPublishedEventResultsInOneIndexUpsert() {
        itemRepository.save(buildPublishedItem());
        revisionRepository.save(buildPublishedRevision());

        final LocalCodexEventDispatcher dispatcher = new LocalCodexEventDispatcher(List.of(subscriber));
        dispatcher.dispatch(buildEvent());

        final IndexDocument doc = indexWriter.upserts().getFirst();
        assertEquals("content-item:acme:blog-post:welcome-to-codex", doc.id().value());
        assertEquals(IndexResourceType.CONTENT_ITEM, doc.resourceType());
    }
}
