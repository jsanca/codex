package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MemoryContentItemRepository}.
 */
class MemoryContentItemRepositoryTest {

    private MemoryContentItemRepository repository;

    private static final SiteKey SITE_ACME = SiteKey.of("acme");
    private static final SiteKey SITE_BETA = SiteKey.of("beta");
    private static final ContentTypeKey BLOG_POST = ContentTypeKey.of("blog-post");
    private static final ContentTypeKey PRODUCT = ContentTypeKey.of("product");
    private static final ContentTypeVersionId VERSION_ID = ContentTypeVersionId.of("content-type-version:acme:blog-post:v1");

    @BeforeEach
    void setUp() {
        repository = new MemoryContentItemRepository();
    }

    private ContentItem buildItem(final SiteKey siteKey, final ContentTypeKey contentTypeKey,
                                   final ContentItemKey itemKey) {
        final ActorId actor = ActorId.of("user-1");
        return ContentItem.builder()
                .id(ContentItemId.forItem(siteKey, contentTypeKey, itemKey))
                .siteKey(siteKey)
                .contentTypeKey(contentTypeKey)
                .contentTypeVersionId(VERSION_ID)
                .key(itemKey)
                .owner(actor)
                .createdBy(actor)
                .updatedBy(actor)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void saveReturnsSavedItem() {
        final ContentItemKey key = ContentItemKey.of("my-post");
        final ContentItem item = buildItem(SITE_ACME, BLOG_POST, key);
        final ContentItem saved = repository.save(item);
        assertEquals(item, saved);
    }

    @Test
    void findByKeyReturnsSavedItem() {
        final ContentItemKey key = ContentItemKey.of("my-post");
        final ContentItem item = buildItem(SITE_ACME, BLOG_POST, key);
        repository.save(item);

        final Optional<ContentItem> found = repository.findByKey(SITE_ACME, BLOG_POST, key);
        assertTrue(found.isPresent());
        assertEquals(item, found.get());
    }

    @Test
    void findByKeyReturnsEmptyWhenMissing() {
        final Optional<ContentItem> found = repository.findByKey(SITE_ACME, BLOG_POST, ContentItemKey.of("ghost"));
        assertTrue(found.isEmpty());
    }

    @Test
    void findByKeyReturnsEmptyWhenSiteKeyDiffers() {
        final ContentItemKey key = ContentItemKey.of("my-post");
        repository.save(buildItem(SITE_ACME, BLOG_POST, key));

        assertTrue(repository.findByKey(SITE_BETA, BLOG_POST, key).isEmpty());
    }

    @Test
    void findByKeyReturnsEmptyWhenContentTypeKeyDiffers() {
        final ContentItemKey key = ContentItemKey.of("my-post");
        repository.save(buildItem(SITE_ACME, BLOG_POST, key));

        assertTrue(repository.findByKey(SITE_ACME, PRODUCT, key).isEmpty());
    }

    @Test
    void existsByKeyReturnsTrueWhenSaved() {
        final ContentItemKey key = ContentItemKey.of("my-post");
        repository.save(buildItem(SITE_ACME, BLOG_POST, key));

        assertTrue(repository.existsByKey(SITE_ACME, BLOG_POST, key));
    }

    @Test
    void existsByKeyReturnsFalseWhenMissing() {
        assertFalse(repository.existsByKey(SITE_ACME, BLOG_POST, ContentItemKey.of("ghost")));
    }

    @Test
    void sameItemKeyCanExistUnderDifferentContentTypes() {
        final ContentItemKey key = ContentItemKey.of("item-one");
        repository.save(buildItem(SITE_ACME, BLOG_POST, key));
        repository.save(buildItem(SITE_ACME, PRODUCT, key));

        assertTrue(repository.existsByKey(SITE_ACME, BLOG_POST, key));
        assertTrue(repository.existsByKey(SITE_ACME, PRODUCT, key));
    }

    @Test
    void sameItemKeyCanExistUnderDifferentSites() {
        final ContentItemKey key = ContentItemKey.of("item-one");
        repository.save(buildItem(SITE_ACME, BLOG_POST, key));
        repository.save(buildItem(SITE_BETA, BLOG_POST, key));

        assertTrue(repository.existsByKey(SITE_ACME, BLOG_POST, key));
        assertTrue(repository.existsByKey(SITE_BETA, BLOG_POST, key));
    }

    @Test
    void findByContentTypeReturnsOnlyMatchingItems() {
        repository.save(buildItem(SITE_ACME, BLOG_POST, ContentItemKey.of("post-one")));
        repository.save(buildItem(SITE_ACME, BLOG_POST, ContentItemKey.of("post-two")));
        repository.save(buildItem(SITE_ACME, PRODUCT, ContentItemKey.of("item-one")));
        repository.save(buildItem(SITE_BETA, BLOG_POST, ContentItemKey.of("post-three")));

        final List<ContentItem> results = repository.findByContentType(SITE_ACME, BLOG_POST);
        assertEquals(2, results.size());
        assertTrue(results.stream().allMatch(item -> item.siteKey().equals(SITE_ACME)
                && item.contentTypeKey().equals(BLOG_POST)));
    }

    @Test
    void findAllReturnsAllSavedItems() {
        repository.save(buildItem(SITE_ACME, BLOG_POST, ContentItemKey.of("post-one")));
        repository.save(buildItem(SITE_BETA, PRODUCT, ContentItemKey.of("item-one")));

        final List<ContentItem> all = repository.findAll();
        assertEquals(2, all.size());
    }

    @Test
    void saveRejectsNull() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
    }

    @Test
    void findByKeyRejectsNullSiteKey() {
        assertThrows(NullPointerException.class,
                () -> repository.findByKey(null, BLOG_POST, ContentItemKey.of("post")));
    }

    @Test
    void findByKeyRejectsNullContentTypeKey() {
        assertThrows(NullPointerException.class,
                () -> repository.findByKey(SITE_ACME, null, ContentItemKey.of("post")));
    }

    @Test
    void findByKeyRejectsNullItemKey() {
        assertThrows(NullPointerException.class,
                () -> repository.findByKey(SITE_ACME, BLOG_POST, null));
    }

    @Test
    void existsByKeyRejectsNullArguments() {
        assertThrows(NullPointerException.class,
                () -> repository.existsByKey(null, BLOG_POST, ContentItemKey.of("post")));
        assertThrows(NullPointerException.class,
                () -> repository.existsByKey(SITE_ACME, null, ContentItemKey.of("post")));
        assertThrows(NullPointerException.class,
                () -> repository.existsByKey(SITE_ACME, BLOG_POST, null));
    }
}
