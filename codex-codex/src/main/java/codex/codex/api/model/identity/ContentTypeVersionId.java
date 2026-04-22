package codex.codex.api.model.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a specific version of a content type definition.
 */
public record ContentTypeVersionId(String value) {
    /**
     * Canonical constructor for ContentTypeVersionId.
     * 
     * @param value the raw identifier value, cannot be null or blank
     */
    public ContentTypeVersionId {
        Objects.requireNonNull(value, "ContentTypeVersionId value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("ContentTypeVersionId value cannot be blank");
        }
    }

    /**
     * Creates a ContentTypeVersionId from a raw string value.
     * 
     * @param value the raw string value
     * @return a new ContentTypeVersionId instance
     */
    public static ContentTypeVersionId of(String value) {
        return new ContentTypeVersionId(value);
    }

    /**
     * Generates a new ContentTypeVersionId using a random UUID.
     * 
     * @return a new ContentTypeVersionId instance with a random UUID value
     */
    public static ContentTypeVersionId generate() {
        return new ContentTypeVersionId(UUID.randomUUID().toString());
    }
}
