package codex.codex.internal.repository;

import codex.codex.api.model.entity.ContentRevision;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentRevisionStatus;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class MemoryContentRevisionRepositoryTest {

    private MemoryContentRevisionRepository repository;

    private static final SiteKey SITE = SiteKey.of("acme");
    private static final ContentTypeKey CONTENT_TYPE = ContentTypeKey.of("blog-post");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("hello-world");
    private static final ContentTypeVersionId VERSION_ID = ContentTypeVersionId.of("content-type-version:acme:blog-post:v1");
    private static final ContentItemId ITEM_ID = ContentItemId.forItem(SITE, CONTENT_TYPE, ITEM_KEY);
    private static final ActorId ACTOR = ActorId.of("user-1");

    @BeforeEach
    void setUp() {
        repository = new MemoryContentRevisionRepository();
    }

    private ContentRevision buildRevision(int revisionNumber, ContentRevisionStatus status) {
        final ContentRevisionId id = ContentRevisionId.forRevision(SITE, CONTENT_TYPE, ITEM_KEY, revisionNumber);
        return ContentRevision.builder()
                .id(id)
                .contentItemId(ITEM_ID)
                .siteKey(SITE)
                .contentTypeKey(CONTENT_TYPE)
                .contentTypeVersionId(VERSION_ID)
                .contentItemKey(ITEM_KEY)
                .revisionNumber(revisionNumber)
                .status(status)
                .createdBy(ACTOR)
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void saveReturnsSavedRevision() {
        final ContentRevision revision = buildRevision(1, ContentRevisionStatus.WORKING);
        final ContentRevision saved = repository.save(revision);
        assertEquals(revision, saved);
    }

    @Test
    void findByIdReturnsSavedRevision() {
        final ContentRevision revision = buildRevision(1, ContentRevisionStatus.WORKING);
        repository.save(revision);

        final Optional<ContentRevision> found = repository.findById(revision.id());
        assertTrue(found.isPresent());
        assertEquals(revision, found.get());
    }

    @Test
    void findByIdReturnsEmptyWhenMissing() {
        final ContentRevisionId id = ContentRevisionId.forRevision(SITE, CONTENT_TYPE, ITEM_KEY, 1);
        assertTrue(repository.findById(id).isEmpty());
    }

    @Test
    void findByContentItemAndRevisionReturnsSavedRevision() {
        final ContentRevision revision = buildRevision(2, ContentRevisionStatus.WORKING);
        repository.save(revision);

        final Optional<ContentRevision> found = repository.findByContentItemAndRevision(ITEM_ID, 2);
        assertTrue(found.isPresent());
        assertEquals(revision, found.get());
    }

    @Test
    void findByContentItemAndRevisionReturnsEmptyWhenMissing() {
        assertTrue(repository.findByContentItemAndRevision(ITEM_ID, 1).isEmpty());
    }

    @Test
    void findLatestWorkingReturnsHighestWorkingRevision() {
        repository.save(buildRevision(1, ContentRevisionStatus.PUBLISHED));
        repository.save(buildRevision(2, ContentRevisionStatus.WORKING));
        repository.save(buildRevision(3, ContentRevisionStatus.ARCHIVED));
        repository.save(buildRevision(4, ContentRevisionStatus.WORKING));
        repository.save(buildRevision(5, ContentRevisionStatus.PUBLISHED));

        final Optional<ContentRevision> latest = repository.findLatestWorking(ITEM_ID);
        assertTrue(latest.isPresent());
        assertEquals(4, latest.get().revisionNumber());
    }

    @Test
    void findLatestPublishedReturnsHighestPublishedRevision() {
        repository.save(buildRevision(1, ContentRevisionStatus.PUBLISHED));
        repository.save(buildRevision(2, ContentRevisionStatus.WORKING));
        repository.save(buildRevision(3, ContentRevisionStatus.PUBLISHED));
        repository.save(buildRevision(4, ContentRevisionStatus.WORKING));
        
        final Optional<ContentRevision> latest = repository.findLatestPublished(ITEM_ID);
        assertTrue(latest.isPresent());
        assertEquals(3, latest.get().revisionNumber());
    }

    @Test
    void findByContentItemReturnsAllRevisionsSorted() {
        repository.save(buildRevision(3, ContentRevisionStatus.WORKING));
        repository.save(buildRevision(1, ContentRevisionStatus.PUBLISHED));
        repository.save(buildRevision(2, ContentRevisionStatus.ARCHIVED));

        final List<ContentRevision> revisions = repository.findByContentItem(ITEM_ID);
        assertEquals(3, revisions.size());
        assertEquals(1, revisions.get(0).revisionNumber());
        assertEquals(2, revisions.get(1).revisionNumber());
        assertEquals(3, revisions.get(2).revisionNumber());
    }

    @Test
    void requiresNonNullArguments() {
        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.findById(null));
        assertThrows(NullPointerException.class, () -> repository.findByContentItemAndRevision(null, 1));
        assertThrows(NullPointerException.class, () -> repository.findLatestWorking(null));
        assertThrows(NullPointerException.class, () -> repository.findLatestPublished(null));
        assertThrows(NullPointerException.class, () -> repository.findByContentItem(null));
    }

    @Test
    void rejectsInvalidRevisionNumber() {
        assertThrows(IllegalArgumentException.class, () -> repository.findByContentItemAndRevision(ITEM_ID, 0));
        assertThrows(IllegalArgumentException.class, () -> repository.findByContentItemAndRevision(ITEM_ID, -1));
    }
}
