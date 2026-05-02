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

    /**
     * Creates a deterministic {@code ContentTypeVersionId} for a given site scope,
     * content type key, and version number.
     * <p>
     * Format: {@code content-type-version:{siteKey}:{contentTypeKey}:v{version}}
     * <p>
     * Example: {@code content-type-version:system:blog-post:v1}
     *
     * @param siteKey        the raw site key value; must not be null or blank
     * @param contentTypeKey the raw content type key value; must not be null or blank
     * @param version        the version number; must be >= 1
     * @return a deterministic content type version id
     */
    public static ContentTypeVersionId forVersion(final String siteKey,
                                                   final String contentTypeKey,
                                                   final int version) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
        return new ContentTypeVersionId(
                "content-type-version:" + siteKey + ":" + contentTypeKey + ":v" + version);
    }

    public static ContentTypeVersionId forVersion(
            final SiteKey siteKey,
            final ContentTypeKey contentTypeKey,
            final int version
    ) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        return forVersion(siteKey.value(), contentTypeKey.value(), version);
    }
}
