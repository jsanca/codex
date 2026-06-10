package codex.custos.api.model;

import java.util.Objects;
import java.util.Set;

/**
 * A permission blueprint — a named collection of {@link PermissionKey} values that can
 * be assigned to actors via a {@code RoleAssignment} (Phase 1+).
 * <p>
 * A {@code Role} has no scope and no actor. Scope belongs to {@code RoleAssignment};
 * actor belongs to {@code RoleAssignment}. This separation keeps the role model reusable
 * across sites and content types without baking in assignment context.
 * <p>
 * <strong>Empty roles are allowed.</strong> A role with no permissions is valid — custom
 * roles may be assembled gradually. Callers should treat an empty role as "no access
 * granted through this role."
 */
public record Role(RoleKey key, Set<PermissionKey> permissions) {

    public Role {
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(permissions, "permissions must not be null");
        permissions = Set.copyOf(permissions); // defensive copy; throws NPE on null elements
    }

    /**
     * Creates a {@link Role} from a key and a permission set.
     *
     * @param key         the role identity; must not be null
     * @param permissions the permissions this role grants; must not be null or contain nulls
     * @return an immutable {@code Role}
     */
    public static Role of(final RoleKey key, final Set<PermissionKey> permissions) {
        return new Role(key, permissions);
    }

    /**
     * Creates a {@link Role} from a key and zero or more permission keys.
     *
     * @param key         the role identity; must not be null
     * @param permissions the permissions this role grants; must not contain nulls
     * @return an immutable {@code Role}
     */
    public static Role of(final RoleKey key, final PermissionKey... permissions) {
        Objects.requireNonNull(permissions, "permissions varargs must not be null");
        return new Role(key, Set.of(permissions));
    }
}
