package codex.chronicon.api;

import codex.fundamentum.api.model.ActorId;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for {@link AuditRecord} entities.
 * <p>
 * Audit records are append-only. There is no update or delete operation.
 * All query results are immutable snapshots sorted chronologically by
 * {@link AuditRecord#occurredAt()}.
 * <p>
 * Implementations must validate all arguments and reject {@code null} inputs.
 */
public interface ChroniconRepository {

    /**
     * Persists an audit record.
     *
     * @param record the audit record to save; must not be null
     * @return the saved audit record
     * @throws NullPointerException if {@code record} is null
     */
    AuditRecord save(AuditRecord record);

    /**
     * Finds an audit record by its unique identifier.
     *
     * @param id the record identifier; must not be null
     * @return the audit record wrapped in an {@link Optional}, or empty if not found
     * @throws NullPointerException if {@code id} is null
     */
    Optional<AuditRecord> findById(AuditRecordId id);

    /**
     * Returns all audit records for a given subject, sorted chronologically.
     *
     * @param subject the audit subject to filter by; must not be null
     * @return immutable list of matching records; never null
     * @throws NullPointerException if {@code subject} is null
     */
    List<AuditRecord> findBySubject(AuditSubject subject);

    /**
     * Returns all audit records attributed to a given actor, sorted chronologically.
     *
     * @param actorId the actor to filter by; must not be null
     * @return immutable list of matching records; never null
     * @throws NullPointerException if {@code actorId} is null
     */
    List<AuditRecord> findByActor(ActorId actorId);

    /**
     * Returns all audit records for a given action, sorted chronologically.
     *
     * @param action the action to filter by; must not be null
     * @return immutable list of matching records; never null
     * @throws NullPointerException if {@code action} is null
     */
    List<AuditRecord> findByAction(AuditAction action);

    /**
     * Returns all audit records sorted chronologically.
     *
     * @return immutable list of all records; never null
     */
    List<AuditRecord> findAll();
}
