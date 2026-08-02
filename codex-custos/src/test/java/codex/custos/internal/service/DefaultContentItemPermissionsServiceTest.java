package codex.custos.internal.service;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.ContentItemResourceRef;
import codex.custos.api.model.ContentItemScope;
import codex.custos.api.model.ContentTypeResourceRef;
import codex.custos.api.model.ContentTypeScope;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DefaultContentItemPermissionsServiceTest {

    private static final SiteKey SITE_A = SiteKey.of("site-a");
    private static final ContentTypeKey BLOG = ContentTypeKey.of("blog-post");
    private static final ContentItemKey WELCOME = ContentItemKey.of("welcome-post");
    private static final Actor ALICE = Actor.human(ActorId.of("user-alice"), "Alice");
    private static final PermissionResolutionSnapshot EMPTY_SNAPSHOT =
            PermissionResolutionSnapshot.of(List.<RoleAssignment>of(), Map.of());

    private static final AccessDecision STUBBED_GRANTED =
            AccessDecision.granted(ALICE, Permissions.CONTENT_ITEM_READ, GlobalResourceRef.INSTANCE, "stub grant");

    private RecordingAccessDecisionService recording;
    private DefaultContentItemPermissionsService service;

    @BeforeEach
    void setUp() {
        recording = new RecordingAccessDecisionService(STUBBED_GRANTED);
        service = new DefaultContentItemPermissionsService(recording);
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
                    new DefaultContentItemPermissionsService(null));
        }
    }

    // -----------------------------------------------------------------------
    // canCreateContentItem
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canCreateContentItem")
    class CanCreateContentItem {

        @Test
        @DisplayName("uses CONTENT_ITEM_CREATE with ContentTypeResourceRef and ContentTypeScope")
        void mapsToContentTypeScopeWithContentItemCreate() {
            service.canCreateContentItem(ALICE, SITE_A, BLOG, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.CONTENT_ITEM_CREATE);
            assertThat(recording.lastRequest().resource()).isEqualTo(new ContentTypeResourceRef(SITE_A, BLOG));
            assertThat(recording.lastRequest().targetScope()).isEqualTo(new ContentTypeScope(SITE_A, BLOG));
            assertThat(recording.lastRequest().actor()).isEqualTo(ALICE);
        }

        @Test
        @DisplayName("returns the AccessDecision from the underlying service")
        void returnsDelegatedDecision() {
            AccessDecision decision = service.canCreateContentItem(ALICE, SITE_A, BLOG, EMPTY_SNAPSHOT);
            assertThat(decision).isSameAs(STUBBED_GRANTED);
        }

        @Test
        @DisplayName("null actor is rejected")
        void nullActor() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canCreateContentItem(null, SITE_A, BLOG, EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null siteKey is rejected")
        void nullSiteKey() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canCreateContentItem(ALICE, null, BLOG, EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null contentTypeKey is rejected")
        void nullContentTypeKey() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canCreateContentItem(ALICE, SITE_A, null, EMPTY_SNAPSHOT));
        }

        @Test
        @DisplayName("null snapshot is rejected")
        void nullSnapshot() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canCreateContentItem(ALICE, SITE_A, BLOG, null));
        }
    }

    // -----------------------------------------------------------------------
    // canReadContentItem
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canReadContentItem")
    class CanReadContentItem {

        @Test
        @DisplayName("uses CONTENT_ITEM_READ with ContentItemResourceRef and ContentItemScope")
        void mapsToContentItemScopeWithContentItemRead() {
            service.canReadContentItem(ALICE, SITE_A, BLOG, WELCOME, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.CONTENT_ITEM_READ);
            assertThat(recording.lastRequest().resource())
                    .isEqualTo(new ContentItemResourceRef(SITE_A, BLOG, WELCOME));
            assertThat(recording.lastRequest().targetScope())
                    .isEqualTo(new ContentItemScope(SITE_A, BLOG, WELCOME));
            assertThat(recording.lastRequest().actor()).isEqualTo(ALICE);
        }

        @Test
        @DisplayName("null contentItemKey is rejected")
        void nullContentItemKey() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.canReadContentItem(ALICE, SITE_A, BLOG, null, EMPTY_SNAPSHOT));
        }
    }

    // -----------------------------------------------------------------------
    // canUpdateContentItem
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canUpdateContentItem")
    class CanUpdateContentItem {

        @Test
        @DisplayName("uses CONTENT_ITEM_UPDATE with ContentItemResourceRef and ContentItemScope")
        void mapsToContentItemScopeWithContentItemUpdate() {
            service.canUpdateContentItem(ALICE, SITE_A, BLOG, WELCOME, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.CONTENT_ITEM_UPDATE);
            assertThat(recording.lastRequest().resource())
                    .isEqualTo(new ContentItemResourceRef(SITE_A, BLOG, WELCOME));
            assertThat(recording.lastRequest().targetScope())
                    .isEqualTo(new ContentItemScope(SITE_A, BLOG, WELCOME));
        }
    }

    // -----------------------------------------------------------------------
    // canPublishContentItem
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canPublishContentItem")
    class CanPublishContentItem {

        @Test
        @DisplayName("uses CONTENT_ITEM_PUBLISH with ContentItemResourceRef and ContentItemScope")
        void mapsToContentItemScopeWithContentItemPublish() {
            service.canPublishContentItem(ALICE, SITE_A, BLOG, WELCOME, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.CONTENT_ITEM_PUBLISH);
            assertThat(recording.lastRequest().resource())
                    .isEqualTo(new ContentItemResourceRef(SITE_A, BLOG, WELCOME));
            assertThat(recording.lastRequest().targetScope())
                    .isEqualTo(new ContentItemScope(SITE_A, BLOG, WELCOME));
        }
    }

    // -----------------------------------------------------------------------
    // canUnpublishContentItem
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canUnpublishContentItem")
    class CanUnpublishContentItem {

        @Test
        @DisplayName("uses CONTENT_ITEM_UNPUBLISH with ContentItemResourceRef and ContentItemScope")
        void mapsToContentItemScopeWithContentItemUnpublish() {
            service.canUnpublishContentItem(ALICE, SITE_A, BLOG, WELCOME, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.CONTENT_ITEM_UNPUBLISH);
            assertThat(recording.lastRequest().resource())
                    .isEqualTo(new ContentItemResourceRef(SITE_A, BLOG, WELCOME));
            assertThat(recording.lastRequest().targetScope())
                    .isEqualTo(new ContentItemScope(SITE_A, BLOG, WELCOME));
        }
    }

    // -----------------------------------------------------------------------
    // canArchiveContentItem
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("canArchiveContentItem")
    class CanArchiveContentItem {

        @Test
        @DisplayName("uses CONTENT_ITEM_ARCHIVE with ContentItemResourceRef and ContentItemScope")
        void mapsToContentItemScopeWithContentItemArchive() {
            service.canArchiveContentItem(ALICE, SITE_A, BLOG, WELCOME, EMPTY_SNAPSHOT);

            assertThat(recording.lastRequest().permission()).isEqualTo(Permissions.CONTENT_ITEM_ARCHIVE);
            assertThat(recording.lastRequest().resource())
                    .isEqualTo(new ContentItemResourceRef(SITE_A, BLOG, WELCOME));
            assertThat(recording.lastRequest().targetScope())
                    .isEqualTo(new ContentItemScope(SITE_A, BLOG, WELCOME));
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
            service.canReadContentItem(ALICE, SITE_A, BLOG, WELCOME, EMPTY_SNAPSHOT);
            assertThat(recording.lastSnapshot()).isSameAs(EMPTY_SNAPSHOT);
        }
    }
}
