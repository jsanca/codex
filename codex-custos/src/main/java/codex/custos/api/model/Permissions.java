package codex.custos.api.model;

/**
 * Catalog of built-in Custos permission keys.
 * <p>
 * Permission keys use dot-separated namespacing: {@code <resource>.<operation>}.
 * They represent domain operations, not HTTP endpoints.
 * <p>
 * Implication rules (from ADR-009):
 * <ul>
 *   <li>{@code contentItem.update} implies {@code contentItem.read}</li>
 *   <li>{@code contentItem.publish} implies {@code contentItem.read}</li>
 *   <li>{@code contentItem.publish} does <em>not</em> imply {@code contentItem.update}</li>
 * </ul>
 */
public final class Permissions {

    private Permissions() {}

    // --- Site ---

    /** Read site metadata and configuration. */
    public static final PermissionKey SITE_READ = PermissionKey.of("site.read");

    /** Create a new site. */
    public static final PermissionKey SITE_CREATE = PermissionKey.of("site.create");

    /** Transition a site from SUSPENDED to STARTED. */
    public static final PermissionKey SITE_START = PermissionKey.of("site.start");

    /** Transition a site from STARTED to SUSPENDED. */
    public static final PermissionKey SITE_SUSPEND = PermissionKey.of("site.suspend");

    /** Transition a site to ARCHIVED. */
    public static final PermissionKey SITE_ARCHIVE = PermissionKey.of("site.archive");

    // --- Content Type ---

    /** Read content type schema and metadata. */
    public static final PermissionKey CONTENT_TYPE_READ = PermissionKey.of("contentType.read");

    /** Create a new content type. */
    public static final PermissionKey CONTENT_TYPE_CREATE = PermissionKey.of("contentType.create");

    /** Modify an existing content type schema. */
    public static final PermissionKey CONTENT_TYPE_UPDATE = PermissionKey.of("contentType.update");

    /** Delete a content type. */
    public static final PermissionKey CONTENT_TYPE_DELETE = PermissionKey.of("contentType.delete");

    // --- Content Item ---

    /** Read a content item. */
    public static final PermissionKey CONTENT_ITEM_READ = PermissionKey.of("contentItem.read");

    /** Create a new content item. */
    public static final PermissionKey CONTENT_ITEM_CREATE = PermissionKey.of("contentItem.create");

    /** Modify the body or metadata of a content item. Implies {@link #CONTENT_ITEM_READ}. */
    public static final PermissionKey CONTENT_ITEM_UPDATE = PermissionKey.of("contentItem.update");

    /** Publish a content item. Implies {@link #CONTENT_ITEM_READ}. */
    public static final PermissionKey CONTENT_ITEM_PUBLISH = PermissionKey.of("contentItem.publish");

    /** Unpublish a previously published content item. */
    public static final PermissionKey CONTENT_ITEM_UNPUBLISH = PermissionKey.of("contentItem.unpublish");

    /** Archive a content item. */
    public static final PermissionKey CONTENT_ITEM_ARCHIVE = PermissionKey.of("contentItem.archive");

    // --- Permission management ---

    /** Read permission grants and role assignments. */
    public static final PermissionKey PERMISSION_READ = PermissionKey.of("permission.read");

    /** Grant a permission to an actor or role. */
    public static final PermissionKey PERMISSION_GRANT = PermissionKey.of("permission.grant");

    /** Revoke a permission from an actor or role. */
    public static final PermissionKey PERMISSION_REVOKE = PermissionKey.of("permission.revoke");

    // --- Role management ---

    /** Assign a role to an actor. */
    public static final PermissionKey ROLE_ASSIGN = PermissionKey.of("role.assign");

    /** Revoke a role from an actor. */
    public static final PermissionKey ROLE_REVOKE = PermissionKey.of("role.revoke");
}
