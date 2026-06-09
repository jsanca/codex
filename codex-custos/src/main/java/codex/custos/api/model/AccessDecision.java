package codex.custos.api.model;

import codex.custos.api.exception.AccessDeniedException;
import codex.fundamentum.api.model.Actor;

import java.util.Objects;

/**
 * The outcome of a Custos authorization check.
 * <p>
 * An {@code AccessDecision} always explains itself: it carries the actor, the permission,
 * the resource, and a human-readable reason. Prefer pattern-matching on the sealed subtypes
 * ({@link Granted}, {@link Denied}) over checking {@link #isGranted()}.
 *
 * <pre>{@code
 * switch (decision) {
 *     case AccessDecision.Granted g -> proceed();
 *     case AccessDecision.Denied  d -> d.requireGranted(); // throws
 * }
 * }</pre>
 */
public sealed interface AccessDecision permits AccessDecision.Granted, AccessDecision.Denied {

    Actor actor();
    PermissionKey permission();
    ResourceRef resource();
    String reason();

    /** Returns {@code true} if the operation was granted. */
    boolean isGranted();

    /**
     * Asserts that the decision is {@link Granted}.
     *
     * @throws AccessDeniedException if the decision is {@link Denied}
     */
    void requireGranted();

    // --- factory methods ---

    static Granted granted(final Actor actor, final PermissionKey permission,
                           final ResourceRef resource, final String reason) {
        return new Granted(actor, permission, resource, reason);
    }

    static Denied denied(final Actor actor, final PermissionKey permission,
                         final ResourceRef resource, final String reason) {
        return new Denied(actor, permission, resource, reason);
    }

    // --- subtypes ---

    /**
     * Indicates the actor is allowed to perform the requested operation.
     */
    record Granted(Actor actor, PermissionKey permission, ResourceRef resource, String reason)
            implements AccessDecision {

        public Granted {
            Objects.requireNonNull(actor, "actor must not be null");
            Objects.requireNonNull(permission, "permission must not be null");
            Objects.requireNonNull(resource, "resource must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }

        @Override
        public boolean isGranted() {
            return true;
        }

        @Override
        public void requireGranted() {
            // already granted — no-op
        }
    }

    /**
     * Indicates the actor is not allowed to perform the requested operation.
     */
    record Denied(Actor actor, PermissionKey permission, ResourceRef resource, String reason)
            implements AccessDecision {

        public Denied {
            Objects.requireNonNull(actor, "actor must not be null");
            Objects.requireNonNull(permission, "permission must not be null");
            Objects.requireNonNull(resource, "resource must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
        }

        @Override
        public boolean isGranted() {
            return false;
        }

        @Override
        public void requireGranted() {
            throw new AccessDeniedException(
                    "Access denied: " + actor.id().value() + " may not perform [" + permission + "] on [" + resource + "]. Reason: " + reason);
        }
    }
}
