package codex.codex.internal.service.internal;

import codex.codex.api.model.command.ArchiveSiteCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.StartSiteCommand;
import codex.codex.api.model.command.SuspendSiteCommand;
import codex.codex.api.model.command.UnarchiveSiteCommand;

import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.SiteStatus;
import codex.codex.internal.repository.MemorySiteRepository;
import codex.fundamentum.api.exception.NotFoundException;
import codex.fundamentum.api.model.Actor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CodexSiteServiceTest {

    private MemorySiteRepository siteRepository;

    private CodexSiteService siteService;

    private final Actor testActor = Actor.system("test");
    private final SiteKey testKey = SiteKey.of("test-site");
    private final SiteId testId = SiteId.of(UUID.randomUUID().toString());
    private final Clock testClock = Clock.fixed(Instant.parse("2026-04-24T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        siteRepository = new MemorySiteRepository();
        siteService = new CodexSiteService(siteRepository, testClock,  command -> testId);
    }

    @Test
    void create_ShouldSaveSite_WhenItDoesNotExist() {
        CreateSiteCommand command = createSiteCommand(testKey, SiteStatus.STARTED);

        Site result = siteService.create(command, testActor);

        assertNotNull(result);
        assertEquals(testId, result.id());
        assertEquals(testKey, result.key());
        assertEquals("Test Site", result.displayName());
        assertEquals(SiteStatus.STARTED, result.status());
        assertTrue(siteRepository.findByKey(testKey).isPresent());
    }

    @Test
    void create_ShouldThrowException_WhenSiteAlreadyExists() {
        Site site = createSite(testKey, SiteStatus.STARTED);
        siteRepository.save(site);
        CreateSiteCommand command = createSiteCommand(testKey, SiteStatus.STARTED);

        assertThrows(SiteAlreadyExistException.class, () -> siteService.create(command, testActor));
    }

    @Test
    void findByKey_ShouldDelegateToRepository() {
        Site site = createSite(testKey, SiteStatus.STARTED);
        siteRepository.save(site);

        Optional<Site> result = siteService.findByKey(testKey, testActor);

        assertTrue(result.isPresent());
        assertEquals(site, result.get());
    }

    @Test
    void findByAlias_ShouldDelegateToRepository() {
        SiteAlias alias = SiteAlias.of("alias.com");
        Site site = Site.copyOf(createSite(testKey, SiteStatus.STARTED))
                .aliases(Set.of(alias))
                .build();
        siteRepository.save(site);

        Optional<Site> result = siteService.findByAlias(alias, testActor);

        assertTrue(result.isPresent());
        assertEquals(site, result.get());
    }

    @Test
    void findAll_ShouldDelegateToRepository() {
        Site site = createSite(testKey, SiteStatus.STARTED);
        siteRepository.save(site);

        List<Site> result = siteService.findAll(testActor);

        assertEquals(List.of(site), result);
    }

    @Test
    void findAll_ShouldThrowNPE_WhenActorIsNull() {
        assertThrows(NullPointerException.class, () -> siteService.findAll(null));
    }

    @Test
    void nullArguments_ShouldThrowNPE() {
        assertThrows(NullPointerException.class, () -> siteService.create(null, testActor));
        assertThrows(NullPointerException.class, () -> siteService.create(createSiteCommand(testKey, SiteStatus.STARTED), null));
        assertThrows(NullPointerException.class, () -> siteService.findByKey(null, testActor));
        assertThrows(NullPointerException.class, () -> siteService.findByKey(testKey, null));
        assertThrows(NullPointerException.class, () -> siteService.start(null, testActor));
        assertThrows(NullPointerException.class, () -> siteService.start(StartSiteCommand.of(testKey), null));
        assertThrows(NullPointerException.class, () -> siteService.suspend(null, testActor));
        assertThrows(NullPointerException.class, () -> siteService.suspend(SuspendSiteCommand.of(testKey), null));
        assertThrows(NullPointerException.class, () -> siteService.archive(null, testActor));
        assertThrows(NullPointerException.class, () -> siteService.archive(ArchiveSiteCommand.of(testKey), null));
        assertThrows(NullPointerException.class, () -> siteService.unarchive(null, testActor));
        assertThrows(NullPointerException.class, () -> siteService.unarchive(UnarchiveSiteCommand.of(testKey), null));
    }

    @Test
    void missingSite_ShouldThrowNotFoundException() {
        assertThrows(NotFoundException.class, () -> siteService.start(StartSiteCommand.of(testKey), testActor));
        assertThrows(NotFoundException.class, () -> siteService.suspend(SuspendSiteCommand.of(testKey), testActor));
        assertThrows(NotFoundException.class, () -> siteService.archive(ArchiveSiteCommand.of(testKey), testActor));
        assertThrows(NotFoundException.class, () -> siteService.unarchive(UnarchiveSiteCommand.of(testKey), testActor));
    }

    // State Machine Tests

    @Test
    void transition_StartedToSuspended_ShouldBeValid() {
        Site site = createSite(testKey, SiteStatus.STARTED);
        siteRepository.save(site);

        Site result = siteService.suspend(SuspendSiteCommand.of(testKey), testActor);

        assertEquals(SiteStatus.SUSPENDED, result.status());
        assertEquals(SiteStatus.SUSPENDED, siteRepository.findByKey(testKey).orElseThrow().status());
    }

    @Test
    void transition_StartedToArchived_ShouldBeInvalid() {
        Site site = createSite(testKey, SiteStatus.STARTED);
        siteRepository.save(site);

        assertThrows(InvalidSiteStatusTransitionException.class, () -> siteService.archive(ArchiveSiteCommand.of(testKey), testActor));
    }

    @Test
    void transition_StartedToStarted_ShouldBeIdempotent() {
        Site site = createSite(testKey, SiteStatus.STARTED);
        siteRepository.save(site);

        Site result = siteService.start(StartSiteCommand.of(testKey), testActor);

        assertEquals(SiteStatus.STARTED, result.status());
        assertSame(site, result);
    }

    @Test
    void transition_SuspendedToStarted_ShouldBeValid() {
        Site site = createSite(testKey, SiteStatus.SUSPENDED);
        siteRepository.save(site);

        Site result = siteService.start(StartSiteCommand.of(testKey), testActor);

        assertEquals(SiteStatus.STARTED, result.status());
        assertEquals(SiteStatus.STARTED, siteRepository.findByKey(testKey).orElseThrow().status());
    }

    @Test
    void transition_SuspendedToArchived_ShouldBeValid() {
        Site site = createSite(testKey, SiteStatus.SUSPENDED);
        siteRepository.save(site);

        Site result = siteService.archive(ArchiveSiteCommand.of(testKey), testActor);

        assertEquals(SiteStatus.ARCHIVED, result.status());
        assertEquals(SiteStatus.ARCHIVED, siteRepository.findByKey(testKey).orElseThrow().status());
    }

    @Test
    void transition_SuspendedToSuspended_ShouldBeIdempotent() {
        Site site = createSite(testKey, SiteStatus.SUSPENDED);
        siteRepository.save(site);

        Site result = siteService.suspend(SuspendSiteCommand.of(testKey), testActor);

        assertEquals(SiteStatus.SUSPENDED, result.status());
        assertSame(site, result);
    }

    @Test
    void transition_ArchivedToSuspended_ShouldBeValidViaUnarchive() {
        Site site = createSite(testKey, SiteStatus.ARCHIVED);
        siteRepository.save(site);

        Site result = siteService.unarchive(UnarchiveSiteCommand.of(testKey), testActor);

        assertEquals(SiteStatus.SUSPENDED, result.status());
        assertEquals(SiteStatus.SUSPENDED, siteRepository.findByKey(testKey).orElseThrow().status());
    }

    @Test
    void transition_ArchivedToStarted_ShouldBeInvalid() {
        Site site = createSite(testKey, SiteStatus.ARCHIVED);
        siteRepository.save(site);

        assertThrows(InvalidSiteStatusTransitionException.class, () -> siteService.start(StartSiteCommand.of(testKey), testActor));
    }

    @Test
    void transition_ArchivedToArchived_ShouldBeIdempotent() {
        Site site = createSite(testKey, SiteStatus.ARCHIVED);
        siteRepository.save(site);

        Site result = siteService.archive(ArchiveSiteCommand.of(testKey), testActor);

        assertEquals(SiteStatus.ARCHIVED, result.status());
        assertSame(site, result);
    }

    private CreateSiteCommand createSiteCommand(SiteKey key, SiteStatus status) {
        return new CreateSiteCommand(
                key,
                "Test Site",
                status,
                Set.of()
        );
    }

    private Site createSite(SiteKey key, SiteStatus status) {
        return Site.builder()
                .id(testId)
                .key(key)
                .displayName("Test Site")
                .status(status)
                .build();
    }
}
