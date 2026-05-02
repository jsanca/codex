package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.fundamentum.api.model.ActorId;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * An immutable snapshot of a {@link ContentItem}'s field values at a specific point in time.
 * <p>
 * {@code ContentRevision} separates the stable identity of content ({@link ContentItem}) from
 * the historical value snapshots. Every edit to a content item creates a new revision; revisions
 * are append-only and never mutated once saved.
 * <p>
 * Values are validated on creation against the referenced {@link ContentTypeVersion}'s fields.
 * The first revision created for a new content item has {@link #revisionNumber()} {@code 1} and
 * status {@link ContentRevisionStatus#WORKING}.
 * <p>
 * Publish/unpublish transitions and revision history operations are future work.
 */
public record ContentRevision(
        ContentRevisionId id,
        ContentItemId contentItemId,
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentTypeVersionId contentTypeVersionId,
        ContentItemKey contentItemKey,
        int revisionNumber,
        ContentRevisionStatus status,
        Map<FieldKey, Object> values,
        ActorId createdBy,
        Instant createdAt
) {

    /**
     * Canonical constructor for {@link ContentRevision}.
     *
     * @param id                   unique identifier; must not be null
     * @param contentItemId        the parent content item; must not be null
     * @param siteKey              the site scope; must not be null
     * @param contentTypeKey       the content type key; must not be null
     * @param contentTypeVersionId the schema snapshot the values were validated against; must not be null
     * @param contentItemKey       the content item key; must not be null
     * @param revisionNumber       the revision sequence number; must be >= 1
     * @param status               defaults to {@link ContentRevisionStatus#WORKING} if null
     * @param values               field values; defaults to empty map if null; null keys/values rejected
     * @param createdBy            the creating actor; must not be null
     * @param createdAt            creation timestamp; defaults to {@link Instant#now()} if null
     */
    public ContentRevision {
        Objects.requireNonNull(id, "ContentRevision id cannot be null");
        Objects.requireNonNull(contentItemId, "ContentRevision contentItemId cannot be null");
        Objects.requireNonNull(siteKey, "ContentRevision siteKey cannot be null");
        Objects.requireNonNull(contentTypeKey, "ContentRevision contentTypeKey cannot be null");
        Objects.requireNonNull(contentTypeVersionId, "ContentRevision contentTypeVersionId cannot be null");
        Objects.requireNonNull(contentItemKey, "ContentRevision contentItemKey cannot be null");
        if (revisionNumber < 1) {
            throw new IllegalArgumentException("revisionNumber must be >= 1");
        }
        if (status == null) {
            status = ContentRevisionStatus.WORKING;
        }
        if (values == null) {
            values = Map.of();
        } else {
            for (final var entry : values.entrySet()) {
                Objects.requireNonNull(entry.getKey(), "values map key cannot be null");
                Objects.requireNonNull(entry.getValue(), "values map value cannot be null");
            }
            values = Map.copyOf(values);
        }
        Objects.requireNonNull(createdBy, "ContentRevision createdBy cannot be null");
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    /**
     * Creates a new {@link Builder} for {@link ContentRevision}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a pre-populated {@link Builder} from an existing {@link ContentRevision}.
     *
     * @param revision the source; must not be null
     * @return a builder pre-populated with all fields from {@code revision}
     */
    public static Builder copyOf(final ContentRevision revision) {
        Objects.requireNonNull(revision, "revision cannot be null");
        return builder()
                .id(revision.id())
                .contentItemId(revision.contentItemId())
                .siteKey(revision.siteKey())
                .contentTypeKey(revision.contentTypeKey())
                .contentTypeVersionId(revision.contentTypeVersionId())
                .contentItemKey(revision.contentItemKey())
                .revisionNumber(revision.revisionNumber())
                .status(revision.status())
                .values(revision.values())
                .createdBy(revision.createdBy())
                .createdAt(revision.createdAt());
    }

    @Override
    public String toString() {
        return "ContentRevision{" +
                "id=" + id +
                ", contentItemId=" + contentItemId +
                ", siteKey=" + siteKey +
                ", contentTypeKey=" + contentTypeKey +
                ", contentTypeVersionId=" + contentTypeVersionId +
                ", contentItemKey=" + contentItemKey +
                ", revisionNumber=" + revisionNumber +
                ", status=" + status +
                ", values=" + values.size() +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Builder for {@link ContentRevision}.
     */
    public static class Builder {
        private ContentRevisionId id;
        private ContentItemId contentItemId;
        private SiteKey siteKey;
        private ContentTypeKey contentTypeKey;
        private ContentTypeVersionId contentTypeVersionId;
        private ContentItemKey contentItemKey;
        private int revisionNumber;
        private ContentRevisionStatus status;
        private Map<FieldKey, Object> values;
        private ActorId createdBy;
        private Instant createdAt;

        public Builder id(ContentRevisionId id) { this.id = id; return this; }
        public Builder contentItemId(ContentItemId contentItemId) { this.contentItemId = contentItemId; return this; }
        public Builder siteKey(SiteKey siteKey) { this.siteKey = siteKey; return this; }
        public Builder contentTypeKey(ContentTypeKey contentTypeKey) { this.contentTypeKey = contentTypeKey; return this; }
        public Builder contentTypeVersionId(ContentTypeVersionId contentTypeVersionId) { this.contentTypeVersionId = contentTypeVersionId; return this; }
        public Builder contentItemKey(ContentItemKey contentItemKey) { this.contentItemKey = contentItemKey; return this; }
        public Builder revisionNumber(int revisionNumber) { this.revisionNumber = revisionNumber; return this; }
        public Builder status(ContentRevisionStatus status) { this.status = status; return this; }
        public Builder values(Map<FieldKey, Object> values) { this.values = values; return this; }
        public Builder createdBy(ActorId createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        /**
         * Builds a new {@link ContentRevision} instance.
         *
         * @return a validated {@code ContentRevision}
         */
        public ContentRevision build() {
            return new ContentRevision(id, contentItemId, siteKey, contentTypeKey,
                    contentTypeVersionId, contentItemKey, revisionNumber, status,
                    values, createdBy, createdAt);
        }
    }
}
