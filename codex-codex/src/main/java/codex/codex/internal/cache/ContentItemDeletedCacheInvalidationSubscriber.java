package codex.codex.internal.cache;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.event.ContentItemDeletedEvent;
import codex.fundamentum.api.cache.CacheRegion;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Evicts the {@link ContentItemCacheKey} entry from the identity-read cache when a
 * {@link ContentItemDeletedEvent} is dispatched.
 *
 * <p>A deleted {@link codex.codex.api.model.entity.ContentItem} must not be served from
 * cache. This subscriber evicts the entry so the next read returns the canonical
 * not-found result from the delegate.</p>
 *
 * <p>This subscriber is not wired into runtime yet. Runtime wiring is a separate task.</p>
 */
public final class ContentItemDeletedCacheInvalidationSubscriber
        implements CodexEventSubscriber<ContentItemDeletedEvent> {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ContentItemDeletedCacheInvalidationSubscriber.class);

    private final CacheRegion<ContentItemCacheKey, ContentItem> cache;

    /**
     * @param cache the content item identity-read cache; must not be null
     */
    public ContentItemDeletedCacheInvalidationSubscriber(
            final CacheRegion<ContentItemCacheKey, ContentItem> cache) {
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
    }

    @Override
    public Class<ContentItemDeletedEvent> eventType() {
        return ContentItemDeletedEvent.class;
    }

    @Override
    public void handle(final ContentItemDeletedEvent event) {
        Objects.requireNonNull(event, "event must not be null");

        final ContentItemCacheKey cacheKey =
                new ContentItemCacheKey(event.siteKey(), event.contentTypeKey(), event.key());

        LOGGER.debug("Evicting content item cache entry on delete: site={} contentType={} key={}",
                event.siteKey().value(), event.contentTypeKey().value(), event.key().value());

        cache.evict(cacheKey);
    }
}
