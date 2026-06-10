package codex.custos.internal.service;

import codex.custos.api.model.PermissionKey;

import java.util.Set;

/**
 * Encapsulates the permission implication rules defined in ADR-009.
 * <p>
 * A permission is "permitted" by a role if either:
 * <ul>
 *   <li>the role's permission set contains the requested permission directly, or</li>
 *   <li>the role's permission set contains a permission that implies the requested permission.</li>
 * </ul>
 */
interface PermissionImplicationRules {

    /**
     * Returns {@code true} if {@code rolePermissions} directly contains {@code requested}
     * or if any permission in {@code rolePermissions} implies {@code requested}.
     *
     * @param rolePermissions the set of permissions held by the role
     * @param requested       the permission being checked
     * @return {@code true} if the role effectively grants the requested permission
     */
    boolean permits(Set<PermissionKey> rolePermissions, PermissionKey requested);
}
