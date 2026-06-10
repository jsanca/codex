package codex.custos.api.model;

import java.util.Objects;

/**
 * Represents a permission made available at a specific {@link ResourceScope}.
 * <p>
 * A {@code PermissionGrant} is a data record only — it does not evaluate authorization,
 * does not carry an actor, does not carry a role, and does not implement implication
 * or inheritance semantics. Resolution logic belongs in a future {@code PermissionResolver}.
 *
 * <p>Example: grant {@code contentItem.update} at {@code ContentTypeScope(site-a, blog-post)}
 * means "this permission is available at that scope." Whether a given actor benefits from
 * it depends on role assignments and resolution order, which are out of scope here.
 */
public record PermissionGrant(PermissionKey permission, ResourceScope scope) {

    public PermissionGrant {
        Objects.requireNonNull(permission, "permission must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
    }

    /**
     * Creates a {@link PermissionGrant} for the given permission at the given scope.
     *
     * @param permission the domain permission being granted; must not be null
     * @param scope      the resource scope at which the permission is granted; must not be null
     * @return an immutable {@code PermissionGrant}
     */
    public static PermissionGrant of(final PermissionKey permission, final ResourceScope scope) {
        return new PermissionGrant(permission, scope);
    }
}
