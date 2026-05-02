package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * In-memory implementation of {@link ContentItemRepository}.
 * Suitable for local development and testing.
 * <p>
 * Content items are keyed by the composite {@code siteKey + contentTypeKey + itemKey} identity,
 * allowing the same {@link ContentItemKey} to exist independently under different sites or
 * content types.
 */
public final class MemoryContentItemRepository implements ContentItemRepository {

    private final MemoryStore<RepositoryKey, ContentItem> store =
            new MemoryStore<>(item -> new RepositoryKey(item.siteKey(), item.contentTypeKey(), item.key()));

    @Override
    public ContentItem save(final ContentItem item) {
        return store.save(item);
    }

    @Override
    public Optional<ContentItem> findByKey(final SiteKey siteKey,
                                            final ContentTypeKey contentTypeKey,
                                            final ContentItemKey key) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return store.findByKey(new RepositoryKey(siteKey, contentTypeKey, key));
    }

    @Override
    public boolean existsByKey(final SiteKey siteKey,
                                final ContentTypeKey contentTypeKey,
                                final ContentItemKey key) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        return store.existsByKey(new RepositoryKey(siteKey, contentTypeKey, key));
    }

    @Override
    public List<ContentItem> findByContentType(final SiteKey siteKey, final ContentTypeKey contentTypeKey) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        return store.findWhere(item -> item.siteKey().equals(siteKey)
                        && item.contentTypeKey().equals(contentTypeKey))
                .stream()
                .sorted(Comparator.comparing(item -> item.key().value()))
                .toList();
    }

    @Override
    public List<ContentItem> findAll() {
        return store.findAll();
    }

    private record RepositoryKey(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey itemKey) {}
}
