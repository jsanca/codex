package codex.index.internal;

import codex.index.api.IndexDocument;
import codex.index.api.IndexDocumentId;
import codex.index.api.IndexWriter;

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
