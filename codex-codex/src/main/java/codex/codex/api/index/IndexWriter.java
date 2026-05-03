package codex.codex.api.index;

/**
 * Write-side contract for projecting Codex resources into a search or index backend.
 * <p>
 * {@code IndexWriter} is intentionally backend-neutral. Implementations may target
 * OpenSearch, Elasticsearch, Lucene, myIR, in-memory structures for tests, or no-ops
 * for environments where indexing is disabled.
 * <p>
 * This interface is write-only. Search and query APIs will be modeled separately.
 * <p>
 * Canonical domain services ({@code CodexContentItemService}, etc.) must not call
 * {@code IndexWriter} directly. Indexing is a projection concern driven by domain events.
 * Future subscribers will translate {@code ContentItemPublishedEvent} and similar events
 * into {@link IndexDocument} objects and write them through this interface.
 */
public interface IndexWriter {

    /**
     * Inserts or updates an index document.
     * <p>
     * The document's {@link IndexDocument#id()} is used as the idempotent upsert key.
     * Re-indexing the same document replaces the previous entry.
     *
     * @param document the document to index; must not be null
     */
    void upsert(IndexDocument document);

    /**
     * Removes a document from the index by its id.
     * <p>
     * Deleting a non-existent document is a no-op.
     *
     * @param id the document id to remove; must not be null
     */
    void delete(IndexDocumentId id);
}
