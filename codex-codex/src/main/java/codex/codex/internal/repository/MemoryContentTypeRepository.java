package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link ContentTypeRepository}.
 * Suitable for local development and testing.
 * <p>
 * Content types are keyed by the composite {@code siteKey + contentTypeKey} identity,
 * allowing the same {@link ContentTypeKey} to exist independently under different sites.
 */
public final class MemoryContentTypeRepository implements ContentTypeRepository {

    private final Map<RepositoryKey, ContentType> store = new ConcurrentHashMap<>();

    @Override
    public ContentType save(final ContentType contentType) {
        Objects.requireNonNull(contentType, "contentType must not be null");
        store.put(new RepositoryKey(contentType.siteKey(), contentType.key()), contentType);
        return contentType;
    }

    @Override
    public Optional<ContentType> findByKey(final SiteKey siteKey, final ContentTypeKey key) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(store.get(new RepositoryKey(siteKey, key)));
    }

    @Override
    public boolean existsByKey(final SiteKey siteKey, final ContentTypeKey key) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return store.containsKey(new RepositoryKey(siteKey, key));
    }

    @Override
    public List<ContentType> findBySiteKey(final SiteKey siteKey) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        return store.entrySet().stream()
                .filter(entry -> entry.getKey().siteKey().equals(siteKey))
                .map(Map.Entry::getValue)
                .toList();
    }

    @Override
    public List<ContentType> findAll() {
        return List.copyOf(store.values());
    }

    private record RepositoryKey(SiteKey siteKey, ContentTypeKey contentTypeKey) {}
}
