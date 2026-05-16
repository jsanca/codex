package codex.index.internal;

import codex.codex.api.model.event.ContentItemUnpublishedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentItemUnpublishedIndexingSubscriber}.
 */
class ContentItemUnpublishedIndexingSubscriberTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Test User");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    private RecordingIndexWriter indexWriter;
    private ContentItemUnpublishedIndexingSubscriber subscriber;
    private ContentItemUnpublishedEvent event;

    @BeforeEach
    void setUp() {
        indexWriter = new RecordingIndexWriter();
        subscriber = new ContentItemUnpublishedIndexingSubscriber(indexWriter);
        event = new ContentItemUnpublishedEvent(ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, ACTOR, NOW);
    }

    @Test
    void rejectsNullIndexWriter() {
        assertThrows(NullPointerException.class, () -> new ContentItemUnpublishedIndexingSubscriber(null));
    }

    @Test
    void rejectsNullEvent() {
        assertThrows(NullPointerException.class, () -> subscriber.handle(null));
    }

    @Test
    void eventTypeIsContentItemUnpublishedEvent() {
        assertEquals(ContentItemUnpublishedEvent.class, subscriber.eventType());
    }

    @Test
    void handleDeletesCorrectDocumentId() {
        subscriber.handle(event);
        assertEquals("content-item:acme:blog-post:my-post", indexWriter.deletes().get(0).value());
    }

    @Test
    void handleCallsDeleteExactlyOnce() {
        subscriber.handle(event);
        assertEquals(1, indexWriter.deletes().size());
    }
}
