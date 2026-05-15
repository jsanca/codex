package codex.chronicon.api;

/**
 * The action recorded in an {@link AuditRecord}.
 * <p>
 * Values are kept generic enough to apply across multiple resource types (sites,
 * content types, content items, workflow, users). Resource-specific action semantics
 * should be captured in {@link AuditRecord#summary()} or {@link AuditRecord#metadata()}.
 */
public enum AuditAction {

    /** A new resource was created. */
    CREATED,

    /** An existing resource was updated. */
    UPDATED,

    /** A resource was published or made live. */
    PUBLISHED,

    /** A resource was unpublished and reverted to an editable state. */
    UNPUBLISHED,

    /** A resource was archived or removed from active use. */
    ARCHIVED,

    /** A resource was restored from an archived state back to an editable state. */
    RESTORED,

    /** A resource transitioned to a started/active state. */
    STARTED,

    /** A resource was suspended. */
    SUSPENDED,

    /** A resource or feature was activated. */
    ACTIVATED,

    /** A resource or feature was deactivated. */
    DEACTIVATED,

    /** A resource was deleted. */
    DELETED,

    /** The action is not known or could not be mapped from the originating event. */
    UNKNOWN
}
