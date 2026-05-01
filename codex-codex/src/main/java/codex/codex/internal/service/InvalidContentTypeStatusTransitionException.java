package codex.codex.internal.service;

import codex.codex.api.model.entity.ContentType;
import codex.fundamentum.api.exception.InvalidStateTransitionException;

/**
 * Thrown when a requested {@link codex.codex.api.model.value.ContentTypeStatus} transition
 * is not permitted by the content type state machine.
 */
public class InvalidContentTypeStatusTransitionException extends InvalidStateTransitionException {

    public InvalidContentTypeStatusTransitionException(final String message, final ContentType contentType) {
        super(message);
    }
}
