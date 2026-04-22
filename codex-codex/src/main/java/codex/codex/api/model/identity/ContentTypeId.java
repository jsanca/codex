package codex.codex.api.model.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a Content Type.
 */
public record ContentTypeId(String value) {
    /**
     * Canonical constructor for ContentTypeId.
     * 
     * @param value the raw identifier value, cannot be null or blank
     */
    public ContentTypeId {
        Objects.requireNonNull(value, "ContentTypeId value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("ContentTypeId value cannot be blank");
        }
    }

    /**
     * Creates a ContentTypeId from a raw string value.
     * 
     * @param value the raw string value
     * @return a new ContentTypeId instance
     */
    public static ContentTypeId of(String value) {
        return new ContentTypeId(value);
    }

    /**
     * Generates a new ContentTypeId using a random UUID.
     * 
     * @return a new ContentTypeId instance with a random UUID value
     */
    public static ContentTypeId generate() {
        return new ContentTypeId(UUID.randomUUID().toString());
    }
}
