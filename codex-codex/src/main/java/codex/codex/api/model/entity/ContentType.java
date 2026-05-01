package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.value.ContentTypeStatus;

import java.time.Instant;
import java.util.Objects;

/**
 * Defines a logical schema for content items in Codex.
 * <p>
 * A {@code ContentType} acts as the root for a set of versioned schema definitions.
 * Its lifecycle follows the {@link ContentTypeStatus} state machine:
 * {@code DRAFT → ACTIVE}, {@code DRAFT → ARCHIVED}, {@code ACTIVE → ARCHIVED}.
 */
public record ContentType(
        ContentTypeId id,
        ContentTypeKey key,
        String displayName,
        ContentTypeStatus status,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Canonical constructor for {@link ContentType}.
     *
     * @param id          unique identifier; must not be null
     * @param key         stable human-friendly key; must not be null
     * @param displayName human-readable label; must not be null or blank
     * @param status      lifecycle status; defaults to {@link ContentTypeStatus#DRAFT} if null
     * @param createdAt   creation timestamp; defaults to {@link Instant#now()} if null
     * @param updatedAt   last-update timestamp; defaults to {@code createdAt} if null
     */
    public ContentType {
        Objects.requireNonNull(id, "ContentType id cannot be null");
        Objects.requireNonNull(key, "ContentType key cannot be null");
        Objects.requireNonNull(displayName, "ContentType displayName cannot be null");
        displayName = displayName.trim();
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("ContentType displayName cannot be blank");
        }
        if (status == null) {
            status = ContentTypeStatus.DRAFT;
        }
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    /**
     * Creates a new {@link Builder} for {@link ContentType}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a pre-populated {@link Builder} from an existing {@link ContentType}.
     * Use this to produce a modified copy without constructing from scratch.
     *
     * @param contentType the source; must not be null
     * @return a builder pre-populated with all fields from {@code contentType}
     */
    public static Builder copyOf(final ContentType contentType) {
        Objects.requireNonNull(contentType, "contentType cannot be null");
        return builder()
                .id(contentType.id())
                .key(contentType.key())
                .displayName(contentType.displayName())
                .status(contentType.status())
                .createdAt(contentType.createdAt())
                .updatedAt(contentType.updatedAt());
    }

    @Override
    public String toString() {
        return "ContentType{" +
                "id=" + id +
                ", key=" + key +
                ", displayName='" + displayName + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    /**
     * Builder for {@link ContentType}.
     */
    public static class Builder {
        private ContentTypeId id;
        private ContentTypeKey key;
        private String displayName;
        private ContentTypeStatus status;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(ContentTypeId id) { this.id = id; return this; }
        public Builder key(ContentTypeKey key) { this.key = key; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder status(ContentTypeStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        /**
         * Builds a new {@link ContentType} instance.
         *
         * @return a validated {@code ContentType}
         */
        public ContentType build() {
            return new ContentType(id, key, displayName, status, createdAt, updatedAt);
        }
    }
}
