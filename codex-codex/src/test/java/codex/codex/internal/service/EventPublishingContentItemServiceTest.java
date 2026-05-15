package codex.codex.internal.service;

import codex.codex.api.model.command.ArchiveContentItemCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.DeleteContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.RestoreContentItemCommand;
import codex.codex.api.model.command.UnpublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.event.ContentItemArchivedEvent;
import codex.codex.api.model.event.ContentItemCreatedEvent;
import codex.codex.api.model.event.ContentItemDeletedEvent;
import codex.codex.api.model.event.ContentItemPublishedEvent;
import codex.codex.api.model.event.ContentItemRestoredEvent;
import codex.codex.api.model.event.ContentItemUnpublishedEvent;
import codex.codex.api.model.event.ContentItemUpdatedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentRevisionId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.ContentTypeVersionId;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentItemService;
import codex.codex.api.model.value.ContentItemStatus;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EventPublishingContentItemServiceTest {

    private FakeContentItemService delegate;
    private RecordingEventDispatcher eventDispatcher;
    private Clock clock;
    private EventPublishingContentItemService service;

    private static final Instant FIXED = Instant.parse("2026-05-02T10:00:00Z");
    private static final Actor ACTOR = Actor.system("test");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("blog-post");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("my-post");
    private static final ContentTypeVersionId VERSION_ID = ContentTypeVersionId.of("content-type-version:acme:blog-post:v1");
    private static final ContentRevisionId REVISION_ID = ContentRevisionId.forRevision(SITE_KEY, CT_KEY, ITEM_KEY, 1);

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED, ZoneOffset.UTC);
        delegate = new FakeContentItemService();
        eventDispatcher = new RecordingEventDispatcher();
        service = new EventPublishingContentItemService(delegate, eventDispatcher, clock);
    }

    // --- constructor null checks ---

    @Test
    void constructorRejectsNullDelegate() {
        assertThrows(NullPointerException.class,
                () -> new EventPublishingContentItemService(null, eventDispatcher, clock));
    }

    @Test
    void constructorRejectsNullDispatcher() {
        assertThrows(NullPointerException.class,
                () -> new EventPublishingContentItemService(delegate, null, clock));
    }

    @Test
    void constructorRejectsNullClock() {
        assertThrows(NullPointerException.class,
                () -> new EventPublishingContentItemService(delegate, eventDispatcher, null));
    }

    // --- create: happy path ---

    @Test
    void createDelegatesAndPublishesContentItemCreatedEvent() {
        final ContentItem item = buildItem();
        delegate.nextCreateResult = item;
        final CreateContentItemCommand command = CreateContentItemCommand.of(
                SITE_KEY, CT_KEY, ITEM_KEY, Map.of(FieldKey.of("title"), "Hello"));

        final ContentItem result = service.create(command, ACTOR);

        assertEquals(item, result);
        assertSame(command, delegate.lastCreateCommand);

        final ContentItemCreatedEvent event = eventDispatcher.singleEvent(ContentItemCreatedEvent.class);
        assertEquals(item.id(), event.id());
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.contentTypeKey());
        assertEquals(VERSION_ID, event.contentTypeVersionId());
        assertEquals(ITEM_KEY, event.key());
        assertEquals(ACTOR, event.actor());
        assertEquals(FIXED, event.occurredAt());
    }

    // --- create: invalid operations must not publish ---

    @Test
    void createDoesNotPublishWhenDelegateThrows() {
        delegate.throwOnCreate = new ContentItemAlreadyExistsException(ITEM_KEY, CT_KEY, SITE_KEY);

        assertThrows(ContentItemAlreadyExistsException.class, () ->
                service.create(CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void createRejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> service.create(null, ACTOR));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void createRejectsNullActor() {
        assertThrows(NullPointerException.class, () ->
                service.create(CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), null));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    // --- read-only methods: forwarded without event ---

    @Test
    void findByKeyForwardsWithoutEvent() {
        final ContentItem item = buildItem();
        delegate.nextFindByKeyResult = Optional.of(item);

        final Optional<ContentItem> result = service.findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ACTOR);

        assertEquals(Optional.of(item), result);
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void findByContentTypeForwardsWithoutEvent() {
        final List<ContentItem> items = List.of(buildItem());
        delegate.nextFindByContentTypeResult = items;

        final List<ContentItem> result = service.findByContentType(SITE_KEY, CT_KEY, ACTOR);

        assertEquals(items, result);
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void findAllForwardsWithoutEvent() {
        final List<ContentItem> items = List.of(buildItem());
        delegate.nextFindAllResult = items;

        final List<ContentItem> result = service.findAll(ACTOR);

        assertEquals(items, result);
        assertTrue(eventDispatcher.events.isEmpty());
    }

    // --- update: event publishing ---

    @Test
    void updateDelegatesAndPublishesContentItemUpdatedEvent() {
        final ContentItem item = buildItem();
        delegate.nextUpdateResult = item;
        final UpdateContentItemCommand command = UpdateContentItemCommand.of(
                SITE_KEY, CT_KEY, ITEM_KEY, Map.of(FieldKey.of("title"), "Updated Title"));

        final ContentItem result = service.update(command, ACTOR);

        assertEquals(item, result);
        assertSame(command, delegate.lastUpdateCommand);

        final ContentItemUpdatedEvent event = eventDispatcher.singleEvent(ContentItemUpdatedEvent.class);
        assertEquals(item.id(), event.id());
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.contentTypeKey());
        assertEquals(VERSION_ID, event.contentTypeVersionId());
        assertEquals(ITEM_KEY, event.key());
        assertEquals(ACTOR, event.actor());
        assertEquals(FIXED, event.occurredAt());
    }

    @Test
    void updateDoesNotPublishEventWhenDelegateThrows() {
        delegate.throwOnUpdate = new RuntimeException("forced failure");

        assertThrows(RuntimeException.class, () ->
                service.update(UpdateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), ACTOR));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void updateRejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> service.update(null, ACTOR));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void updateRejectsNullActor() {
        assertThrows(NullPointerException.class, () ->
                service.update(UpdateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY, Map.of()), null));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    // --- archive: event publishing ---

    @Test
    void archiveDelegatesAndPublishesContentItemArchivedEvent() {
        final ContentItem item = buildItem();
        delegate.nextArchiveResult = item;
        final ArchiveContentItemCommand command = ArchiveContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY);

        final ContentItem result = service.archive(command, ACTOR);

        assertEquals(item, result);
        assertSame(command, delegate.lastArchiveCommand);

        final ContentItemArchivedEvent event = eventDispatcher.singleEvent(ContentItemArchivedEvent.class);
        assertEquals(item.id(), event.id());
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.contentTypeKey());
        assertEquals(VERSION_ID, event.contentTypeVersionId());
        assertEquals(ITEM_KEY, event.key());
        assertEquals(ACTOR, event.actor());
        assertEquals(FIXED, event.occurredAt());
    }

    @Test
    void archiveDoesNotPublishEventWhenDelegateThrows() {
        delegate.throwOnArchive = new InvalidContentItemArchiveException("forced failure");

        assertThrows(InvalidContentItemArchiveException.class, () ->
                service.archive(ArchiveContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void archiveRejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> service.archive(null, ACTOR));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void archiveRejectsNullActor() {
        assertThrows(NullPointerException.class, () ->
                service.archive(ArchiveContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), null));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    // --- delete: event publishing ---

    @Test
    void deleteDelegatesAndPublishesContentItemDeletedEvent() {
        final ContentItem item = buildItem();
        delegate.nextFindByKeyResult = Optional.of(item);
        final DeleteContentItemCommand command = DeleteContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY);

        service.delete(command, ACTOR);

        assertSame(command, delegate.lastDeleteCommand);

        final ContentItemDeletedEvent event = eventDispatcher.singleEvent(ContentItemDeletedEvent.class);
        assertEquals(item.id(), event.id());
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.contentTypeKey());
        assertEquals(VERSION_ID, event.contentTypeVersionId());
        assertEquals(ITEM_KEY, event.key());
        assertEquals(ACTOR, event.actor());
        assertEquals(FIXED, event.occurredAt());
    }

    @Test
    void deleteDoesNotPublishEventWhenDelegateThrows() {
        final ContentItem item = buildItem();
        delegate.nextFindByKeyResult = Optional.of(item);
        delegate.throwOnDelete = new InvalidContentItemDeleteException("forced failure");

        assertThrows(InvalidContentItemDeleteException.class, () ->
                service.delete(DeleteContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void deleteRejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> service.delete(null, ACTOR));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void deleteRejectsNullActor() {
        assertThrows(NullPointerException.class, () ->
                service.delete(DeleteContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), null));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    // --- restore: event publishing ---

    @Test
    void restoreDelegatesAndPublishesContentItemRestoredEvent() {
        final ContentItem item = buildItem();
        delegate.nextRestoreResult = item;
        final RestoreContentItemCommand command = RestoreContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY);

        final ContentItem result = service.restore(command, ACTOR);

        assertEquals(item, result);
        assertSame(command, delegate.lastRestoreCommand);

        final ContentItemRestoredEvent event = eventDispatcher.singleEvent(ContentItemRestoredEvent.class);
        assertEquals(item.id(), event.id());
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.contentTypeKey());
        assertEquals(VERSION_ID, event.contentTypeVersionId());
        assertEquals(ITEM_KEY, event.key());
        assertEquals(ACTOR, event.actor());
        assertEquals(FIXED, event.occurredAt());
    }

    @Test
    void restoreDoesNotPublishEventWhenDelegateThrows() {
        delegate.throwOnRestore = new InvalidContentItemRestoreException("forced failure");

        assertThrows(InvalidContentItemRestoreException.class, () ->
                service.restore(RestoreContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void restoreRejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> service.restore(null, ACTOR));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void restoreRejectsNullActor() {
        assertThrows(NullPointerException.class, () ->
                service.restore(RestoreContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), null));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    // --- unpublish: event publishing ---

    @Test
    void unpublishDelegatesAndPublishesContentItemUnpublishedEvent() {
        final ContentItem item = buildItem();
        delegate.nextUnpublishResult = item;
        final UnpublishContentItemCommand command = UnpublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY);

        final ContentItem result = service.unpublish(command, ACTOR);

        assertEquals(item, result);
        assertSame(command, delegate.lastUnpublishCommand);

        final ContentItemUnpublishedEvent event = eventDispatcher.singleEvent(ContentItemUnpublishedEvent.class);
        assertEquals(item.id(), event.id());
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.contentTypeKey());
        assertEquals(VERSION_ID, event.contentTypeVersionId());
        assertEquals(ITEM_KEY, event.key());
        assertEquals(ACTOR, event.actor());
        assertEquals(FIXED, event.occurredAt());
    }

    @Test
    void unpublishDoesNotPublishEventWhenDelegateThrows() {
        delegate.throwOnUnpublish = new InvalidContentItemUnpublishException("forced failure");

        assertThrows(InvalidContentItemUnpublishException.class, () ->
                service.unpublish(UnpublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void unpublishRejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> service.unpublish(null, ACTOR));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void unpublishRejectsNullActor() {
        assertThrows(NullPointerException.class, () ->
                service.unpublish(UnpublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), null));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    // --- publish: event publishing ---

    @Test
    void publishDelegatesAndPublishesContentItemPublishedEvent() {
        final ContentItem draft = buildItem();
        final ContentItem published = buildPublishedItem();
        delegate.nextFindByKeyResult = Optional.of(draft);
        delegate.nextPublishResult = published;
        final PublishContentItemCommand command = PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY);

        final ContentItem result = service.publish(command, ACTOR);

        assertEquals(published, result);
        assertSame(command, delegate.lastPublishCommand);

        final ContentItemPublishedEvent event = eventDispatcher.singleEvent(ContentItemPublishedEvent.class);
        assertEquals(published.id(), event.id());
        assertEquals(SITE_KEY, event.siteKey());
        assertEquals(CT_KEY, event.contentTypeKey());
        assertEquals(VERSION_ID, event.contentTypeVersionId());
        assertEquals(ITEM_KEY, event.key());
        assertEquals(REVISION_ID, event.publishedRevisionId());
        assertEquals(ACTOR, event.actor());
        assertEquals(FIXED, event.occurredAt());
    }

    @Test
    void publishDoesNotPublishEventWhenDelegateThrows() {
        final ContentItem draft = buildItem();
        delegate.nextFindByKeyResult = Optional.of(draft);
        delegate.throwOnPublish = new InvalidContentItemPublishException("forced failure");

        assertThrows(InvalidContentItemPublishException.class, () ->
                service.publish(PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void idempotentPublishDoesNotPublishEvent() {
        final ContentItem alreadyPublished = buildPublishedItem();
        delegate.nextFindByKeyResult = Optional.of(alreadyPublished);
        delegate.nextPublishResult = alreadyPublished;

        service.publish(PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), ACTOR);

        assertTrue(eventDispatcher.events.isEmpty(),
                "Idempotent publish must not dispatch a ContentItemPublishedEvent");
    }

    @Test
    void publishRejectsNullCommand() {
        assertThrows(NullPointerException.class, () -> service.publish(null, ACTOR));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void publishRejectsNullActor() {
        assertThrows(NullPointerException.class, () ->
                service.publish(PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), null));
        assertTrue(eventDispatcher.events.isEmpty());
    }

    // --- helpers ---

    private ContentItem buildItem() {
        final ActorId actorId = ACTOR.id();
        return ContentItem.builder()
                .id(ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY))
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(VERSION_ID)
                .key(ITEM_KEY)
                .status(ContentItemStatus.DRAFT)
                .currentWorkingRevisionId(REVISION_ID)
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .createdAt(FIXED)
                .build();
    }

    private ContentItem buildPublishedItem() {
        final ActorId actorId = ACTOR.id();
        return ContentItem.builder()
                .id(ContentItemId.forItem(SITE_KEY, CT_KEY, ITEM_KEY))
                .siteKey(SITE_KEY)
                .contentTypeKey(CT_KEY)
                .contentTypeVersionId(VERSION_ID)
                .key(ITEM_KEY)
                .status(ContentItemStatus.PUBLISHED)
                .currentWorkingRevisionId(REVISION_ID)
                .currentPublishedRevisionId(REVISION_ID)
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .createdAt(FIXED)
                .updatedAt(FIXED)
                .build();
    }

    // --- fakes ---

    private static final class RecordingEventDispatcher implements CodexEventDispatcher {

        private final List<CodexEvent> events = new ArrayList<>();

        @Override
        public void dispatch(final CodexEvent event) {
            events.add(event);
        }

        <E extends CodexEvent> E singleEvent(final Class<E> type) {
            assertEquals(1, events.size(), "Expected exactly one event but found: " + events);
            assertInstanceOf(type, events.getFirst());
            return type.cast(events.getFirst());
        }
    }

    private static final class FakeContentItemService implements ContentItemService {

        ContentItem nextCreateResult;
        ContentItem nextUpdateResult;
        ContentItem nextArchiveResult;
        ContentItem nextRestoreResult;
        ContentItem nextUnpublishResult;
        ContentItem nextPublishResult;
        Optional<ContentItem> nextFindByKeyResult = Optional.empty();
        List<ContentItem> nextFindByContentTypeResult = List.of();
        List<ContentItem> nextFindAllResult = List.of();

        CreateContentItemCommand lastCreateCommand;
        UpdateContentItemCommand lastUpdateCommand;
        ArchiveContentItemCommand lastArchiveCommand;
        RestoreContentItemCommand lastRestoreCommand;
        DeleteContentItemCommand lastDeleteCommand;
        UnpublishContentItemCommand lastUnpublishCommand;
        PublishContentItemCommand lastPublishCommand;
        RuntimeException throwOnCreate;
        RuntimeException throwOnUpdate;
        RuntimeException throwOnArchive;
        RuntimeException throwOnRestore;
        RuntimeException throwOnDelete;
        RuntimeException throwOnUnpublish;
        RuntimeException throwOnPublish;

        @Override
        public ContentItem create(final CreateContentItemCommand command, final Actor actor) {
            lastCreateCommand = command;
            if (throwOnCreate != null) throw throwOnCreate;
            return nextCreateResult;
        }

        @Override
        public ContentItem update(final UpdateContentItemCommand command, final Actor actor) {
            Objects.requireNonNull(command, "command must not be null");
            Objects.requireNonNull(actor, "actor must not be null");
            lastUpdateCommand = command;
            if (throwOnUpdate != null) throw throwOnUpdate;
            return nextUpdateResult;
        }

        @Override
        public ContentItem unpublish(final UnpublishContentItemCommand command, final Actor actor) {
            Objects.requireNonNull(command, "command must not be null");
            Objects.requireNonNull(actor, "actor must not be null");
            lastUnpublishCommand = command;
            if (throwOnUnpublish != null) throw throwOnUnpublish;
            return nextUnpublishResult;
        }

        @Override
        public ContentItem archive(final ArchiveContentItemCommand command, final Actor actor) {
            Objects.requireNonNull(command, "command must not be null");
            Objects.requireNonNull(actor, "actor must not be null");
            lastArchiveCommand = command;
            if (throwOnArchive != null) throw throwOnArchive;
            return nextArchiveResult;
        }

        @Override
        public void delete(final DeleteContentItemCommand command, final Actor actor) {
            Objects.requireNonNull(command, "command must not be null");
            Objects.requireNonNull(actor, "actor must not be null");
            lastDeleteCommand = command;
            if (throwOnDelete != null) throw throwOnDelete;
        }

        @Override
        public ContentItem restore(final RestoreContentItemCommand command, final Actor actor) {
            Objects.requireNonNull(command, "command must not be null");
            Objects.requireNonNull(actor, "actor must not be null");
            lastRestoreCommand = command;
            if (throwOnRestore != null) throw throwOnRestore;
            return nextRestoreResult;
        }

        @Override
        public ContentItem publish(final PublishContentItemCommand command, final Actor actor) {
            Objects.requireNonNull(command, "command must not be null");
            Objects.requireNonNull(actor, "actor must not be null");
            lastPublishCommand = command;
            if (throwOnPublish != null) throw throwOnPublish;
            return nextPublishResult;
        }

        @Override
        public Optional<ContentItem> findByKey(final SiteKey siteKey, final ContentTypeKey contentTypeKey,
                                                final ContentItemKey key, final Actor actor) {
            return nextFindByKeyResult;
        }

        @Override
        public List<ContentItem> findByContentType(final SiteKey siteKey,
                                                    final ContentTypeKey contentTypeKey, final Actor actor) {
            return nextFindByContentTypeResult;
        }

        @Override
        public List<ContentItem> findAll(final Actor actor) {
            return nextFindAllResult;
        }
    }
}
