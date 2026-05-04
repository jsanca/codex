package codex.chronicon.api;

import codex.fundamentum.api.model.ActorId;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable record of a domain action — the who, what, on which resource, and when.
 * <p>
 * {@code AuditRecord} is the core projection unit of {@code codex-chronicon}. It is produced
 * by event subscribers listening to domain events emitted by {@code codex-codex} and is
 * stored in a {@link ChroniconRepository}.
 * <p>
 * Records are append-only; they are never updated or deleted after being saved.
 * <p>
 * The {@link #metadata()} map carries optional structured context (e.g. previous status,
 * content type key, revision number). It does not carry full before/after diffs or
 * payload snapshots — those are future work.
 */
public record AuditRecord(
        AuditRecordId id,
        AuditAction action,
        AuditSubject subject,
        ActorId actorId,
        Instant occurredAt,
        String summary,
        Map<String, String> metadata
) {

    /**
     * Canonical constructor for {@link AuditRecord}.
     *
     * @param id          unique identifier; must not be null
     * @param action      the action that occurred; must not be null
     * @param subject     the resource acted upon; must not be null
     * @param actorId     the actor who triggered the action; must not be null
     * @param occurredAt  the instant at which the action occurred; must not be null
     * @param summary     short human-readable description; null defaults to empty string; trimmed
     * @param metadata    optional structured context; null defaults to empty map;
     *                    keys must not be null or blank (trimmed); values must not be null
     */
    public AuditRecord {
        Objects.requireNonNull(id, "AuditRecord id must not be null");
        Objects.requireNonNull(action, "AuditRecord action must not be null");
        Objects.requireNonNull(subject, "AuditRecord subject must not be null");
        Objects.requireNonNull(actorId, "AuditRecord actorId must not be null");
        Objects.requireNonNull(occurredAt, "AuditRecord occurredAt must not be null");
        summary = summary == null ? "" : summary.trim();
        if (metadata == null) {
            metadata = Map.of();
        } else {
            final Map<String, String> validated = new HashMap<>();
            for (final Map.Entry<String, String> entry : metadata.entrySet()) {
                final String rawKey = entry.getKey();
                Objects.requireNonNull(rawKey, "AuditRecord metadata key must not be null");
                final String trimmedKey = rawKey.trim();
                if (trimmedKey.isBlank()) {
                    throw new IllegalArgumentException("AuditRecord metadata key must not be blank");
                }
                Objects.requireNonNull(entry.getValue(), "AuditRecord metadata value must not be null for key: " + trimmedKey);
                validated.put(trimmedKey, entry.getValue());
            }
            metadata = Map.copyOf(validated);
        }
    }

    /**
     * Creates a new {@link Builder} for {@link AuditRecord}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a pre-populated {@link Builder} from an existing {@link AuditRecord}.
     *
     * @param record the source; must not be null
     * @return a builder pre-populated with all fields from {@code record}
     */
    public static Builder copyOf(final AuditRecord record) {
        Objects.requireNonNull(record, "record must not be null");
        return builder()
                .id(record.id())
                .action(record.action())
                .subject(record.subject())
                .actorId(record.actorId())
                .occurredAt(record.occurredAt())
                .summary(record.summary())
                .metadata(record.metadata());
    }

    @Override
    public String toString() {
        return "AuditRecord{" +
                "id=" + id +
                ", action=" + action +
                ", subject=" + subject +
                ", actorId=" + actorId +
                ", occurredAt=" + occurredAt +
                ", summary='" + summary + '\'' +
                ", metadata.size=" + metadata.size() +
                '}';
    }

    /**
     * Builder for {@link AuditRecord}.
     */
    public static final class Builder {
        private AuditRecordId id;
        private AuditAction action;
        private AuditSubject subject;
        private ActorId actorId;
        private Instant occurredAt;
        private String summary;
        private Map<String, String> metadata;

        private Builder() {}

        public Builder id(final AuditRecordId id) { this.id = id; return this; }
        public Builder action(final AuditAction action) { this.action = action; return this; }
        public Builder subject(final AuditSubject subject) { this.subject = subject; return this; }
        public Builder actorId(final ActorId actorId) { this.actorId = actorId; return this; }
        public Builder occurredAt(final Instant occurredAt) { this.occurredAt = occurredAt; return this; }
        public Builder summary(final String summary) { this.summary = summary; return this; }
        public Builder metadata(final Map<String, String> metadata) { this.metadata = metadata; return this; }

        /**
         * Builds a new {@link AuditRecord} instance.
         *
         * @return a validated {@code AuditRecord}
         */
        public AuditRecord build() {
            return new AuditRecord(id, action, subject, actorId, occurredAt, summary, metadata);
        }
    }
}
