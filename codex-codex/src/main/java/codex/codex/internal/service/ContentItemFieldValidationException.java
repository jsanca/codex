package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentTypeKey;

/**
 * Thrown when content item field values fail validation against the referenced
 * {@code ContentTypeVersion.fields}.
 * <p>
 * Current checks: unknown fields and missing required fields.
 * Deeper type and constraint validation will be added in a later task.
 */
public class ContentItemFieldValidationException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message        the validation failure description
     * @param contentTypeKey the content type scope of the failed validation
     */
    public ContentItemFieldValidationException(final String message, final ContentTypeKey contentTypeKey) {
        super(message + " for content type " + contentTypeKey.value());
    }
}
