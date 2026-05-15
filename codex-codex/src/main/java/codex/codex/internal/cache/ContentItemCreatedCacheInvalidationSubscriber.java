package codex.codex.internal.cache;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.event.ContentItemCreatedEvent;
import codex.fundamentum.api.cache.CacheRegion;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Evicts the {@link ContentItemCacheKey} entry from the identity-read cache when a
 * {@link ContentItemCreatedEvent} is dispatched.
 *
 * <p>A newly created content item may resolve a key that was previously cached as
 * {@link codex.fundamentum.api.cache.CacheEntry.NotFound}. This subscriber evicts that
 * stale negative entry so the next read hits the delegate and returns the real item.</p>
 *
 * <p>This subscriber is not wired into runtime yet. Runtime wiring is a separate task.</p>
 */
public final class ContentItemCreatedCacheInvalidationSubscriber
        implements CodexEventSubscriber<ContentItemCreatedEvent> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ContentItemCreatedCacheInvalidationSubscriber.class);

    private final CacheRegion<ContentItemCacheKey, ContentItem> cache;

    /**
     * @param cache the content item identity-read cache; must not be null
     */
    public ContentItemCreatedCacheInvalidationSubscriber(
            final CacheRegion<ContentItemCacheKey, ContentItem> cache) {
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
    }

    @Override
    public Class<ContentItemCreatedEvent> eventType() {
        return ContentItemCreatedEvent.class;
    }

    @Override
    public void handle(final ContentItemCreatedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        final ContentItemCacheKey cacheKey =
                new ContentItemCacheKey(event.siteKey(), event.contentTypeKey(), event.key());

        LOGGER.debug("Evicting content item cache entry on create: site={} contentType={} key={}",
                event.siteKey().value(), event.contentTypeKey().value(), event.key().value());

        cache.evict(cacheKey);
    }
}
