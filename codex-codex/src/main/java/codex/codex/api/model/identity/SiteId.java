package codex.codex.api.model.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a site (tenant).
 */
public record SiteId(String value) {
    /**
     * Canonical constructor for SiteId.
     * 
     * @param value the raw identifier value, cannot be null or blank
     */
    public SiteId {
        Objects.requireNonNull(value, "SiteId value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("SiteId value cannot be blank");
        }
    }

    /**
     * Creates a SiteId from a raw string value.
     * 
     * @param value the raw string value
     * @return a new SiteId instance
     */
    public static SiteId of(String value) {
        return new SiteId(value);
    }

    /**
     * Generates a new SiteId using a random UUID.
     * 
     * @return a new SiteId instance with a random UUID value
     */
    public static SiteId generate() {
        return new SiteId(UUID.randomUUID().toString());
    }
}
