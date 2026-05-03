package codex.codex.internal.index;

import codex.codex.api.index.IndexDocument;
import codex.codex.api.index.IndexWriter;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.internal.repository.ContentItemRepository;
import codex.codex.internal.repository.ContentRevisionRepository;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Projection subscriber that handles {@link ContentItemPublishedEvent} and writes a
 * backend-neutral {@link IndexDocument} to the configured {@link IndexWriter}.
 * <p>
 * This is the first indexing projection in Codex. Indexing is event-driven:
 * canonical services ({@link codex.codex.internal.service.CodexContentItemService},
 * {@link codex.codex.internal.service.EventPublishingContentItemService}) do not call
 * indexing code directly. This subscriber listens for the domain event and drives the projection.
 * <p>
 * {@code ContentItemPublishedEvent} is the natural trigger for public content indexing.
 * Actual backends such as OpenSearch, myIR, Lucene, and embeddings are future adapters.
 * Search and query APIs are future work.
 * <p>
 * If the canonical item or revision cannot be found after the event is received, an
 * {@link IllegalStateException} is thrown because the projection would be inconsistent.
 * Production-grade retry and dead-letter behavior can be layered on top later.
 * Future: introduce ContentItemIndexingSource / ContentProjectionSource to decouple subscribers from repositories.
 * Future:
 * Indexing subscribers may eventually enqueue IndexingRequests instead of writing eagerly.
 * Projection reads may eventually run inside read-only unit-of-work/transaction boundaries.
 * Indexing policy will define DEFER vs WAIT_FOR semantics later.
 *
 */
public final class ContentItemPublishedIndexingSubscriber
        implements CodexEventSubscriber<ContentItemPublishedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentItemPublishedIndexingSubscriber.class);

    private final ContentItemRepository contentItemRepository;
    private final ContentRevisionRepository contentRevisionRepository;
    private final IndexWriter indexWriter;
    private final ContentItemIndexDocumentMapper mapper;

    /**
     * @param contentItemRepository     repository for loading the published content item; must not be null
     * @param contentRevisionRepository repository for loading the published revision; must not be null
     * @param indexWriter               the writer that receives the resulting {@link IndexDocument}; must not be null
     * @param mapper                    maps item + revision to {@link IndexDocument}; must not be null
     */
    public ContentItemPublishedIndexingSubscriber(
            final ContentItemRepository contentItemRepository,
            final ContentRevisionRepository contentRevisionRepository,
            final IndexWriter indexWriter,
            final ContentItemIndexDocumentMapper mapper) {
        this.contentItemRepository = Objects.requireNonNull(contentItemRepository, "contentItemRepository must not be null");
        this.contentRevisionRepository = Objects.requireNonNull(contentRevisionRepository, "contentRevisionRepository must not be null");
        this.indexWriter = Objects.requireNonNull(indexWriter, "indexWriter must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Class<ContentItemPublishedEvent> eventType() {
        return ContentItemPublishedEvent.class;
    }

    /**
     * Handles a {@link ContentItemPublishedEvent} by loading the canonical item and revision,
     * mapping them to an {@link IndexDocument}, and writing the document to the index.
     *
     * @param event the published event; must not be null
     * @throws NullPointerException  if {@code event} is null
     * @throws IllegalStateException if the content item or its published revision cannot be found
     */
    @Override
    public void handle(final ContentItemPublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Indexing content item published: siteKey={} contentTypeKey={} key={}",
                event.siteKey(), event.contentTypeKey(), event.key());

        final ContentItem item = contentItemRepository
                .findByKey(event.siteKey(), event.contentTypeKey(), event.key())
                .orElseThrow(() -> new IllegalStateException(
                        "Content item not found for published event: "
                                + event.siteKey() + "/" + event.contentTypeKey() + "/" + event.key()));

        final ContentRevision revision = contentRevisionRepository
                .findById(event.publishedRevisionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Published revision not found: " + event.publishedRevisionId()));

        final IndexDocument document = mapper.toDocument(item, revision);
        indexWriter.upsert(document);

        LOGGER.info("Indexed content item: id={} siteKey={} contentTypeKey={} key={}",
                document.id(), item.siteKey(), item.contentTypeKey(), item.key());
    }
}
