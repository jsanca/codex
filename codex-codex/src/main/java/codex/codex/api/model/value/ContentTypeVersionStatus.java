package codex.codex.api.model.value;

/**
 * Lifecycle status of a {@link codex.codex.api.model.entity.ContentTypeVersion}.
 * <p>
 * Draft schema lives in {@link codex.codex.api.model.entity.ContentType#fields()}.
 * Versions are only created when a content type is published (activated).
 */
public enum ContentTypeVersionStatus {

    /**
     * The schema version is published and is the active snapshot.
     * It can be referenced by new content items and is usually the latest version
     * for its content type.
     */
    PUBLISHED,

    /**
     * The schema version is retained and may still be referenced by existing content items,
     * but should not be used for new content item creation.
     */
    DEPRECATED,

    /**
     * The schema version is retained for history or compatibility purposes.
     * It is not used for normal content creation.
     */
    ARCHIVED
}
