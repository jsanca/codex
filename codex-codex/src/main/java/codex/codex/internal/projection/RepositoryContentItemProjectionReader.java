package codex.codex.internal.projection;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.projection.ContentItemProjectionReader;
import codex.codex.internal.repository.ContentItemRepository;
import codex.codex.internal.repository.ContentRevisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Repository-backed implementation of {@link ContentItemProjectionReader}.
 *
 * <p>Delegates directly to internal repositories. Remains internal to {@code codex-codex}:
 * projection consumers depend only on the public {@link ContentItemProjectionReader} contract.</p>
 */
public final class RepositoryContentItemProjectionReader implements ContentItemProjectionReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryContentItemProjectionReader.class);

    private final ContentItemRepository contentItemRepository;
    private final ContentRevisionRepository contentRevisionRepository;

    /**
     * @param contentItemRepository     repository for loading content items; must not be null
     * @param contentRevisionRepository repository for loading revisions; must not be null
     */
    public RepositoryContentItemProjectionReader(
            final ContentItemRepository contentItemRepository,
            final ContentRevisionRepository contentRevisionRepository) {
        this.contentItemRepository = Objects.requireNonNull(contentItemRepository,
                "contentItemRepository must not be null");
        this.contentRevisionRepository = Objects.requireNonNull(contentRevisionRepository,
                "contentRevisionRepository must not be null");
    }

    @Override
    public Optional<ContentItem> findContentItem(
            final SiteKey siteKey,
            final ContentTypeKey contentTypeKey,
            final ContentItemKey key) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        LOGGER.debug("findContentItem siteKey={} contentTypeKey={} key={}", siteKey, contentTypeKey, key);
        return contentItemRepository.findByKey(siteKey, contentTypeKey, key);
    }

    @Override
    public Optional<ContentRevision> findContentRevision(final ContentRevisionId revisionId) {
        Objects.requireNonNull(revisionId, "revisionId must not be null");
        LOGGER.debug("findContentRevision revisionId={}", revisionId);
        return contentRevisionRepository.findById(revisionId);
    }
}
