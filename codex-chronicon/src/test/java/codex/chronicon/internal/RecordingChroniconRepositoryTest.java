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
 * Tests for {@link RecordingChroniconRepository}.
 */
class RecordingChroniconRepositoryTest {

    private static final AuditSubject SITE_SUBJECT = AuditSubject.of("site", "site-id-1", "acme");
    private static final ActorId ACTOR = ActorId.of("user-1");
    private static final Instant T1 = Instant.parse("2026-05-04T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-05-04T10:00:00Z");

    private RecordingChroniconRepository repository;

    @BeforeEach
    void setUp() {
        repository = new RecordingChroniconRepository();
    }

    private AuditRecord record(final String id, final Instant occurredAt) {
        return AuditRecord.builder()
                .id(AuditRecordId.of(id))
                .action(AuditAction.CREATED)
                .subject(SITE_SUBJECT)
                .actorId(ACTOR)
                .occurredAt(occurredAt)
                .build();
    }

    @Test
    void saveRejectsNull() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
    }

    @Test
    void saveRecordsEntry() {
        repository.save(record("r1", T1));
        assertEquals(1, repository.savedRecords().size());
    }

    @Test
    void savedRecordsSnapshotIsImmutable() {
        repository.save(record("r1", T1));
        assertThrows(UnsupportedOperationException.class,
                () -> repository.savedRecords().clear());
    }

    @Test
    void savedRecordsInInsertionOrder() {
        repository.save(record("r1", T1));
        repository.save(record("r2", T2));
        final var saved = repository.savedRecords();
        assertEquals(AuditRecordId.of("r1"), saved.get(0).id());
        assertEquals(AuditRecordId.of("r2"), saved.get(1).id());
    }

    @Test
    void clearRecordingResetsRecording() {
        repository.save(record("r1", T1));
        repository.clearRecording();
        assertTrue(repository.savedRecords().isEmpty());
    }

    @Test
    void clearRecordingDoesNotRemoveStoredRecords() {
        repository.save(record("r1", T1));
        repository.clearRecording();
        assertTrue(repository.findById(AuditRecordId.of("r1")).isPresent());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void findByIdDelegatesToUnderlyingRepository() {
        repository.save(record("r1", T1));
        assertTrue(repository.findById(AuditRecordId.of("r1")).isPresent());
        assertTrue(repository.findById(AuditRecordId.of("no-such-id")).isEmpty());
    }

    @Test
    void findBySubjectDelegatesToUnderlyingRepository() {
        repository.save(record("r1", T1));
        assertEquals(1, repository.findBySubject(SITE_SUBJECT).size());
    }

    @Test
    void findByActorDelegatesToUnderlyingRepository() {
        repository.save(record("r1", T1));
        assertEquals(1, repository.findByActor(ACTOR).size());
    }

    @Test
    void findByActionDelegatesToUnderlyingRepository() {
        repository.save(record("r1", T1));
        assertEquals(1, repository.findByAction(AuditAction.CREATED).size());
    }

    @Test
    void findAllDelegatesToUnderlyingRepository() {
        repository.save(record("r1", T1));
        repository.save(record("r2", T2));
        assertEquals(2, repository.findAll().size());
    }

    @Test
    void initialStateIsEmpty() {
        assertTrue(repository.savedRecords().isEmpty());
        assertTrue(repository.findAll().isEmpty());
    }
}
