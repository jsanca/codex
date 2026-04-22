package codex.codex.api.model.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a specific revision of a content item.
 */
public record ContentRevisionId(String value) {
    /**
     * Canonical constructor for ContentRevisionId.
     * 
     * @param value the raw identifier value, cannot be null or blank
     */
    public ContentRevisionId {
        Objects.requireNonNull(value, "ContentRevisionId value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("ContentRevisionId value cannot be blank");
        }
    }

    /**
     * Creates a ContentRevisionId from a raw string value.
     * 
     * @param value the raw string value
     * @return a new ContentRevisionId instance
     */
    public static ContentRevisionId of(String value) {
        return new ContentRevisionId(value);
    }

    /**
     * Generates a new ContentRevisionId using a random UUID.
     * 
     * @return a new ContentRevisionId instance with a random UUID value
     */
    public static ContentRevisionId generate() {
        return new ContentRevisionId(UUID.randomUUID().toString());
    }
}
