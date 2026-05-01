package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for {@link ContentType} entities.
 * Implementations own only storage concerns; no business logic belongs here.
 */
public interface ContentTypeRepository {

    /**
     * Persists a content type, creating or replacing any existing record with the same key.
     *
     * @param contentType the content type to save; must not be null
     * @return the saved content type
     */
    ContentType save(ContentType contentType);

    /**
     * Looks up a content type by its stable key.
     *
     * @param key the content type key; must not be null
     * @return the content type wrapped in an {@link Optional}, or empty if not found
     */
    Optional<ContentType> findByKey(ContentTypeKey key);

    /**
     * Returns {@code true} if a content type with the given key already exists.
     *
     * @param key the content type key; must not be null
     * @return {@code true} if found
     */
    boolean existsByKey(ContentTypeKey key);

    /**
     * Returns all stored content types as an immutable snapshot.
     *
     * @return list of all content types; never null
     */
    List<ContentType> findAll();
}
