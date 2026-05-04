package codex.codex.internal.index;

import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentItemStatus;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.codex.internal.repository.MemoryContentItemRepository;
import codex.codex.internal.repository.MemoryContentRevisionRepository;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link RepositoryContentItemProjectionSource}.
 */
class RepositoryContentItemProjectionSourceTest {

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentTypeVersionId CT_VERSION_ID = ContentTypeVersionId.of("ctv-1");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("welcome-to-codex");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY);
    private static final ContentRevisionId REVISION_ID =
            ContentRevisionId.forRevision(SITE_KEY, CT_KEY, ITEM_KEY, 1);
    private static final Actor ACTOR = Actor.system("test");

    private MemoryContentItemRepository itemRepository;
    private MemoryContentRevisionRepository revisionRepository;
    private RepositoryContentItemProjectionSource source;

    @BeforeEach
    void setUp() {
        itemRepository = new MemoryContentItemRepository();
        revisionRepository = new MemoryContentRevisionRepository();
        source = new RepositoryContentItemProjectionSource(itemRepository, revisionRepository);
    }

    private ContentItemPublishedEvent buildEvent() {
        return new ContentItemPublishedEvent(
                ITEM_ID, SITE_KEY, CT_KEY, CT_VERSION_ID, ITEM_KEY,
                REVISION_ID, ACTOR, Instant.now());
    }

    private ContentItem buildItem() {
        return ContentItem.builder()
                .id(ITEM_ID).siteKey(SITE_KEY).contentTypeKey(CT_KEY)
                .contentTypeVersionId(CT_VERSION_ID).key(ITEM_KEY)
                .status(ContentItemStatus.PUBLISHED)
                .currentWorkingRevisionId(REVISION_ID)
                .currentPublishedRevisionId(REVISION_ID)
                .owner(ACTOR.id()).createdBy(ACTOR.id()).updatedBy(ACTOR.id())
                .build();
    }

    private ContentRevision buildRevision() {
        return ContentRevision.builder()
                .id(REVISION_ID).contentItemId(ITEM_ID).siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY).contentTypeVersionId(CT_VERSION_ID)
                .contentItemKey(ITEM_KEY).revisionNumber(1)
                .status(ContentRevisionStatus.PUBLISHED)
                .values(Map.of(FieldKey.TITLE, "Welcome to Codex"))
                .createdBy(ACTOR.id())
                .build();
    }

    // --- constructor ---

    @Test
    void constructorRejectsNullContentItemRepository() {
        assertThrows(NullPointerException.class,
                () -> new RepositoryContentItemProjectionSource(null, revisionRepository));
    }

    @Test
    void constructorRejectsNullContentRevisionRepository() {
        assertThrows(NullPointerException.class,
                () -> new RepositoryContentItemProjectionSource(itemRepository, null));
    }

    // --- loadItem ---

    @Test
    void loadItemRejectsNullEvent() {
        assertThrows(NullPointerException.class, () -> source.loadItem(null));
    }

    @Test
    void loadItemReturnsExistingItem() {
        final ContentItem saved = itemRepository.save(buildItem());
        final ContentItem loaded = source.loadItem(buildEvent());
        assertEquals(saved.id(), loaded.id());
    }

    @Test
    void loadItemThrowsIllegalStateExceptionWhenItemIsMissing() {
        assertThrows(IllegalStateException.class, () -> source.loadItem(buildEvent()));
    }

    // --- loadPublishedRevision ---

    @Test
    void loadPublishedRevisionRejectsNullEvent() {
        assertThrows(NullPointerException.class, () -> source.loadPublishedRevision(null));
    }

    @Test
    void loadPublishedRevisionReturnsExistingRevision() {
        final ContentRevision saved = revisionRepository.save(buildRevision());
        final ContentRevision loaded = source.loadPublishedRevision(buildEvent());
        assertEquals(saved.id(), loaded.id());
    }

    @Test
    void loadPublishedRevisionThrowsIllegalStateExceptionWhenRevisionIsMissing() {
        assertThrows(IllegalStateException.class, () -> source.loadPublishedRevision(buildEvent()));
    }
}
