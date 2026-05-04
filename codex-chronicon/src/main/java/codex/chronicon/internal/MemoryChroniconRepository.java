package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditRecordId;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.fundamentum.api.model.ActorId;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link ChroniconRepository}.
 * <p>
 * Backed by a {@link ConcurrentHashMap}. Suitable for tests and early development.
 * Not suitable for production persistence — records are lost on restart.
 * <p>
 * Query results are sorted by {@link AuditRecord#occurredAt()}, then by
 * {@link AuditRecord#id()} value for deterministic ordering in tests.
 */
public final class MemoryChroniconRepository implements ChroniconRepository {

    private static final Comparator<AuditRecord> CHRONOLOGICAL =
            Comparator.comparing(AuditRecord::occurredAt)
                    .thenComparing(r -> r.id().value());

    private final Map<AuditRecordId, AuditRecord> store = new ConcurrentHashMap<>();

    @Override
    public AuditRecord save(final AuditRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        store.put(record.id(), record);
        return record;
    }

    @Override
    public Optional<AuditRecord> findById(final AuditRecordId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<AuditRecord> findBySubject(final AuditSubject subject) {
        Objects.requireNonNull(subject, "subject must not be null");
        return store.values().stream()
                .filter(r -> r.subject().equals(subject))
                .sorted(CHRONOLOGICAL)
                .toList();
    }

    @Override
    public List<AuditRecord> findByActor(final ActorId actorId) {
        Objects.requireNonNull(actorId, "actorId must not be null");
        return store.values().stream()
                .filter(r -> r.actorId().equals(actorId))
                .sorted(CHRONOLOGICAL)
                .toList();
    }

    @Override
    public List<AuditRecord> findByAction(final AuditAction action) {
        Objects.requireNonNull(action, "action must not be null");
        return store.values().stream()
                .filter(r -> r.action() == action)
                .sorted(CHRONOLOGICAL)
                .toList();
    }

    @Override
    public List<AuditRecord> findAll() {
        return store.values().stream()
                .sorted(CHRONOLOGICAL)
                .toList();
    }
}
