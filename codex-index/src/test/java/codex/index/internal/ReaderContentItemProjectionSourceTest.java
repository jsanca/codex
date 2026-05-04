package codex.index.internal;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.codex.api.projection.ContentItemProjectionReader;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link ReaderContentItemProjectionSource}.
 */
class ReaderContentItemProjectionSourceTest {

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
    private ContentItemPublishedEvent event;

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

        event = new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID,
                ITEM_KEY, REVISION_ID, Actor.system("test"), NOW);
    }

    @Test
    void rejectsNullReader() {
        assertThrows(NullPointerException.class, () -> new ReaderContentItemProjectionSource(null));
    }

    @Test
    void loadItemRejectsNullEvent() {
        final ReaderContentItemProjectionSource source =
                new ReaderContentItemProjectionSource(new StubReader(item, revision));
        assertThrows(NullPointerException.class, () -> source.loadItem(null));
    }

    @Test
    void loadPublishedRevisionRejectsNullEvent() {
        final ReaderContentItemProjectionSource source =
                new ReaderContentItemProjectionSource(new StubReader(item, revision));
        assertThrows(NullPointerException.class, () -> source.loadPublishedRevision(null));
    }

    @Test
    void loadItemReturnsItemFromReader() {
        final ReaderContentItemProjectionSource source =
                new ReaderContentItemProjectionSource(new StubReader(item, revision));
        assertEquals(item, source.loadItem(event));
    }

    @Test
    void loadPublishedRevisionReturnsRevisionFromReader() {
        final ReaderContentItemProjectionSource source =
                new ReaderContentItemProjectionSource(new StubReader(item, revision));
        assertEquals(revision, source.loadPublishedRevision(event));
    }

    @Test
    void loadItemThrowsWhenReaderReturnsEmpty() {
        final ReaderContentItemProjectionSource source =
                new ReaderContentItemProjectionSource(new StubReader(null, revision));
        assertThrows(IllegalStateException.class, () -> source.loadItem(event));
    }

    @Test
    void loadPublishedRevisionThrowsWhenReaderReturnsEmpty() {
        final ReaderContentItemProjectionSource source =
                new ReaderContentItemProjectionSource(new StubReader(item, null));
        assertThrows(IllegalStateException.class, () -> source.loadPublishedRevision(event));
    }

    private static final class StubReader implements ContentItemProjectionReader {
        private final ContentItem item;
        private final ContentRevision revision;

        StubReader(final ContentItem item, final ContentRevision revision) {
            this.item = item;
            this.revision = revision;
        }

        @Override
        public Optional<ContentItem> findContentItem(
                final SiteKey siteKey, final ContentTypeKey contentTypeKey, final ContentItemKey key) {
            return Optional.ofNullable(item);
        }

        @Override
        public Optional<ContentRevision> findContentRevision(final ContentRevisionId revisionId) {
            return Optional.ofNullable(revision);
        }
    }
}
