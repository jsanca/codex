package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Command expressing intent to publish a content item's current working revision.
 * <p>
 * Publishing is pointer-based: no values are copied into {@code ContentItem}.
 * The service resolves the current working revision from the item and transitions it
 * to {@code PUBLISHED} status. The actor is passed separately to the service.
 * <p>
 * For this first foundation, publish always operates on the current working revision.
 * Future commands may target a specific revision.
 */
public record PublishContentItemCommand(
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentItemKey key
) {

    /**
     * Canonical constructor for {@link PublishContentItemCommand}.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param key            the content item key; must not be null
     */
    public PublishContentItemCommand {
        Objects.requireNonNull(siteKey, "siteKey cannot be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
    }

    /**
     * Creates a {@link PublishContentItemCommand} using typed value objects.
     *
     * @param siteKey        the site scope
     * @param contentTypeKey the content type key
     * @param key            the content item key
     * @return a new command instance
     */
    public static PublishContentItemCommand of(final SiteKey siteKey,
                                                final ContentTypeKey contentTypeKey,
                                                final ContentItemKey key) {
        return new PublishContentItemCommand(siteKey, contentTypeKey, key);
    }

    /**
     * Creates a {@link PublishContentItemCommand} using raw string values.
     *
     * @param siteKey        the raw site key
     * @param contentTypeKey the raw content type key
     * @param key            the raw content item key
     * @return a new command instance
     */
    public static PublishContentItemCommand of(final String siteKey,
                                                final String contentTypeKey,
                                                final String key) {
        return new PublishContentItemCommand(
                SiteKey.of(siteKey),
                ContentTypeKey.of(contentTypeKey),
                ContentItemKey.of(key));
    }
}
