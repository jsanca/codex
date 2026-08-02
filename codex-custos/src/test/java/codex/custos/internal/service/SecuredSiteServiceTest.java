package codex.custos.internal.service;

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
import codex.custos.api.exception.AccessDeniedException;
import codex.custos.api.exception.CustosAgentSuperAdminInvariantViolationException;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.GlobalResourceRef;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.Permissions;
import codex.custos.api.model.RoleAssignment;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class SecuredSiteServiceTest {

    private static final SiteKey SITE_A = SiteKey.of("site-a");
    private static final Actor ALICE = Actor.human(ActorId.of("user-alice"), "Alice");
    private static final Actor BOT   = Actor.agent("indexer");

    private static final PermissionResolutionSnapshot EMPTY_SNAPSHOT =
            PermissionResolutionSnapshot.of(List.<RoleAssignment>of(), Map.of());

    private static final AccessDecision GRANTED =
            AccessDecision.granted(ALICE, Permissions.SITE_READ, GlobalResourceRef.INSTANCE, "stub grant");
    private static final AccessDecision DENIED =
            AccessDecision.denied(ALICE, Permissions.SITE_READ, GlobalResourceRef.INSTANCE, "stub deny");

    private StubSitePermissionsService permissionsStub;
    private SpySiteService delegateSpy;
    private SecuredSiteService service;

    @BeforeEach
    void setUp() {
        permissionsStub = new StubSitePermissionsService(GRANTED);
        delegateSpy = new SpySiteService();
        service = new SecuredSiteService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);
    }

    // -----------------------------------------------------------------------
    // Constructor null guards
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("null guard — constructor")
    class ConstructorNullGuards {

        @Test
        @DisplayName("null delegate is rejected")
        void nullDelegate() {
            assertThatNullPointerException().isThrownBy(() ->
                    new SecuredSiteService(null, permissionsStub, () -> EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null permissionsService is rejected")
        void nullPermissionsService() {
            assertThatNullPointerException().isThrownBy(() ->
                    new SecuredSiteService(delegateSpy, null, () -> EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null snapshotProvider is rejected")
        void nullSnapshotProvider() {
            assertThatNullPointerException().isThrownBy(() ->
                    new SecuredSiteService(delegateSpy, permissionsStub, null));
        }
    }

    // -----------------------------------------------------------------------
    // create
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("create")
    class Create {

        private final CreateSiteCommand CREATE_CMD = CreateSiteCommand.of(SITE_A, "Site A");

        @Test
        @DisplayName("checks canCreateSite before delegating")
        void checksPermissionBeforeDelegate() {
            service.create(CREATE_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canCreateSite");
        }

        @Test
        @DisplayName("denied create does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubSitePermissionsService(DENIED);
            service = new SecuredSiteService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.create(CREATE_CMD, ALICE));
            assertThat(delegateSpy.createCallCount()).isZero();
        }

        @Test
        @DisplayName("granted create delegates exactly once")
        void grantedDelegatesOnce() {
            service.create(CREATE_CMD, ALICE);
            assertThat(delegateSpy.createCallCount()).isOne();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.create(null, ALICE));
        }

        @Test
        @DisplayName("null actor is rejected")
        void nullActor() {
            assertThatNullPointerException().isThrownBy(() -> service.create(CREATE_CMD, null));
        }
    }

    // -----------------------------------------------------------------------
    // findByKey
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("findByKey")
    class FindByKey {

        @Test
        @DisplayName("checks canReadSite before delegating")
        void checksPermissionBeforeDelegate() {
            service.findByKey(SITE_A, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canReadSite");
        }

        @Test
        @DisplayName("denied findByKey does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubSitePermissionsService(DENIED);
            service = new SecuredSiteService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.findByKey(SITE_A, ALICE));
            assertThat(delegateSpy.findByKeyCallCount()).isZero();
        }

        @Test
        @DisplayName("granted findByKey delegates exactly once")
        void grantedDelegatesOnce() {
            service.findByKey(SITE_A, ALICE);
            assertThat(delegateSpy.findByKeyCallCount()).isOne();
        }

        @Test
        @DisplayName("delegate return value is preserved")
        void delegateReturnValuePreserved() {
            Site stubSite = minimalSite();
            delegateSpy.stubFindByKeyResult(Optional.of(stubSite));

            Optional<Site> result = service.findByKey(SITE_A, ALICE);

            assertThat(result).containsSame(stubSite);
        }

        @Test
        @DisplayName("null siteKey is rejected")
        void nullSiteKey() {
            assertThatNullPointerException().isThrownBy(() -> service.findByKey(null, ALICE));
        }

        @Test
        @DisplayName("null actor is rejected")
        void nullActor() {
            assertThatNullPointerException().isThrownBy(() -> service.findByKey(SITE_A, null));
        }
    }

    // -----------------------------------------------------------------------
    // start
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("start")
    class Start {

        private final StartSiteCommand START_CMD = StartSiteCommand.of(SITE_A);

        @Test
        @DisplayName("checks canStartSite before delegating")
        void checksPermissionBeforeDelegate() {
            service.start(START_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canStartSite");
        }

        @Test
        @DisplayName("denied start does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubSitePermissionsService(DENIED);
            service = new SecuredSiteService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.start(START_CMD, ALICE));
            assertThat(delegateSpy.startCallCount()).isZero();
        }

        @Test
        @DisplayName("granted start delegates exactly once")
        void grantedDelegatesOnce() {
            service.start(START_CMD, ALICE);
            assertThat(delegateSpy.startCallCount()).isOne();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.start(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // suspend
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("suspend")
    class Suspend {

        private final SuspendSiteCommand SUSPEND_CMD = SuspendSiteCommand.of(SITE_A);

        @Test
        @DisplayName("checks canSuspendSite before delegating")
        void checksPermissionBeforeDelegate() {
            service.suspend(SUSPEND_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canSuspendSite");
        }

        @Test
        @DisplayName("denied suspend does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubSitePermissionsService(DENIED);
            service = new SecuredSiteService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.suspend(SUSPEND_CMD, ALICE));
            assertThat(delegateSpy.suspendCallCount()).isZero();
        }

        @Test
        @DisplayName("granted suspend delegates exactly once")
        void grantedDelegatesOnce() {
            service.suspend(SUSPEND_CMD, ALICE);
            assertThat(delegateSpy.suspendCallCount()).isOne();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.suspend(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // archive
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("archive")
    class Archive {

        private final ArchiveSiteCommand ARCHIVE_CMD = ArchiveSiteCommand.of(SITE_A);

        @Test
        @DisplayName("checks canArchiveSite before delegating")
        void checksPermissionBeforeDelegate() {
            service.archive(ARCHIVE_CMD, ALICE);
            assertThat(permissionsStub.lastOperationChecked()).isEqualTo("canArchiveSite");
        }

        @Test
        @DisplayName("denied archive does not call the delegate")
        void deniedDoesNotCallDelegate() {
            permissionsStub = new StubSitePermissionsService(DENIED);
            service = new SecuredSiteService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.archive(ARCHIVE_CMD, ALICE));
            assertThat(delegateSpy.archiveCallCount()).isZero();
        }

        @Test
        @DisplayName("granted archive delegates exactly once")
        void grantedDelegatesOnce() {
            service.archive(ARCHIVE_CMD, ALICE);
            assertThat(delegateSpy.archiveCallCount()).isOne();
        }

        @Test
        @DisplayName("null command is rejected")
        void nullCommand() {
            assertThatNullPointerException().isThrownBy(() -> service.archive(null, ALICE));
        }
    }

    // -----------------------------------------------------------------------
    // unarchive — fail closed
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("unarchive — fail closed")
    class Unarchive {

        private final UnarchiveSiteCommand UNARCHIVE_CMD = UnarchiveSiteCommand.of(SITE_A);

        @Test
        @DisplayName("unarchive throws UnsupportedOperationException")
        void throwsUnsupportedOperationException() {
            assertThatExceptionOfType(UnsupportedOperationException.class)
                    .isThrownBy(() -> service.unarchive(UNARCHIVE_CMD, ALICE));
        }

        @Test
        @DisplayName("unarchive does not call the delegate")
        void doesNotCallDelegate() {
            try { service.unarchive(UNARCHIVE_CMD, ALICE); } catch (UnsupportedOperationException ignored) {}
            assertThat(delegateSpy.unarchiveCallCount()).isZero();
        }
    }

    // -----------------------------------------------------------------------
    // Collection reads — documented pass-through
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("collection reads — pass-through")
    class CollectionReads {

        @Test
        @DisplayName("findByAlias delegates without permission check")
        void findByAliasDelegates() {
            service.findByAlias(SiteAlias.of("www.example.com"), ALICE);
            assertThat(delegateSpy.findByAliasCallCount()).isOne();
        }

        @Test
        @DisplayName("findAll delegates without permission check")
        void findAllDelegates() {
            service.findAll(ALICE);
            assertThat(delegateSpy.findAllCallCount()).isOne();
        }
    }

    // -----------------------------------------------------------------------
    // Cross-cutting: AccessDeniedException and invariant propagation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("authorization outcome")
    class AuthorizationOutcome {

        @Test
        @DisplayName("denied decision throws AccessDeniedException")
        void deniedThrowsAccessDeniedException() {
            permissionsStub = new StubSitePermissionsService(DENIED);
            service = new SecuredSiteService(delegateSpy, permissionsStub, () -> EMPTY_SNAPSHOT);

            assertThatExceptionOfType(AccessDeniedException.class)
                    .isThrownBy(() -> service.create(CreateSiteCommand.of(SITE_A, "Site A"), ALICE));
        }

        @Test
        @DisplayName("CustosAgentSuperAdminInvariantViolationException propagates uncaught")
        void invariantExceptionPropagates() {
            CustosAgentSuperAdminInvariantViolationException invariantViolation =
                    new CustosAgentSuperAdminInvariantViolationException(BOT);
            permissionsStub.throwInstead(invariantViolation);

            assertThatExceptionOfType(CustosAgentSuperAdminInvariantViolationException.class)
                    .isThrownBy(() -> service.create(CreateSiteCommand.of(SITE_A, "Site A"), BOT))
                    .satisfies(ex -> assertThat(ex.actor()).isEqualTo(BOT));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static Site minimalSite() {
        return Site.builder()
                .id(SiteId.of("site-id-1"))
                .key(SITE_A)
                .displayName("Site A")
                .build();
    }

    // -----------------------------------------------------------------------
    // Spy delegate
    // -----------------------------------------------------------------------

    private static final class SpySiteService implements SiteService {

        private int createCount;
        private int findByKeyCount;
        private int startCount;
        private int suspendCount;
        private int archiveCount;
        private int unarchiveCount;
        private int findByAliasCount;
        private int findAllCount;
        private Optional<Site> findByKeyResult = Optional.empty();

        void stubFindByKeyResult(final Optional<Site> result) {
            this.findByKeyResult = result;
        }

        int createCallCount()      { return createCount; }
        int findByKeyCallCount()   { return findByKeyCount; }
        int startCallCount()       { return startCount; }
        int suspendCallCount()     { return suspendCount; }
        int archiveCallCount()     { return archiveCount; }
        int unarchiveCallCount()   { return unarchiveCount; }
        int findByAliasCallCount() { return findByAliasCount; }
        int findAllCallCount()     { return findAllCount; }

        @Override
        public Site create(final CreateSiteCommand createSiteCommand, final Actor actor) {
            createCount++;
            return null;
        }

        @Override
        public Optional<Site> findByKey(final SiteKey siteKey, final Actor actor) {
            findByKeyCount++;
            return findByKeyResult;
        }

        @Override
        public Site start(final StartSiteCommand startSiteCommand, final Actor actor) {
            startCount++;
            return null;
        }

        @Override
        public Site suspend(final SuspendSiteCommand suspendSiteCommand, final Actor actor) {
            suspendCount++;
            return null;
        }

        @Override
        public Site archive(final ArchiveSiteCommand archiveSiteCommand, final Actor actor) {
            archiveCount++;
            return null;
        }

        @Override
        public Site unarchive(final UnarchiveSiteCommand unarchiveSiteCommand, final Actor actor) {
            unarchiveCount++;
            return null;
        }

        @Override
        public Optional<Site> findByAlias(final SiteAlias alias, final Actor actor) {
            findByAliasCount++;
            return Optional.empty();
        }

        @Override
        public List<Site> findAll(final Actor actor) {
            findAllCount++;
            return List.of();
        }
    }
}
