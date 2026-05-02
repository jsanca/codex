package codex.codex.api.model.identity;

import java.util.Objects;

/**
 * Unique identifier for a specific revision of a {@link codex.codex.api.model.entity.ContentItem}.
 * <p>
 * Prefer the deterministic {@link #forRevision(SiteKey, ContentTypeKey, ContentItemKey, int)} factory
 * when a stable, reproducible id is needed.
 * <p>
 * Deterministic id format: {@code content-revision:{siteKey}:{contentTypeKey}:{itemKey}:r{revisionNumber}}
 * <br>Example: {@code content-revision:acme:blog-post:welcome-to-codex:r1}
 */
public record ContentRevisionId(String value) {

    /**
     * Canonical constructor for {@link ContentRevisionId}.
     *
     * @param value the raw identifier value; must not be null or blank
     */
    public ContentRevisionId {
        Objects.requireNonNull(value, "ContentRevisionId value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("ContentRevisionId value cannot be blank");
        }
    }

    /**
     * Creates a {@link ContentRevisionId} from a raw string value.
     *
     * @param value the raw string value
     * @return a new {@code ContentRevisionId} instance
     */
    public static ContentRevisionId of(final String value) {
        return new ContentRevisionId(value);
    }

    /**
     * Creates a deterministic {@link ContentRevisionId} for a given site scope,
     * content type key, content item key, and revision number.
     * <p>
     * Format: {@code content-revision:{siteKey}:{contentTypeKey}:{itemKey}:r{revisionNumber}}
     * <p>
     * Example: {@code content-revision:acme:blog-post:welcome-to-codex:r1}
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param itemKey        the content item key; must not be null
     * @param revisionNumber the revision number; must be >= 1
     * @return a deterministic content revision id
     */
    public static ContentRevisionId forRevision(final SiteKey siteKey,
                                                 final ContentTypeKey contentTypeKey,
                                                 final ContentItemKey itemKey,
                                                 final int revisionNumber) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(itemKey, "itemKey must not be null");
        if (revisionNumber < 1) {
            throw new IllegalArgumentException("revisionNumber must be >= 1");
        }
        return new ContentRevisionId(
                "content-revision:" + siteKey.value() + ":" + contentTypeKey.value()
                        + ":" + itemKey.value() + ":r" + revisionNumber);
    }
}
