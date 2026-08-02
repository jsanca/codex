package codex.custos.internal.service;

import codex.codex.api.model.command.ArchiveContentItemCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.DeleteContentItemCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.RestoreContentItemCommand;
import codex.codex.api.model.command.UnpublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.service.ContentItemService;
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
 * Service-level authorization matrix test for {@link SecuredContentItemService}.
 * <p>
 * Uses the full real Custos chain:
 * {@code DefaultPermissionResolver → DefaultAccessDecisionService → DefaultContentItemPermissionsService → SecuredContentItemService}
 * with a {@link SpyDelegate} to verify whether the delegate was reached.
 */
class SecuredContentItemServiceAuthorizationMatrixTest {

    // --- site keys ---
    private static final SiteKey SITE_A = SiteKey.of("site-a");
    private static final SiteKey SITE_B = SiteKey.of("site-b");

    // --- content keys ---
    private static final ContentTypeKey BLOG    = ContentTypeKey.of("blog-post");
    private static final ContentItemKey WELCOME = ContentItemKey.of("welcome-post");

    // --- actors ---
    private static final Actor ALICE = Actor.human(ActorId.of("user-alice"), "Alice");   // VIEWER
    private static final Actor BOB   = Actor.human(ActorId.of("user-bob"),   "Bob");     // COPYWRITER
    private static final Actor CAROL = Actor.human(ActorId.of("user-carol"), "Carol");   // REVIEWER
    private static final Actor DAN   = Actor.human(ActorId.of("user-dan"),   "Dan");     // EDITOR
    private static final Actor EVE   = Actor.human(ActorId.of("user-eve"),   "Eve");     // EDITOR on site-a only
    private static final Actor BOT   = Actor.agent("sync-bot");                           // AGENT + SUPER_ADMIN

    // --- commands scoped to site-a ---
    private static final CreateContentItemCommand  CREATE_CMD  = CreateContentItemCommand.of(SITE_A, BLOG, WELCOME, null);
    private static final UpdateContentItemCommand  UPDATE_CMD  = UpdateContentItemCommand.of(SITE_A, BLOG, WELCOME, null);
    private static final PublishContentItemCommand PUBLISH_CMD = PublishContentItemCommand.of(SITE_A, BLOG, WELCOME);
    private static final ArchiveContentItemCommand ARCHIVE_CMD = ArchiveContentItemCommand.of(SITE_A, BLOG, WELCOME);

    // --- command scoped to site-b for boundary test ---
    private static final UpdateContentItemCommand UPDATE_SITE_B_CMD = UpdateContentItemCommand.of(SITE_B, BLOG, WELCOME, null);

    // --- role registry shared by all tests ---
    private static final Map<RoleKey, Role> ALL_ROLES = Map.of(
            BuiltInRoles.VIEWER.key(),      BuiltInRoles.VIEWER,
            BuiltInRoles.COPYWRITER.key(),  BuiltInRoles.COPYWRITER,
            BuiltInRoles.REVIEWER.key(),    BuiltInRoles.REVIEWER,
            BuiltInRoles.EDITOR.key(),      BuiltInRoles.EDITOR,
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

    private SecuredContentItemService buildService(final PermissionResolutionSnapshot snapshot) {
        final DefaultPermissionResolver resolver = new DefaultPermissionResolver();
        final DefaultAccessDecisionService decisions = new DefaultAccessDecisionService(resolver);
        final DefaultContentItemPermissionsService perms = new DefaultContentItemPermissionsService(decisions);
        return new SecuredContentItemService(delegateSpy, perms, () -> snapshot);
    }

    private static PermissionResolutionSnapshot snapshotWith(final RoleAssignment... assignments) {
        return PermissionResolutionSnapshot.of(Arrays.asList(assignments), ALL_ROLES);
    }

    // -----------------------------------------------------------------------
    // Scenario 1 — VIEWER: read yes, update no
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Viewer — read yes, update no")
    class ViewerRole {

        private SecuredContentItemService service;

        @BeforeEach
        void setUp() {
            service = buildService(snapshotWith(
                    RoleAssignment.of(ALICE, BuiltInRoles.VIEWER.key(), new SiteScope(SITE_A))));
        }

        @Test
        @DisplayName("findByKey is granted and reaches the delegate")
        void findByKeyGranted() {
            service.findByKey(SITE_A, BLOG, WELCOME, ALICE);
            assertThat(delegateSpy.findByKeyCount()).isOne();
        }

        @Test
        @DisplayName("update throws AccessDeniedException carrying a denied decision")
        void updateDeniedWithDecision() {
            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.update(UPDATE_CMD, ALICE))
                    .satisfies(ex -> {
                        assertThat(ex.decision()).isPresent();
                        assertThat(ex.decision().get().actor()).isEqualTo(ALICE);
                        assertThat(ex.decision().get().reason()).isNotBlank();
                    });
        }

        @Test
        @DisplayName("denied update does not reach the delegate")
        void deniedUpdateDoesNotCallDelegate() {
            try { service.update(UPDATE_CMD, ALICE); } catch (AccessDeniedException ignored) {}
            assertThat(delegateSpy.updateCount()).isZero();
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 2 — COPYWRITER: create/update yes, publish no
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Copywriter — create and update yes, publish no")
    class CopywriterRole {

        private SecuredContentItemService service;

        @BeforeEach
        void setUp() {
            service = buildService(snapshotWith(
                    RoleAssignment.of(BOB, BuiltInRoles.COPYWRITER.key(), new SiteScope(SITE_A))));
        }

        @Test
        @DisplayName("create is granted and reaches the delegate")
        void createGranted() {
            service.create(CREATE_CMD, BOB);
            assertThat(delegateSpy.createCount()).isOne();
        }

        @Test
        @DisplayName("update is granted and reaches the delegate")
        void updateGranted() {
            service.update(UPDATE_CMD, BOB);
            assertThat(delegateSpy.updateCount()).isOne();
        }

        @Test
        @DisplayName("publish is denied")
        void publishDenied() {
            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.publish(PUBLISH_CMD, BOB));
        }

        @Test
        @DisplayName("denied publish does not reach the delegate")
        void deniedPublishDoesNotCallDelegate() {
            try { service.publish(PUBLISH_CMD, BOB); } catch (AccessDeniedException ignored) {}
            assertThat(delegateSpy.publishCount()).isZero();
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 3 — REVIEWER: publish yes, update no
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Reviewer — publish yes, update no")
    class ReviewerRole {

        private SecuredContentItemService service;

        @BeforeEach
        void setUp() {
            service = buildService(snapshotWith(
                    RoleAssignment.of(CAROL, BuiltInRoles.REVIEWER.key(), new SiteScope(SITE_A))));
        }

        @Test
        @DisplayName("publish is granted and reaches the delegate")
        void publishGranted() {
            service.publish(PUBLISH_CMD, CAROL);
            assertThat(delegateSpy.publishCount()).isOne();
        }

        @Test
        @DisplayName("update is denied — reviewer cannot author content")
        void updateDenied() {
            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.update(UPDATE_CMD, CAROL))
                    .satisfies(ex -> assertThat(ex.decision()).isPresent());
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 4 — EDITOR: full lifecycle
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Editor — full lifecycle access")
    class EditorRole {

        private SecuredContentItemService service;

        @BeforeEach
        void setUp() {
            service = buildService(snapshotWith(
                    RoleAssignment.of(DAN, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A))));
        }

        @Test
        @DisplayName("create is granted")
        void createGranted() {
            service.create(CREATE_CMD, DAN);
            assertThat(delegateSpy.createCount()).isOne();
        }

        @Test
        @DisplayName("update is granted")
        void updateGranted() {
            service.update(UPDATE_CMD, DAN);
            assertThat(delegateSpy.updateCount()).isOne();
        }

        @Test
        @DisplayName("publish is granted")
        void publishGranted() {
            service.publish(PUBLISH_CMD, DAN);
            assertThat(delegateSpy.publishCount()).isOne();
        }

        @Test
        @DisplayName("archive is granted")
        void archiveGranted() {
            service.archive(ARCHIVE_CMD, DAN);
            assertThat(delegateSpy.archiveCount()).isOne();
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 5 — Site boundary
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Site boundary — role on site-a does not cover site-b")
    class SiteBoundary {

        private SecuredContentItemService service;

        @BeforeEach
        void setUp() {
            service = buildService(snapshotWith(
                    RoleAssignment.of(EVE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A))));
        }

        @Test
        @DisplayName("operation on site-b is denied when role is scoped to site-a")
        void operationOnSiteBDenied() {
            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.update(UPDATE_SITE_B_CMD, EVE))
                    .satisfies(ex -> {
                        assertThat(ex.decision()).isPresent();
                        assertThat(ex.decision().get().reason()).isNotBlank();
                    });
        }

        @Test
        @DisplayName("denied cross-site operation does not call delegate")
        void crossSiteDoesNotCallDelegate() {
            try { service.update(UPDATE_SITE_B_CMD, EVE); } catch (AccessDeniedException ignored) {}
            assertThat(delegateSpy.updateCount()).isZero();
        }

        @Test
        @DisplayName("same actor is still granted on site-a")
        void siteAOperationStillGranted() {
            service.update(UPDATE_CMD, EVE);
            assertThat(delegateSpy.updateCount()).isOne();
        }
    }

    // -----------------------------------------------------------------------
    // Scenario 6 — AGENT + SUPER_ADMIN invariant
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("Agent + SUPER_ADMIN — hard invariant propagates")
    class AgentSuperAdminInvariant {

        private SecuredContentItemService service;

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

    private static final class SpyDelegate implements ContentItemService {

        private int createCount;
        private int findByKeyCount;
        private int updateCount;
        private int publishCount;
        private int archiveCount;

        int createCount()     { return createCount; }
        int findByKeyCount()  { return findByKeyCount; }
        int updateCount()     { return updateCount; }
        int publishCount()    { return publishCount; }
        int archiveCount()    { return archiveCount; }

        @Override
        public ContentItem create(final CreateContentItemCommand command, final Actor actor) {
            createCount++;
            return null;
        }

        @Override
        public Optional<ContentItem> findByKey(final SiteKey siteKey, final ContentTypeKey contentTypeKey,
                                               final ContentItemKey key, final Actor actor) {
            findByKeyCount++;
            return Optional.empty();
        }

        @Override
        public List<ContentItem> findByContentType(final SiteKey siteKey, final ContentTypeKey contentTypeKey,
                                                   final Actor actor) {
            return List.of();
        }

        @Override
        public List<ContentItem> findAll(final Actor actor) {
            return List.of();
        }

        @Override
        public ContentItem update(final UpdateContentItemCommand command, final Actor actor) {
            updateCount++;
            return null;
        }

        @Override
        public ContentItem archive(final ArchiveContentItemCommand command, final Actor actor) {
            archiveCount++;
            return null;
        }

        @Override
        public ContentItem unpublish(final UnpublishContentItemCommand command, final Actor actor) {
            return null;
        }

        @Override
        public void delete(final DeleteContentItemCommand command, final Actor actor) {
        }

        @Override
        public ContentItem restore(final RestoreContentItemCommand command, final Actor actor) {
            return null;
        }

        @Override
        public ContentItem publish(final PublishContentItemCommand command, final Actor actor) {
            publishCount++;
            return null;
        }
    }
}
