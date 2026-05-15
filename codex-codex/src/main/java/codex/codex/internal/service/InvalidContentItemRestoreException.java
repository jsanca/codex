package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentItemKey;

/**
 * Thrown when a content item cannot be restored due to invalid state.
 * <p>
 * Typical cause: the item is not in {@code ARCHIVED} status.
 */
public class InvalidContentItemRestoreException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message the reason for rejection
     */
    public InvalidContentItemRestoreException(final String message) {
        super(message);
    }

    /**
     * Creates the exception using a structured key and reason.
     *
     * @param key    the content item key
     * @param reason the specific reason restoring is rejected
     */
    public InvalidContentItemRestoreException(final ContentItemKey key, final String reason) {
        super("Cannot restore content item " + key.value() + " because " + reason);
    }
}
