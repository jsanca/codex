package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentItemKey;

/**
 * Thrown when a content item cannot be archived due to invalid state.
 * <p>
 * Typical cause: the item is already in {@code ARCHIVED} status.
 */
public class InvalidContentItemArchiveException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message the reason for rejection
     */
    public InvalidContentItemArchiveException(final String message) {
        super(message);
    }

    /**
     * Creates the exception using a structured key and reason.
     *
     * @param key    the content item key
     * @param reason the specific reason archiving is rejected
     */
    public InvalidContentItemArchiveException(final ContentItemKey key, final String reason) {
        super("Cannot archive content item " + key.value() + " because " + reason);
    }
}
