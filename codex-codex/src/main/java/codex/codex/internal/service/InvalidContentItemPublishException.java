package codex.codex.internal.service;

import codex.codex.api.model.identity.ContentItemKey;

/**
 * Thrown when a content item cannot be published due to invalid state.
 * <p>
 * Typical causes: item is {@code ARCHIVED}, item has no working revision,
 * or the current working revision is {@code ARCHIVED}.
 */
public class InvalidContentItemPublishException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message the reason for rejection
     */
    public InvalidContentItemPublishException(final String message) {
        super(message);
    }

    /**
     * Creates the exception using a structured key and reason.
     *
     * @param key    the content item key
     * @param reason the specific reason publishing is rejected
     */
    public InvalidContentItemPublishException(final ContentItemKey key, final String reason) {
        super("Cannot publish content item " + key.value() + " because " + reason);
    }
}
