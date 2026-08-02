package codex.custos.internal.service;

import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.GlobalResourceRef;
import codex.custos.api.model.GlobalScope;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.Permissions;
import codex.custos.api.model.RoleAssignment;
import codex.custos.api.model.SiteResourceRef;
import codex.custos.api.model.SiteScope;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DefaultSitePermissionsServiceTest {

    private static final SiteKey SITE_A = SiteKey.of("site-a");
    private static final Actor ALICE = Actor.human(ActorId.of("user-alice"), "Alice");
    private static final PermissionResolutionSnapshot EMPTY_SNAPSHOT =
            PermissionResolutionSnapshot.of(List.<RoleAssignment>of(), Map.of());

    private static final AccessDecision STUBBED_GRANTED =
            AccessDecision.granted(ALICE, Permissions.SITE_READ, GlobalResourceRef.INSTANCE, "stub grant");

    private RecordingAccessDecisionService recording;
    private DefaultSitePermissionsService service;

    @BeforeEach
    void setUp() {
        recording = new RecordingAccessDecisionService(STUBBED_GRANTED);
        service = new DefaultSitePermissionsService(recording);
    }

    // -----------------------------------------------------------------------
    // Constructor null guard
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("null guard — constructor")
    class ConstructorNullGuard {

        @Test
        @DisplayName("null decisionService is rejected")
        void nullDecisionService() {
            assertThatNullPointerException().isThrownBy(() ->
                    new DefaultSitePermissionsService(null));
        }
    }

    // -----------------------------------------------------------------------
    // canCreateSite
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canCreateSite")
    class CanCreateSite {

        @Test
        @DisplayName("uses SITE_CREATE with GlobalResourceRef and GlobalScope")
        void mapsToGlobalScopeWithSiteCreate() {
            service.canCreateSite(ALICE, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.SITE_CREATE);
            assertThat(recording.lastRequest().resource()).isEqualTo(GlobalResourceRef.INSTANCE);
            assertThat(recording.lastRequest().targetScope()).isEqualTo(GlobalScope.INSTANCE);
            assertThat(recording.lastRequest().actor()).isEqualTo(ALICE);
        }

        @Test
        @DisplayName("returns the AccessDecision from the underlying service")
        void returnsDelegatedDecision() {
            AccessDecision decision = service.canCreateSite(ALICE, EMPTY_SNAPSHOT);
            assertThat(decision).isSameAs(STUBBED_GRANTED);
        }

        @Test
        @DisplayName("null actor is rejected")
        void nullActor() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canCreateSite(null, EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null snapshot is rejected")
        void nullSnapshot() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canCreateSite(ALICE, null));
        }
    }

    // -----------------------------------------------------------------------
    // canReadSite
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canReadSite")
    class CanReadSite {

        @Test
        @DisplayName("uses SITE_READ with SiteResourceRef and SiteScope")
        void mapsToSiteScopeWithSiteRead() {
            service.canReadSite(ALICE, SITE_A, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.SITE_READ);
            assertThat(recording.lastRequest().resource()).isEqualTo(new SiteResourceRef(SITE_A));
            assertThat(recording.lastRequest().targetScope()).isEqualTo(new SiteScope(SITE_A));
            assertThat(recording.lastRequest().actor()).isEqualTo(ALICE);
        }

        @Test
        @DisplayName("null siteKey is rejected")
        void nullSiteKey() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canReadSite(ALICE, null, EMPTY_SNAPSHOT));
        }
    }

    // -----------------------------------------------------------------------
    // canStartSite
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canStartSite")
    class CanStartSite {

        @Test
        @DisplayName("uses SITE_START with SiteResourceRef and SiteScope")
        void mapsToSiteScopeWithSiteStart() {
            service.canStartSite(ALICE, SITE_A, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.SITE_START);
            assertThat(recording.lastRequest().resource()).isEqualTo(new SiteResourceRef(SITE_A));
            assertThat(recording.lastRequest().targetScope()).isEqualTo(new SiteScope(SITE_A));
        }
    }

    // -----------------------------------------------------------------------
    // canSuspendSite
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canSuspendSite")
    class CanSuspendSite {

        @Test
        @DisplayName("uses SITE_SUSPEND with SiteResourceRef and SiteScope")
        void mapsToSiteScopeWithSiteSuspend() {
            service.canSuspendSite(ALICE, SITE_A, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.SITE_SUSPEND);
            assertThat(recording.lastRequest().resource()).isEqualTo(new SiteResourceRef(SITE_A));
            assertThat(recording.lastRequest().targetScope()).isEqualTo(new SiteScope(SITE_A));
        }
    }

    // -----------------------------------------------------------------------
    // canArchiveSite
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canArchiveSite")
    class CanArchiveSite {

        @Test
        @DisplayName("uses SITE_ARCHIVE with SiteResourceRef and SiteScope")
        void mapsToSiteScopeWithSiteArchive() {
            service.canArchiveSite(ALICE, SITE_A, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.SITE_ARCHIVE);
            assertThat(recording.lastRequest().resource()).isEqualTo(new SiteResourceRef(SITE_A));
            assertThat(recording.lastRequest().targetScope()).isEqualTo(new SiteScope(SITE_A));
        }
    }

    // -----------------------------------------------------------------------
    // snapshot forwarding
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("snapshot forwarding")
    class SnapshotForwarding {

        @Test
        @DisplayName("snapshot is forwarded as-is to AccessDecisionService")
        void snapshotIsForwarded() {
            service.canReadSite(ALICE, SITE_A, EMPTY_SNAPSHOT);
            assertThat(recording.lastSnapshot()).isSameAs(EMPTY_SNAPSHOT);
        }
    }
}
