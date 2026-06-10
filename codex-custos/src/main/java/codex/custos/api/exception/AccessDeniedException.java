package codex.custos.api.exception;

import codex.custos.api.model.AccessDecision;

import java.util.Objects;
import java.util.Optional;

/**
 * Thrown when an {@link codex.fundamentum.api.model.Actor} is denied a domain operation.
 * <p>
 * The original {@link AccessDecision.Denied} decision is preserved and accessible via
 * {@link #decision()}, giving callers full context (actor, permission, resource, reason)
 * without parsing the message string.
 * <p>
 * Use {@link AccessDecision.Denied#requireGranted()} as the idiomatic throw site rather
 * than constructing this exception directly.
 */
public class AccessDeniedException extends RuntimeException {

    private final AccessDecision.Denied decision;

    public AccessDeniedException(final String message) {
        super(message);
        this.decision = null;
    }

    public AccessDeniedException(final String message, final Throwable cause) {
        super(message, cause);
        this.decision = null;
    }

    /**
     * Constructs an {@code AccessDeniedException} carrying the full {@link AccessDecision.Denied}
     * that caused the denial. Prefer this constructor from {@link AccessDecision.Denied#requireGranted()}.
     *
     * @param decision the denied decision; must not be null
     */
    public AccessDeniedException(final AccessDecision.Denied decision) {
        super("Access denied: " + Objects.requireNonNull(decision, "decision must not be null").actor().id().value()
                + " may not perform [" + decision.permission() + "] on [" + decision.resource()
                + "]. Reason: " + decision.reason());
        this.decision = decision;
    }

    /**
     * Returns the {@link AccessDecision.Denied} that caused this exception, if available.
     * Empty when the exception was constructed from a raw message rather than a decision.
     */
    public Optional<AccessDecision.Denied> decision() {
        return Optional.ofNullable(decision);
    }
}
