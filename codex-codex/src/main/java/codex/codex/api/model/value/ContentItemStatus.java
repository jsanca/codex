package codex.codex.api.model.value;

/**
 * Lifecycle status of a {@link codex.codex.api.model.entity.ContentItem}.
 * <p>
 * Content items begin as {@code DRAFT}. Publishing and archiving operations
 * will be introduced in later tasks.
 */
public enum ContentItemStatus {

    /**
     * Item exists but has not been published. Default status on creation.
     */
    DRAFT,

    /**
     * Item is published and available for normal consumption.
     */
    PUBLISHED,

    /**
     * Item has been retired from normal use. Retained for history.
     */
    ARCHIVED
}
