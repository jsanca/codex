package codex.codex.api.model.entity;

import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.FieldKey;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Defines a logical type of content in Codex.
 * <p>
 * ContentType acts as the root for a set of versioned schema definitions.
 */
public record ContentType(
    ContentTypeId id,
    SiteId siteId,
    String key,
    String displayName,
    String description,
    Map<FieldKey, Object> attributes,
    Instant createdAt
) {
    /**
     * Canonical constructor for ContentType.
     * 
     * @param id the unique content type identifier, cannot be null
     * @param siteId the owner site identifier, cannot be null
     * @param key the stable unique key within the site, cannot be null or blank
     * @param displayName the display name for the type, cannot be null or blank
     * @param description optional description
     * @param attributes extensible metadata, defaults to empty map if null
     * @param createdAt the creation timestamp, defaults to now if null
     */
    public ContentType {
        Objects.requireNonNull(id, "ContentType id cannot be null");
        Objects.requireNonNull(siteId, "ContentType siteId cannot be null");
        Objects.requireNonNull(key, "ContentType key cannot be null");
        key = key.trim();
        if (key.isBlank()) {
            throw new IllegalArgumentException("ContentType key cannot be blank");
        }

        Objects.requireNonNull(displayName, "ContentType displayName cannot be null");
        displayName = displayName.trim();
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("ContentType displayName cannot be blank");
        }

        if (description != null) {
            description = description.trim();
            if (description.isBlank()) {
                description = null;
            }
        }

        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    @Override
    public String toString() {
        return "ContentType{" +
                "id=" + id +
                ", siteId=" + siteId +
                ", key='" + key + '\'' +
                ", displayName='" + displayName + '\'' +
                ", description='" + description + '\'' +
                ", attributes=" + attributes +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Creates a new builder for ContentType.
     * 
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link ContentType}.
     */
    public static class Builder {
        private ContentTypeId id;
        private SiteId siteId;
        private String key;
        private String displayName;
        private String description;
        private Map<FieldKey, Object> attributes;
        private Instant createdAt;

        public Builder id(ContentTypeId id) { this.id = id; return this; }
        public Builder siteId(SiteId siteId) { this.siteId = siteId; return this; }
        public Builder key(String key) { this.key = key; return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }
        public Builder description(String description) { this.description = description; return this; }

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
         * Builds a new ContentType instance.
         * 
         * @return a new ContentType instance
         */
        public ContentType build() {
            return new ContentType(id, siteId, key, displayName, description, attributes, createdAt);
        }
    }
}
