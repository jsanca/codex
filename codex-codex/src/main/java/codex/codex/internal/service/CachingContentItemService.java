package codex.codex.internal.service;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentItemService;
import codex.codex.internal.cache.ContentItemCacheKey;
import codex.fundamentum.api.cache.CacheEntry;
import codex.fundamentum.api.cache.CacheRegion;
import codex.fundamentum.api.model.Actor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * A {@link ContentItemService} decorator that caches identity-read results for
 * {@link #findByKey(SiteKey, ContentTypeKey, ContentItemKey, Actor)}.
 *
 * <p>Both positive ({@link CacheEntry.Found}) and negative ({@link CacheEntry.NotFound})
 * results are cached. Caching negative results avoids repeated canonical-storage hits for
 * absent resources. Cache invalidation for stale entries is handled by a future
 * event-driven invalidation subscriber — it is not the responsibility of this decorator.</p>
 *
 * <p>All mutating operations are forwarded transparently to the delegate via
 * {@link ForwardingContentItemService}. This decorator does not evict on mutation;
 * that will be driven by domain events in a future task.</p>
 *
 * <p>This decorator must not be wired into the runtime until cache invalidation subscribers
 * exist.</p>
 *
 * @see ContentItemCacheKey
 * @see codex.fundamentum.api.cache.CaffeineCacheRegion
 */
public final class CachingContentItemService implements ForwardingContentItemService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CachingContentItemService.class);

    private final ContentItemService delegate;
    private final CacheRegion<ContentItemCacheKey, ContentItem> byKeyCache;

    /**
     * @param delegate    the service to forward all calls to; must not be null
     * @param byKeyCache  the cache region used for identity-read results; must not be null
     */
    public CachingContentItemService(
            final ContentItemService delegate,
            final CacheRegion<ContentItemCacheKey, ContentItem> byKeyCache) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        this.byKeyCache = Objects.requireNonNull(byKeyCache, "byKeyCache must not be null");
    }

    @Override
    public ContentItemService getDelegate() {
        return delegate;
    }

    /**
     * Returns the content item for the given composite key, using the cache as a read-through.
     *
     * <p>On a cache miss, the delegate is consulted and the result is stored:
     * {@link CacheEntry.Found} for an existing item, {@link CacheEntry.NotFound} for an absent one.
     * Subsequent calls for the same key return the cached result without consulting the delegate.</p>
     *
     * @param siteKey        the site; must not be null
     * @param contentTypeKey the content type; must not be null
     * @param key            the item key; must not be null
     * @param actor          the requesting actor; must not be null
     * @return the content item, or empty if absent
     */
    @Override
    public Optional<ContentItem> findByKey(
            final SiteKey siteKey,
            final ContentTypeKey contentTypeKey,
            final ContentItemKey key,
            final Actor actor) {
        Objects.requireNonNull(siteKey, "siteKey must not be null");
        Objects.requireNonNull(contentTypeKey, "contentTypeKey must not be null");
        Objects.requireNonNull(key, "key must not be null");
        Objects.requireNonNull(actor, "actor must not be null");

        final ContentItemCacheKey cacheKey = new ContentItemCacheKey(siteKey, contentTypeKey, key);

        final CacheEntry<ContentItem> entry = byKeyCache.getOrLoad(cacheKey, () -> {
            LOGGER.debug("Cache miss for content item [{}/{}/{}] — loading from delegate",
                    siteKey.value(), contentTypeKey.value(), key.value());
            return delegate.findByKey(siteKey, contentTypeKey, key, actor)
                    .<CacheEntry<ContentItem>>map(CacheEntry::found)
                    .orElseGet(CacheEntry::notFound);
        });

        return switch (entry) {
            case CacheEntry.Found<ContentItem> found -> {
                LOGGER.debug("Cache hit (found) for content item [{}/{}/{}]",
                        siteKey.value(), contentTypeKey.value(), key.value());
                yield Optional.of(found.value());
            }
            case CacheEntry.NotFound<ContentItem> ignored -> {
                LOGGER.debug("Cache hit (not found) for content item [{}/{}/{}]",
                        siteKey.value(), contentTypeKey.value(), key.value());
                yield Optional.empty();
            }
        };
    }
}
