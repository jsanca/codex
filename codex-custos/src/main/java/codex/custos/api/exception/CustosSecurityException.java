package codex.custos.api.exception;

/**
 * Base exception for Custos authorization failures and security invariant violations.
 * <p>
 * Subclasses distinguish between normal access-denial ({@link AccessDeniedException}) and
 * structural invariant violations ({@link CustosInvariantViolationException}) that indicate
 * corrupted or invalid security state — not legitimate authorization decisions.
 */
public class CustosSecurityException extends RuntimeException {

    public CustosSecurityException(final String message) {
        super(message);
    }

    public CustosSecurityException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
