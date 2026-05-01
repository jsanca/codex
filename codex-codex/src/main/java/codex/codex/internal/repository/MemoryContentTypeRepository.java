package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link ContentTypeRepository}.
 * Suitable for local development and testing.
 */
public final class MemoryContentTypeRepository implements ContentTypeRepository {

    private final Map<ContentTypeKey, ContentType> store = new ConcurrentHashMap<>();

    @Override
    public ContentType save(final ContentType contentType) {
        Objects.requireNonNull(contentType, "contentType must not be null");
        store.put(contentType.key(), contentType);
        return contentType;
    }

    @Override
    public Optional<ContentType> findByKey(final ContentTypeKey key) {
        Objects.requireNonNull(key, "key must not be null");
        return Optional.ofNullable(store.get(key));
    }

    @Override
    public boolean existsByKey(final ContentTypeKey key) {
        return store.containsKey(Objects.requireNonNull(key, "key must not be null"));
    }

    @Override
    public List<ContentType> findAll() {
        return List.copyOf(store.values());
    }
}
