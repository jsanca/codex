package codex.custos.internal.service;

import codex.codex.api.model.command.ArchiveSiteCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.StartSiteCommand;
import codex.codex.api.model.command.SuspendSiteCommand;
import codex.codex.api.model.command.UnarchiveSiteCommand;
import codex.codex.api.model.entity.Site;
import codex.codex.api.model.entity.SiteAlias;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.SiteService;
import codex.custos.api.exception.AccessDeniedException;
import codex.custos.api.exception.CustosAgentSuperAdminInvariantViolationException;
import codex.custos.api.model.BuiltInRoles;
import codex.custos.api.model.GlobalScope;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.Role;
import codex.custos.api.model.RoleAssignment;
import codex.custos.api.model.RoleKey;
import codex.custos.api.model.SiteScope;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Service-level authorization matrix test for {@link SecuredSiteService}.
 * <p>
 * Uses the full real Custos chain:
 * {@code DefaultPermissionResolver → DefaultAccessDecisionService → DefaultSitePermissionsService → SecuredSiteService}
 * with a {@link SpyDelegate} to verify whether the delegate was reached.
 */
class SecuredSiteServiceAuthorizationMatrixTest {

    // --- site keys ---
    private static final SiteKey SITE_A = SiteKey.of("site-a");
    private static final SiteKey SITE_B = SiteKey.of("site-b");

    // --- actors ---
    private static final Actor ALICE = Actor.human(ActorId.of("user-alice"), "Alice");   // SITE_ADMIN on site-a
    private static final Actor BOB   = Actor.human(ActorId.of("user-bob"),   "Bob");     // EDITOR on site-a
    private static final Actor CAROL = Actor.human(ActorId.of("user-carol"), "Carol");   // SUPER_ADMIN (global)
    private static final Actor EVE   = Actor.human(ActorId.of("user-eve"),   "Eve");     // SITE_ADMIN on site-a only
    private static final Actor BOT   = Actor.agent("sync-bot");                           // AGENT + SUPER_ADMIN

    // --- commands ---
    private static final UnarchiveSiteCommand UNARCHIVE_CMD        = UnarchiveSiteCommand.of(SITE_A);
    private static final UnarchiveSiteCommand UNARCHIVE_SITE_B_CMD = UnarchiveSiteCommand.of(SITE_B);
    private static final CreateSiteCommand    CREATE_CMD            = CreateSiteCommand.of(SITE_A, "Site A");

    // --- role registry shared by all tests ---
    private static final Map<RoleKey, Role> ALL_ROLES = Map.of(
            BuiltInRoles.EDITOR.key(),      BuiltInRoles.EDITOR,
            BuiltInRoles.SITE_ADMIN.key(),  BuiltInRoles.SITE_ADMIN,
            BuiltInRoles.SUPER_ADMIN.key(), BuiltInRoles.SUPER_ADMIN
    );

    private SpyDelegate delegateSpy;

    @BeforeEach
    void setUp() {
        delegateSpy = new SpyDelegate();
    }

    // -----------------------------------------------------------------------
    // Assembly helpers
    // -----------------------------------------------------------------------

    private SecuredSiteService buildService(final PermissionResolutionSnapshot snapshot) {
        final DefaultPermissionResolver resolver = new DefaultPermissionResolver();
        final DefaultAccessDecisionService decisions = new DefaultAccessDecisionService(resolver);
        final DefaultSitePermissionsService perms = new DefaultSitePermissionsService(decisions);
        return new SecuredSiteService(delegateSpy, perms, () -> snapshot);
    }

    private static PermissionResolutionSnapshot snapshotWith(final RoleAssignment... assignments) {
        return PermissionResolutionSnapshot.of(Arrays.asList(assignments), ALL_ROLES);
    }

    // -----------------------------------------------------------------------
    // Scenario 1 — SITE_ADMIN: unarchive granted
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("SiteAdmin — unarchive granted")
    class SiteAdminRole {

        private SecuredSiteService service;

        @BeforeEach
        void setUp() {
            service = buildService(snapshotWith(
                    RoleAssignment.of(ALICE, BuiltInRoles.SITE_ADMIN.key(), new SiteScope(SITE_A))));
        }

        @Test
        @DisplayName("unarchive is granted and reaches the delegate")
        void unarchiveGranted() {
            service.unarchive(UNARCHIVE_CMD, ALICE);
            assertThat(delegateSpy.unarchiveCount()).isOne();
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 2 — EDITOR: unarchive denied
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Editor — unarchive denied")
    class EditorRole {

        private SecuredSiteService service;

        @BeforeEach
        void setUp() {
            service = buildService(snapshotWith(
                    RoleAssignment.of(BOB, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A))));
        }

        @Test
        @DisplayName("unarchive throws AccessDeniedException carrying a denied decision")
        void unarchiveDeniedWithDecision() {
            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.unarchive(UNARCHIVE_CMD, BOB))
                    .satisfies(ex -> {
                        assertThat(ex.decision()).isPresent();
                        assertThat(ex.decision().get().actor()).isEqualTo(BOB);
                        assertThat(ex.decision().get().reason()).isNotBlank();
                    });
        }

        @Test
        @DisplayName("denied unarchive does not reach the delegate")
        void deniedUnarchiveDoesNotCallDelegate() {
            try { service.unarchive(UNARCHIVE_CMD, BOB); } catch (AccessDeniedException ignored) {}
            assertThat(delegateSpy.unarchiveCount()).isZero();
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 3 — SUPER_ADMIN (human): unarchive granted at global scope
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("SuperAdmin — unarchive granted at global scope")
    class SuperAdminRole {

        private SecuredSiteService service;

        @BeforeEach
        void setUp() {
            service = buildService(snapshotWith(
                    RoleAssignment.of(CAROL, BuiltInRoles.SUPER_ADMIN.key(), GlobalScope.INSTANCE)));
        }

        @Test
        @DisplayName("unarchive is granted and reaches the delegate")
        void unarchiveGranted() {
            service.unarchive(UNARCHIVE_CMD, CAROL);
            assertThat(delegateSpy.unarchiveCount()).isOne();
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 4 — Site boundary
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Site boundary — role on site-a does not cover site-b")
    class SiteBoundary {

        private SecuredSiteService service;

        @BeforeEach
        void setUp() {
            service = buildService(snapshotWith(
                    RoleAssignment.of(EVE, BuiltInRoles.SITE_ADMIN.key(), new SiteScope(SITE_A))));
        }

        @Test
        @DisplayName("unarchive on site-b is denied when role is scoped to site-a")
        void unarchiveOnSiteBDenied() {
            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.unarchive(UNARCHIVE_SITE_B_CMD, EVE))
                    .satisfies(ex -> {
                        assertThat(ex.decision()).isPresent();
                        assertThat(ex.decision().get().reason()).isNotBlank();
                    });
        }

        @Test
        @DisplayName("denied cross-site unarchive does not call delegate")
        void crossSiteDoesNotCallDelegate() {
            try { service.unarchive(UNARCHIVE_SITE_B_CMD, EVE); } catch (AccessDeniedException ignored) {}
            assertThat(delegateSpy.unarchiveCount()).isZero();
        }

        @Test
        @DisplayName("same actor is still granted on site-a")
        void siteAUnarchiveStillGranted() {
            service.unarchive(UNARCHIVE_CMD, EVE);
            assertThat(delegateSpy.unarchiveCount()).isOne();
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 5 — AGENT + SUPER_ADMIN invariant
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Agent + SUPER_ADMIN — hard invariant propagates")
    class AgentSuperAdminInvariant {

        private SecuredSiteService service;

        @BeforeEach
        void setUp() {
            service = buildService(snapshotWith(
                    RoleAssignment.of(BOT, BuiltInRoles.SUPER_ADMIN.key(), GlobalScope.INSTANCE)));
        }

        @Test
        @DisplayName("CustosAgentSuperAdminInvariantViolationException is thrown")
        void invariantExceptionThrown() {
            assertThatExceptionOfType(CustosAgentSuperAdminInvariantViolationException.class)
                    .isThrownBy(() -> service.create(CREATE_CMD, BOT));
        }

        @Test
        @DisplayName("invariant exception is not converted to AccessDeniedException")
        void invariantExceptionIsNotDeniedException() {
            assertThatExceptionOfType(CustosAgentSuperAdminInvariantViolationException.class)
                    .isThrownBy(() -> service.create(CREATE_CMD, BOT))
                    .isNotInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("delegate is not called when invariant is violated")
        void delegateNotCalled() {
            try { service.create(CREATE_CMD, BOT); } catch (CustosAgentSuperAdminInvariantViolationException ignored) {}
            assertThat(delegateSpy.createCount()).isZero();
        }
    }

    // -----------------------------------------------------------------------
    // Spy delegate
    // -----------------------------------------------------------------------

    private static final class SpyDelegate implements SiteService {

        private int createCount;
        private int unarchiveCount;

        int createCount()    { return createCount; }
        int unarchiveCount() { return unarchiveCount; }

        @Override
        public Site create(final CreateSiteCommand createSiteCommand, final Actor actor) {
            createCount++;
            return null;
        }

        @Override
        public Optional<Site> findByKey(final SiteKey siteKey, final Actor actor) {
            return Optional.empty();
        }

        @Override
        public Site start(final StartSiteCommand startSiteCommand, final Actor actor) {
            return null;
        }

        @Override
        public Site suspend(final SuspendSiteCommand suspendSiteCommand, final Actor actor) {
            return null;
        }

        @Override
        public Site archive(final ArchiveSiteCommand archiveSiteCommand, final Actor actor) {
            return null;
        }

        @Override
        public Site unarchive(final UnarchiveSiteCommand unarchiveSiteCommand, final Actor actor) {
            unarchiveCount++;
            return null;
        }

        @Override
        public Optional<Site> findByAlias(final SiteAlias alias, final Actor actor) {
            return Optional.empty();
        }

        @Override
        public List<Site> findAll(final Actor actor) {
            return List.of();
        }
    }
}
