package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.codex.api.model.event.SiteStartedEvent;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link SiteStartedChroniconSubscriber}.
 */
class SiteStartedChroniconSubscriberTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final SiteId SITE_ID = SiteId.of("site-id-1");
    private static final ActorId ACTOR_ID = ActorId.of("user-1");
    private static final Actor ACTOR = Actor.human(ACTOR_ID, "Test User");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    private RecordingChroniconRepository repository;
    private SiteStartedChroniconSubscriber subscriber;
    private SiteStartedEvent event;

    @BeforeEach
    void setUp() {
        repository = new RecordingChroniconRepository();
        subscriber = new SiteStartedChroniconSubscriber(repository);
        event = new SiteStartedEvent(SITE_ID, SITE_KEY, ACTOR, NOW);
    }

    @Test
    void rejectsNullRepository() {
        assertThrows(NullPointerException.class, () -> new SiteStartedChroniconSubscriber(null));
    }

    @Test
    void eventTypeIsSiteStartedEvent() {
        assertEquals(SiteStartedEvent.class, subscriber.eventType());
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
    void savedRecordActionIsStarted() {
        subscriber.handle(event);
        assertEquals(AuditAction.STARTED, repository.savedRecords().get(0).action());
    }

    @Test
    void savedRecordSubjectTypeIsSite() {
        subscriber.handle(event);
        assertEquals("site", repository.savedRecords().get(0).subject().type());
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
    void savedRecordSummaryIsNonBlank() {
        subscriber.handle(event);
        assertFalse(repository.savedRecords().get(0).summary().isBlank());
    }

    @Test
    void savedRecordMetadataContainsSiteKey() {
        subscriber.handle(event);
        assertEquals(SITE_KEY.value(), repository.savedRecords().get(0).metadata().get("siteKey"));
    }
}
