package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.fundamentum.api.model.ActorId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Defines a logical schema for content items in Codex.
 * <p>
 * A {@code ContentType} is scoped by {@link SiteKey}. The logical identity of a content type
 * is the combination of {@code siteKey + key} — a {@code ContentTypeKey} alone is not globally
 * unique. Global (platform-wide) content types use {@link SiteKey#SYSTEM} as their site scope.
 * <p>
 * Its lifecycle follows the {@link ContentTypeStatus} state machine:
 * {@code DRAFT → ACTIVE}, {@code DRAFT → ARCHIVED}, {@code ACTIVE → ARCHIVED}.
 * <p>
 * Fields are stored as {@code Map<FieldKey, Field>} representing the current draft schema.
 * Field modification is only allowed while the status is {@link ContentTypeStatus#DRAFT}.
 */
public record ContentType(
        ContentTypeId id,
        SiteKey siteKey,
        ContentTypeKey key,
        String displayName,
        ContentTypeStatus status,
        ActorId owner,
        ActorId createdBy,
        ActorId updatedBy,
        Instant createdAt,
        Instant updatedAt,
        Map<FieldKey, Field> fields,
        ContentTypeVersionId latestPublishedVersionId,
        Integer latestPublishedVersion
) {

    /**
     * Canonical constructor for {@link ContentType}.
     *
     * @param id          unique identifier; must not be null
     * @param siteKey     owning site scope; must not be null
     * @param key         stable human-friendly key within the site scope; must not be null
     * @param displayName human-readable label; must not be null or blank
     * @param status      lifecycle status; defaults to {@link ContentTypeStatus#DRAFT} if null
     * @param owner       the actor that owns this content type; must not be null
     * @param createdBy   the actor that created this content type; must not be null
     * @param updatedBy   the actor that last updated this content type; must not be null
     * @param createdAt                  creation timestamp; defaults to {@link Instant#now()} if null
     * @param updatedAt                  last-update timestamp; defaults to {@code createdAt} if null
     * @param fields                     field schema; defaults to empty map if null; map keys must match field keys
     * @param latestPublishedVersionId   the id of the latest published version; may be null for unpublished types
     * @param latestPublishedVersion     the latest published version number (>= 1 if set); may be null for unpublished types
     */
    public ContentType {
        Objects.requireNonNull(id, "ContentType id cannot be null");
        Objects.requireNonNull(siteKey, "ContentType siteKey cannot be null");
        Objects.requireNonNull(key, "ContentType key cannot be null");
        Objects.requireNonNull(displayName, "ContentType displayName cannot be null");
        displayName = displayName.trim();
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("ContentType displayName cannot be blank");
        }
        if (status == null) {
            status = ContentTypeStatus.DRAFT;
        }
        Objects.requireNonNull(owner, "ContentType owner cannot be null");
        Objects.requireNonNull(createdBy, "ContentType createdBy cannot be null");
        Objects.requireNonNull(updatedBy, "ContentType updatedBy cannot be null");
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
        if (fields == null) {
            fields = Map.of();
        } else {
            for (final var entry : fields.entrySet()) {
                if (!entry.getKey().equals(entry.getValue().key())) {
                    throw new IllegalArgumentException(
                            "Map key '" + entry.getKey().value() + "' does not match field key '"
                                    + entry.getValue().key().value() + "'");
                }
            }
            fields = Map.copyOf(fields);
        }
        if (latestPublishedVersion != null && latestPublishedVersion < 1) {
            throw new IllegalArgumentException("latestPublishedVersion must be >= 1 when set");
        }
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
                .siteKey(contentType.siteKey())
                .key(contentType.key())
                .displayName(contentType.displayName())
                .status(contentType.status())
                .owner(contentType.owner())
                .createdBy(contentType.createdBy())
                .updatedBy(contentType.updatedBy())
                .createdAt(contentType.createdAt())
                .updatedAt(contentType.updatedAt())
                .fields(contentType.fields())
                .latestPublishedVersionId(contentType.latestPublishedVersionId())
                .latestPublishedVersion(contentType.latestPublishedVersion());
    }

    @Override
    public String toString() {
        return "ContentType{" +
                "id=" + id +
                ", siteKey=" + siteKey +
                ", key=" + key +
                ", displayName='" + displayName + '\'' +
                ", status=" + status +
                ", owner=" + owner +
                ", createdBy=" + createdBy +
                ", updatedBy=" + updatedBy +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", fields=" + fields.size() +
                ", latestPublishedVersionId=" + latestPublishedVersionId +
                ", latestPublishedVersion=" + latestPublishedVersion +
                '}';
    }

    /**
     * Builder for {@link ContentType}.
     */
    public static class Builder {
        private ContentTypeId id;
        private SiteKey siteKey;
        private ContentTypeKey key;
        private String displayName;
        private ContentTypeStatus status;
        private ActorId owner;
        private ActorId createdBy;
        private ActorId updatedBy;
        private Instant createdAt;
        private Instant updatedAt;
        private Map<FieldKey, Field> fields;
        private ContentTypeVersionId latestPublishedVersionId;
        private Integer latestPublishedVersion;

        public Builder id(ContentTypeId id) { this.id = id; return this; }
        public Builder siteKey(SiteKey siteKey) { this.siteKey = siteKey; return this; }
        public Builder key(ContentTypeKey key) { this.key = key; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder status(ContentTypeStatus status) { this.status = status; return this; }
        public Builder owner(ActorId owner) { this.owner = owner; return this; }
        public Builder createdBy(ActorId createdBy) { this.createdBy = createdBy; return this; }
        public Builder updatedBy(ActorId updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }
        public Builder fields(Map<FieldKey, Field> fields) { this.fields = fields; return this; }
        public Builder latestPublishedVersionId(ContentTypeVersionId id) { this.latestPublishedVersionId = id; return this; }
        public Builder latestPublishedVersion(Integer version) { this.latestPublishedVersion = version; return this; }

        /**
         * Builds a new {@link ContentType} instance.
         *
         * @return a validated {@code ContentType}
         */
        public ContentType build() {
            return new ContentType(id, siteKey, key, displayName, status,
                    owner, createdBy, updatedBy, createdAt, updatedAt, fields,
                    latestPublishedVersionId, latestPublishedVersion);
        }
    }
}
