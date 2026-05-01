package codex.codex.internal.service;

import codex.codex.api.model.command.ArchiveSiteCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.SuspendSiteCommand;
import codex.codex.api.model.event.SiteCreatedEvent;
import codex.codex.api.model.event.SiteSuspendedEvent;
import codex.codex.api.model.identity.SiteKey;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.tx.TransactionContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SiteEventPipelineIntegrationTest {

    private static final Actor ACTOR = Actor.system("demo");
    private static final SiteKey SITE_KEY = SiteKey.of("integration-test-site");

    private TestCodexContext ctx;

    @BeforeEach
    void setUp() {
        ctx = TestCodexContext.create();
    }

    // --- create ---

    @Test
    @DisplayName("create inside transaction does not dispatch before commit")
    void createInsideTransactionDoesNotDispatchBeforeCommit() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.siteService().create(CreateSiteCommand.of(SITE_KEY, "Integration Site"), ACTOR);
            ctx.assertNoEvents();
            return null;
        });
    }

    @Test
    @DisplayName("create inside transaction dispatches SiteCreatedEvent after commit")
    void createInsideTransactionDispatchesSiteCreatedEventAfterCommit() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.siteService().create(CreateSiteCommand.of(SITE_KEY, "Integration Site"), ACTOR);
            return null;
        });

        final SiteCreatedEvent event = ctx.assertSingleEvent(SiteCreatedEvent.class);
        assertEquals(SITE_KEY, event.key());
        assertEquals(ACTOR, event.actor());
        assertEquals(TestCodexContext.CLOCK.instant(), event.occurredAt());
    }

    @Test
    @DisplayName("create rollback does not dispatch event")
    void createRollbackDoesNotDispatchEvent() {
        assertThrows(RuntimeException.class, () ->
            TransactionContext.runInTransaction(() -> {
                ctx.siteService().create(CreateSiteCommand.of(SITE_KEY, "Integration Site"), ACTOR);
                throw new RuntimeException("forced rollback");
            })
        );

        ctx.assertNoEvents();
    }

    // --- suspend ---

    @Test
    @DisplayName("suspend dispatches SiteSuspendedEvent only when status changes")
    void suspendDispatchesSiteSuspendedEventOnlyWhenStatusChanges() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.siteService().create(CreateSiteCommand.of(SITE_KEY, "Integration Site"), ACTOR);
            return null;
        });
        ctx.clearEvents();

        TransactionContext.runInTransaction(() -> {
            ctx.siteService().suspend(SuspendSiteCommand.of(SITE_KEY), ACTOR);
            return null;
        });

        final SiteSuspendedEvent event = ctx.assertSingleEvent(SiteSuspendedEvent.class);
        assertEquals(SITE_KEY, event.key());
    }

    @Test
    @DisplayName("idempotent suspend does not dispatch event")
    void idempotentSuspendDoesNotDispatchEvent() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.siteService().create(CreateSiteCommand.of(SITE_KEY, "Integration Site"), ACTOR);
            return null;
        });
        TransactionContext.runInTransaction(() -> {
            ctx.siteService().suspend(SuspendSiteCommand.of(SITE_KEY), ACTOR);
            return null;
        });
        ctx.clearEvents();

        TransactionContext.runInTransaction(() -> {
            ctx.siteService().suspend(SuspendSiteCommand.of(SITE_KEY), ACTOR);
            return null;
        });

        ctx.assertNoEvents();
    }

    // --- invalid transition ---

    @Test
    @DisplayName("invalid transition does not dispatch event")
    void invalidTransitionDoesNotDispatchEvent() throws Exception {
        TransactionContext.runInTransaction(() -> {
            ctx.siteService().create(CreateSiteCommand.of(SITE_KEY, "Integration Site"), ACTOR);
            return null;
        });
        ctx.clearEvents();

        assertThrows(InvalidSiteStatusTransitionException.class, () ->
            TransactionContext.runInTransaction(() -> {
                ctx.siteService().archive(ArchiveSiteCommand.of(SITE_KEY), ACTOR);
                return null;
            })
        );

        ctx.assertNoEvents();
    }
}
