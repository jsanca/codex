package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.ArchiveContentItemCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.DeleteContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.RestoreContentItemCommand;
import codex.codex.api.model.command.UnpublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.value.FieldType;
import codex.codex.api.runtime.CodexRuntime;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import codex.fundamentum.api.observance.InMemoryObservance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static codex.codex.internal.service.ContentItemServiceMetricNames.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TimedContentItemService}.
 *
 * <p>Validates that every operation records its duration timer and — only on failure —
 * increments its failure counter, without changing return values, exception propagation,
 * or delegate call count.</p>
 */
class TimedContentItemServiceTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Test User");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("article");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("first-post");

    private InMemoryObservance observance;
    private StubContentItemService stub;
    private TimedContentItemService service;

    private CodexRuntime runtime;

    @BeforeEach
    void setUp() {
        observance = new InMemoryObservance();
        stub = new StubContentItemService();
        service = new TimedContentItemService(stub, observance);
    }

    @AfterEach
    void tearDown() {
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    // --- constructor ---

    @Test
    void constructorRejectsNullDelegate() {
        assertThrows(NullPointerException.class,
                () -> new TimedContentItemService(null, observance));
    }

    @Test
    void constructorRejectsNullObservance() {
        assertThrows(NullPointerException.class,
                () -> new TimedContentItemService(stub, null));
    }

    @Test
    void getDelegateReturnsDelegate() {
        assertSame(stub, service.getDelegate());
    }

    // --- create ---

    @Test
    void createRecordsDuration() {
        stub.nextItem = sampleItem();
        service.create(CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR);
        assertEquals(1, observance.timerCount(CREATE_DURATION));
    }

    @Test
    void createReturnsDelegateResult() {
        final ContentItem item = sampleItem();
        stub.nextItem = item;
        assertSame(item, service.create(
                CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR));
    }

    @Test
    void createDelegatesExactlyOnce() {
        stub.nextItem = sampleItem();
        service.create(CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR);
        assertEquals(1, stub.createCallCount);
    }

    @Test
    void createFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "create";
        assertThrows(RuntimeException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR));
        assertEquals(1, observance.timerCount(CREATE_DURATION));
        assertEquals(1, observance.counterValue(CREATE_FAILED));
    }

    @Test
    void createFailurePropagatesException() {
        stub.throwOn = "create";
        final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.create(
                        CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR));
        assertEquals("stub failure: create", ex.getMessage());
    }

    @Test
    void createSuccessDoesNotIncrementFailedCounter() {
        stub.nextItem = sampleItem();
        service.create(CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR);
        assertEquals(0, observance.counterValue(CREATE_FAILED));
    }

    // --- findByKey ---

    @Test
    void findByKeyRecordsDuration() {
        stub.nextOptional = Optional.of(sampleItem());
        service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);
        assertEquals(1, observance.timerCount(FIND_BY_KEY_DURATION));
    }

    @Test
    void findByKeyReturnsDelegateResult() {
        final Optional<ContentItem> result = Optional.of(sampleItem());
        stub.nextOptional = result;
        assertSame(result, service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR));
    }

    @Test
    void findByKeyFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "findByKey";
        assertThrows(RuntimeException.class,
                () -> service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR));
        assertEquals(1, observance.timerCount(FIND_BY_KEY_DURATION));
        assertEquals(1, observance.counterValue(FIND_BY_KEY_FAILED));
    }

    // --- findByContentType ---

    @Test
    void findByContentTypeRecordsDuration() {
        service.findByContentType(SITE_KEY, CT_KEY, ACTOR);
        assertEquals(1, observance.timerCount(FIND_BY_CONTENT_TYPE_DURATION));
    }

    @Test
    void findByContentTypeReturnsDelegateResult() {
        final List<ContentItem> result = List.of(sampleItem());
        stub.nextList = result;
        assertSame(result, service.findByContentType(SITE_KEY, CT_KEY, ACTOR));
    }

    @Test
    void findByContentTypeFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "findByContentType";
        assertThrows(RuntimeException.class,
                () -> service.findByContentType(SITE_KEY, CT_KEY, ACTOR));
        assertEquals(1, observance.timerCount(FIND_BY_CONTENT_TYPE_DURATION));
        assertEquals(1, observance.counterValue(FIND_BY_CONTENT_TYPE_FAILED));
    }

    // --- findAll ---

    @Test
    void findAllRecordsDuration() {
        service.findAll(ACTOR);
        assertEquals(1, observance.timerCount(FIND_ALL_DURATION));
    }

    @Test
    void findAllFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "findAll";
        assertThrows(RuntimeException.class, () -> service.findAll(ACTOR));
        assertEquals(1, observance.timerCount(FIND_ALL_DURATION));
        assertEquals(1, observance.counterValue(FIND_ALL_FAILED));
    }

    // --- update ---

    @Test
    void updateRecordsDuration() {
        stub.nextItem = sampleItem();
        service.update(UpdateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR);
        assertEquals(1, observance.timerCount(UPDATE_DURATION));
    }

    @Test
    void updateReturnsDelegateResult() {
        final ContentItem item = sampleItem();
        stub.nextItem = item;
        assertSame(item, service.update(
                UpdateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR));
    }

    @Test
    void updateFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "update";
        assertThrows(RuntimeException.class,
                () -> service.update(
                        UpdateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR));
        assertEquals(1, observance.timerCount(UPDATE_DURATION));
        assertEquals(1, observance.counterValue(UPDATE_FAILED));
    }

    // --- archive ---

    @Test
    void archiveRecordsDuration() {
        stub.nextItem = sampleItem();
        service.archive(ArchiveContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);
        assertEquals(1, observance.timerCount(ARCHIVE_DURATION));
    }

    @Test
    void archiveFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "archive";
        assertThrows(RuntimeException.class,
                () -> service.archive(ArchiveContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));
        assertEquals(1, observance.timerCount(ARCHIVE_DURATION));
        assertEquals(1, observance.counterValue(ARCHIVE_FAILED));
    }

    // --- unpublish ---

    @Test
    void unpublishRecordsDuration() {
        stub.nextItem = sampleItem();
        service.unpublish(UnpublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);
        assertEquals(1, observance.timerCount(UNPUBLISH_DURATION));
    }

    @Test
    void unpublishFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "unpublish";
        assertThrows(RuntimeException.class,
                () -> service.unpublish(
                        UnpublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));
        assertEquals(1, observance.timerCount(UNPUBLISH_DURATION));
        assertEquals(1, observance.counterValue(UNPUBLISH_FAILED));
    }

    // --- delete (void) ---

    @Test
    void deleteRecordsDuration() {
        service.delete(DeleteContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);
        assertEquals(1, observance.timerCount(DELETE_DURATION));
    }

    @Test
    void deleteDelegatesExactlyOnce() {
        service.delete(DeleteContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);
        assertEquals(1, stub.deleteCallCount);
    }

    @Test
    void deleteFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "delete";
        assertThrows(RuntimeException.class,
                () -> service.delete(DeleteContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));
        assertEquals(1, observance.timerCount(DELETE_DURATION));
        assertEquals(1, observance.counterValue(DELETE_FAILED));
    }

    @Test
    void deleteFailurePropagatesException() {
        stub.throwOn = "delete";
        final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.delete(DeleteContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));
        assertEquals("stub failure: delete", ex.getMessage());
    }

    @Test
    void deleteSuccessDoesNotIncrementFailedCounter() {
        service.delete(DeleteContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);
        assertEquals(0, observance.counterValue(DELETE_FAILED));
    }

    // --- restore ---

    @Test
    void restoreRecordsDuration() {
        stub.nextItem = sampleItem();
        service.restore(RestoreContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);
        assertEquals(1, observance.timerCount(RESTORE_DURATION));
    }

    @Test
    void restoreFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "restore";
        assertThrows(RuntimeException.class,
                () -> service.restore(RestoreContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));
        assertEquals(1, observance.timerCount(RESTORE_DURATION));
        assertEquals(1, observance.counterValue(RESTORE_FAILED));
    }

    // --- publish ---

    @Test
    void publishRecordsDuration() {
        stub.nextItem = sampleItem();
        service.publish(PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);
        assertEquals(1, observance.timerCount(PUBLISH_DURATION));
    }

    @Test
    void publishReturnsDelegateResult() {
        final ContentItem item = sampleItem();
        stub.nextItem = item;
        assertSame(item, service.publish(
                PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));
    }

    @Test
    void publishFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "publish";
        assertThrows(RuntimeException.class,
                () -> service.publish(PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));
        assertEquals(1, observance.timerCount(PUBLISH_DURATION));
        assertEquals(1, observance.counterValue(PUBLISH_FAILED));
    }

    // --- no cross-contamination ---

    @Test
    void eachOperationUsesItsOwnMetrics() {
        stub.nextItem = sampleItem();

        service.create(CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR);
        service.findAll(ACTOR);

        assertEquals(1, observance.timerCount(CREATE_DURATION));
        assertEquals(1, observance.timerCount(FIND_ALL_DURATION));
        assertEquals(0, observance.timerCount(PUBLISH_DURATION));
    }

    // --- runtime integration ---

    @Test
    void runtimeCreatedContentItemServiceRecordsDuration() {
        final InMemoryObservance runtimeObservance = new InMemoryObservance();
        runtime = CodexRuntime.inMemory(runtimeObservance);

        runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ACTOR);
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                        Field.builder()
                                .key(FieldKey.TITLE)
                                .displayName("Title")
                                .type(FieldType.TEXT)
                                .required(true)
                                .build()),
                ACTOR);
        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ACTOR);
        runtime.contentItemService().create(
                CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                        Map.of(FieldKey.TITLE, "First Post")),
                ACTOR);

        assertEquals(1, runtimeObservance.timerCount(CREATE_DURATION));
    }

    @Test
    void runtimeNoArgFactoryStillWorks() {
        runtime = CodexRuntime.inMemory();
        assertNotNull(runtime.contentItemService());
    }

    // --- private helpers ---

    private static ContentItem sampleItem() {
        final ActorId actorId = ActorId.of("user-1");
        return ContentItem.builder()
                .id(ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY))
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(ContentTypeVersionId.of("ctv-1"))
                .key(ITEM_KEY)
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .updatedAt(Instant.now())
                .build();
    }

    // --- inner stub ---

    private static final class StubContentItemService implements ContentItemService {

        ContentItem nextItem;
        Optional<ContentItem> nextOptional = Optional.empty();
        List<ContentItem> nextList = List.of();
        String throwOn;
        int createCallCount;
        int deleteCallCount;

        private void maybeThrow(final String op) {
            if (op.equals(throwOn)) {
                throw new RuntimeException("stub failure: " + op);
            }
        }

        @Override
        public ContentItem create(final CreateContentItemCommand command, final Actor actor) {
            maybeThrow("create");
            createCallCount++;
            return nextItem;
        }

        @Override
        public Optional<ContentItem> findByKey(final SiteKey siteKey,
                                               final ContentTypeKey contentTypeKey,
                                               final ContentItemKey key,
                                               final Actor actor) {
            maybeThrow("findByKey");
            return nextOptional;
        }

        @Override
        public List<ContentItem> findByContentType(final SiteKey siteKey,
                                                   final ContentTypeKey contentTypeKey,
                                                   final Actor actor) {
            maybeThrow("findByContentType");
            return nextList;
        }

        @Override
        public List<ContentItem> findAll(final Actor actor) {
            maybeThrow("findAll");
            return nextList;
        }

        @Override
        public ContentItem update(final UpdateContentItemCommand command, final Actor actor) {
            maybeThrow("update");
            return nextItem;
        }

        @Override
        public ContentItem archive(final ArchiveContentItemCommand command, final Actor actor) {
            maybeThrow("archive");
            return nextItem;
        }

        @Override
        public ContentItem unpublish(final UnpublishContentItemCommand command, final Actor actor) {
            maybeThrow("unpublish");
            return nextItem;
        }

        @Override
        public void delete(final DeleteContentItemCommand command, final Actor actor) {
            maybeThrow("delete");
            deleteCallCount++;
        }

        @Override
        public ContentItem restore(final RestoreContentItemCommand command, final Actor actor) {
            maybeThrow("restore");
            return nextItem;
        }

        @Override
        public ContentItem publish(final PublishContentItemCommand command, final Actor actor) {
            maybeThrow("publish");
            return nextItem;
        }
    }
}
