package codex.codex.internal.service;

import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.runtime.CodexRuntime;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import codex.fundamentum.api.observance.InMemoryObservance;
import codex.fundamentum.api.observance.Observance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static codex.codex.internal.service.SiteServiceMetricNames.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests verifying that {@link TimedSiteService} is wired into
 * {@link CodexRuntime} and records service-level metrics when an {@link Observance}
 * instance is provided.
 */
class CodexRuntimeObservanceTest {

    private static final Actor ACTOR = Actor.human(ActorId.of("user-1"), "Test User");
    private static final SiteKey SITE_KEY = SiteKey.of("acme");

    private CodexRuntime runtime;

    @AfterEach
    void tearDown() {
        if (runtime != null) {
            runtime.shutdown();
        }
    }

    // --- factory: inMemory(Observance) ---

    @Test
    void inMemoryWithObservanceCreatesRuntime() {
        runtime = CodexRuntime.inMemory(new InMemoryObservance());
        assertNotNull(runtime);
    }

    @Test
    void inMemoryWithObservanceRejectsNull() {
        assertThrows(NullPointerException.class,
                () -> CodexRuntime.inMemory((Observance) null));
    }

    // --- duration recorded on success ---

    @Test
    void createSiteRecordsDuration() {
        final InMemoryObservance observance = new InMemoryObservance();
        runtime = CodexRuntime.inMemory(observance);

        runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);

        assertEquals(1, observance.timerCount(CREATE_DURATION));
    }

    @Test
    void findByKeyRecordsDuration() {
        final InMemoryObservance observance = new InMemoryObservance();
        runtime = CodexRuntime.inMemory(observance);

        runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        runtime.siteService().findByKey(SITE_KEY, ACTOR);

        assertEquals(1, observance.timerCount(FIND_BY_KEY_DURATION));
    }

    @Test
    void findAllRecordsDuration() {
        final InMemoryObservance observance = new InMemoryObservance();
        runtime = CodexRuntime.inMemory(observance);

        runtime.siteService().findAll(ACTOR);

        assertEquals(1, observance.timerCount(FIND_ALL_DURATION));
    }

    // --- failure counter recorded on failure ---

    @Test
    void duplicateCreateRecordsDurationAndFailedCounter() {
        final InMemoryObservance observance = new InMemoryObservance();
        runtime = CodexRuntime.inMemory(observance);

        runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        assertThrows(RuntimeException.class,
                () -> runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR));

        assertEquals(2, observance.timerCount(CREATE_DURATION));
        assertEquals(1, observance.counterValue(CREATE_FAILED));
    }

    // --- behavior preserved ---

    @Test
    void createReturnsSite() {
        runtime = CodexRuntime.inMemory(new InMemoryObservance());

        final Site site = runtime.siteService().create(
                CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);

        assertNotNull(site);
        assertEquals(SITE_KEY, site.key());
    }

    @Test
    void findByKeyReturnsSiteAfterCreate() {
        runtime = CodexRuntime.inMemory(new InMemoryObservance());

        runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        final Optional<Site> found = runtime.siteService().findByKey(SITE_KEY, ACTOR);

        assertTrue(found.isPresent());
        assertEquals(SITE_KEY, found.get().key());
    }

    // --- no-op path still works ---

    @Test
    void noArgFactoryStillWorks() {
        runtime = CodexRuntime.inMemory();
        final Site site = runtime.siteService().create(
                CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);
        assertNotNull(site);
    }

    @Test
    void noArgFactoryRecordsNoMetrics() {
        final InMemoryObservance observance = new InMemoryObservance();
        runtime = CodexRuntime.inMemory();

        runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), ACTOR);

        // no-op runtime uses Observance.noop() — InMemoryObservance should have nothing
        assertEquals(0, observance.timerCount(CREATE_DURATION));
    }
}
