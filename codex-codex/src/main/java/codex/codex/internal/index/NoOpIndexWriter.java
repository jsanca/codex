package codex.codex.internal.index;

import codex.codex.api.index.IndexDocument;
import codex.codex.api.index.IndexDocumentId;
import codex.codex.api.index.IndexWriter;

import java.util.Objects;

/**
 * An {@link IndexWriter} implementation that silently discards all operations.
 * <p>
 * Useful for runtime wiring where indexing is disabled or not yet configured.
 * All arguments are still validated so callers receive feedback for programming errors.
 */
public final class NoOpIndexWriter implements IndexWriter {

    @Override
    public void upsert(final IndexDocument document) {
        Objects.requireNonNull(document, "document must not be null");
    }

    @Override
    public void delete(final IndexDocumentId id) {
        Objects.requireNonNull(id, "id must not be null");
    }
}
