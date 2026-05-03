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
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
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
    private RecordingIndexWriter indexWriter;
    private ContentItemPublishedIndexingSubscriber subscriber;

    @BeforeEach
    void setUp() {
        itemRepository = new MemoryContentItemRepository();
        revisionRepository = new MemoryContentRevisionRepository();
        indexWriter = new RecordingIndexWriter();
        subscriber = new ContentItemPublishedIndexingSubscriber(
                itemRepository, revisionRepository, indexWriter, new ContentItemIndexDocumentMapper());
    }

    private ContentItem buildPublishedItem() {
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

    private ContentRevision buildPublishedRevision() {
        return ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
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

    // --- 1: handle publishes index document ---

    @Test
    void handlePublishedEventLoadsItemAndRevisionAndUpsertsDocument() {
        itemRepository.save(buildPublishedItem());
        revisionRepository.save(buildPublishedRevision());

        subscriber.handle(buildEvent());

        assertEquals(1, indexWriter.upserts().size());
    }

    // --- 2: upserted document has expected id and resource type ---

    @Test
    void upsertedDocumentHasExpectedIdAndResourceType() {
        itemRepository.save(buildPublishedItem());
        revisionRepository.save(buildPublishedRevision());

        subscriber.handle(buildEvent());

        final IndexDocument doc = indexWriter.upserts().getFirst();
        assertEquals("content-item:acme:blog-post:welcome-to-codex", doc.id().value());
        assertEquals(IndexResourceType.CONTENT_ITEM, doc.resourceType());
    }

    // --- 3: null event rejection ---

    @Test
    void handleRejectsNullEvent() {
        assertThrows(NullPointerException.class, () -> subscriber.handle(null));
    }

    // --- 4: missing content item ---

    @Test
    void handleThrowsWhenContentItemIsMissing() {
        // only save revision, not item
        revisionRepository.save(buildPublishedRevision());

        assertThrows(IllegalStateException.class, () -> subscriber.handle(buildEvent()));
    }

    // --- 5: missing revision ---

    @Test
    void handleThrowsWhenRevisionIsMissing() {
        // only save item, not revision
        itemRepository.save(buildPublishedItem());

        assertThrows(IllegalStateException.class, () -> subscriber.handle(buildEvent()));
    }

    // --- 6: no index write when item is missing ---

    @Test
    void handleDoesNotWriteToIndexWhenContentItemIsMissing() {
        revisionRepository.save(buildPublishedRevision());

        assertThrows(IllegalStateException.class, () -> subscriber.handle(buildEvent()));

        assertTrue(indexWriter.upserts().isEmpty());
    }

    // --- 7: no index write when revision is missing ---

    @Test
    void handleDoesNotWriteToIndexWhenRevisionIsMissing() {
        itemRepository.save(buildPublishedItem());

        assertThrows(IllegalStateException.class, () -> subscriber.handle(buildEvent()));

        assertTrue(indexWriter.upserts().isEmpty());
    }
}
