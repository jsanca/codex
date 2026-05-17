package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.codex.api.model.event.ContentTypeArchivedEvent;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentTypeArchivedChroniconSubscriber}.
 */
class ContentTypeArchivedChroniconSubscriberTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeId CT_ID = ContentTypeId.of("ct-id-1");
    private static final ActorId ACTOR_ID = ActorId.of("user-1");
    private static final Actor ACTOR = Actor.human(ACTOR_ID, "Test User");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    private RecordingChroniconRepository repository;
    private ContentTypeArchivedChroniconSubscriber subscriber;
    private ContentTypeArchivedEvent event;

    @BeforeEach
    void setUp() {
        repository = new RecordingChroniconRepository();
        subscriber = new ContentTypeArchivedChroniconSubscriber(repository);
        event = new ContentTypeArchivedEvent(
                CT_ID, SITE_KEY, CT_KEY, ContentTypeStatus.ACTIVE, ContentTypeStatus.ARCHIVED, ACTOR, NOW);
    }

    @Test
    void rejectsNullRepository() {
        assertThrows(NullPointerException.class, () -> new ContentTypeArchivedChroniconSubscriber(null));
    }

    @Test
    void eventTypeIsContentTypeArchivedEvent() {
        assertEquals(ContentTypeArchivedEvent.class, subscriber.eventType());
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
    void savedRecordActionIsArchived() {
        subscriber.handle(event);
        assertEquals(AuditAction.ARCHIVED, repository.savedRecords().get(0).action());
    }

    @Test
    void savedRecordSubjectTypeIsContentType() {
        subscriber.handle(event);
        assertEquals("content-type", repository.savedRecords().get(0).subject().type());
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
    void savedRecordMetadataContainsContentTypeKey() {
        subscriber.handle(event);
        assertEquals(CT_KEY.value(), repository.savedRecords().get(0).metadata().get("contentTypeKey"));
    }

    @Test
    void savedRecordMetadataContainsSiteKey() {
        subscriber.handle(event);
        assertEquals(SITE_KEY.value(), repository.savedRecords().get(0).metadata().get("siteKey"));
    }
}
