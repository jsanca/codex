package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;

/**
 * Thrown when a requested {@link codex.codex.api.model.entity.Field} key does not exist
 * in the schema of the target content type.
 */
public class ContentTypeFieldNotFoundException extends RuntimeException {

    public ContentTypeFieldNotFoundException(final FieldKey fieldKey, final ContentTypeKey contentTypeKey) {
        super("Field " + fieldKey.value() + " does not exist in content type " + contentTypeKey.value());
    }
}
