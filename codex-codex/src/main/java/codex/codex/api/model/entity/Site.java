package codex.codex.api.model.entity;

import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.SiteStatus;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a site (tenant) in the Codex platform.
 * <p>
 * A site is a top-level isolation boundary for content, types, and settings.
 */
public record Site(
    SiteId id,
    SiteKey key,
    String displayName,
    Set<SiteAlias> aliases,
    SiteStatus status,
    Map<String, Object> attributes,
    Instant createdAt
) {
    /**
     * Canonical constructor for Site.
     *
     * @param id the unique site identifier, cannot be null
     * @param key the stable human-friendly site key, cannot be null
     * @param displayName the display name for the site, cannot be null or blank
     * @param aliases external resolution aliases, defaults to empty set if null
     * @param status the operational status, defaults to STARTED if null
     * @param attributes extensible metadata, defaults to empty map if null
     * @param createdAt the creation timestamp, defaults to now if null
     */
    public Site {
        Objects.requireNonNull(id, "Site id cannot be null");
        Objects.requireNonNull(key, "Site key cannot be null");

        Objects.requireNonNull(displayName, "Site displayName cannot be null");
        displayName = displayName.trim();
        if (displayName.isBlank()) {
            throw new IllegalArgumentException("Site displayName cannot be blank");
        }

        aliases = aliases == null ? Set.of() : Set.copyOf(aliases);
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        createdAt = createdAt == null ? Instant.now() : createdAt;
        if (status == null) {
            status = SiteStatus.STARTED;
        }
    }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", key=" + key +
                ", displayName='" + displayName + '\'' +
                ", aliases=" + aliases +
                ", status=" + status +
                ", attributes=" + attributes +
                ", createdAt=" + createdAt +
                '}';
    }

    /**
     * Creates a new builder for Site.
     *
     * @return a new Builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Site}.
     */
    public static class Builder {
        private SiteId id;
        private SiteKey key;
        private String displayName;
        private Set<SiteAlias> aliases;
        private SiteStatus status;
        private Map<String, Object> attributes;
        private Instant createdAt;

        public Builder id(SiteId id) { this.id = id; return this; }
        public Builder key(SiteKey key) { this.key = key; return this; }
        public Builder key(String key) { this.key = SiteKey.of(key); return this; }
        public Builder displayName(String displayName) { this.displayName = displayName; return this; }

        public Builder aliases(Set<SiteAlias> aliases) { this.aliases = aliases; return this; }

        /**
         * Sets aliases from a set of raw strings.
         * 
         * @param aliases the set of raw alias strings
         * @return this builder
         */
        public Builder aliasesFromStrings(Set<String> aliases) {
            this.aliases = aliases == null ? null : aliases.stream()
                    .map(SiteAlias::of)
                    .collect(java.util.stream.Collectors.toSet());
            return this;
        }

        public Builder status(SiteStatus status) { this.status = status; return this; }
        public Builder attributes(Map<String, Object> attributes) { this.attributes = attributes; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        /**
         * Builds a new Site instance.
         * 
         * @return a new Site instance
         */
        public Site build() {
            return new Site(id, key, displayName, aliases, status, attributes, createdAt);
        }
    }
}
