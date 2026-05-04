package codex.index.api;

/**
 * Write-side contract for projecting Codex resources into a search or index backend.
 * <p>
 * {@code IndexWriter} is intentionally backend-neutral. Implementations may target
 * OpenSearch, Elasticsearch, Lucene, myIR, in-memory structures for tests, or no-ops
 * for environments where indexing is disabled.
 * <p>
 * This interface is write-only. Search and query APIs will be modeled separately.
 * <p>
 * Canonical domain services must not call {@code IndexWriter} directly. Indexing is a
 * projection concern driven by domain events.
 */
public interface IndexWriter {

    /**
     * Inserts or updates an index document.
     * The document's {@link IndexDocument#id()} is used as the idempotent upsert key.
     *
     * @param document the document to index; must not be null
     */
    void upsert(IndexDocument document);

    /**
     * Removes a document from the index by its id.
     * Deleting a non-existent document is a no-op.
     *
     * @param id the document id to remove; must not be null
     */
    void delete(IndexDocumentId id);
}
