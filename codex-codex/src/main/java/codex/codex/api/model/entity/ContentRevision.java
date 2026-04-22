package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.value.EditorialState;
import codex.fundamentum.api.model.ActorId;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a specific revision of a content item.
 * <p>
 * A revision captures the snapshot of data, editorial state, and audit information.
 */
public record ContentRevision(
    ContentRevisionId id,
    ContentItemId contentItemId,
    int revisionNumber,
    String title,
    Set<String> tags,
    Map<FieldKey, Object> data,
    EditorialState state,
    ActorId createdBy,
    Instant createdAt,
    ContentRevisionId sourceRevisionId,
    String changeSummary,
    String comment
) {
    /**
     * Canonical constructor for ContentRevision.
     * 
     * @param id the unique revision identifier, cannot be null
     * @param contentItemId the identifier of the parent content item, cannot be null
     * @param revisionNumber the positive revision number
     * @param title optional title for this revision
     * @param tags optional set of tags
     * @param data the snapshot of field data, defaults to empty map if null
     * @param state the editorial state, defaults to DRAFT if null
     * @param createdBy the identifier of the actor who created this revision
     * @param createdAt the creation timestamp, defaults to now if null
     * @param sourceRevisionId optional identifier of the source revision this was based on
     * @param changeSummary optional summary of changes
     * @param comment optional internal comment
     */
    public ContentRevision {
        Objects.requireNonNull(id, "ContentRevision id cannot be null");
        Objects.requireNonNull(contentItemId, "ContentRevision contentItemId cannot be null");
        if (revisionNumber < 1) {
            throw new IllegalArgumentException("Revision number must be >= 1");
        }

        if (title != null) {
            title = title.trim();
            if (title.isBlank()) {
                title = null;
            }
        }

        tags = tags == null ? Set.of() : Collections.unmodifiableSet(new LinkedHashSet<>(tags));
        data = data == null ? Map.of() : Map.copyOf(data);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        if (state == null) {
            state = EditorialState.DRAFT;
        }
        if (changeSummary != null) {
            changeSummary = changeSummary.trim();
        }
        if (comment != null) {
            comment = comment.trim();
        }
    }

    @Override
    public String toString() {
        return "ContentRevision{" +
                "id=" + id +
                ", contentItemId=" + contentItemId +
                ", revisionNumber=" + revisionNumber +
                ", title='" + title + '\'' +
                ", tags=" + tags +
                ", data=" + data +
                ", state=" + state +
                ", createdBy=" + createdBy +
                ", createdAt=" + createdAt +
                ", sourceRevisionId=" + sourceRevisionId +
                ", changeSummary='" + changeSummary + '\'' +
                '}';
    }

    /**
     * Creates a new builder for ContentRevision.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ContentRevision}.
     */
    public static class Builder {
        private ContentRevisionId id;
        private ContentItemId contentItemId;
        private int revisionNumber;
        private String title;
        private Set<String> tags;
        private Map<FieldKey, Object> data;
        private EditorialState state;
        private ActorId createdBy;
        private Instant createdAt;
        private ContentRevisionId sourceRevisionId;
        private String changeSummary;
        private String comment;

        public Builder id(ContentRevisionId id) { this.id = id; return this; }
        public Builder contentItemId(ContentItemId contentItemId) { this.contentItemId = contentItemId; return this; }
        public Builder revisionNumber(int revisionNumber) { this.revisionNumber = revisionNumber; return this; }
        public Builder title(String title) { this.title = title; return this; }
        public Builder tags(Set<String> tags) { this.tags = tags; return this; }
        public Builder data(Map<FieldKey, Object> data) { this.data = data; return this; }

        /**
         * Sets data from a map of raw strings.
         * 
         * @param data the map of raw data strings
         * @return this builder
         */
        public Builder dataFromStrings(Map<String, Object> data) {
            this.data = data == null ? null : data.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> FieldKey.of(entry.getKey()),
                            Map.Entry::getValue
                    ));
            return this;
        }

        public Builder state(EditorialState state) { this.state = state; return this; }
        public Builder createdBy(ActorId createdBy) { this.createdBy = createdBy; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }
        public Builder sourceRevisionId(ContentRevisionId sourceRevisionId) { this.sourceRevisionId = sourceRevisionId; return this; }
        public Builder changeSummary(String changeSummary) { this.changeSummary = changeSummary; return this; }
        public Builder comment(String comment) { this.comment = comment; return this; }

        /**
         * Builds a new ContentRevision instance.
         * 
         * @return a new ContentRevision instance
         */
        public ContentRevision build() {
            return new ContentRevision(id, contentItemId, revisionNumber, title, tags, data, state, createdBy, createdAt, sourceRevisionId, changeSummary, comment);
        }
    }
}
