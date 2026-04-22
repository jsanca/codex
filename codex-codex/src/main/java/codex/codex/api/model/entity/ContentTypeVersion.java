package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.value.ContentTypeVersionStatus;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Represents a specific version of a content type schema.
 * <p>
 * This record defines the fields that instances of this type must conform to.
 */
public record ContentTypeVersion(
    ContentTypeVersionId id,
    ContentTypeId contentTypeId,
    int version,
    List<Field> fields,
    ContentTypeVersionStatus status,
    Instant createdAt
) {
    /**
     * Canonical constructor for ContentTypeVersion.
     * 
     * @param id the unique content type version identifier, cannot be null
     * @param contentTypeId the identifier of the parent content type, cannot be null
     * @param version the positive version number
     * @param fields the list of field definitions, defaults to empty list if null
     * @param status the lifecycle status, defaults to DRAFT if null
     * @param createdAt the creation timestamp, defaults to now if null
     */
    public ContentTypeVersion {
        Objects.requireNonNull(id, "ContentTypeVersion id cannot be null");
        Objects.requireNonNull(contentTypeId, "ContentTypeVersion contentTypeId cannot be null");
        if (version < 1) {
            throw new IllegalArgumentException("Version must be >= 1");
        }
        fields = fields == null ? List.of() : List.copyOf(fields);
        if (status == null) {
            status = ContentTypeVersionStatus.DRAFT;
        }
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    @Override
    public String toString() {
        return "ContentTypeVersion{" +
                "id=" + id +
                ", contentTypeId=" + contentTypeId +
                ", version=" + version +
                ", fieldKeys=" + fields.stream().map(Field::key).toList() +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Creates a new builder for ContentTypeVersion.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ContentTypeVersion}.
     */
    public static class Builder {
        private ContentTypeVersionId id;
        private ContentTypeId contentTypeId;
        private int version;
        private List<Field> fields;
        private ContentTypeVersionStatus status;
        private Instant createdAt;

        public Builder id(ContentTypeVersionId id) { this.id = id; return this; }
        public Builder contentTypeId(ContentTypeId contentTypeId) { this.contentTypeId = contentTypeId; return this; }
        public Builder version(int version) { this.version = version; return this; }
        public Builder fields(List<Field> fields) { this.fields = fields; return this; }
        public Builder status(ContentTypeVersionStatus status) { this.status = status; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        /**
         * Builds a new ContentTypeVersion instance.
         * 
         * @return a new ContentTypeVersion instance
         */
        public ContentTypeVersion build() {
            return new ContentTypeVersion(id, contentTypeId, version, fields, status, createdAt);
        }
    }
}
