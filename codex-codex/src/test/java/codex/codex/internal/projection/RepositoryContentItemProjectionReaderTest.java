package codex.codex.internal.projection;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.codex.internal.repository.ContentItemRepository;
import codex.codex.internal.repository.ContentRevisionRepository;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RepositoryContentItemProjectionReader}.
 */
class RepositoryContentItemProjectionReaderTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID = ContentRevisionId.of("rev-1");
    private static final ActorId ACTOR_ID = ActorId.of("user-1");
    private static final Instant NOW = Instant.parse("2026-05-01T10:00:00Z");

    private ContentItem item;
    private ContentRevision revision;

    @BeforeEach
    void setUp() {
        item = ContentItem.builder()
                .id(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .key(ITEM_KEY)
                .currentWorkingRevisionId(REVISION_ID)
                .owner(ACTOR_ID)
                .createdBy(ACTOR_ID)
                .updatedBy(ACTOR_ID)
                .updatedAt(NOW)
                .build();

        revision = ContentRevision.builder()
                .id(REVISION_ID)
                .contentItemId(ITEM_ID)
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .createdBy(ACTOR_ID)
                .build();
    }

    @Test
    void rejectsNullContentItemRepository() {
        assertThrows(NullPointerException.class, () ->
                new RepositoryContentItemProjectionReader(null, new StubRevisionRepository(revision)));
    }

    @Test
    void rejectsNullContentRevisionRepository() {
        assertThrows(NullPointerException.class, () ->
                new RepositoryContentItemProjectionReader(new StubItemRepository(item), null));
    }

    @Test
    void findContentItemRejectsNullSiteKey() {
        final RepositoryContentItemProjectionReader reader =
                new RepositoryContentItemProjectionReader(
                        new StubItemRepository(item), new StubRevisionRepository(revision));
        assertThrows(NullPointerException.class,
                () -> reader.findContentItem(null, CT_KEY, ITEM_KEY));
    }

    @Test
    void findContentItemRejectsNullContentTypeKey() {
        final RepositoryContentItemProjectionReader reader =
                new RepositoryContentItemProjectionReader(
                        new StubItemRepository(item), new StubRevisionRepository(revision));
        assertThrows(NullPointerException.class,
                () -> reader.findContentItem(SITE_KEY, null, ITEM_KEY));
    }

    @Test
    void findContentItemRejectsNullKey() {
        final RepositoryContentItemProjectionReader reader =
                new RepositoryContentItemProjectionReader(
                        new StubItemRepository(item), new StubRevisionRepository(revision));
        assertThrows(NullPointerException.class,
                () -> reader.findContentItem(SITE_KEY, CT_KEY, null));
    }

    @Test
    void findContentRevisionRejectsNullRevisionId() {
        final RepositoryContentItemProjectionReader reader =
                new RepositoryContentItemProjectionReader(
                        new StubItemRepository(item), new StubRevisionRepository(revision));
        assertThrows(NullPointerException.class, () -> reader.findContentRevision(null));
    }

    @Test
    void findContentItemReturnsExistingItem() {
        final RepositoryContentItemProjectionReader reader =
                new RepositoryContentItemProjectionReader(
                        new StubItemRepository(item), new StubRevisionRepository(revision));
        assertEquals(Optional.of(item), reader.findContentItem(SITE_KEY, CT_KEY, ITEM_KEY));
    }

    @Test
    void findContentItemReturnsEmptyWhenMissing() {
        final RepositoryContentItemProjectionReader reader =
                new RepositoryContentItemProjectionReader(
                        new StubItemRepository(null), new StubRevisionRepository(revision));
        assertEquals(Optional.empty(), reader.findContentItem(SITE_KEY, CT_KEY, ITEM_KEY));
    }

    @Test
    void findContentRevisionReturnsExistingRevision() {
        final RepositoryContentItemProjectionReader reader =
                new RepositoryContentItemProjectionReader(
                        new StubItemRepository(item), new StubRevisionRepository(revision));
        assertEquals(Optional.of(revision), reader.findContentRevision(REVISION_ID));
    }

    @Test
    void findContentRevisionReturnsEmptyWhenMissing() {
        final RepositoryContentItemProjectionReader reader =
                new RepositoryContentItemProjectionReader(
                        new StubItemRepository(item), new StubRevisionRepository(null));
        assertEquals(Optional.empty(), reader.findContentRevision(REVISION_ID));
    }

    private static final class StubItemRepository implements ContentItemRepository {
        private final ContentItem result;

        StubItemRepository(final ContentItem result) {
            this.result = result;
        }

        @Override
        public ContentItem save(final ContentItem item) { return item; }

        @Override
        public Optional<ContentItem> findByKey(
                final SiteKey siteKey, final ContentTypeKey contentTypeKey, final ContentItemKey key) {
            return Optional.ofNullable(result);
        }

        @Override
        public boolean existsByKey(
                final SiteKey siteKey, final ContentTypeKey contentTypeKey, final ContentItemKey key) {
            return result != null;
        }

        @Override
        public List<ContentItem> findByContentType(
                final SiteKey siteKey, final ContentTypeKey contentTypeKey) {
            return result == null ? List.of() : List.of(result);
        }

        @Override
        public List<ContentItem> findAll() {
            return result == null ? List.of() : List.of(result);
        }
    }

    private static final class StubRevisionRepository implements ContentRevisionRepository {
        private final ContentRevision result;

        StubRevisionRepository(final ContentRevision result) {
            this.result = result;
        }

        @Override
        public ContentRevision save(final ContentRevision revision) { return revision; }

        @Override
        public Optional<ContentRevision> findById(final ContentRevisionId id) {
            return Optional.ofNullable(result);
        }

        @Override
        public Optional<ContentRevision> findByContentItemAndRevision(
                final ContentItemId contentItemId, final int revisionNumber) {
            return Optional.ofNullable(result);
        }

        @Override
        public Optional<ContentRevision> findLatestWorking(final ContentItemId contentItemId) {
            return Optional.ofNullable(result);
        }

        @Override
        public Optional<ContentRevision> findLatestPublished(final ContentItemId contentItemId) {
            return Optional.ofNullable(result);
        }

        @Override
        public List<ContentRevision> findByContentItem(final ContentItemId contentItemId) {
            return result == null ? List.of() : List.of(result);
        }
    }
}
