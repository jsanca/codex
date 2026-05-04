package codex.codex.internal.index;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.internal.repository.ContentItemRepository;
import codex.codex.internal.repository.ContentRevisionRepository;

import java.util.Objects;

/**
 * Repository-backed implementation of {@link ContentItemProjectionSource}.
 * <p>
 * This is the MVP implementation. It loads canonical objects directly from
 * {@link ContentItemRepository} and {@link ContentRevisionRepository}.
 * <p>
 * Future implementations may wrap these reads inside a read-only unit-of-work,
 * a cache layer, or a dedicated read model without changing the subscriber contract:
 * <pre>
 * // Intended future shape (not implemented here):
 * readOnlyUnitOfWork.call(() -&gt; {
 *     ContentItem item = source.loadItem(event);
 *     ContentRevision revision = source.loadPublishedRevision(event);
 *     IndexDocument document = mapper.toDocument(item, revision);
 *     indexWriter.upsert(document);
 * });
 * </pre>
 */
public final class RepositoryContentItemProjectionSource implements ContentItemProjectionSource {

    private final ContentItemRepository contentItemRepository;
    private final ContentRevisionRepository contentRevisionRepository;

    /**
     * @param contentItemRepository     repository for loading content items; must not be null
     * @param contentRevisionRepository repository for loading revisions; must not be null
     */
    public RepositoryContentItemProjectionSource(
            final ContentItemRepository contentItemRepository,
            final ContentRevisionRepository contentRevisionRepository) {
        this.contentItemRepository = Objects.requireNonNull(contentItemRepository, "contentItemRepository must not be null");
        this.contentRevisionRepository = Objects.requireNonNull(contentRevisionRepository, "contentRevisionRepository must not be null");
    }

    @Override
    public ContentItem loadItem(final ContentItemPublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return contentItemRepository
                .findByKey(event.siteKey(), event.contentTypeKey(), event.key())
                .orElseThrow(() -> new IllegalStateException(
                        "Content item not found for published event: "
                                + event.siteKey() + "/" + event.contentTypeKey() + "/" + event.key()));
    }

    @Override
    public ContentRevision loadPublishedRevision(final ContentItemPublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return contentRevisionRepository
                .findById(event.publishedRevisionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Published revision not found: " + event.publishedRevisionId()));
    }
}
