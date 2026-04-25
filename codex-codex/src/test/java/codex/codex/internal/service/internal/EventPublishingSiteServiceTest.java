package codex.codex.internal.service.internal;

import codex.codex.api.model.command.*;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.event.*;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventDispatcher;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class EventPublishingSiteServiceTest {

    private FakeSiteService delegate;
    private RecordingEventDispatcher eventDispatcher;
    private Clock clock;
    private EventPublishingSiteService service;

    private final Instant fixedInstant = Instant.parse("2026-04-25T12:00:00Z");
    private final Actor testActor = Actor.system("test");
    private final SiteKey testKey = SiteKey.of("test-site");
    private final SiteId testId = SiteId.of(UUID.randomUUID().toString());

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));
        delegate = new FakeSiteService();
        eventDispatcher = new RecordingEventDispatcher();
        service = new EventPublishingSiteService(delegate, eventDispatcher, clock);
    }

    @Test
    void create_ShouldDelegateAndPublishEvent() {
        CreateSiteCommand command = CreateSiteCommand.of(testKey, "Test Site");
        Site site = createSite();
        delegate.nextCreateResult = site;

        Site result = service.create(command, testActor);

        assertEquals(site, result);
        assertSame(command, delegate.lastCreateCommand);
        assertEquals(testActor, delegate.lastActor);

        CodexEvent publishedEvent = eventDispatcher.singleEvent();
        assertInstanceOf(SiteCreatedEvent.class, publishedEvent);
        SiteCreatedEvent event = (SiteCreatedEvent) publishedEvent;
        assertEquals(site.id(), event.id());
        assertEquals(site.key(), event.key());
        assertEquals(testActor, event.actor());
        assertEquals(fixedInstant, event.occurredAt());
    }

    @Test
    void start_ShouldDelegateAndPublishEvent() {
        StartSiteCommand command = StartSiteCommand.of(testKey);
        Site site = createSite();
        delegate.nextStartResult = site;

        Site result = service.start(command, testActor);

        assertEquals(site, result);
        assertSame(command, delegate.lastStartCommand);
        assertEquals(testActor, delegate.lastActor);

        CodexEvent publishedEvent = eventDispatcher.singleEvent();
        assertInstanceOf(SiteStartedEvent.class, publishedEvent);
    }

    @Test
    void suspend_ShouldDelegateAndPublishEvent() {
        SuspendSiteCommand command = SuspendSiteCommand.of(testKey);
        Site site = createSite();
        delegate.nextSuspendResult = site;

        Site result = service.suspend(command, testActor);

        assertEquals(site, result);
        assertSame(command, delegate.lastSuspendCommand);
        assertEquals(testActor, delegate.lastActor);

        CodexEvent publishedEvent = eventDispatcher.singleEvent();
        assertInstanceOf(SiteSuspendedEvent.class, publishedEvent);
    }

    @Test
    void archive_ShouldDelegateAndPublishEvent() {
        ArchiveSiteCommand command = ArchiveSiteCommand.of(testKey);
        Site site = createSite();
        delegate.nextArchiveResult = site;

        Site result = service.archive(command, testActor);

        assertEquals(site, result);
        assertSame(command, delegate.lastArchiveCommand);
        assertEquals(testActor, delegate.lastActor);

        CodexEvent publishedEvent = eventDispatcher.singleEvent();
        assertInstanceOf(SiteArchivedEvent.class, publishedEvent);
    }

    @Test
    void unarchive_ShouldDelegateAndPublishEvent() {
        UnarchiveSiteCommand command = UnarchiveSiteCommand.of(testKey);
        Site site = createSite();
        delegate.nextUnarchiveResult = site;

        Site result = service.unarchive(command, testActor);

        assertEquals(site, result);
        assertSame(command, delegate.lastUnarchiveCommand);
        assertEquals(testActor, delegate.lastActor);

        CodexEvent publishedEvent = eventDispatcher.singleEvent();
        assertInstanceOf(SiteUnarchivedEvent.class, publishedEvent);
    }

    @Test
    void findByKey_ShouldDelegateOnly() {
        Site site = createSite();
        delegate.nextFindByKeyResult = Optional.of(site);

        Optional<Site> result = service.findByKey(testKey, testActor);

        assertEquals(Optional.of(site), result);
        assertEquals(testKey, delegate.lastFindByKey);
        assertEquals(testActor, delegate.lastActor);
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void findByAlias_ShouldDelegateOnly() {
        SiteAlias alias = SiteAlias.of("alias.com");
        Site site = createSite();
        delegate.nextFindByAliasResult = Optional.of(site);

        Optional<Site> result = service.findByAlias(alias, testActor);

        assertEquals(Optional.of(site), result);
        assertEquals(alias, delegate.lastFindByAlias);
        assertEquals(testActor, delegate.lastActor);
        assertTrue(eventDispatcher.events.isEmpty());
    }

    @Test
    void findAll_ShouldDelegateOnly() {
        List<Site> sites = List.of(createSite());
        delegate.nextFindAllResult = sites;

        List<Site> result = service.findAll(testActor);

        assertEquals(sites, result);
        assertEquals(testActor, delegate.lastActor);
        assertTrue(eventDispatcher.events.isEmpty());
    }

    private Site createSite() {
        return Site.builder()
                .id(testId)
                .key(testKey)
                .displayName("Test Site")
                .build();
    }

    private static final class RecordingEventDispatcher implements CodexEventDispatcher {

        private final List<CodexEvent> events = new java.util.ArrayList<>();

        @Override
        public void dispatch(final CodexEvent event) {
            events.add(event);
        }

        private CodexEvent singleEvent() {
            assertEquals(1, events.size());
            return events.getFirst();
        }
    }

    private static final class FakeSiteService implements SiteService {

        private Site nextCreateResult;
        private Site nextStartResult;
        private Site nextSuspendResult;
        private Site nextArchiveResult;
        private Site nextUnarchiveResult;
        private Optional<Site> nextFindByKeyResult = Optional.empty();
        private Optional<Site> nextFindByAliasResult = Optional.empty();
        private List<Site> nextFindAllResult = List.of();

        private CreateSiteCommand lastCreateCommand;
        private StartSiteCommand lastStartCommand;
        private SuspendSiteCommand lastSuspendCommand;
        private ArchiveSiteCommand lastArchiveCommand;
        private UnarchiveSiteCommand lastUnarchiveCommand;
        private SiteKey lastFindByKey;
        private SiteAlias lastFindByAlias;
        private Actor lastActor;

        @Override
        public Site create(final CreateSiteCommand command, final Actor actor) {
            this.lastCreateCommand = command;
            this.lastActor = actor;
            return nextCreateResult;
        }

        @Override
        public Optional<Site> findByKey(final SiteKey siteKey, final Actor actor) {
            this.lastFindByKey = siteKey;
            this.lastActor = actor;
            return nextFindByKeyResult;
        }

        @Override
        public Site start(final StartSiteCommand command, final Actor actor) {
            this.lastStartCommand = command;
            this.lastActor = actor;
            return nextStartResult;
        }

        @Override
        public Site suspend(final SuspendSiteCommand command, final Actor actor) {
            this.lastSuspendCommand = command;
            this.lastActor = actor;
            return nextSuspendResult;
        }

        @Override
        public Site archive(final ArchiveSiteCommand command, final Actor actor) {
            this.lastArchiveCommand = command;
            this.lastActor = actor;
            return nextArchiveResult;
        }

        @Override
        public Site unarchive(final UnarchiveSiteCommand command, final Actor actor) {
            this.lastUnarchiveCommand = command;
            this.lastActor = actor;
            return nextUnarchiveResult;
        }

        @Override
        public Optional<Site> findByAlias(final SiteAlias alias, final Actor actor) {
            this.lastFindByAlias = alias;
            this.lastActor = actor;
            return nextFindByAliasResult;
        }

        @Override
        public List<Site> findAll(final Actor actor) {
            this.lastActor = actor;
            return nextFindAllResult;
        }
    }
}
