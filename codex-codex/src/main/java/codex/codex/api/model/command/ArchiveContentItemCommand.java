package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Command expressing intent to archive a content item, transitioning it to
 * {@code ARCHIVED} status from either {@code DRAFT} or {@code PUBLISHED}.
 * <p>
 * The actor is passed separately to the service.
 */
public record ArchiveContentItemCommand(
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentItemKey key
) {

    /**
     * Canonical constructor for {@link ArchiveContentItemCommand}.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param key            the content item key; must not be null
     */
    public ArchiveContentItemCommand {
        Objects.requireNonNull(siteKey, "siteKey cannot be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
    }

    /**
     * Creates an {@link ArchiveContentItemCommand} using typed value objects.
     *
     * @param siteKey        the site scope
     * @param contentTypeKey the content type key
     * @param key            the content item key
     * @return a new command instance
     */
    public static ArchiveContentItemCommand of(final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final ContentItemKey key) {
        return new ArchiveContentItemCommand(siteKey, contentTypeKey, key);
    }

    /**
     * Creates an {@link ArchiveContentItemCommand} using raw string values.
     *
     * @param siteKey        the raw site key
     * @param contentTypeKey the raw content type key
     * @param key            the raw content item key
     * @return a new command instance
     */
    public static ArchiveContentItemCommand of(final String siteKey,
                                                final String contentTypeKey,
                                                final String key) {
        return new ArchiveContentItemCommand(
                SiteKey.of(siteKey),
                ContentTypeKey.of(contentTypeKey),
                ContentItemKey.of(key));
    }
}
