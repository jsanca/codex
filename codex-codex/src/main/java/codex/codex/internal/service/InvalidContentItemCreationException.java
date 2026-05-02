package codex.codex.internal.service;

/**
 * Thrown when a content item cannot be created due to an invalid content type state.
 * <p>
 * Typical causes: content type is in {@code DRAFT} or {@code ARCHIVED} status,
 * or has no latest published version id.
 */
public class InvalidContentItemCreationException extends RuntimeException {

    /**
     * Creates the exception with a descriptive message.
     *
     * @param message the reason for rejection
     */
    public InvalidContentItemCreationException(final String message) {
        super(message);
    }
}
