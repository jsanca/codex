package codex.custos.api.model;

import java.util.Set;

/**
 * Catalog of canonical built-in {@link Role} blueprints.
 * <p>
 * Each constant is a {@link Role} — a {@link RoleKey} paired with a fixed set of
 * {@link PermissionKey} values. These are blueprints only: they carry no actor, no scope,
 * and no assignment. Scopes and actors belong to {@link RoleAssignment}.
 *
 * <h2>SUPER_ADMIN</h2>
 * {@link #SUPER_ADMIN} holds all permissions from the {@link Permissions} catalog. This
 * serves introspection and UI display: callers can inspect which permissions the role
 * nominally covers. <strong>This is not where bypass semantics live.</strong>
 *
 * <p>The future {@code PermissionResolver} / {@code PermissionEvaluator} / policy layer
 * must apply the following evaluation order:
 * <ol>
 *   <li><strong>Hard safety rules first</strong> — always, before any lookup.</li>
 *   <li>If {@code actor.type == AGENT} and the actor holds {@code SUPER_ADMIN}: <strong>DENY</strong>
 *       (system invariant violation — agents must never receive elevated trust).</li>
 *   <li>If the requested operation would assign {@code SUPER_ADMIN} to an {@code AGENT}: <strong>DENY</strong>.</li>
 *   <li>If hard rules pass and the actor holds a valid {@code SUPER_ADMIN} assignment:
 *       bypass normal scoped permission lookup.</li>
 *   <li>Otherwise: resolve grants and assignments normally.</li>
 * </ol>
 *
 * <p>{@code SUPER_ADMIN} does not bypass hard system invariants. A valid
 * {@code SUPER_ADMIN} may skip <em>scoped grant lookup</em>, but cannot violate
 * structural rules such as the agent restriction above.
 */
public final class BuiltInRoles {

    private BuiltInRoles() {}

    /**
     * Platform-wide super administrator.
     * <p>
     * Holds all permissions from the {@link Permissions} catalog for blueprint and
     * introspection purposes. <strong>Bypass semantics are not encoded here.</strong>
     * The resolver/evaluator/policy layer is solely responsible for:
     * <ul>
     *   <li>Denying {@code SUPER_ADMIN} to {@code AGENT} actors (hard invariant).</li>
     *   <li>Denying assignment of {@code SUPER_ADMIN} to {@code AGENT} actors.</li>
     *   <li>Allowing a valid {@code SUPER_ADMIN} to skip scoped grant lookup.</li>
     * </ul>
     * {@code SUPER_ADMIN} does not bypass hard system invariants regardless of this blueprint.
     */
    public static final Role SUPER_ADMIN = Role.of(
            RoleKey.of("SUPER_ADMIN"),
            Set.of(
                    Permissions.SITE_READ,
                    Permissions.SITE_CREATE,
                    Permissions.SITE_START,
                    Permissions.SITE_SUSPEND,
                    Permissions.SITE_ARCHIVE,
                    Permissions.CONTENT_TYPE_READ,
                    Permissions.CONTENT_TYPE_CREATE,
                    Permissions.CONTENT_TYPE_UPDATE,
                    Permissions.CONTENT_TYPE_DELETE,
                    Permissions.CONTENT_ITEM_READ,
                    Permissions.CONTENT_ITEM_CREATE,
                    Permissions.CONTENT_ITEM_UPDATE,
                    Permissions.CONTENT_ITEM_PUBLISH,
                    Permissions.CONTENT_ITEM_UNPUBLISH,
                    Permissions.CONTENT_ITEM_ARCHIVE,
                    Permissions.PERMISSION_READ,
                    Permissions.PERMISSION_GRANT,
                    Permissions.PERMISSION_REVOKE,
                    Permissions.ROLE_ASSIGN,
                    Permissions.ROLE_REVOKE
            ));

    /**
     * Site-level administrator with full content and permission management capabilities.
     */
    public static final Role SITE_ADMIN = Role.of(
            RoleKey.of("SITE_ADMIN"),
            Set.of(
                    Permissions.SITE_READ,
                    Permissions.SITE_START,
                    Permissions.SITE_SUSPEND,
                    Permissions.SITE_ARCHIVE,
                    Permissions.CONTENT_TYPE_READ,
                    Permissions.CONTENT_TYPE_CREATE,
                    Permissions.CONTENT_TYPE_UPDATE,
                    Permissions.CONTENT_TYPE_DELETE,
                    Permissions.CONTENT_ITEM_READ,
                    Permissions.CONTENT_ITEM_CREATE,
                    Permissions.CONTENT_ITEM_UPDATE,
                    Permissions.CONTENT_ITEM_PUBLISH,
                    Permissions.CONTENT_ITEM_UNPUBLISH,
                    Permissions.CONTENT_ITEM_ARCHIVE,
                    Permissions.PERMISSION_READ,
                    Permissions.ROLE_ASSIGN,
                    Permissions.ROLE_REVOKE
            ));

    /**
     * Full content lifecycle editor: read, create, update, publish, unpublish, and archive.
     */
    public static final Role EDITOR = Role.of(
            RoleKey.of("EDITOR"),
            Set.of(
                    Permissions.CONTENT_ITEM_READ,
                    Permissions.CONTENT_ITEM_CREATE,
                    Permissions.CONTENT_ITEM_UPDATE,
                    Permissions.CONTENT_ITEM_PUBLISH,
                    Permissions.CONTENT_ITEM_UNPUBLISH,
                    Permissions.CONTENT_ITEM_ARCHIVE
            ));

    /**
     * Content author: read, create, and update — but not publish.
     */
    public static final Role COPYWRITER = Role.of(
            RoleKey.of("COPYWRITER"),
            Set.of(
                    Permissions.CONTENT_ITEM_READ,
                    Permissions.CONTENT_ITEM_CREATE,
                    Permissions.CONTENT_ITEM_UPDATE
            ));

    /**
     * Content reviewer: read, publish, and unpublish — but not modify body.
     */
    public static final Role REVIEWER = Role.of(
            RoleKey.of("REVIEWER"),
            Set.of(
                    Permissions.CONTENT_ITEM_READ,
                    Permissions.CONTENT_ITEM_PUBLISH,
                    Permissions.CONTENT_ITEM_UNPUBLISH
            ));

    /**
     * Read-only observer.
     */
    public static final Role VIEWER = Role.of(
            RoleKey.of("VIEWER"),
            Set.of(
                    Permissions.CONTENT_ITEM_READ
            ));
}
