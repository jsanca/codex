package codex.custos.api.exception;

import codex.fundamentum.api.model.Actor;

import java.util.Objects;

/**
 * Thrown when an {@code AGENT} actor is found to hold a {@code SUPER_ADMIN} assignment.
 * <p>
 * This is a hard system invariant violation. Agents must never be granted elevated platform
 * trust. If this exception is thrown, the assignment data is corrupted or invalid — the
 * resolver cannot proceed and callers must not suppress or retry.
 * <p>
 * The offending {@link Actor} is preserved for incident logging and audit.
 */
public class CustosAgentSuperAdminInvariantViolationException extends CustosInvariantViolationException {

    private final Actor actor;

    /**
     * Constructs the exception, deriving the message from the offending agent actor.
     *
     * @param actor the {@code AGENT} actor found holding {@code SUPER_ADMIN}; must not be null
     */
    public CustosAgentSuperAdminInvariantViolationException(final Actor actor) {
        super("System invariant violation: AGENT actor may not hold SUPER_ADMIN: "
                + Objects.requireNonNull(actor, "actor must not be null").id().value());
        this.actor = actor;
    }

    /** Returns the {@code AGENT} actor that triggered the invariant violation. */
    public Actor actor() {
        return actor;
    }
}
