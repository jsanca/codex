package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentItemKey;

/**
 * Thrown when a content item cannot be deleted due to invalid state.
 * <p>
 * Typical cause: the item is not in {@code ARCHIVED} status. Items must be
 * archived before they can be permanently deleted.
 */
public class InvalidContentItemDeleteException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message the reason for rejection
     */
    public InvalidContentItemDeleteException(final String message) {
        super(message);
    }

    /**
     * Creates the exception using a structured key and reason.
     *
     * @param key    the content item key
     * @param reason the specific reason deletion is rejected
     */
    public InvalidContentItemDeleteException(final ContentItemKey key, final String reason) {
        super("Cannot delete content item " + key.value() + " because " + reason);
    }
}
