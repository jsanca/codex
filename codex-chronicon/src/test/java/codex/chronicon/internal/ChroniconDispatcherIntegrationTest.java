package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.ChroniconRepository;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.event.ContentTypeCreatedEvent;
import codex.codex.api.model.event.SiteCreatedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.fundamentum.api.event.LocalCodexEventDispatcher;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test proving Chronicon subscribers work correctly through
 * {@link LocalCodexEventDispatcher}.
 *
 * <p>Wires real Chronicon subscribers with a {@link RecordingChroniconRepository}.
 * Dispatches domain events manually without {@code CodexRuntime}, Spring, or durable persistence.</p>
 *
 * <p>Failure propagation for dispatchers is covered by
 * {@code codex.fundamentum.api.event.LocalCodexEventDispatcherTest}.
 * This class adds one minimal Chronicon-specific failure test.</p>
 */
class ChroniconDispatcherIntegrationTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final SiteId SITE_ID = SiteId.of("site-id-1");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeId CT_ID = ContentTypeId.of("ct-id-1");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID = ContentRevisionId.of("rev-1");
    private static final ActorId ACTOR_ID = ActorId.of("user-1");
    private static final Actor ACTOR = Actor.human(ACTOR_ID, "Test User");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    private RecordingChroniconRepository repository;
    private LocalCodexEventDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        repository = new RecordingChroniconRepository();
        dispatcher = new LocalCodexEventDispatcher(List.of(
                new SiteCreatedChroniconSubscriber(repository),
                new ContentTypeCreatedChroniconSubscriber(repository),
                new ContentItemPublishedChroniconSubscriber(repository)));
    }

    // --- comprehensive pipeline test ---

    @Test
    void dispatchingSupportedEventsRecordsAuditEntries() {
        final Instant siteTime = Instant.parse("2026-05-01T10:00:00Z");
        final Instant ctTime = Instant.parse("2026-05-01T10:01:00Z");
        final Instant itemTime = Instant.parse("2026-05-01T10:02:00Z");

        dispatcher.dispatch(new SiteCreatedEvent(SITE_ID, SITE_KEY, ACTOR, siteTime));
        dispatcher.dispatch(new ContentTypeCreatedEvent(CT_ID, SITE_KEY, CT_KEY, ACTOR, ctTime));
        dispatcher.dispatch(new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, REVISION_ID, ACTOR, itemTime));

        final List<AuditRecord> records = repository.savedRecords();
        assertEquals(3, records.size());

        final Set<AuditAction> actions = recordActions(records);
        assertTrue(actions.contains(AuditAction.CREATED), "actions should contain CREATED");
        assertTrue(actions.contains(AuditAction.PUBLISHED), "actions should contain PUBLISHED");

        final Set<String> subjectTypes = recordSubjectTypes(records);
        assertTrue(subjectTypes.contains("site"), "subject types should contain site");
        assertTrue(subjectTypes.contains("content-type"), "subject types should contain content-type");
        assertTrue(subjectTypes.contains("content-item"), "subject types should contain content-item");

        records.forEach(r -> assertEquals(ACTOR_ID, r.actorId(), "actorId should match event actor"));

        final Set<Instant> occurredAts = records.stream()
                .map(AuditRecord::occurredAt)
                .collect(Collectors.toSet());
        assertTrue(occurredAts.contains(siteTime), "occurredAt should contain site event time");
        assertTrue(occurredAts.contains(ctTime), "occurredAt should contain content type event time");
        assertTrue(occurredAts.contains(itemTime), "occurredAt should contain content item event time");

        records.forEach(r -> assertFalse(r.summary().isBlank(), "summary should not be blank"));
    }

    // --- unsupported event test ---

    @Test
    void dispatchingUnsupportedEventRecordsNothing() {
        dispatcher.dispatch(new UnsupportedChroniconTestEvent(NOW));
        assertEquals(0, repository.savedRecords().size());
    }

    // --- failure propagation (minimal; full coverage is in LocalCodexEventDispatcherTest) ---

    @Test
    void subscriberFailurePropagatesAndStopsDispatch() {
        final LocalCodexEventDispatcher failingDispatcher = new LocalCodexEventDispatcher(List.of(
                new SiteCreatedChroniconSubscriber(new ThrowingChroniconRepository())));

        assertThrows(RuntimeException.class, () ->
                failingDispatcher.dispatch(new SiteCreatedEvent(SITE_ID, SITE_KEY, ACTOR, NOW)));
    }

    // --- duplicate dispatch behavior (documents current non-idempotent behavior) ---

    @Test
    void duplicateDispatchRecordsSaveTwiceButStoresOne() {
        final SiteCreatedEvent event = new SiteCreatedEvent(SITE_ID, SITE_KEY, ACTOR, NOW);
        dispatcher.dispatch(event);
        dispatcher.dispatch(event);

        // RecordingChroniconRepository records every save() call
        assertEquals(2, repository.savedRecords().size(),
                "save is called twice for duplicate dispatch");

        // MemoryChroniconRepository is id-keyed; second save overwrites the first
        assertEquals(1, repository.findAll().size(),
                "underlying store deduplicates by audit record id");
    }

    // --- focused per-subscriber subject type tests ---

    @Test
    void siteCreatedEventSavesRecordWithSiteSubjectType() {
        dispatcher.dispatch(new SiteCreatedEvent(SITE_ID, SITE_KEY, ACTOR, NOW));
        final AuditRecord record = repository.savedRecords().get(0);
        assertEquals("site", record.subject().type());
        assertEquals(SITE_ID.value(), record.subject().id());
        assertEquals(SITE_KEY.value(), record.subject().key());
    }

    @Test
    void contentTypeCreatedEventSavesRecordWithContentTypeSubjectType() {
        dispatcher.dispatch(new ContentTypeCreatedEvent(CT_ID, SITE_KEY, CT_KEY, ACTOR, NOW));
        final AuditRecord record = repository.savedRecords().get(0);
        assertEquals("content-type", record.subject().type());
        assertEquals(CT_ID.value(), record.subject().id());
        assertEquals(CT_KEY.value(), record.subject().key());
    }

    @Test
    void contentItemPublishedEventSavesRecordWithContentItemSubjectType() {
        dispatcher.dispatch(new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY, REVISION_ID, ACTOR, NOW));
        final AuditRecord record = repository.savedRecords().get(0);
        assertEquals("content-item", record.subject().type());
        assertEquals(ITEM_ID.value(), record.subject().id());
        assertEquals(ITEM_KEY.value(), record.subject().key());
    }

    // --- helpers ---

    private static Set<AuditAction> recordActions(final List<AuditRecord> records) {
        return records.stream().map(AuditRecord::action).collect(Collectors.toSet());
    }

    private static Set<String> recordSubjectTypes(final List<AuditRecord> records) {
        return records.stream().map(r -> r.subject().type()).collect(Collectors.toSet());
    }

    // --- inner types ---

    private record UnsupportedChroniconTestEvent(Instant occurredAt) implements CodexEvent {}

    private static final class ThrowingChroniconRepository implements ChroniconRepository {
        @Override
        public AuditRecord save(final AuditRecord record) {
            throw new RuntimeException("simulated repository failure");
        }

        @Override
        public Optional<AuditRecord> findById(final codex.chronicon.api.AuditRecordId id) {
            return Optional.empty();
        }

        @Override
        public List<AuditRecord> findBySubject(final codex.chronicon.api.AuditSubject subject) {
            return List.of();
        }

        @Override
        public List<AuditRecord> findByActor(final ActorId actorId) {
            return List.of();
        }

        @Override
        public List<AuditRecord> findByAction(final AuditAction action) {
            return List.of();
        }

        @Override
        public List<AuditRecord> findAll() {
            return List.of();
        }
    }
}
