package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentTypeVersionStatus;
import codex.fundamentum.api.model.ActorId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable published schema snapshot of a {@link ContentType} at a specific version.
 * <p>
 * {@code ContentTypeVersion} is created when a content type is activated. It captures
 * the exact field schema at that point in time. Later changes to {@link ContentType#fields()}
 * do not affect existing versions.
 * <p>
 * Draft schema lives in {@link ContentType#fields()}. Only published (activated) content
 * types produce a version snapshot.
 * <p>
 * Content items will later reference a {@code ContentTypeVersion} to validate their structure
 * against a stable, immutable schema.
 */
public record ContentTypeVersion(
        ContentTypeVersionId id,
        ContentTypeId contentTypeId,
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        int version,
        Map<FieldKey, Field> fields,
        ContentTypeVersionStatus status,
        ActorId createdBy,
        Instant createdAt
) {

    /**
     * Canonical constructor for {@link ContentTypeVersion}.
     *
     * @param id             unique identifier; must not be null
     * @param contentTypeId  the parent content type identifier; must not be null
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param version        the positive version number; must be >= 1
     * @param fields         the schema snapshot; defaults to empty map if null; map keys must match field keys
     * @param status         lifecycle status; defaults to {@link ContentTypeVersionStatus#PUBLISHED} if null
     * @param createdBy      the actor that created this version; must not be null
     * @param createdAt      creation timestamp; defaults to {@link Instant#now()} if null
     */
    public ContentTypeVersion {
        Objects.requireNonNull(id, "ContentTypeVersion id cannot be null");
        Objects.requireNonNull(contentTypeId, "ContentTypeVersion contentTypeId cannot be null");
        Objects.requireNonNull(siteKey, "ContentTypeVersion siteKey cannot be null");
        Objects.requireNonNull(contentTypeKey, "ContentTypeVersion contentTypeKey cannot be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
        if (fields == null) {
            fields = Map.of();
        } else {
            for (final var entry : fields.entrySet()) {

                Objects.requireNonNull(entry.getKey(), "Field map key cannot be null");
                Objects.requireNonNull(entry.getValue(), "Field map value cannot be null");
                if (!entry.getKey().equals(entry.getValue().key())) {
                    throw new IllegalArgumentException(
                            "Map key '" + entry.getKey().value() + "' does not match field key '"
                                    + entry.getValue().key().value() + "'");
                }
            }
            fields = Map.copyOf(fields);
        }
        if (status == null) {
            status = ContentTypeVersionStatus.PUBLISHED;
        }
        Objects.requireNonNull(createdBy, "ContentTypeVersion createdBy cannot be null");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    /**
     * Creates a new {@link Builder} for {@link ContentTypeVersion}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a pre-populated {@link Builder} from an existing {@link ContentTypeVersion}.
     *
     * @param version the source; must not be null
     * @return a builder pre-populated with all fields from {@code version}
     */
    public static Builder copyOf(final ContentTypeVersion version) {
        Objects.requireNonNull(version, "version cannot be null");
        return builder()
                .id(version.id())
                .contentTypeId(version.contentTypeId())
                .siteKey(version.siteKey())
                .contentTypeKey(version.contentTypeKey())
                .version(version.version())
                .fields(version.fields())
                .status(version.status())
                .createdBy(version.createdBy())
                .createdAt(version.createdAt());
    }

    @Override
    public String toString() {
        return "ContentTypeVersion{" +
                "id=" + id +
                ", contentTypeId=" + contentTypeId +
                ", siteKey=" + siteKey +
                ", contentTypeKey=" + contentTypeKey +
                ", version=" + version +
                ", fields=" + fields.size() +
                ", status=" + status +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Builder for {@link ContentTypeVersion}.
     */
    public static class Builder {
        private ContentTypeVersionId id;
        private ContentTypeId contentTypeId;
        private SiteKey siteKey;
        private ContentTypeKey contentTypeKey;
        private int version;
        private Map<FieldKey, Field> fields;
        private ContentTypeVersionStatus status;
        private ActorId createdBy;
        private Instant createdAt;

        public Builder id(ContentTypeVersionId id) { this.id = id; return this; }
        public Builder contentTypeId(ContentTypeId contentTypeId) { this.contentTypeId = contentTypeId; return this; }
        public Builder siteKey(SiteKey siteKey) { this.siteKey = siteKey; return this; }
        public Builder contentTypeKey(ContentTypeKey contentTypeKey) { this.contentTypeKey = contentTypeKey; return this; }
        public Builder version(int version) { this.version = version; return this; }
        public Builder fields(Map<FieldKey, Field> fields) { this.fields = fields; return this; }
        public Builder status(ContentTypeVersionStatus status) { this.status = status; return this; }
        public Builder createdBy(ActorId createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        /**
         * Builds a new {@link ContentTypeVersion} instance.
         *
         * @return a validated {@code ContentTypeVersion}
         */
        public ContentTypeVersion build() {
            return new ContentTypeVersion(id, contentTypeId, siteKey, contentTypeKey,
                    version, fields, status, createdBy, createdAt);
        }
    }
}
