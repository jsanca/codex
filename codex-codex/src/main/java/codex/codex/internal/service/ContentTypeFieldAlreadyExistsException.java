package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;

/**
 * Thrown when attempting to add a {@link codex.codex.api.model.entity.Field} whose
 * key already exists in the schema of the target content type.
 */
public class ContentTypeFieldAlreadyExistsException extends RuntimeException {

    public ContentTypeFieldAlreadyExistsException(final FieldKey fieldKey, final ContentTypeKey contentTypeKey) {
        super("Field " + fieldKey.value() + " already exists in content type " + contentTypeKey.value());
    }
}
