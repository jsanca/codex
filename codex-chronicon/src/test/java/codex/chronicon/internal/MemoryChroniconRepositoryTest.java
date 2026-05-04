package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditRecordId;
import codex.chronicon.api.AuditSubject;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MemoryChroniconRepository}.
 */
class MemoryChroniconRepositoryTest {

    private static final AuditSubject SITE_SUBJECT = AuditSubject.of("site", "site-id-1", "acme");
    private static final AuditSubject ITEM_SUBJECT = AuditSubject.of("content-item", "item-id-1", "my-post");
    private static final ActorId ACTOR_A = ActorId.of("user-1");
    private static final ActorId ACTOR_B = ActorId.of("user-2");

    private MemoryChroniconRepository repository;

    @BeforeEach
    void setUp() {
        repository = new MemoryChroniconRepository();
    }

    private AuditRecord record(final String id, final AuditAction action,
                               final AuditSubject subject, final ActorId actor,
                               final Instant occurredAt) {
        return AuditRecord.builder()
                .id(AuditRecordId.of(id))
                .action(action)
                .subject(subject)
                .actorId(actor)
                .occurredAt(occurredAt)
                .build();
    }

    @Test
    void saveReturnsRecord() {
        final AuditRecord record = record("r1", AuditAction.CREATED, SITE_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T09:00:00Z"));
        assertSame(record, repository.save(record));
    }

    @Test
    void saveRejectsNull() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
    }

    @Test
    void findByIdReturnsSavedRecord() {
        final AuditRecord record = record("r1", AuditAction.CREATED, SITE_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T09:00:00Z"));
        repository.save(record);
        assertTrue(repository.findById(AuditRecordId.of("r1")).isPresent());
        assertEquals(record, repository.findById(AuditRecordId.of("r1")).get());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        assertTrue(repository.findById(AuditRecordId.of("no-such-id")).isEmpty());
    }

    @Test
    void findByIdRejectsNull() {
        assertThrows(NullPointerException.class, () -> repository.findById(null));
    }

    @Test
    void findBySubjectReturnsMatchingRecords() {
        repository.save(record("r1", AuditAction.CREATED, SITE_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T09:00:00Z")));
        repository.save(record("r2", AuditAction.PUBLISHED, ITEM_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T10:00:00Z")));
        final var results = repository.findBySubject(SITE_SUBJECT);
        assertEquals(1, results.size());
        assertEquals(AuditRecordId.of("r1"), results.get(0).id());
    }

    @Test
    void findBySubjectRejectsNull() {
        assertThrows(NullPointerException.class, () -> repository.findBySubject(null));
    }

    @Test
    void findByActorReturnsMatchingRecords() {
        repository.save(record("r1", AuditAction.CREATED, SITE_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T09:00:00Z")));
        repository.save(record("r2", AuditAction.CREATED, ITEM_SUBJECT, ACTOR_B,
                Instant.parse("2026-05-04T10:00:00Z")));
        assertEquals(1, repository.findByActor(ACTOR_A).size());
        assertEquals(1, repository.findByActor(ACTOR_B).size());
    }

    @Test
    void findByActorRejectsNull() {
        assertThrows(NullPointerException.class, () -> repository.findByActor(null));
    }

    @Test
    void findByActionReturnsMatchingRecords() {
        repository.save(record("r1", AuditAction.CREATED, SITE_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T09:00:00Z")));
        repository.save(record("r2", AuditAction.PUBLISHED, ITEM_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T10:00:00Z")));
        assertEquals(1, repository.findByAction(AuditAction.CREATED).size());
        assertEquals(1, repository.findByAction(AuditAction.PUBLISHED).size());
    }

    @Test
    void findByActionRejectsNull() {
        assertThrows(NullPointerException.class, () -> repository.findByAction(null));
    }

    @Test
    void findAllReturnsAllRecords() {
        repository.save(record("r1", AuditAction.CREATED, SITE_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T09:00:00Z")));
        repository.save(record("r2", AuditAction.PUBLISHED, ITEM_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T10:00:00Z")));
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void findAllSortedChronologically() {
        repository.save(record("r2", AuditAction.PUBLISHED, ITEM_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T10:00:00Z")));
        repository.save(record("r1", AuditAction.CREATED, SITE_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T09:00:00Z")));
        final var all = repository.findAll();
        assertEquals(AuditRecordId.of("r1"), all.get(0).id());
        assertEquals(AuditRecordId.of("r2"), all.get(1).id());
    }

    @Test
    void findAllSnapshotIsImmutable() {
        repository.save(record("r1", AuditAction.CREATED, SITE_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T09:00:00Z")));
        assertThrows(UnsupportedOperationException.class,
                () -> repository.findAll().clear());
    }

    @Test
    void saveSameIdOverwritesPreviousRecord() {
        repository.save(record("r1", AuditAction.CREATED, SITE_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T09:00:00Z")));
        final AuditRecord updated = record("r1", AuditAction.UPDATED, SITE_SUBJECT, ACTOR_A,
                Instant.parse("2026-05-04T09:00:00Z"));
        repository.save(updated);
        assertEquals(AuditAction.UPDATED, repository.findById(AuditRecordId.of("r1")).get().action());
        assertEquals(1, repository.findAll().size());
    }
}
