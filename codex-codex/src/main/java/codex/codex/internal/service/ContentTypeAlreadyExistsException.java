package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentTypeKey;

/**
 * Thrown when attempting to create a {@link codex.codex.api.model.entity.ContentType}
 * whose key already exists in the repository.
 */
public class ContentTypeAlreadyExistsException extends RuntimeException {

    public ContentTypeAlreadyExistsException(final ContentTypeKey key) {
        super("ContentType already exists: " + key);
    }
}
