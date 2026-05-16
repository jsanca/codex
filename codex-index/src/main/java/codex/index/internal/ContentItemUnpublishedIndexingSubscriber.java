package codex.index.internal;

import codex.codex.api.model.event.ContentItemUnpublishedEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import codex.index.api.IndexDocumentId;
import codex.index.api.IndexWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Removes a content item from the public index when it is unpublished.
 *
 * <p>Computes the deterministic {@link IndexDocumentId} directly from event fields.
 * Does not reload {@code ContentItem} or {@code ContentRevision} from any repository.</p>
 */
public final class ContentItemUnpublishedIndexingSubscriber
        implements CodexEventSubscriber<ContentItemUnpublishedEvent> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ContentItemUnpublishedIndexingSubscriber.class);

    private final IndexWriter indexWriter;

    /**
     * @param indexWriter the index write port; must not be null
     */
    public ContentItemUnpublishedIndexingSubscriber(final IndexWriter indexWriter) {
        this.indexWriter = Objects.requireNonNull(indexWriter, "indexWriter must not be null");
    }

    @Override
    public Class<ContentItemUnpublishedEvent> eventType() {
        return ContentItemUnpublishedEvent.class;
    }

    @Override
    public void handle(final ContentItemUnpublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        LOGGER.debug("Removing unpublished content item from index: siteKey={} contentTypeKey={} key={}",
                event.siteKey(), event.contentTypeKey(), event.key());

        final IndexDocumentId docId = IndexDocumentIds.contentItem(
                event.siteKey(), event.contentTypeKey(), event.key());
        indexWriter.delete(docId);

        LOGGER.info("Removed content item from index: id={} siteKey={} contentTypeKey={} key={}",
                docId, event.siteKey(), event.contentTypeKey(), event.key());
    }
}
