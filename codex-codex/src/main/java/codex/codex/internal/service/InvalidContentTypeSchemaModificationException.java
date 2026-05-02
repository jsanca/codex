package codex.codex.internal.service;

import codex.codex.api.model.entity.ContentType;

/**
 * Thrown when attempting to modify the field schema of a {@link ContentType} that is not
 * in {@code DRAFT} status. Schema modifications (add/remove fields) are only allowed
 * while a content type is in draft. Active schema changes require {@code ContentTypeVersion}.
 */
public class InvalidContentTypeSchemaModificationException extends RuntimeException {

    private final ContentType contentType;

    public InvalidContentTypeSchemaModificationException(final String message, final ContentType contentType) {
        super(message);
        this.contentType = contentType;
    }

    /**
     * Returns the content type whose schema modification was rejected.
     *
     * @return the content type; never null
     */
    public ContentType contentType() {
        return contentType;
    }
}
