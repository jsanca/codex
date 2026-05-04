package codex.index.internal;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.projection.ContentItemProjectionReader;

import java.util.Objects;

/**
 * Adapts {@link ContentItemProjectionReader} to the index-side {@link ContentItemProjectionSource}.
 *
 * <p>Translates a {@link ContentItemPublishedEvent} into the arguments required by the public
 * core projection contract, then raises an {@link IllegalStateException} if canonical data
 * cannot be found — which would indicate a system inconsistency.</p>
 */
public final class ReaderContentItemProjectionSource implements ContentItemProjectionSource {

    private final ContentItemProjectionReader projectionReader;

    /**
     * @param projectionReader the public core projection contract; must not be null
     */
    public ReaderContentItemProjectionSource(final ContentItemProjectionReader projectionReader) {
        this.projectionReader = Objects.requireNonNull(projectionReader,
                "projectionReader must not be null");
    }

    @Override
    public ContentItem loadItem(final ContentItemPublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return projectionReader
                .findContentItem(event.siteKey(), event.contentTypeKey(), event.key())
                .orElseThrow(() -> new IllegalStateException(
                        "Content item not found for published event: "
                                + event.siteKey() + "/" + event.contentTypeKey() + "/" + event.key()));
    }

    @Override
    public ContentRevision loadPublishedRevision(final ContentItemPublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        return projectionReader
                .findContentRevision(event.publishedRevisionId())
                .orElseThrow(() -> new IllegalStateException(
                        "Published revision not found: " + event.publishedRevisionId()));
    }
}
