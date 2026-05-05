package codex.codex.internal.cache;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;

import java.util.Objects;

/**
 * Identity-read cache key for {@link codex.codex.api.model.entity.ContentItem} lookups.
 *
 * <p>Identifies a content item by its three-part composite identity: site, content type,
 * and item key. Used by {@link codex.codex.internal.service.CachingContentItemService} to
 * index positive ({@link codex.fundamentum.api.cache.CacheEntry.Found}) and negative
 * ({@link codex.fundamentum.api.cache.CacheEntry.NotFound}) cache entries.</p>
 *
 * <p>Actor is intentionally excluded — it is audit context, not part of item identity.
 * Locale, variant, tenant id, and channel are not yet part of the key; they may be added
 * in a future task when multi-tenant or multi-locale caching is required.</p>
 *
 * @param siteKey         the site the item belongs to; must not be null
 * @param contentTypeKey  the content type the item belongs to; must not be null
 * @param contentItemKey  the item's own key within its site and content type; must not be null
 */
public record ContentItemCacheKey(
        SiteKey siteKey,
        ContentTypeKey contentTypeKey,
        ContentItemKey contentItemKey) {

    public ContentItemCacheKey {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(contentItemKey, "contentItemKey must not be null");
    }
}
