package codex.codex.internal.service;

import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.ArchiveContentTypeCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.RemoveContentTypeFieldCommand;
import codex.codex.api.model.entity.ContentType;
import codex.codex.api.model.event.ContentTypeActivatedEvent;
import codex.codex.api.model.event.ContentTypeArchivedEvent;
import codex.codex.api.model.event.ContentTypeCreatedEvent;
import codex.codex.api.model.identity.ContentTypeId;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentTypeService;
import codex.codex.api.model.value.ContentTypeStatus;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.exception.NotFoundException;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EventPublishingContentTypeServiceTest {

    private FakeContentTypeService delegate;
    private RecordingEventDispatcher eventDispatcher;
    private Clock clock;
    private EventPublishingContentTypeService service;

    private final Instant fixedInstant = Instant.parse("2026-05-01T00:00:00Z");
    private final Actor actor = Actor.system("test");
    private final SiteKey siteKey = SiteKey.of("acme");
    private final ContentTypeKey key = ContentTypeKey.of("blog-post");
    private final ContentTypeId id = ContentTypeId.generate();

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(fixedInstant, ZoneOffset.UTC);
        delegate = new FakeContentTypeService();
        eventDispatcher = new RecordingEventDispatcher();
        service = new EventPublishingContentTypeService(delegate, eventDispatcher, clock);
    }

    // --- constructor null checks ---

    @Test
    @DisplayName("constructor should reject null arguments")
    void constructorShouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () ->
                new EventPublishingContentTypeService(null, eventDispatcher, clock));
        assertThrows(NullPointerException.class, () ->
                new EventPublishingContentTypeService(delegate, null, clock));
        assertThrows(NullPointerException.class, () ->
                new EventPublishingContentTypeService(delegate, eventDispatcher, null));
    }

    // --- create ---

    @Test
    @DisplayName("create should delegate and publish ContentTypeCreatedEvent")
    void createShouldDelegateAndPublishCreatedEvent() {
        final ContentType ct = buildContentType(ContentTypeStatus.DRAFT);
        delegate.nextCreateResult = ct;
        final CreateContentTypeCommand command = CreateContentTypeCommand.of(siteKey, key, "Blog Post");

        final ContentType result = service.create(command, actor);

        assertEquals(ct, result);
        assertSame(command, delegate.lastCreateCommand);

        final ContentTypeCreatedEvent event = eventDispatcher.singleEvent(ContentTypeCreatedEvent.class);
        assertEquals(ct.id(), event.id());
        assertEquals(ct.siteKey(), event.siteKey());
        assertEquals(ct.key(), event.key());
        assertEquals(actor, event.actor());
        assertEquals(fixedInstant, event.occurredAt());
    }

    @Test
    @DisplayName("create should not publish when delegate throws")
    void createShouldNotPublishWhenDelegateThrows() {
        delegate.throwOnCreate = new ContentTypeAlreadyExistsException(siteKey, key);

        assertThrows(ContentTypeAlreadyExistsException.class, () ->
                service.create(CreateContentTypeCommand.of(siteKey, key, "Blog Post"), actor));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    @DisplayName("create should reject null arguments")
    void createShouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () ->
                service.create(null, actor));
        assertThrows(NullPointerException.class, () ->
                service.create(CreateContentTypeCommand.of(siteKey, key, "Blog Post"), null));
    }

    // --- activate ---

    @Test
    @DisplayName("activate should publish ContentTypeActivatedEvent when status changes from DRAFT to ACTIVE")
    void activateShouldPublishEventWhenStatusChanges() {
        final ContentType before = buildContentType(ContentTypeStatus.DRAFT);
        final ContentType after = buildContentType(ContentTypeStatus.ACTIVE);
        delegate.nextFindByKeyResult = Optional.of(before);
        delegate.nextActivateResult = after;

        final ContentType result = service.activate(ActivateContentTypeCommand.of(siteKey, key), actor);

        assertEquals(after, result);
        final ContentTypeActivatedEvent event = eventDispatcher.singleEvent(ContentTypeActivatedEvent.class);
        assertEquals(id, event.id());
        assertEquals(siteKey, event.siteKey());
        assertEquals(key, event.key());
        assertEquals(ContentTypeStatus.DRAFT, event.previousStatus());
        assertEquals(ContentTypeStatus.ACTIVE, event.newStatus());
        assertEquals(actor, event.actor());
        assertEquals(fixedInstant, event.occurredAt());
    }

    @Test
    @DisplayName("activate should not publish event when already ACTIVE")
    void activateShouldNotPublishWhenAlreadyActive() {
        final ContentType active = buildContentType(ContentTypeStatus.ACTIVE);
        delegate.nextFindByKeyResult = Optional.of(active);
        delegate.nextActivateResult = active;

        service.activate(ActivateContentTypeCommand.of(siteKey, key), actor);

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    @DisplayName("activate should not publish when delegate throws")
    void activateShouldNotPublishWhenDelegateThrows() {
        delegate.nextFindByKeyResult = Optional.of(buildContentType(ContentTypeStatus.ARCHIVED));
        delegate.throwOnActivate = new InvalidContentTypeStatusTransitionException(
                "Cannot activate archived", buildContentType(ContentTypeStatus.ARCHIVED));

        assertThrows(InvalidContentTypeStatusTransitionException.class, () ->
                service.activate(ActivateContentTypeCommand.of(siteKey, key), actor));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    @DisplayName("activate should not publish when content type is missing")
    void activateShouldNotPublishWhenMissing() {
        delegate.nextFindByKeyResult = Optional.empty();
        delegate.throwOnActivate = new NotFoundException("ContentType not found");

        assertThrows(NotFoundException.class, () ->
                service.activate(ActivateContentTypeCommand.of(siteKey, key), actor));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    @DisplayName("activate should reject null arguments")
    void activateShouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () ->
                service.activate(null, actor));
        assertThrows(NullPointerException.class, () ->
                service.activate(ActivateContentTypeCommand.of(siteKey, key), null));
    }

    // --- archive ---

    @Test
    @DisplayName("archive should publish ContentTypeArchivedEvent when status changes from DRAFT to ARCHIVED")
    void archiveShouldPublishEventFromDraft() {
        final ContentType before = buildContentType(ContentTypeStatus.DRAFT);
        final ContentType after = buildContentType(ContentTypeStatus.ARCHIVED);
        delegate.nextFindByKeyResult = Optional.of(before);
        delegate.nextArchiveResult = after;

        final ContentType result = service.archive(ArchiveContentTypeCommand.of(siteKey, key), actor);

        assertEquals(after, result);
        final ContentTypeArchivedEvent event = eventDispatcher.singleEvent(ContentTypeArchivedEvent.class);
        assertEquals(ContentTypeStatus.DRAFT, event.previousStatus());
        assertEquals(ContentTypeStatus.ARCHIVED, event.newStatus());
        assertEquals(actor, event.actor());
        assertEquals(fixedInstant, event.occurredAt());
    }

    @Test
    @DisplayName("archive should publish ContentTypeArchivedEvent when status changes from ACTIVE to ARCHIVED")
    void archiveShouldPublishEventFromActive() {
        final ContentType before = buildContentType(ContentTypeStatus.ACTIVE);
        final ContentType after = buildContentType(ContentTypeStatus.ARCHIVED);
        delegate.nextFindByKeyResult = Optional.of(before);
        delegate.nextArchiveResult = after;

        service.archive(ArchiveContentTypeCommand.of(siteKey, key), actor);

        final ContentTypeArchivedEvent event = eventDispatcher.singleEvent(ContentTypeArchivedEvent.class);
        assertEquals(ContentTypeStatus.ACTIVE, event.previousStatus());
        assertEquals(ContentTypeStatus.ARCHIVED, event.newStatus());
    }

    @Test
    @DisplayName("archive should not publish event when already ARCHIVED")
    void archiveShouldNotPublishWhenAlreadyArchived() {
        final ContentType archived = buildContentType(ContentTypeStatus.ARCHIVED);
        delegate.nextFindByKeyResult = Optional.of(archived);
        delegate.nextArchiveResult = archived;

        service.archive(ArchiveContentTypeCommand.of(siteKey, key), actor);

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    @DisplayName("archive should not publish when delegate throws")
    void archiveShouldNotPublishWhenDelegateThrows() {
        delegate.nextFindByKeyResult = Optional.empty();
        delegate.throwOnArchive = new NotFoundException("ContentType not found");

        assertThrows(NotFoundException.class, () ->
                service.archive(ArchiveContentTypeCommand.of(siteKey, key), actor));

        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    @DisplayName("archive should reject null arguments")
    void archiveShouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () ->
                service.archive(null, actor));
        assertThrows(NullPointerException.class, () ->
                service.archive(ArchiveContentTypeCommand.of(siteKey, key), null));
    }

    // --- read-only forwarding ---

    @Test
    @DisplayName("findByKey should forward to delegate without publishing events")
    void findByKeyShouldForwardWithoutEvent() {
        final ContentType ct = buildContentType(ContentTypeStatus.DRAFT);
        delegate.nextFindByKeyResult = Optional.of(ct);

        final Optional<ContentType> result = service.findByKey(siteKey, key, actor);

        assertEquals(Optional.of(ct), result);
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    @DisplayName("findAll should forward to delegate without publishing events")
    void findAllShouldForwardWithoutEvent() {
        final List<ContentType> all = List.of(buildContentType(ContentTypeStatus.DRAFT));
        delegate.nextFindAllResult = all;

        final List<ContentType> result = service.findAll(actor);

        assertEquals(all, result);
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    @DisplayName("findBySiteKey should forward to delegate without publishing events")
    void findBySiteKeyShouldForwardWithoutEvent() {
        final List<ContentType> all = List.of(buildContentType(ContentTypeStatus.DRAFT));
        delegate.nextFindBySiteKeyResult = all;

        final List<ContentType> result = service.findBySiteKey(siteKey, actor);

        assertEquals(all, result);
        assertTrue(eventDispatcher.events.isEmpty());
    }

    // --- helpers ---

    private ContentType buildContentType(final ContentTypeStatus status) {
        final ActorId actorId = actor.id();
        return ContentType.builder()
                .id(id)
                .siteKey(siteKey)
                .key(key)
                .displayName("Blog Post")
                .status(status)
                .owner(actorId)
                .createdBy(actorId)
                .updatedBy(actorId)
                .createdAt(fixedInstant)
                .updatedAt(fixedInstant)
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

    private static final class FakeContentTypeService implements ContentTypeService {

        ContentType nextCreateResult;
        ContentType nextActivateResult;
        ContentType nextArchiveResult;
        Optional<ContentType> nextFindByKeyResult = Optional.empty();
        List<ContentType> nextFindAllResult = List.of();
        List<ContentType> nextFindBySiteKeyResult = List.of();

        CreateContentTypeCommand lastCreateCommand;

        RuntimeException throwOnCreate;
        RuntimeException throwOnActivate;
        RuntimeException throwOnArchive;

        @Override
        public ContentType create(final CreateContentTypeCommand command, final Actor actor) {
            lastCreateCommand = command;
            if (throwOnCreate != null) throw throwOnCreate;
            return nextCreateResult;
        }

        @Override
        public ContentType activate(final ActivateContentTypeCommand command, final Actor actor) {
            if (throwOnActivate != null) throw throwOnActivate;
            return nextActivateResult;
        }

        @Override
        public ContentType archive(final ArchiveContentTypeCommand command, final Actor actor) {
            if (throwOnArchive != null) throw throwOnArchive;
            return nextArchiveResult;
        }

        @Override
        public Optional<ContentType> findByKey(final SiteKey siteKey, final ContentTypeKey key, final Actor actor) {
            return nextFindByKeyResult;
        }

        @Override
        public List<ContentType> findBySiteKey(final SiteKey siteKey, final Actor actor) {
            return nextFindBySiteKeyResult;
        }

        @Override
        public List<ContentType> findAll(final Actor actor) {
            return nextFindAllResult;
        }

        @Override
        public ContentType addField(final AddContentTypeFieldCommand command, final Actor actor) {
            throw new UnsupportedOperationException("not used in unit tests");
        }

        @Override
        public ContentType removeField(final RemoveContentTypeFieldCommand command, final Actor actor) {
            throw new UnsupportedOperationException("not used in unit tests");
        }
    }
}
