package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory implementation of {@link ContentTypeRepository}.
 * Suitable for local development and testing.
 * <p>
 * Content types are keyed by the composite {@code siteKey + contentTypeKey} identity,
 * allowing the same {@link ContentTypeKey} to exist independently under different sites.
 */
public final class MemoryContentTypeRepository implements ContentTypeRepository {

    private final MemoryStore<RepositoryKey, ContentType> store =
            new MemoryStore<>(ct -> new RepositoryKey(ct.siteKey(), ct.key()));

    @Override
    public ContentType save(final ContentType contentType) {
        return store.save(contentType);
    }

    @Override
    public Optional<ContentType> findByKey(final SiteKey siteKey, final ContentTypeKey key) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return store.findByKey(new RepositoryKey(siteKey, key));
    }

    @Override
    public boolean existsByKey(final SiteKey siteKey, final ContentTypeKey key) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return store.existsByKey(new RepositoryKey(siteKey, key));
    }

    @Override
    public List<ContentType> findBySiteKey(final SiteKey siteKey) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        return store.findWhere(ct -> ct.siteKey().equals(siteKey));
    }

    @Override
    public List<ContentType> findAll() {
        return store.findAll();
    }

    private record RepositoryKey(SiteKey siteKey, ContentTypeKey contentTypeKey) {}
}
