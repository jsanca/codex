package codex.custos.internal.service;

import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.ContentTypeResourceRef;
import codex.custos.api.model.ContentTypeScope;
import codex.custos.api.model.GlobalResourceRef;
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

class DefaultContentTypePermissionsServiceTest {

    private static final SiteKey SITE_A = SiteKey.of("site-a");
    private static final ContentTypeKey BLOG = ContentTypeKey.of("blog-post");
    private static final Actor ALICE = Actor.human(ActorId.of("user-alice"), "Alice");
    private static final PermissionResolutionSnapshot EMPTY_SNAPSHOT =
            PermissionResolutionSnapshot.of(List.<RoleAssignment>of(), Map.of());

    private static final AccessDecision STUBBED_GRANTED =
            AccessDecision.granted(ALICE, Permissions.CONTENT_TYPE_READ, GlobalResourceRef.INSTANCE, "stub grant");

    private RecordingAccessDecisionService recording;
    private DefaultContentTypePermissionsService service;

    @BeforeEach
    void setUp() {
        recording = new RecordingAccessDecisionService(STUBBED_GRANTED);
        service = new DefaultContentTypePermissionsService(recording);
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
                    new DefaultContentTypePermissionsService(null));
        }
    }

    // -----------------------------------------------------------------------
    // canCreateContentType
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canCreateContentType")
    class CanCreateContentType {

        @Test
        @DisplayName("uses CONTENT_TYPE_CREATE with SiteResourceRef and SiteScope")
        void mapsToSiteScopeWithContentTypeCreate() {
            service.canCreateContentType(ALICE, SITE_A, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.CONTENT_TYPE_CREATE);
            assertThat(recording.lastRequest().resource()).isEqualTo(new SiteResourceRef(SITE_A));
            assertThat(recording.lastRequest().targetScope()).isEqualTo(new SiteScope(SITE_A));
            assertThat(recording.lastRequest().actor()).isEqualTo(ALICE);
        }

        @Test
        @DisplayName("returns the AccessDecision from the underlying service")
        void returnsDelegatedDecision() {
            AccessDecision decision = service.canCreateContentType(ALICE, SITE_A, EMPTY_SNAPSHOT);
            assertThat(decision).isSameAs(STUBBED_GRANTED);
        }

        @Test
        @DisplayName("null actor is rejected")
        void nullActor() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canCreateContentType(null, SITE_A, EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null siteKey is rejected")
        void nullSiteKey() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canCreateContentType(ALICE, null, EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null snapshot is rejected")
        void nullSnapshot() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canCreateContentType(ALICE, SITE_A, null));
        }
    }

    // -----------------------------------------------------------------------
    // canReadContentType
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canReadContentType")
    class CanReadContentType {

        @Test
        @DisplayName("uses CONTENT_TYPE_READ with ContentTypeResourceRef and ContentTypeScope")
        void mapsToContentTypeScopeWithContentTypeRead() {
            service.canReadContentType(ALICE, SITE_A, BLOG, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.CONTENT_TYPE_READ);
            assertThat(recording.lastRequest().resource()).isEqualTo(new ContentTypeResourceRef(SITE_A, BLOG));
            assertThat(recording.lastRequest().targetScope()).isEqualTo(new ContentTypeScope(SITE_A, BLOG));
            assertThat(recording.lastRequest().actor()).isEqualTo(ALICE);
        }

        @Test
        @DisplayName("null contentTypeKey is rejected")
        void nullContentTypeKey() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canReadContentType(ALICE, SITE_A, null, EMPTY_SNAPSHOT));
        }
    }

    // -----------------------------------------------------------------------
    // canUpdateContentType
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canUpdateContentType")
    class CanUpdateContentType {

        @Test
        @DisplayName("uses CONTENT_TYPE_UPDATE with ContentTypeResourceRef and ContentTypeScope")
        void mapsToContentTypeScopeWithContentTypeUpdate() {
            service.canUpdateContentType(ALICE, SITE_A, BLOG, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.CONTENT_TYPE_UPDATE);
            assertThat(recording.lastRequest().resource()).isEqualTo(new ContentTypeResourceRef(SITE_A, BLOG));
            assertThat(recording.lastRequest().targetScope()).isEqualTo(new ContentTypeScope(SITE_A, BLOG));
        }
    }

    // -----------------------------------------------------------------------
    // canArchiveContentType
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canArchiveContentType")
    class CanArchiveContentType {

        @Test
        @DisplayName("uses CONTENT_TYPE_ARCHIVE with ContentTypeResourceRef and ContentTypeScope")
        void mapsToContentTypeScopeWithContentTypeArchive() {
            service.canArchiveContentType(ALICE, SITE_A, BLOG, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.CONTENT_TYPE_ARCHIVE);
            assertThat(recording.lastRequest().resource()).isEqualTo(new ContentTypeResourceRef(SITE_A, BLOG));
            assertThat(recording.lastRequest().targetScope()).isEqualTo(new ContentTypeScope(SITE_A, BLOG));
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
            service.canReadContentType(ALICE, SITE_A, BLOG, EMPTY_SNAPSHOT);
            assertThat(recording.lastSnapshot()).isSameAs(EMPTY_SNAPSHOT);
        }
    }
}
