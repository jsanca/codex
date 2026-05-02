package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentTypeVersion;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.value.ContentTypeVersionStatus;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link ContentTypeVersionRepository}.
 * <p>
 * Keyed by {@link ContentTypeVersionId}. Suitable for local development and testing.
 */
public final class MemoryContentTypeVersionRepository implements ContentTypeVersionRepository {

    private final Map<ContentTypeVersionId, ContentTypeVersion> store = new ConcurrentHashMap<>();

    @Override
    public ContentTypeVersion save(final ContentTypeVersion version) {
        Objects.requireNonNull(version, "version must not be null");
        store.put(version.id(), version);
        return version;
    }

    @Override
    public Optional<ContentTypeVersion> findById(final ContentTypeVersionId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public Optional<ContentTypeVersion> findByContentTypeAndVersion(final ContentTypeId contentTypeId,
                                                                     final int version) {
        Objects.requireNonNull(contentTypeId, "contentTypeId must not be null");
        if (version < 1) {
            throw new IllegalArgumentException("version must be >= 1");
        }
        return store.values().stream()
                .filter(v -> v.contentTypeId().equals(contentTypeId) && v.version() == version)
                .findFirst();
    }

    @Override
    public Optional<ContentTypeVersion> findLatestPublished(final ContentTypeId contentTypeId) {
        Objects.requireNonNull(contentTypeId, "contentTypeId must not be null");
        return store.values().stream()
                .filter(v -> v.contentTypeId().equals(contentTypeId)
                        && v.status() == ContentTypeVersionStatus.PUBLISHED)
                .max(Comparator.comparingInt(ContentTypeVersion::version));
    }

    @Override
    public List<ContentTypeVersion> findByContentType(final ContentTypeId contentTypeId) {
        Objects.requireNonNull(contentTypeId, "contentTypeId must not be null");
        return store.values().stream()
                .filter(v -> v.contentTypeId().equals(contentTypeId))
                .toList();
    }
}
