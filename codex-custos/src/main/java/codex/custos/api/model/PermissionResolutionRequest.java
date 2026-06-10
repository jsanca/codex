package codex.custos.api.model;

import codex.fundamentum.api.model.Actor;

import java.util.Objects;

/**
 * Encapsulates the inputs to a single permission resolution check.
 * <p>
 * A request captures: <em>who</em> (actor) wants to do <em>what</em> (permission) on
 * <em>where</em> (target scope). Pass this alongside a {@link PermissionResolutionSnapshot}
 * to {@code PermissionResolver.resolve()} to obtain a {@link PermissionResolution}.
 */
public record PermissionResolutionRequest(Actor actor, PermissionKey permission, ResourceScope target) {

    public PermissionResolutionRequest {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(permission, "permission must not be null");
        Objects.requireNonNull(target, "target must not be null");
    }

    /**
     * Creates a {@link PermissionResolutionRequest} for the given actor, permission, and target scope.
     *
     * @param actor      the actor requesting the operation; must not be null
     * @param permission the domain permission being requested; must not be null
     * @param target     the resource scope the operation targets; must not be null
     * @return an immutable request
     */
    public static PermissionResolutionRequest of(final Actor actor, final PermissionKey permission,
                                                 final ResourceScope target) {
        return new PermissionResolutionRequest(actor, permission, target);
    }
}
