package codex.codex.internal.index;

import codex.codex.api.index.IndexDocument;
import codex.codex.api.index.IndexDocumentId;
import codex.codex.api.index.IndexWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An {@link IndexWriter} that records all operations for inspection in tests.
 * <p>
 * Thread-safe for concurrent use via synchronized methods. Snapshots returned by
 * {@link #upserts()} and {@link #deletes()} are immutable copies of the state at call time.
 */
public final class RecordingIndexWriter implements IndexWriter {

    private final List<IndexDocument> upserted = new ArrayList<>();
    private final List<IndexDocumentId> deleted = new ArrayList<>();

    @Override
    public synchronized void upsert(final IndexDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        upserted.add(document);
    }

    @Override
    public synchronized void delete(final IndexDocumentId id) {
        Objects.requireNonNull(id, "id must not be null");
        deleted.add(id);
    }

    /**
     * Returns an immutable snapshot of all upserted documents in insertion order.
     *
     * @return immutable list; never null
     */
    public synchronized List<IndexDocument> upserts() {
        return List.copyOf(upserted);
    }

    /**
     * Returns an immutable snapshot of all deleted document ids in deletion order.
     *
     * @return immutable list; never null
     */
    public synchronized List<IndexDocumentId> deletes() {
        return List.copyOf(deleted);
    }

    /**
     * Clears all recorded upserts and deletes.
     */
    public synchronized void clear() {
        upserted.clear();
        deleted.clear();
    }
}
