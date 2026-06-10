package codex.custos.api.exception;

/**
 * Thrown when a hard Custos security invariant is violated.
 * <p>
 * An invariant violation is <strong>not</strong> a normal authorization denial. It signals
 * corrupted or invalid security state (e.g., an {@code AGENT} actor holding a
 * {@code SUPER_ADMIN} assignment) that must never occur in a correctly configured system.
 * <p>
 * Callers must treat this as a fatal error rather than catching and retrying.
 *
 * @see CustosAgentSuperAdminInvariantViolationException
 */
public class CustosInvariantViolationException extends CustosSecurityException {

    public CustosInvariantViolationException(final String message) {
        super(message);
    }

    public CustosInvariantViolationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
