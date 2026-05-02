package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentTypeVersion;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.value.ContentTypeVersionStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory implementation of {@link ContentTypeVersionRepository}.
 * <p>
 * Keyed by {@link ContentTypeVersionId}. Suitable for local development and testing.
 */
public final class MemoryContentTypeVersionRepository implements ContentTypeVersionRepository {

    private final MemoryStore<ContentTypeVersionId, ContentTypeVersion> store =
            new MemoryStore<>(ContentTypeVersion::id);

    @Override
    public ContentTypeVersion save(final ContentTypeVersion version) {
        Objects.requireNonNull(version, "version must not be null");
        return store.save(version);
    }

    @Override
    public Optional<ContentTypeVersion> findById(final ContentTypeVersionId id) {
        return store.findByKey(id);
    }

    @Override
    public Optional<ContentTypeVersion> findByContentTypeAndVersion(final ContentTypeId contentTypeId,
                                                                     final int version) {
        Objects.requireNonNull(contentTypeId, "contentTypeId must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
        return store.findFirstWhere(v -> v.contentTypeId().equals(contentTypeId) && v.version() == version);
    }

    @Override
    public Optional<ContentTypeVersion> findLatestPublished(final ContentTypeId contentTypeId) {
        Objects.requireNonNull(contentTypeId, "contentTypeId must not be null");
        return store.findWhere(v -> v.contentTypeId().equals(contentTypeId)
                        && v.status() == ContentTypeVersionStatus.PUBLISHED)
                .stream()
                .max(Comparator.comparingInt(ContentTypeVersion::version));
    }

    @Override
    public List<ContentTypeVersion> findByContentType(final ContentTypeId contentTypeId) {
        Objects.requireNonNull(contentTypeId, "contentTypeId must not be null");
        return store.findWhere(v -> v.contentTypeId().equals(contentTypeId));
    }
}
