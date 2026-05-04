package codex.index.internal;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.index.api.IndexDocument;
import codex.index.api.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Projection subscriber that handles {@link ContentItemPublishedEvent} and writes a
 * backend-neutral {@link IndexDocument} to the configured {@link IndexWriter}.
 * <p>
 * Indexing is event-driven. Canonical services do not call indexing code directly.
 * This subscriber reacts to the domain event and drives the projection.
 * <p>
 * Canonical data is loaded through a {@link ContentItemProjectionSource}. Future sources
 * may use cache, read-only unit-of-work, or read models.
 * <p>
 * If canonical data cannot be found, an {@link IllegalStateException} is thrown because
 * the projection would be inconsistent. Production retry and dead-letter behavior are future work.
 */
public final class ContentItemPublishedIndexingSubscriber
        implements CodexEventSubscriber<ContentItemPublishedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentItemPublishedIndexingSubscriber.class);

    private final ContentItemProjectionSource projectionSource;
    private final IndexWriter indexWriter;
    private final ContentItemIndexDocumentMapper mapper;

    /**
     * @param projectionSource read source for canonical projection data; must not be null
     * @param indexWriter      receives the resulting {@link IndexDocument}; must not be null
     * @param mapper           maps item + revision to {@link IndexDocument}; must not be null
     */
    public ContentItemPublishedIndexingSubscriber(
            final ContentItemProjectionSource projectionSource,
            final IndexWriter indexWriter,
            final ContentItemIndexDocumentMapper mapper) {
        this.projectionSource = Objects.requireNonNull(projectionSource, "projectionSource must not be null");
        this.indexWriter = Objects.requireNonNull(indexWriter, "indexWriter must not be null");
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    @Override
    public Class<ContentItemPublishedEvent> eventType() {
        return ContentItemPublishedEvent.class;
    }

    @Override
    public void handle(final ContentItemPublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Indexing content item published: siteKey={} contentTypeKey={} key={}",
                event.siteKey(), event.contentTypeKey(), event.key());

        final ContentItem item = projectionSource.loadItem(event);
        final ContentRevision revision = projectionSource.loadPublishedRevision(event);
        final IndexDocument document = mapper.toDocument(item, revision);
        indexWriter.upsert(document);

        LOGGER.info("Indexed content item: id={} siteKey={} contentTypeKey={} key={}",
                document.id(), item.siteKey(), item.contentTypeKey(), item.key());
    }
}
