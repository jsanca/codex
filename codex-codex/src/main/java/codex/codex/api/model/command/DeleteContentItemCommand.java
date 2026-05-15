package codex.codex.api.model.command;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Command expressing intent to permanently delete a content item.
 * <p>
 * Only items in {@code ARCHIVED} status may be deleted. Items must be archived
 * before they can be deleted, ensuring any publication state is cleared first.
 * <p>
 * The actor is passed separately to the service.
 */
public record DeleteContentItemCommand(
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentItemKey key
) {

    /**
     * Canonical constructor for {@link DeleteContentItemCommand}.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param key            the content item key; must not be null
     */
    public DeleteContentItemCommand {
        Objects.requireNonNull(siteKey, "siteKey cannot be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey cannot be null");
        Objects.requireNonNull(key, "key cannot be null");
    }

    /**
     * Creates a {@link DeleteContentItemCommand} using typed value objects.
     *
     * @param siteKey        the site scope
     * @param contentTypeKey the content type key
     * @param key            the content item key
     * @return a new command instance
     */
    public static DeleteContentItemCommand of(final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final ContentItemKey key) {
        return new DeleteContentItemCommand(siteKey, contentTypeKey, key);
    }

    /**
     * Creates a {@link DeleteContentItemCommand} using raw string values.
     *
     * @param siteKey        the raw site key
     * @param contentTypeKey the raw content type key
     * @param key            the raw content item key
     * @return a new command instance
     */
    public static DeleteContentItemCommand of(final String siteKey,
                                               final String contentTypeKey,
                                               final String key) {
        return new DeleteContentItemCommand(
                SiteKey.of(siteKey),
                ContentTypeKey.of(contentTypeKey),
                ContentItemKey.of(key));
    }
}
