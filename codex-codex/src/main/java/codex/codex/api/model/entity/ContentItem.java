package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.VariantGroupId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.value.Locale;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Represents a content entry in Codex.
 * <p>
 * A content item is specific to a site and a locale, and is linked to a content type.
 */
public record ContentItem(
    ContentItemId id,
    SiteId siteId,
    ContentTypeId contentTypeId,
    ContentTypeVersionId contentTypeVersionId,
    VariantGroupId variantGroupId,
    Locale locale,
    ContentRevisionId currentLiveRevisionId,
    ContentRevisionId currentWorkingRevisionId,
    Map<FieldKey, Object> attributes,
    Instant createdAt
) {
    /**
     * Canonical constructor for ContentItem.
     * 
     * @param id the unique content item identifier, cannot be null
     * @param siteId the owner site identifier, cannot be null
     * @param contentTypeId the identifier of the content type, cannot be null
     * @param contentTypeVersionId the identifier of the current schema version, cannot be null
     * @param variantGroupId the variant group identifier, cannot be null
     * @param locale the locale of this item, cannot be null
     * @param currentLiveRevisionId optional identifier of the live revision
     * @param currentWorkingRevisionId optional identifier of the current working revision
     * @param attributes extensible metadata, defaults to empty map if null
     * @param createdAt the creation timestamp, defaults to now if null
     */
    public ContentItem {
        Objects.requireNonNull(id, "ContentItem id cannot be null");
        Objects.requireNonNull(siteId, "ContentItem siteId cannot be null");
        Objects.requireNonNull(contentTypeId, "ContentItem contentTypeId cannot be null");
        Objects.requireNonNull(contentTypeVersionId, "ContentItem contentTypeVersionId cannot be null");
        Objects.requireNonNull(variantGroupId, "ContentItem variantGroupId cannot be null");
        Objects.requireNonNull(locale, "ContentItem locale cannot be null");
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    @Override
    public String toString() {
        return "ContentItem{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", contentTypeId=" + contentTypeId +
                ", contentTypeVersionId=" + contentTypeVersionId +
                ", variantGroupId=" + variantGroupId +
                ", locale=" + locale +
                ", currentLiveRevisionId=" + currentLiveRevisionId +
                ", currentWorkingRevisionId=" + currentWorkingRevisionId +
                ", attributes=" + attributes +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Creates a new builder for ContentItem.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ContentItem}.
     */
    public static class Builder {
        private ContentItemId id;
        private SiteId siteId;
        private ContentTypeId contentTypeId;
        private ContentTypeVersionId contentTypeVersionId;
        private VariantGroupId variantGroupId;
        private Locale locale;
        private ContentRevisionId currentLiveRevisionId;
        private ContentRevisionId currentWorkingRevisionId;
        private Map<FieldKey, Object> attributes;
        private Instant createdAt;

        public Builder id(ContentItemId id) { this.id = id; return this; }
        public Builder siteId(SiteId siteId) { this.siteId = siteId; return this; }
        public Builder contentTypeId(ContentTypeId contentTypeId) { this.contentTypeId = contentTypeId; return this; }
        public Builder contentTypeVersionId(ContentTypeVersionId contentTypeVersionId) { this.contentTypeVersionId = contentTypeVersionId; return this; }
        public Builder variantGroupId(VariantGroupId variantGroupId) { this.variantGroupId = variantGroupId; return this; }
        public Builder locale(Locale locale) { this.locale = locale; return this; }
        public Builder currentLiveRevisionId(ContentRevisionId currentLiveRevisionId) { this.currentLiveRevisionId = currentLiveRevisionId; return this; }
        public Builder currentWorkingRevisionId(ContentRevisionId currentWorkingRevisionId) { this.currentWorkingRevisionId = currentWorkingRevisionId; return this; }
        public Builder attributes(Map<FieldKey, Object> attributes) { this.attributes = attributes; return this; }

        /**
         * Sets attributes from a map of raw strings.
         * 
         * @param attributes the map of raw attribute strings
         * @return this builder
         */
        public Builder attributesFromStrings(Map<String, Object> attributes) {
            this.attributes = attributes == null ? null : attributes.entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                            entry -> FieldKey.of(entry.getKey()),
                            Map.Entry::getValue
                    ));
            return this;
        }

        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        /**
         * Builds a new ContentItem instance.
         * 
         * @return a new ContentItem instance
         */
        public ContentItem build() {
            return new ContentItem(
                id,
                siteId,
                contentTypeId,
                contentTypeVersionId,
                variantGroupId,
                locale,
                currentLiveRevisionId,
                currentWorkingRevisionId,
                attributes,
                createdAt
            );
        }
    }
}
