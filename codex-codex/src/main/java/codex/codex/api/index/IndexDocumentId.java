package codex.codex.api.index;

import java.util.Objects;

/**
 * Stable, unique identifier for an {@link IndexDocument}.
 * <p>
 * Wraps a plain string and ensures it is non-null and non-blank after trimming.
 * Callers are encouraged to use deterministic formats so that re-indexing an existing
 * resource produces the same id and triggers an upsert rather than a duplicate insert.
 * <p>
 * Suggested formats:
 * <ul>
 *   <li>{@code site:{siteKey}}</li>
 *   <li>{@code content-type:{siteKey}:{contentTypeKey}}</li>
 *   <li>{@code content-item:{siteKey}:{contentTypeKey}:{contentItemKey}}</li>
 *   <li>{@code content-revision:{siteKey}:{contentTypeKey}:{contentItemKey}:r{N}}</li>
 * </ul>
 *
 * @param value the raw document id; never null or blank
 */
public record IndexDocumentId(String value) {

    /**
     * Canonical constructor for {@link IndexDocumentId}.
     *
     * @param value the raw id; must not be null or blank after trimming
     */
    public IndexDocumentId {
        Objects.requireNonNull(value, "IndexDocumentId value cannot be null");
        value = value.trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("IndexDocumentId value cannot be blank");
        }
    }

    /**
     * Creates an {@link IndexDocumentId} from a raw string value.
     *
     * @param value the raw id; must not be null or blank
     * @return a new instance
     */
    public static IndexDocumentId of(final String value) {
        return new IndexDocumentId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
