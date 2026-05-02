package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

/**
 * Thrown when attempting to create a content item whose key already exists
 * within the given site and content type scope.
 */
public class ContentItemAlreadyExistsException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param key            the duplicate item key
     * @param contentTypeKey the content type scope
     * @param siteKey        the site scope
     */
    public ContentItemAlreadyExistsException(final ContentItemKey key,
                                              final ContentTypeKey contentTypeKey,
                                              final SiteKey siteKey) {
        super("Content item " + key.value() + " already exists for content type "
                + contentTypeKey.value() + " in site " + siteKey.value());
    }
}
