package codex.custos.api.model;

import codex.fundamentum.api.model.Actor;

import java.util.Objects;

/**
 * Records that an {@link Actor} holds a {@link RoleKey} within a {@link ResourceScope}.
 * <p>
 * A {@code RoleAssignment} is a data record only. It does not embed the full {@link Role}
 * blueprint, does not evaluate permissions, does not resolve implied permissions, and does
 * not enforce actor-type restrictions (e.g., agent restrictions). Those concerns belong in
 * a future {@code PermissionResolver} or policy engine.
 *
 * <p>Example: assigning {@code COPYWRITER} to an actor at {@code SiteScope(site-a)} means
 * "this actor holds the COPYWRITER role in site-a." Whether that role grants a specific
 * permission requires resolving the Role blueprint against its PermissionGrants.
 */
public record RoleAssignment(Actor actor, RoleKey role, ResourceScope scope) {

    public RoleAssignment {
        Objects.requireNonNull(actor, "actor must not be null");
        Objects.requireNonNull(role, "role must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
    }

    /**
     * Creates a {@link RoleAssignment} for the given actor, role, and scope.
     *
     * @param actor the actor receiving the role; must not be null
     * @param role  the role key being assigned; must not be null
     * @param scope the resource scope at which the role is assigned; must not be null
     * @return an immutable {@code RoleAssignment}
     */
    public static RoleAssignment of(final Actor actor, final RoleKey role, final ResourceScope scope) {
        return new RoleAssignment(actor, role, scope);
    }
}
