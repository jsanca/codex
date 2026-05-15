package codex.codex.internal.cache;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.event.ContentItemRestoredEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentItemStatus;
import codex.fundamentum.api.cache.CacheEntry;
import codex.fundamentum.api.cache.CacheRegion;
import codex.fundamentum.api.cache.ConcurrentMapCacheRegion;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ContentItemRestoredCacheInvalidationSubscriber}.
 */
class ContentItemRestoredCacheInvalidationSubscriberTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Test User");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("article");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("hello-world");

    private CacheRegion<ContentItemCacheKey, ContentItem> cache;
    private ContentItemRestoredCacheInvalidationSubscriber subscriber;

    @BeforeEach
    void setUp() {
        cache = new ConcurrentMapCacheRegion<>();
        subscriber = new ContentItemRestoredCacheInvalidationSubscriber(cache);
    }

    // --- constructor ---

    @Test
    void constructorRejectsNullCache() {
        assertThrows(NullPointerException.class,
                () -> new ContentItemRestoredCacheInvalidationSubscriber(null));
    }

    // --- eventType ---

    @Test
    void eventTypeReturnsContentItemRestoredEventClass() {
        assertEquals(ContentItemRestoredEvent.class, subscriber.eventType());
    }

    // --- handle: null guard ---

    @Test
    void handleRejectsNullEvent() {
        assertThrows(NullPointerException.class, () -> subscriber.handle(null));
    }

    // --- handle: eviction ---

    @Test
    void handleEvictsExpectedCacheKey() {
        final ContentItemCacheKey cacheKey = new ContentItemCacheKey(SITE_KEY, CT_KEY, ITEM_KEY);
        cache.put(cacheKey, CacheEntry.found(buildItem()));

        subscriber.handle(buildEvent());

        assertTrue(cache.get(cacheKey).isEmpty(), "cache entry must be evicted after handle");
    }

    @Test
    void handleEvictsPositiveCacheEntry() {
        final ContentItemCacheKey cacheKey = new ContentItemCacheKey(SITE_KEY, CT_KEY, ITEM_KEY);
        cache.put(cacheKey, CacheEntry.found(buildItem()));

        assertTrue(cache.get(cacheKey).isPresent(), "entry must exist before eviction");
        subscriber.handle(buildEvent());
        assertTrue(cache.get(cacheKey).isEmpty(), "positive entry must be evicted");
    }

    @Test
    void handleEvictsNegativeNotFoundEntry() {
        final ContentItemCacheKey cacheKey = new ContentItemCacheKey(SITE_KEY, CT_KEY, ITEM_KEY);
        cache.put(cacheKey, CacheEntry.notFound());

        assertTrue(cache.get(cacheKey).isPresent(), "not-found entry must exist before eviction");
        subscriber.handle(buildEvent());
        assertTrue(cache.get(cacheKey).isEmpty(), "not-found entry must be evicted");
    }

    @Test
    void handleOnEmptyCacheDoesNotThrow() {
        assertDoesNotThrow(() -> subscriber.handle(buildEvent()));
    }

    // --- helpers ---

    private static ContentItemRestoredEvent buildEvent() {
        return new ContentItemRestoredEvent(
                ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY),
                SITE_KEY,
                CT_KEY,
                ContentTypeVersionId.of("ctv-1"),
                ITEM_KEY,
                ACTOR,
                Instant.now());
    }

    private static ContentItem buildItem() {
        final ActorId actorId = ActorId.of("user-1");
        return ContentItem.builder()
                .id(ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY))
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(ContentTypeVersionId.of("ctv-1"))
                .key(ITEM_KEY)
                .status(ContentItemStatus.DRAFT)
                .currentWorkingRevisionId(ContentRevisionId.of("rev-1"))
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .updatedAt(Instant.now())
                .build();
    }
}
