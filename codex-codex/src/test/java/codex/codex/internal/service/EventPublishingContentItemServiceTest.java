package codex.codex.internal.service;

import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.event.ContentItemCreatedEvent;
import codex.codex.api.model.identity.ContentItemId;
import codex.codex.api.model.identity.ContentItemKey;
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
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .createdAt(FIXED)
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
        Optional<ContentItem> nextFindByKeyResult = Optional.empty();
        List<ContentItem> nextFindByContentTypeResult = List.of();
        List<ContentItem> nextFindAllResult = List.of();

        CreateContentItemCommand lastCreateCommand;
        RuntimeException throwOnCreate;

        @Override
        public ContentItem create(final CreateContentItemCommand command, final Actor actor) {
            lastCreateCommand = command;
            if (throwOnCreate != null) throw throwOnCreate;
            return nextCreateResult;
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
