package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Command expressing intent to restore a content item from {@code ARCHIVED} status back to
 * {@code DRAFT}.
 * <p>
 * The actor is passed separately to the service.
 */
public record RestoreContentItemCommand(
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentItemKey key
) {

    /**
     * Canonical constructor for {@link RestoreContentItemCommand}.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param key            the content item key; must not be null
     */
    public RestoreContentItemCommand {
        Objects.requireNonNull(siteKey, "siteKey cannot be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
    }

    /**
     * Creates a {@link RestoreContentItemCommand} using typed value objects.
     *
     * @param siteKey        the site scope
     * @param contentTypeKey the content type key
     * @param key            the content item key
     * @return a new command instance
     */
    public static RestoreContentItemCommand of(final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final ContentItemKey key) {
        return new RestoreContentItemCommand(siteKey, contentTypeKey, key);
    }

    /**
     * Creates a {@link RestoreContentItemCommand} using raw string values.
     *
     * @param siteKey        the raw site key
     * @param contentTypeKey the raw content type key
     * @param key            the raw content item key
     * @return a new command instance
     */
    public static RestoreContentItemCommand of(final String siteKey,
                                                final String contentTypeKey,
                                                final String key) {
        return new RestoreContentItemCommand(
                SiteKey.of(siteKey),
                ContentTypeKey.of(contentTypeKey),
                ContentItemKey.of(key));
    }
}
