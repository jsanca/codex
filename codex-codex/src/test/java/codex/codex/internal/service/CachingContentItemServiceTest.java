package codex.codex.internal.service;

import codex.codex.api.model.command.ArchiveContentItemCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.DeleteContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.RestoreContentItemCommand;
import codex.codex.api.model.command.UnpublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.value.ContentItemStatus;
import codex.codex.internal.cache.ContentItemCacheKey;
import codex.fundamentum.api.cache.CacheRegion;
import codex.fundamentum.api.cache.ConcurrentMapCacheRegion;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CachingContentItemService}.
 */
class CachingContentItemServiceTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Test User");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("article");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("hello-world");
    private static final ContentItemKey OTHER_ITEM_KEY = ContentItemKey.of("other-post");
    private static final ContentTypeKey OTHER_CT_KEY = ContentTypeKey.of("news");
    private static final SiteKey OTHER_SITE_KEY = SiteKey.of("other-site");

    private FakeContentItemService fakeDelegate;
    private CacheRegion<ContentItemCacheKey, ContentItem> cache;
    private CachingContentItemService service;

    @BeforeEach
    void setUp() {
        fakeDelegate = new FakeContentItemService();
        cache = new ConcurrentMapCacheRegion<>();
        service = new CachingContentItemService(fakeDelegate, cache);
    }

    // --- constructor ---

    @Test
    void constructorRejectsNullDelegate() {
        assertThrows(NullPointerException.class,
                () -> new CachingContentItemService(null, cache));
    }

    @Test
    void constructorRejectsNullCacheRegion() {
        assertThrows(NullPointerException.class,
                () -> new CachingContentItemService(fakeDelegate, null));
    }

    // --- findByKey: null guards ---

    @Test
    void findByKeyRejectsNullSiteKey() {
        assertThrows(NullPointerException.class,
                () -> service.findByKey(null, CT_KEY, ITEM_KEY, ACTOR));
    }

    @Test
    void findByKeyRejectsNullContentTypeKey() {
        assertThrows(NullPointerException.class,
                () -> service.findByKey(SITE_KEY, null, ITEM_KEY, ACTOR));
    }

    @Test
    void findByKeyRejectsNullContentItemKey() {
        assertThrows(NullPointerException.class,
                () -> service.findByKey(SITE_KEY, CT_KEY, null, ACTOR));
    }

    @Test
    void findByKeyRejectsNullActor() {
        assertThrows(NullPointerException.class,
                () -> service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, null));
    }

    // --- findByKey: positive cache behavior ---

    @Test
    void firstLookupCallsDelegateAndReturnsItem() {
        final ContentItem item = buildItem(SITE_KEY, CT_KEY, ITEM_KEY);
        fakeDelegate.register(SITE_KEY, CT_KEY, ITEM_KEY, item);

        final Optional<ContentItem> result = service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);

        assertTrue(result.isPresent());
        assertSame(item, result.get());
        assertEquals(1, fakeDelegate.findByKeyCalls);
    }

    @Test
    void secondLookupReturnsCachedItemAndDoesNotCallDelegateAgain() {
        final ContentItem item = buildItem(SITE_KEY, CT_KEY, ITEM_KEY);
        fakeDelegate.register(SITE_KEY, CT_KEY, ITEM_KEY, item);

        service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);
        final Optional<ContentItem> second = service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);

        assertTrue(second.isPresent());
        assertSame(item, second.get());
        assertEquals(1, fakeDelegate.findByKeyCalls, "delegate must be called only once");
    }

    // --- findByKey: negative cache behavior ---

    @Test
    void firstMissingLookupCallsDelegateAndReturnsEmpty() {
        final Optional<ContentItem> result = service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);

        assertTrue(result.isEmpty());
        assertEquals(1, fakeDelegate.findByKeyCalls);
    }

    @Test
    void secondMissingLookupReturnsCachedNotFoundAndDoesNotCallDelegateAgain() {
        service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);
        final Optional<ContentItem> second = service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);

        assertTrue(second.isEmpty());
        assertEquals(1, fakeDelegate.findByKeyCalls, "delegate must be called only once");
    }

    // --- cache key specificity ---

    @Test
    void differentContentItemKeysProduceDifferentCacheEntries() {
        final ContentItem item1 = buildItem(SITE_KEY, CT_KEY, ITEM_KEY);
        final ContentItem item2 = buildItem(SITE_KEY, CT_KEY, OTHER_ITEM_KEY);
        fakeDelegate.register(SITE_KEY, CT_KEY, ITEM_KEY, item1);
        fakeDelegate.register(SITE_KEY, CT_KEY, OTHER_ITEM_KEY, item2);

        final Optional<ContentItem> r1 = service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);
        final Optional<ContentItem> r2 = service.findByKey(SITE_KEY, CT_KEY, OTHER_ITEM_KEY, ACTOR);

        assertTrue(r1.isPresent());
        assertTrue(r2.isPresent());
        assertNotSame(r1.get(), r2.get());
        assertEquals(2, fakeDelegate.findByKeyCalls);
    }

    @Test
    void differentContentTypeKeysProduceDifferentCacheEntries() {
        final ContentItem item1 = buildItem(SITE_KEY, CT_KEY, ITEM_KEY);
        final ContentItem item2 = buildItem(SITE_KEY, OTHER_CT_KEY, ITEM_KEY);
        fakeDelegate.register(SITE_KEY, CT_KEY, ITEM_KEY, item1);
        fakeDelegate.register(SITE_KEY, OTHER_CT_KEY, ITEM_KEY, item2);

        final Optional<ContentItem> r1 = service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);
        final Optional<ContentItem> r2 = service.findByKey(SITE_KEY, OTHER_CT_KEY, ITEM_KEY, ACTOR);

        assertTrue(r1.isPresent());
        assertTrue(r2.isPresent());
        assertNotSame(r1.get(), r2.get());
        assertEquals(2, fakeDelegate.findByKeyCalls);
    }

    @Test
    void differentSiteKeysProduceDifferentCacheEntries() {
        final ContentItem item1 = buildItem(SITE_KEY, CT_KEY, ITEM_KEY);
        final ContentItem item2 = buildItem(OTHER_SITE_KEY, CT_KEY, ITEM_KEY);
        fakeDelegate.register(SITE_KEY, CT_KEY, ITEM_KEY, item1);
        fakeDelegate.register(OTHER_SITE_KEY, CT_KEY, ITEM_KEY, item2);

        final Optional<ContentItem> r1 = service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);
        final Optional<ContentItem> r2 = service.findByKey(OTHER_SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);

        assertTrue(r1.isPresent());
        assertTrue(r2.isPresent());
        assertNotSame(r1.get(), r2.get());
        assertEquals(2, fakeDelegate.findByKeyCalls);
    }

    // --- delegate accessor ---

    @Test
    void getDelegateReturnsDelegate() {
        assertSame(fakeDelegate, service.getDelegate());
    }

    // --- mutating operations forwarded ---

    @Test
    void publishForwardsToDelegateWithoutCaching() {
        final ContentItem item = buildItem(SITE_KEY, CT_KEY, ITEM_KEY);
        fakeDelegate.register(SITE_KEY, CT_KEY, ITEM_KEY, item);
        fakeDelegate.publishResult = item;

        final ContentItem published = service.publish(
                PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);

        assertSame(item, published);
        assertEquals(1, fakeDelegate.publishCalls);
        assertEquals(0, fakeDelegate.findByKeyCalls, "publish must not trigger a cache read");
    }

    // --- helpers ---

    private static ContentItem buildItem(
            final SiteKey siteKey,
            final ContentTypeKey contentTypeKey,
            final ContentItemKey itemKey) {
        final ContentItemId id = ContentItemId.forItem(siteKey, contentTypeKey, itemKey);
        final ActorId actorId = ActorId.of("user-1");
        return ContentItem.builder()
                .id(id)
                .siteKey(siteKey)
                .contentTypeKey(contentTypeKey)
                .contentTypeVersionId(ContentTypeVersionId.of("ctv-1"))
                .key(itemKey)
                .status(ContentItemStatus.PUBLISHED)
                .currentWorkingRevisionId(ContentRevisionId.of("rev-1"))
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .updatedAt(Instant.now())
                .build();
    }

    // --- inner types ---

    private static final class FakeContentItemService implements ContentItemService {

        private final Map<String, ContentItem> items = new HashMap<>();
        int findByKeyCalls;
        int publishCalls;
        ContentItem publishResult;

        void register(
                final SiteKey siteKey,
                final ContentTypeKey contentTypeKey,
                final ContentItemKey itemKey,
                final ContentItem item) {
            items.put(siteKey.value() + ":" + contentTypeKey.value() + ":" + itemKey.value(), item);
        }

        @Override
        public ContentItem create(final CreateContentItemCommand command, final Actor actor) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public Optional<ContentItem> findByKey(
                final SiteKey siteKey,
                final ContentTypeKey contentTypeKey,
                final ContentItemKey key,
                final Actor actor) {
            findByKeyCalls++;
            return Optional.ofNullable(
                    items.get(siteKey.value() + ":" + contentTypeKey.value() + ":" + key.value()));
        }

        @Override
        public List<ContentItem> findByContentType(
                final SiteKey siteKey, final ContentTypeKey contentTypeKey, final Actor actor) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public List<ContentItem> findAll(final Actor actor) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public ContentItem update(final UpdateContentItemCommand command, final Actor actor) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public ContentItem archive(final ArchiveContentItemCommand command, final Actor actor) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public void delete(final DeleteContentItemCommand command, final Actor actor) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public ContentItem restore(final RestoreContentItemCommand command, final Actor actor) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public ContentItem unpublish(final UnpublishContentItemCommand command, final Actor actor) {
            throw new UnsupportedOperationException("not used in this test");
        }

        @Override
        public ContentItem publish(final PublishContentItemCommand command, final Actor actor) {
            publishCalls++;
            return publishResult;
        }
    }
}
