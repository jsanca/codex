package codex.codex.internal.service;

import codex.codex.api.model.command.ArchiveSiteCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.StartSiteCommand;
import codex.codex.api.model.command.SuspendSiteCommand;
import codex.codex.api.model.command.UnarchiveSiteCommand;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteId;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.codex.api.model.value.SiteStatus;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import codex.fundamentum.api.observance.InMemoryObservance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static codex.codex.internal.service.SiteServiceMetricNames.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link TimedSiteService}.
 *
 * <p>Validates that every operation records its duration timer and — only on failure —
 * increments its failure counter, without changing return values, exception propagation,
 * or delegate call count.</p>
 */
class TimedSiteServiceTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Test User");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");

    private InMemoryObservance observance;
    private StubSiteService stub;
    private TimedSiteService service;

    @BeforeEach
    void setUp() {
        observance = new InMemoryObservance();
        stub = new StubSiteService();
        service = new TimedSiteService(stub, observance);
    }

    // --- constructor ---

    @Test
    void constructorRejectsNullDelegate() {
        assertThrows(NullPointerException.class,
                () -> new TimedSiteService(null, observance));
    }

    @Test
    void constructorRejectsNullObservance() {
        assertThrows(NullPointerException.class,
                () -> new TimedSiteService(stub, null));
    }

    @Test
    void getDelegateReturnsDelegate() {
        assertSame(stub, service.getDelegate());
    }

    // --- create ---

    @Test
    void createRecordsDuration() {
        stub.nextSite = sampleSite();
        service.create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        assertEquals(1, observance.timerCount(CREATE_DURATION));
    }

    @Test
    void createReturnsDelegateResult() {
        final Site site = sampleSite();
        stub.nextSite = site;
        assertSame(site, service.create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR));
    }

    @Test
    void createDelegatesExactlyOnce() {
        stub.nextSite = sampleSite();
        service.create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        assertEquals(1, stub.createCallCount);
    }

    @Test
    void createFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "create";
        assertThrows(RuntimeException.class,
                () -> service.create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR));
        assertEquals(1, observance.timerCount(CREATE_DURATION));
        assertEquals(1, observance.counterValue(CREATE_FAILED));
    }

    @Test
    void createFailurePropagatesException() {
        stub.throwOn = "create";
        final RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR));
        assertEquals("stub failure: create", ex.getMessage());
    }

    @Test
    void createSuccessDoesNotIncrementFailedCounter() {
        stub.nextSite = sampleSite();
        service.create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        assertEquals(0, observance.counterValue(CREATE_FAILED));
    }

    // --- start ---

    @Test
    void startRecordsDuration() {
        stub.nextSite = sampleSite();
        service.start(StartSiteCommand.of(SITE_KEY), ACTOR);
        assertEquals(1, observance.timerCount(START_DURATION));
    }

    @Test
    void startReturnsDelegateResult() {
        final Site site = sampleSite();
        stub.nextSite = site;
        assertSame(site, service.start(StartSiteCommand.of(SITE_KEY), ACTOR));
    }

    @Test
    void startFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "start";
        assertThrows(RuntimeException.class,
                () -> service.start(StartSiteCommand.of(SITE_KEY), ACTOR));
        assertEquals(1, observance.timerCount(START_DURATION));
        assertEquals(1, observance.counterValue(START_FAILED));
    }

    // --- suspend ---

    @Test
    void suspendRecordsDuration() {
        stub.nextSite = sampleSite();
        service.suspend(SuspendSiteCommand.of(SITE_KEY), ACTOR);
        assertEquals(1, observance.timerCount(SUSPEND_DURATION));
    }

    @Test
    void suspendFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "suspend";
        assertThrows(RuntimeException.class,
                () -> service.suspend(SuspendSiteCommand.of(SITE_KEY), ACTOR));
        assertEquals(1, observance.timerCount(SUSPEND_DURATION));
        assertEquals(1, observance.counterValue(SUSPEND_FAILED));
    }

    // --- archive ---

    @Test
    void archiveRecordsDuration() {
        stub.nextSite = sampleSite();
        service.archive(ArchiveSiteCommand.of(SITE_KEY), ACTOR);
        assertEquals(1, observance.timerCount(ARCHIVE_DURATION));
    }

    @Test
    void archiveFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "archive";
        assertThrows(RuntimeException.class,
                () -> service.archive(ArchiveSiteCommand.of(SITE_KEY), ACTOR));
        assertEquals(1, observance.timerCount(ARCHIVE_DURATION));
        assertEquals(1, observance.counterValue(ARCHIVE_FAILED));
    }

    // --- unarchive ---

    @Test
    void unarchiveRecordsDuration() {
        stub.nextSite = sampleSite();
        service.unarchive(UnarchiveSiteCommand.of(SITE_KEY), ACTOR);
        assertEquals(1, observance.timerCount(UNARCHIVE_DURATION));
    }

    @Test
    void unarchiveFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "unarchive";
        assertThrows(RuntimeException.class,
                () -> service.unarchive(UnarchiveSiteCommand.of(SITE_KEY), ACTOR));
        assertEquals(1, observance.timerCount(UNARCHIVE_DURATION));
        assertEquals(1, observance.counterValue(UNARCHIVE_FAILED));
    }

    // --- findByKey ---

    @Test
    void findByKeyRecordsDuration() {
        stub.nextOptionalSite = Optional.of(sampleSite());
        service.findByKey(SITE_KEY, ACTOR);
        assertEquals(1, observance.timerCount(FIND_BY_KEY_DURATION));
    }

    @Test
    void findByKeyReturnsDelegateResult() {
        final Optional<Site> result = Optional.of(sampleSite());
        stub.nextOptionalSite = result;
        assertSame(result, service.findByKey(SITE_KEY, ACTOR));
    }

    @Test
    void findByKeyFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "findByKey";
        assertThrows(RuntimeException.class,
                () -> service.findByKey(SITE_KEY, ACTOR));
        assertEquals(1, observance.timerCount(FIND_BY_KEY_DURATION));
        assertEquals(1, observance.counterValue(FIND_BY_KEY_FAILED));
    }

    // --- findByAlias ---

    @Test
    void findByAliasRecordsDuration() {
        stub.nextOptionalSite = Optional.empty();
        service.findByAlias(SiteAlias.of("www.acme.com"), ACTOR);
        assertEquals(1, observance.timerCount(FIND_BY_ALIAS_DURATION));
    }

    @Test
    void findByAliasFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "findByAlias";
        assertThrows(RuntimeException.class,
                () -> service.findByAlias(SiteAlias.of("www.acme.com"), ACTOR));
        assertEquals(1, observance.timerCount(FIND_BY_ALIAS_DURATION));
        assertEquals(1, observance.counterValue(FIND_BY_ALIAS_FAILED));
    }

    // --- findAll ---

    @Test
    void findAllRecordsDuration() {
        service.findAll(ACTOR);
        assertEquals(1, observance.timerCount(FIND_ALL_DURATION));
    }

    @Test
    void findAllReturnsDelegateResult() {
        final List<Site> all = List.of(sampleSite());
        stub.nextList = all;
        assertSame(all, service.findAll(ACTOR));
    }

    @Test
    void findAllFailureRecordsDurationAndFailedCounter() {
        stub.throwOn = "findAll";
        assertThrows(RuntimeException.class, () -> service.findAll(ACTOR));
        assertEquals(1, observance.timerCount(FIND_ALL_DURATION));
        assertEquals(1, observance.counterValue(FIND_ALL_FAILED));
    }

    // --- no cross-contamination ---

    @Test
    void eachOperationUsesItsOwnMetrics() {
        stub.nextSite = sampleSite();
        stub.nextOptionalSite = Optional.empty();

        service.create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        service.findByKey(SITE_KEY, ACTOR);

        assertEquals(1, observance.timerCount(CREATE_DURATION));
        assertEquals(1, observance.timerCount(FIND_BY_KEY_DURATION));
        assertEquals(0, observance.timerCount(START_DURATION));
    }

    // --- private helpers ---

    private static Site sampleSite() {
        return Site.builder()
                .id(SiteId.of(UUID.randomUUID().toString()))
                .key(SITE_KEY)
                .displayName("Acme")
                .status(SiteStatus.STARTED)
                .build();
    }

    // --- inner stub ---

    private static final class StubSiteService implements SiteService {

        Site nextSite;
        Optional<Site> nextOptionalSite = Optional.empty();
        List<Site> nextList = List.of();
        String throwOn;
        int createCallCount;

        private void maybeThrow(final String op) {
            if (op.equals(throwOn)) {
                throw new RuntimeException("stub failure: " + op);
            }
        }

        @Override
        public Site create(final CreateSiteCommand command, final Actor actor) {
            maybeThrow("create");
            createCallCount++;
            return nextSite;
        }

        @Override
        public Optional<Site> findByKey(final SiteKey siteKey, final Actor actor) {
            maybeThrow("findByKey");
            return nextOptionalSite;
        }

        @Override
        public Site start(final StartSiteCommand command, final Actor actor) {
            maybeThrow("start");
            return nextSite;
        }

        @Override
        public Site suspend(final SuspendSiteCommand command, final Actor actor) {
            maybeThrow("suspend");
            return nextSite;
        }

        @Override
        public Site archive(final ArchiveSiteCommand command, final Actor actor) {
            maybeThrow("archive");
            return nextSite;
        }

        @Override
        public Site unarchive(final UnarchiveSiteCommand command, final Actor actor) {
            maybeThrow("unarchive");
            return nextSite;
        }

        @Override
        public Optional<Site> findByAlias(final SiteAlias alias, final Actor actor) {
            maybeThrow("findByAlias");
            return nextOptionalSite;
        }

        @Override
        public List<Site> findAll(final Actor actor) {
            maybeThrow("findAll");
            return nextList;
        }
    }
}
