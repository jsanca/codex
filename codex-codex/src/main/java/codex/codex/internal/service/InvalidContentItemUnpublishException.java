package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentItemKey;

/**
 * Thrown when a content item cannot be unpublished due to invalid state.
 * <p>
 * Typical cause: the item is not currently in {@code PUBLISHED} status.
 */
public class InvalidContentItemUnpublishException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message the reason for rejection
     */
    public InvalidContentItemUnpublishException(final String message) {
        super(message);
    }

    /**
     * Creates the exception using a structured key and reason.
     *
     * @param key    the content item key
     * @param reason the specific reason unpublishing is rejected
     */
    public InvalidContentItemUnpublishException(final ContentItemKey key, final String reason) {
        super("Cannot unpublish content item " + key.value() + " because " + reason);
    }
}
