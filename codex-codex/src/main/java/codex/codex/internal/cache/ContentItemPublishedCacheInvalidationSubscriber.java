package codex.codex.internal.cache;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.fundamentum.api.cache.CacheRegion;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Evicts the {@link ContentItemCacheKey} entry from the identity-read cache when a
 * {@link ContentItemPublishedEvent} is dispatched.
 *
 * <p>Publishing may change the current published revision pointer on a
 * {@link codex.codex.api.model.entity.ContentItem}. The cached item would be stale if
 * it was loaded before this publication. This subscriber evicts it so the next read
 * returns the updated item from the delegate.</p>
 *
 * <p>This subscriber is not wired into runtime yet. Runtime wiring is a separate task.</p>
 */
public final class ContentItemPublishedCacheInvalidationSubscriber
        implements CodexEventSubscriber<ContentItemPublishedEvent> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ContentItemPublishedCacheInvalidationSubscriber.class);

    private final CacheRegion<ContentItemCacheKey, ContentItem> cache;

    /**
     * @param cache the content item identity-read cache; must not be null
     */
    public ContentItemPublishedCacheInvalidationSubscriber(
            final CacheRegion<ContentItemCacheKey, ContentItem> cache) {
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
    }

    @Override
    public Class<ContentItemPublishedEvent> eventType() {
        return ContentItemPublishedEvent.class;
    }

    @Override
    public void handle(final ContentItemPublishedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        final ContentItemCacheKey cacheKey =
                new ContentItemCacheKey(event.siteKey(), event.contentTypeKey(), event.key());

        LOGGER.debug("Evicting content item cache entry on publish: site={} contentType={} key={}",
                event.siteKey().value(), event.contentTypeKey().value(), event.key().value());

        cache.evict(cacheKey);
    }
}
