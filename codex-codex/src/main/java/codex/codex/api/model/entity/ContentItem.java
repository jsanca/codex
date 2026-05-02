package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentItemStatus;
import codex.fundamentum.api.model.ActorId;

import java.time.Instant;
import java.util.Objects;

/**
 * The stable logical identity of a content entry in Codex.
 * <p>
 * A {@code ContentItem} belongs to the composite identity: {@code siteKey + contentTypeKey + key}.
 * It references a stable, immutable {@link ContentTypeVersion} via {@link #contentTypeVersionId()},
 * ensuring that field validation happened against a frozen schema snapshot at creation time.
 * <p>
 * Content values are not stored here. They live in {@link ContentRevision}:
 * <ul>
 *   <li>{@link #currentWorkingRevisionId()} — points to the current draft/working revision</li>
 *   <li>{@link #currentPublishedRevisionId()} — points to the live published revision; null for DRAFT items</li>
 * </ul>
 * <p>
 * Creating a content item creates revision {@code 1} as {@code WORKING} and sets
 * {@link #currentWorkingRevisionId()} to that revision. Publish/unpublish behavior is future work.
 */
public record ContentItem(
        ContentItemId id,
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentTypeVersionId contentTypeVersionId,
        ContentItemKey key,
        ContentItemStatus status,
        ContentRevisionId currentWorkingRevisionId,
        ContentRevisionId currentPublishedRevisionId,
        ActorId owner,
        ActorId createdBy,
        ActorId updatedBy,
        Instant createdAt,
        Instant updatedAt
) {

    /**
     * Canonical constructor for {@link ContentItem}.
     *
     * @param id                       unique identifier; must not be null
     * @param siteKey                  the site scope; must not be null
     * @param contentTypeKey           the content type key; must not be null
     * @param contentTypeVersionId     the schema snapshot version; must not be null
     * @param key                      the item key within site+contentType scope; must not be null
     * @param status                   defaults to {@link ContentItemStatus#DRAFT} if null
     * @param currentWorkingRevisionId pointer to the current working revision; may be null during staged construction
     * @param currentPublishedRevisionId pointer to the live published revision; null for DRAFT items
     * @param owner                    the owning actor; must not be null
     * @param createdBy                the creating actor; must not be null
     * @param updatedBy                the last updating actor; must not be null
     * @param createdAt                creation timestamp; defaults to {@link Instant#now()} if null
     * @param updatedAt                update timestamp; defaults to {@code createdAt} if null
     */
    public ContentItem {
        Objects.requireNonNull(id, "ContentItem id cannot be null");
        Objects.requireNonNull(siteKey, "ContentItem siteKey cannot be null");
        Objects.requireNonNull(contentTypeKey, "ContentItem contentTypeKey cannot be null");
        Objects.requireNonNull(contentTypeVersionId, "ContentItem contentTypeVersionId cannot be null");
        Objects.requireNonNull(key, "ContentItem key cannot be null");
        if (status == null) {
            status = ContentItemStatus.DRAFT;
        }
        // currentWorkingRevisionId and currentPublishedRevisionId are intentionally nullable
        Objects.requireNonNull(owner, "ContentItem owner cannot be null");
        Objects.requireNonNull(createdBy, "ContentItem createdBy cannot be null");
        Objects.requireNonNull(updatedBy, "ContentItem updatedBy cannot be null");
        createdAt = createdAt == null ? Instant.now() : createdAt;
        updatedAt = updatedAt == null ? createdAt : updatedAt;
    }

    /**
     * Creates a new {@link Builder} for {@link ContentItem}.
     *
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a pre-populated {@link Builder} from an existing {@link ContentItem}.
     *
     * @param item the source; must not be null
     * @return a builder pre-populated with all fields from {@code item}
     */
    public static Builder copyOf(final ContentItem item) {
        Objects.requireNonNull(item, "item cannot be null");
        return builder()
                .id(item.id())
                .siteKey(item.siteKey())
                .contentTypeKey(item.contentTypeKey())
                .contentTypeVersionId(item.contentTypeVersionId())
                .key(item.key())
                .status(item.status())
                .currentWorkingRevisionId(item.currentWorkingRevisionId())
                .currentPublishedRevisionId(item.currentPublishedRevisionId())
                .owner(item.owner())
                .createdBy(item.createdBy())
                .updatedBy(item.updatedBy())
                .createdAt(item.createdAt())
                .updatedAt(item.updatedAt());
    }

    @Override
    public String toString() {
        return "ContentItem{" +
                "id=" + id +
                ", siteKey=" + siteKey +
                ", contentTypeKey=" + contentTypeKey +
                ", contentTypeVersionId=" + contentTypeVersionId +
                ", key=" + key +
                ", status=" + status +
                ", currentWorkingRevisionId=" + currentWorkingRevisionId +
                ", currentPublishedRevisionId=" + currentPublishedRevisionId +
                ", owner=" + owner +
                ", createdBy=" + createdBy +
                ", updatedBy=" + updatedBy +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    /**
     * Builder for {@link ContentItem}.
     */
    public static class Builder {
        private ContentItemId id;
        private SiteKey siteKey;
        private ContentTypeKey contentTypeKey;
        private ContentTypeVersionId contentTypeVersionId;
        private ContentItemKey key;
        private ContentItemStatus status;
        private ContentRevisionId currentWorkingRevisionId;
        private ContentRevisionId currentPublishedRevisionId;
        private ActorId owner;
        private ActorId createdBy;
        private ActorId updatedBy;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(ContentItemId id) { this.id = id; return this; }
        public Builder siteKey(SiteKey siteKey) { this.siteKey = siteKey; return this; }
        public Builder contentTypeKey(ContentTypeKey contentTypeKey) { this.contentTypeKey = contentTypeKey; return this; }
        public Builder contentTypeVersionId(ContentTypeVersionId contentTypeVersionId) { this.contentTypeVersionId = contentTypeVersionId; return this; }
        public Builder key(ContentItemKey key) { this.key = key; return this; }
        public Builder status(ContentItemStatus status) { this.status = status; return this; }
        public Builder currentWorkingRevisionId(ContentRevisionId id) { this.currentWorkingRevisionId = id; return this; }
        public Builder currentPublishedRevisionId(ContentRevisionId id) { this.currentPublishedRevisionId = id; return this; }
        public Builder owner(ActorId owner) { this.owner = owner; return this; }
        public Builder createdBy(ActorId createdBy) { this.createdBy = createdBy; return this; }
        public Builder updatedBy(ActorId updatedBy) { this.updatedBy = updatedBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder updatedAt(Instant updatedAt) { this.updatedAt = updatedAt; return this; }

        /**
         * Builds a new {@link ContentItem} instance.
         *
         * @return a validated {@code ContentItem}
         */
        public ContentItem build() {
            return new ContentItem(id, siteKey, contentTypeKey, contentTypeVersionId,
                    key, status, currentWorkingRevisionId, currentPublishedRevisionId,
                    owner, createdBy, updatedBy, createdAt, updatedAt);
        }
    }
}
