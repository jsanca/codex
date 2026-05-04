package codex.chronicon.internal;

import codex.chronicon.api.AuditAction;
import codex.chronicon.api.AuditRecord;
import codex.chronicon.api.AuditRecordId;
import codex.chronicon.api.AuditSubject;
import codex.chronicon.api.ChroniconRepository;
import codex.fundamentum.api.model.ActorId;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link ChroniconRepository} that records every call to {@link #save(AuditRecord)}
 * for inspection in tests.
 * <p>
 * All repository operations are delegated to an underlying {@link MemoryChroniconRepository},
 * so find methods work as expected. Only {@code save} calls are recorded.
 * <p>
 * {@link #clearRecording()} clears the save recording only — it does not remove stored records.
 * Thread-safe: {@code save} and snapshot methods synchronize on the recording list.
 */
public final class RecordingChroniconRepository implements ChroniconRepository {

    private final MemoryChroniconRepository delegate = new MemoryChroniconRepository();
    private final List<AuditRecord> recording = new ArrayList<>();

    @Override
    public AuditRecord save(final AuditRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        final AuditRecord saved = delegate.save(record);
        synchronized (recording) {
            recording.add(saved);
        }
        return saved;
    }

    @Override
    public Optional<AuditRecord> findById(final AuditRecordId id) {
        return delegate.findById(id);
    }

    @Override
    public List<AuditRecord> findBySubject(final AuditSubject subject) {
        return delegate.findBySubject(subject);
    }

    @Override
    public List<AuditRecord> findByActor(final ActorId actorId) {
        return delegate.findByActor(actorId);
    }

    @Override
    public List<AuditRecord> findByAction(final AuditAction action) {
        return delegate.findByAction(action);
    }

    @Override
    public List<AuditRecord> findAll() {
        return delegate.findAll();
    }

    /**
     * Returns an immutable snapshot of all audit records passed to {@link #save(AuditRecord)}
     * since construction or the last {@link #clearRecording()}, in insertion order.
     *
     * @return immutable list of saved records; never null
     */
    public List<AuditRecord> savedRecords() {
        synchronized (recording) {
            return List.copyOf(recording);
        }
    }

    /**
     * Clears the save recording without removing stored records from the underlying repository.
     * Subsequent calls to {@link #findAll()} and other find methods still return previously
     * saved records.
     */
    public void clearRecording() {
        synchronized (recording) {
            recording.clear();
        }
    }
}
