package codex.custos.api.model;

import codex.fundamentum.api.model.Actor;

import java.util.Objects;

/**
 * The outcome of a Custos permission resolution check.
 * <p>
 * A {@code PermissionResolution} always explains itself: it carries the actor, the
 * permission, the target scope, and a human-readable reason. Prefer pattern-matching
 * on the sealed subtypes ({@link Granted}, {@link Denied}) over {@code instanceof} checks.
 *
 * <pre>{@code
 * switch (resolution) {
 *     case PermissionResolution.Granted g -> proceed();
 *     case PermissionResolution.Denied  d -> throw new AccessDeniedException(d.reason());
 * }
 * }</pre>
 *
 * @see PermissionResolutionRequest
 * @see PermissionResolutionSnapshot
 */
public sealed interface PermissionResolution
        permits PermissionResolution.Granted, PermissionResolution.Denied {

    Actor actor();

    PermissionKey permission();

    ResourceScope target();

    String reason();

    /** Returns {@code true} if the permission was granted. */
    boolean isGranted();

    // --- factory methods ---

    static Granted granted(final Actor actor, final PermissionKey permission,
                           final ResourceScope target, final String reason) {
        return new Granted(actor, permission, target, reason);
    }

    static Denied denied(final Actor actor, final PermissionKey permission,
                         final ResourceScope target, final String reason) {
        return new Denied(actor, permission, target, reason);
    }

    // --- subtypes ---

    /**
     * The actor is allowed to perform the requested operation at the given scope.
     */
    record Granted(Actor actor, PermissionKey permission, ResourceScope target, String reason)
            implements PermissionResolution {

        public Granted {
            Objects.requireNonNull(actor, "actor must not be null");
            Objects.requireNonNull(permission, "permission must not be null");
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }

        @Override
        public boolean isGranted() {
            return true;
        }
    }

    /**
     * The actor is not allowed to perform the requested operation at the given scope.
     */
    record Denied(Actor actor, PermissionKey permission, ResourceScope target, String reason)
            implements PermissionResolution {

        public Denied {
            Objects.requireNonNull(actor, "actor must not be null");
            Objects.requireNonNull(permission, "permission must not be null");
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(reason, "reason must not be null");
            if (reason.isBlank()) {
                throw new IllegalArgumentException("reason must not be blank");
            }
        }

        @Override
        public boolean isGranted() {
            return false;
        }
    }
}
