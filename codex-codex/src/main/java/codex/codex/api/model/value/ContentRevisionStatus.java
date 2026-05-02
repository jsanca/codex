package codex.codex.api.model.value;

/**
 * Lifecycle status of a {@link codex.codex.api.model.entity.ContentRevision}.
 * <p>
 * Newly created revisions begin as {@code WORKING}. Publish and archive transitions
 * will be introduced in later tasks.
 */
public enum ContentRevisionStatus {

    /**
     * Revision exists as the current draft/working version. Not yet published.
     */
    WORKING,

    /**
     * Revision is published and available for normal rendering/consumption.
     */
    PUBLISHED,

    /**
     * Revision is retained for history but retired from normal use.
     */
    ARCHIVED
}
