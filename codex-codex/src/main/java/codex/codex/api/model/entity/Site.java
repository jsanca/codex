package codex.codex.api.model.entity;

import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.SiteStatus;
import codex.fundamentum.api.lifecycle.LifecycleParticipation;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Represents a site (tenant) in the Codex platform.
 * <p>
 * A site is a top-level isolation boundary for content, types, and settings.
 * Each site carries a {@link LifecycleParticipation} that governs whether
 * normal lifecycle operations (start, suspend, archive, unarchive) are permitted.
 * Regular user-created sites default to {@link LifecycleParticipation#MANAGED}.
 * The built-in platform site is obtained via {@link #system()}.
 */
public record Site(
        SiteId id,
        SiteKey key,
        String displayName,
        Set<SiteAlias> aliases,
        SiteStatus status,
        LifecycleParticipation lifecycleParticipation,
        Map<String, Object> attributes,
        Instant createdAt
) {

    /**
     * Canonical constructor for Site.
     *
     * @param id                   the unique site identifier, cannot be null
     * @param key                  the stable human-friendly site key, cannot be null
     * @param displayName          the display name for the site, cannot be null or blank
     * @param aliases              external resolution aliases, defaults to empty set if null
     * @param status               the operational status, defaults to {@link SiteStatus#STARTED} if null
     * @param lifecycleParticipation lifecycle model, defaults to {@link LifecycleParticipation#MANAGED} if null
     * @param attributes           extensible metadata, defaults to empty map if null
     * @param createdAt            the creation timestamp, defaults to now if null
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
        if (status == null) {
            status = SiteStatus.STARTED;
        }
        if (lifecycleParticipation == null) {
            lifecycleParticipation = LifecycleParticipation.MANAGED;
        }
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
        createdAt = createdAt == null ? Instant.now() : createdAt;
    }

    /**
     * Returns a new {@link Site} instance representing the built-in platform system site.
     * <p>
     * The system site uses {@link SiteKey#SYSTEM}, is always {@link SiteStatus#STARTED},
     * and participates as {@link LifecycleParticipation#SYSTEM_MANAGED} — meaning normal
     * user-facing lifecycle operations are rejected for it.
     *
     * @return a new system site instance; never null
     */
    public static Site system() {
        return builder()
                .id(SiteId.system())
                .key(SiteKey.SYSTEM)
                .displayName("System")
                .status(SiteStatus.STARTED)
                .lifecycleParticipation(LifecycleParticipation.SYSTEM_MANAGED)
                .build();
    }

    @Override
    public String toString() {
        return "Site{" +
                "id=" + id +
                ", key=" + key +
                ", displayName='" + displayName + '\'' +
                ", aliases=" + aliases +
                ", status=" + status +
                ", lifecycleParticipation=" + lifecycleParticipation +
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
     * Creates a pre-populated builder from an existing site.
     * All fields including {@link LifecycleParticipation} are preserved.
     *
     * @param site the source site; must not be null
     * @return a builder pre-populated with all fields from {@code site}
     */
    public static Builder copyOf(final Site site) {
        Objects.requireNonNull(site, "site cannot be null");
        return builder()
                .id(site.id())
                .key(site.key())
                .displayName(site.displayName())
                .aliases(site.aliases())
                .status(site.status())
                .lifecycleParticipation(site.lifecycleParticipation())
                .attributes(site.attributes())
                .createdAt(site.createdAt());
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
        private LifecycleParticipation lifecycleParticipation;
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
        public Builder lifecycleParticipation(LifecycleParticipation lifecycleParticipation) {
            this.lifecycleParticipation = lifecycleParticipation;
            return this;
        }
        public Builder attributes(Map<String, Object> attributes) { this.attributes = attributes; return this; }
        public Builder createdAt(Instant createdAt) { this.createdAt = createdAt; return this; }

        /**
         * Builds a new Site instance.
         *
         * @return a new Site instance
         */
        public Site build() {
            return new Site(id, key, displayName, aliases, status, lifecycleParticipation, attributes, createdAt);
        }
    }
}
