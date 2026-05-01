package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for {@link ContentType} entities.
 * <p>
 * The logical identity of a content type is {@code siteKey + key}. All scoped
 * operations therefore require both parameters. Implementations own only storage
 * concerns; no business logic belongs here.
 */
public interface ContentTypeRepository {

    /**
     * Persists a content type, creating or replacing any existing record with the same
     * {@code siteKey + key} identity.
     *
     * @param contentType the content type to save; must not be null
     * @return the saved content type
     */
    ContentType save(ContentType contentType);

    /**
     * Looks up a content type by its scoped identity.
     *
     * @param siteKey the site scope; must not be null
     * @param key     the content type key; must not be null
     * @return the content type wrapped in an {@link Optional}, or empty if not found
     */
    Optional<ContentType> findByKey(SiteKey siteKey, ContentTypeKey key);

    /**
     * Returns {@code true} if a content type with the given scoped identity exists.
     *
     * @param siteKey the site scope; must not be null
     * @param key     the content type key; must not be null
     * @return {@code true} if found
     */
    boolean existsByKey(SiteKey siteKey, ContentTypeKey key);

    /**
     * Returns all content types for a given site as an immutable snapshot.
     *
     * @param siteKey the site scope; must not be null
     * @return list of content types for the site; never null
     */
    List<ContentType> findBySiteKey(SiteKey siteKey);

    /**
     * Returns all stored content types across all sites as an immutable snapshot.
     *
     * @return list of all content types; never null
     */
    List<ContentType> findAll();
}
