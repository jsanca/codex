package codex.codex.api.model.value;

import java.util.Objects;

/**
 * Represents a URL-friendly identifier for content.
 */
public record Slug(String value) {
    /**
     * Canonical constructor for Slug.
     * 
     * @param value the raw slug value, cannot be null or blank
     */
    public Slug {
        Objects.requireNonNull(value, "Slug value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Slug value cannot be blank");
        }
    }

    /**
     * Creates a Slug from a raw string value.
     * 
     * @param value the raw string value
     * @return a new Slug instance
     */
    public static Slug of(String value) {
        return new Slug(value);
    }
}
