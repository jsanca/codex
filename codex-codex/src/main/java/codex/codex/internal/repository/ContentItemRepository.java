package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.List;
import java.util.Optional;

/**
 * Storage contract for {@link ContentItem} entities.
 * <p>
 * The logical identity of a content item is {@code siteKey + contentTypeKey + contentItemKey}.
 * All scoped operations require all three parameters. Implementations own only storage
 * concerns — no business logic belongs here.
 */
public interface ContentItemRepository {

    /**
     * Persists a content item, creating or replacing any existing record with the same
     * {@code siteKey + contentTypeKey + itemKey} identity.
     *
     * @param item the content item to save; must not be null
     * @return the saved content item
     */
    ContentItem save(ContentItem item);

    /**
     * Looks up a content item by its scoped identity.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param key            the content item key; must not be null
     * @return the content item wrapped in an {@link Optional}, or empty if not found
     */
    Optional<ContentItem> findByKey(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey key);

    /**
     * Returns {@code true} if a content item with the given scoped identity exists.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @param key            the content item key; must not be null
     * @return {@code true} if found
     */
    boolean existsByKey(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey key);

    /**
     * Returns all content items for a given site and content type as an immutable snapshot.
     *
     * @param siteKey        the site scope; must not be null
     * @param contentTypeKey the content type key; must not be null
     * @return list of matching content items; never null
     */
    List<ContentItem> findByContentType(SiteKey siteKey, ContentTypeKey contentTypeKey);

    /**
     * Returns all stored content items across all sites as an immutable snapshot.
     *
     * @return list of all content items; never null
     */
    List<ContentItem> findAll();
}
