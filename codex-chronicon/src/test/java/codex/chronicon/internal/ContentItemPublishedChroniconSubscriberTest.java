package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
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
 * Tests for {@link ContentItemPublishedChroniconSubscriber}.
 */
class ContentItemPublishedChroniconSubscriberTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID = ContentRevisionId.of("rev-1");
    private static final ActorId ACTOR_ID = ActorId.of("user-1");
    private static final Actor ACTOR = Actor.human(ACTOR_ID, "Test User");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    private RecordingChroniconRepository repository;
    private ContentItemPublishedChroniconSubscriber subscriber;
    private ContentItemPublishedEvent event;

    @BeforeEach
    void setUp() {
        repository = new RecordingChroniconRepository();
        subscriber = new ContentItemPublishedChroniconSubscriber(repository);
        event = new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, REVISION_ID, ACTOR, NOW);
    }

    @Test
    void rejectsNullRepository() {
        assertThrows(NullPointerException.class, () -> new ContentItemPublishedChroniconSubscriber(null));
    }

    @Test
    void eventTypeIsContentItemPublishedEvent() {
        assertEquals(ContentItemPublishedEvent.class, subscriber.eventType());
    }

    @Test
    void handleRejectsNullEvent() {
        assertThrows(NullPointerException.class, () -> subscriber.handle(null));
    }

    @Test
    void handleSavesOneAuditRecord() {
        subscriber.handle(event);
        assertEquals(1, repository.savedRecords().size());
    }

    @Test
    void savedRecordActionIsPublished() {
        subscriber.handle(event);
        assertEquals(AuditAction.PUBLISHED, repository.savedRecords().get(0).action());
    }

    @Test
    void savedRecordSubjectTypeIsContentItem() {
        subscriber.handle(event);
        assertEquals("content-item", repository.savedRecords().get(0).subject().type());
    }

    @Test
    void savedRecordActorIdMatchesEvent() {
        subscriber.handle(event);
        assertEquals(ACTOR_ID, repository.savedRecords().get(0).actorId());
    }

    @Test
    void savedRecordOccurredAtMatchesEvent() {
        subscriber.handle(event);
        assertEquals(NOW, repository.savedRecords().get(0).occurredAt());
    }

    @Test
    void savedRecordMetadataContainsContentItemId() {
        subscriber.handle(event);
        assertEquals(ITEM_ID.value(), repository.savedRecords().get(0).metadata().get("contentItemId"));
    }

    @Test
    void savedRecordMetadataContainsContentItemKey() {
        subscriber.handle(event);
        assertEquals(ITEM_KEY.value(), repository.savedRecords().get(0).metadata().get("contentItemKey"));
    }

    @Test
    void savedRecordMetadataContainsContentTypeKey() {
        subscriber.handle(event);
        assertEquals(CT_KEY.value(), repository.savedRecords().get(0).metadata().get("contentTypeKey"));
    }

    @Test
    void savedRecordMetadataContainsContentTypeVersionId() {
        subscriber.handle(event);
        assertEquals(CT_VERSION_ID.value(), repository.savedRecords().get(0).metadata().get("contentTypeVersionId"));
    }

    @Test
    void savedRecordMetadataContainsPublishedRevisionId() {
        subscriber.handle(event);
        assertEquals(REVISION_ID.value(), repository.savedRecords().get(0).metadata().get("publishedRevisionId"));
    }
}
