package codex.custos.api.exception;

/**
 * Thrown when an {@link codex.fundamentum.api.model.Actor} is denied a domain operation.
 * <p>
 * Callers may inspect the embedded {@code reason} for a human-readable explanation.
 * Use {@link codex.custos.api.model.AccessDecision.Denied#requireGranted()} as the idiomatic
 * throw site rather than constructing this exception directly.
 */
public class AccessDeniedException extends RuntimeException {

    public AccessDeniedException(final String message) {
        super(message);
    }

    public AccessDeniedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
