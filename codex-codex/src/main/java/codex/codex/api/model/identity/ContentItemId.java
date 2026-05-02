package codex.codex.api.model.identity;

import java.util.Objects;
import java.util.UUID;

/**
 * Unique identifier for a {@link codex.codex.api.model.entity.ContentItem}.
 * <p>
 * Prefer the deterministic {@link #forItem(SiteKey, ContentTypeKey, ContentItemKey)} factory
 * when a stable, reproducible id is needed. The logical identity of a content item is
 * {@code siteKey + contentTypeKey + contentItemKey}.
 * <p>
 * Deterministic id format: {@code content-item:{siteKey}:{contentTypeKey}:{itemKey}}
 * <br>Example: {@code content-item:acme:blog-post:welcome-to-codex}
 */
public record ContentItemId(String value) {

    /**
     * Canonical constructor for {@link ContentItemId}.
     *
     * @param value the raw identifier value; must not be null or blank
     */
    public ContentItemId {
        Objects.requireNonNull(value, "ContentItemId value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("ContentItemId value cannot be blank");
        }
    }

    /**
     * Creates a {@link ContentItemId} from a raw string value.
     *
     * @param value the raw string value
     * @return a new {@code ContentItemId} instance
     */
    public static ContentItemId of(final String value) {
        return new ContentItemId(value);
    }

    /**
     * Creates a deterministic {@link ContentItemId} for a given site scope,
     * content type key, and item key.
     * <p>
     * Format: {@code content-item:{siteKey}:{contentTypeKey}:{itemKey}}
     * <p>
     * Example: {@code content-item:acme:blog-post:welcome-to-codex}
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param itemKey        the content item key; must not be null
     * @return a deterministic content item id
     */
    public static ContentItemId forItem(final SiteKey siteKey,
                                        final ContentTypeKey contentTypeKey,
                                        final ContentItemKey itemKey) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(itemKey, "itemKey must not be null");
        return new ContentItemId(
                "content-item:" + siteKey.value() + ":" + contentTypeKey.value() + ":" + itemKey.value());
    }

    /**
     * Generates a new {@link ContentItemId} using a random UUID.
     *
     * @return a new {@code ContentItemId} instance with a random UUID value
     */
    public static ContentItemId generate() {
        return new ContentItemId(UUID.randomUUID().toString());
    }
}
