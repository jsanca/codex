package codex.fundamentum.api.lifecycle;

/**
 * Describes how a Codex resource participates in the normal Codex lifecycle.
 * <p>
 * Services should validate lifecycle participation semantically rather than
 * checking for specific resource keys (e.g. {@code SiteKey.SYSTEM}).
 * This keeps lifecycle logic open for future resource types such as system resources,
 * read-only resources, externally managed resources, or virtual resources.
 */
public enum LifecycleParticipation {

    /**
     * The resource participates in the normal Codex lifecycle.
     * All lifecycle operations — start, suspend, archive, unarchive — are allowed.
     */
    MANAGED,

    /**
     * The resource is visible and addressable but not mutable through normal services.
     * Lifecycle operations are rejected.
     */
    READ_ONLY,

    /**
     * The resource is managed by the Codex runtime or platform.
     * Normal user-facing lifecycle operations are rejected.
     * Examples: the built-in system site.
     */
    SYSTEM_MANAGED,

    /**
     * The resource's lifecycle is owned outside Codex.
     * Lifecycle operations are rejected through normal Codex services.
     */
    EXTERNAL
}
