package codex.codex.api.model.value;

/**
 * Lifecycle status of a {@link codex.codex.api.model.entity.ContentType}.
 * <ul>
 *   <li>{@link #DRAFT} — schema is being defined; not yet usable by content items</li>
 *   <li>{@link #ACTIVE} — schema is published; content items may reference this type</li>
 *   <li>{@link #ARCHIVED} — schema is retired from normal use</li>
 * </ul>
 */
public enum ContentTypeStatus {
    DRAFT,
    ACTIVE,
    ARCHIVED
}
