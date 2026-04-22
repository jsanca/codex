package codex.codex.api.model.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a content item variant (specific to a locale).
 */
public record ContentItemId(String value) {
    /**
     * Canonical constructor for ContentItemId.
     * 
     * @param value the raw identifier value, cannot be null or blank
     */
    public ContentItemId {
        Objects.requireNonNull(value, "ContentItemId value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("ContentItemId value cannot be blank");
        }
    }

    /**
     * Creates a ContentItemId from a raw string value.
     * 
     * @param value the raw string value
     * @return a new ContentItemId instance
     */
    public static ContentItemId of(String value) {
        return new ContentItemId(value);
    }

    /**
     * Generates a new ContentItemId using a random UUID.
     * 
     * @return a new ContentItemId instance with a random UUID value
     */
    public static ContentItemId generate() {
        return new ContentItemId(UUID.randomUUID().toString());
    }
}
