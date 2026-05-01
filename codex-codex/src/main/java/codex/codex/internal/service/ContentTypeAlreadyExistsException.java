package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

/**
 * Thrown when attempting to create a {@link codex.codex.api.model.entity.ContentType}
 * whose {@code siteKey + key} identity already exists in the repository.
 */
public class ContentTypeAlreadyExistsException extends RuntimeException {

    public ContentTypeAlreadyExistsException(final SiteKey siteKey, final ContentTypeKey key) {
        super("ContentType already exists: " + siteKey + "/" + key);
    }
}
