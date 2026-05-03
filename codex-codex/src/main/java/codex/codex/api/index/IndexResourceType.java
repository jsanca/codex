package codex.codex.api.index;

/**
 * Identifies the kind of Codex resource represented by an {@link IndexDocument}.
 * <p>
 * Used by index backends and subscribers to route, filter, and rank documents by resource type.
 * Future indexing adapters may use this to select different mappings or index names per type.
 */
public enum IndexResourceType {

    /** A site — the top-level tenant scope. */
    SITE,

    /** A content type definition — the schema that governs content items. */
    CONTENT_TYPE,

    /**
     * A published content type schema snapshot — an immutable version of a content type's
     * field definitions at a specific point in time.
     */
    CONTENT_TYPE_VERSION,

    /**
     * A logical content item — the stable identity of a content entry.
     * Usually projected from its latest published revision when indexed.
     */
    CONTENT_ITEM,

    /** A specific content revision snapshot — an immutable values record at a given revision number. */
    CONTENT_REVISION
}
