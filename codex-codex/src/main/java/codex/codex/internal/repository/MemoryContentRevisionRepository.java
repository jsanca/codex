package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.value.ContentRevisionStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory implementation of {@link ContentRevisionRepository}.
 * Suitable for local development and testing.
 * <p>
 * Revisions are keyed by {@link ContentRevisionId}. The {@link MemoryStore} backing is
 * shared with other repository implementations to keep the in-memory pattern consistent.
 */
public final class MemoryContentRevisionRepository implements ContentRevisionRepository {

    private final MemoryStore<ContentRevisionId, ContentRevision> store =
            new MemoryStore<>(ContentRevision::id);

    @Override
    public ContentRevision save(final ContentRevision revision) {
        return store.save(revision);
    }

    @Override
    public Optional<ContentRevision> findById(final ContentRevisionId id) {
        return store.findByKey(id);
    }

    @Override
    public Optional<ContentRevision> findByContentItemAndRevision(final ContentItemId contentItemId,
                                                                   final int revisionNumber) {
        Objects.requireNonNull(contentItemId, "contentItemId must not be null");
        if (revisionNumber < 1) {
            throw new IllegalArgumentException("revisionNumber must be >= 1");
        }
        return store.findFirstWhere(r -> r.contentItemId().equals(contentItemId)
                && r.revisionNumber() == revisionNumber);
    }

    @Override
    public Optional<ContentRevision> findLatestWorking(final ContentItemId contentItemId) {
        Objects.requireNonNull(contentItemId, "contentItemId must not be null");
        return store.findWhere(r -> r.contentItemId().equals(contentItemId)
                        && r.status() == ContentRevisionStatus.WORKING)
                .stream()
                .max(Comparator.comparingInt(ContentRevision::revisionNumber));
    }

    @Override
    public Optional<ContentRevision> findLatestPublished(final ContentItemId contentItemId) {
        Objects.requireNonNull(contentItemId, "contentItemId must not be null");
        return store.findWhere(r -> r.contentItemId().equals(contentItemId)
                        && r.status() == ContentRevisionStatus.PUBLISHED)
                .stream()
                .max(Comparator.comparingInt(ContentRevision::revisionNumber));
    }

    @Override
    public List<ContentRevision> findByContentItem(final ContentItemId contentItemId) {
        Objects.requireNonNull(contentItemId, "contentItemId must not be null");
        return store.findWhere(r -> r.contentItemId().equals(contentItemId))
                .stream()
                .sorted(Comparator.comparingInt(ContentRevision::revisionNumber))
                .toList();
    }
}
