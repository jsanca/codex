package codex.concilium.internal;

import codex.concilium.api.runtime.ConciliumRuntime;
import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.command.UpdateContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.ContentItemStatus;
import codex.codex.api.model.value.FieldType;
import codex.custos.api.exception.AccessDeniedException;
import codex.custos.api.model.BuiltInRoles;
import codex.custos.api.model.GlobalScope;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.RoleAssignment;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves that denied operations in {@link ConciliumRuntime#secured(java.util.function.Supplier)}
 * produce no side effects in any layer of the runtime pipeline.
 *
 * <p>A denied operation must not mutate domain state, emit domain events, create Chronicon
 * audit records, or trigger index subscriber updates. Each test group sets up authorized
 * state, then attempts a denied operation and asserts that no new side effects appeared.</p>
 *
 * <p>Domain operations use the top-level service accessors ({@code runtime.siteService()}, etc.).
 * Low-level state inspection uses {@code runtime.coreRuntime()} accessors explicitly.</p>
 */
class ConciliumRuntimeDeniedSideEffectsTest {

    private static final Actor ADMIN = Actor.human(ActorId.of("admin-1"), "Admin");
    private static final Actor UNAUTHORIZED = Actor.human(ActorId.of("anon-1"), "Anon");

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("article");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("first-post");

    private static final PermissionResolutionSnapshot SUPER_ADMIN_SNAPSHOT =
            PermissionResolutionSnapshot.of(
                    List.of(RoleAssignment.of(ADMIN, BuiltInRoles.SUPER_ADMIN.key(), GlobalScope.INSTANCE)),
                    Map.of(BuiltInRoles.SUPER_ADMIN.key(), BuiltInRoles.SUPER_ADMIN));

    private static final PermissionResolutionSnapshot EMPTY_SNAPSHOT =
            PermissionResolutionSnapshot.of(List.of(), Map.of());

    private AtomicReference<PermissionResolutionSnapshot> activeSnapshot;
    private ConciliumRuntime runtime;

    @BeforeEach
    void createRuntime() {
        activeSnapshot = new AtomicReference<>(SUPER_ADMIN_SNAPSHOT);
        runtime = ConciliumRuntime.secured(activeSnapshot::get);
    }

    private void switchToUnauthorized() {
        activeSnapshot.set(EMPTY_SNAPSHOT);
    }

    // --- Denied site create ---

    @Nested
    @DisplayName("Denied site create")
    class DeniedSiteCreate {

        @BeforeEach
        void switchToEmpty() {
            switchToUnauthorized();
        }

        @Test
        @DisplayName("throws AccessDeniedException")
        void throwsAccessDeniedException() {
            assertThatThrownBy(() -> runtime.siteService().create(
                    CreateSiteCommand.of(SITE_KEY, "Acme"), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("no domain events recorded")
        void noEventsRecorded() {
            assertThatThrownBy(() -> runtime.siteService().create(
                    CreateSiteCommand.of(SITE_KEY, "Acme"), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.coreRuntime().recordedEvents()).isEmpty();
        }

        @Test
        @DisplayName("no Chronicon audit record created")
        void noChroniconAuditRecord() {
            assertThatThrownBy(() -> runtime.siteService().create(
                    CreateSiteCommand.of(SITE_KEY, "Acme"), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.chroniconRuntime().repository().findAll()).isEmpty();
        }

        @Test
        @DisplayName("site state remains empty")
        void siteStateRemainsEmpty() {
            assertThatThrownBy(() -> runtime.siteService().create(
                    CreateSiteCommand.of(SITE_KEY, "Acme"), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.coreRuntime().siteService().findAll(UNAUTHORIZED)).isEmpty();
        }
    }

    // --- Denied content type create ---

    @Nested
    @DisplayName("Denied content type create")
    class DeniedContentTypeCreate {

        @BeforeEach
        void setupSiteThenDeny() {
            runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), ADMIN);
            switchToUnauthorized();
        }

        @Test
        @DisplayName("throws AccessDeniedException")
        void throwsAccessDeniedException() {
            assertThatThrownBy(() -> runtime.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("no new domain events after denied create")
        void noNewEventsAfterDenied() {
            final int baselineEventCount = runtime.coreRuntime().recordedEvents().size();

            assertThatThrownBy(() -> runtime.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.coreRuntime().recordedEvents()).hasSize(baselineEventCount);
        }

        @Test
        @DisplayName("no new Chronicon audit record after denied create")
        void noChroniconRecordAfterDenied() {
            final int baselineAuditCount = runtime.chroniconRuntime().repository().findAll().size();

            assertThatThrownBy(() -> runtime.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.chroniconRuntime().repository().findAll()).hasSize(baselineAuditCount);
        }

        @Test
        @DisplayName("site has no content types after denied create")
        void siteHasNoContentTypesAfterDenied() {
            assertThatThrownBy(() -> runtime.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.coreRuntime().contentTypeService()
                    .findBySiteKey(SITE_KEY, UNAUTHORIZED)).isEmpty();
        }
    }

    // --- Denied content item publish ---

    @Nested
    @DisplayName("Denied content item publish")
    class DeniedContentItemPublish {

        private int baselineEventCount;
        private int baselineAuditCount;

        @BeforeEach
        void setupDraftItemThenDeny() {
            runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), ADMIN);
            runtime.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ADMIN);
            runtime.contentTypeService().addField(
                    AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                            Field.builder().key(FieldKey.TITLE).displayName("Title")
                                    .type(FieldType.TEXT).required(true).build()),
                    ADMIN);
            runtime.contentTypeService().activate(
                    ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ADMIN);
            runtime.contentItemService().create(
                    CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(FieldKey.TITLE, "First Post")),
                    ADMIN);

            switchToUnauthorized();

            baselineEventCount = runtime.coreRuntime().recordedEvents().size();
            baselineAuditCount = runtime.chroniconRuntime().repository().findAll().size();
        }

        @Test
        @DisplayName("throws AccessDeniedException")
        void throwsAccessDeniedException() {
            assertThatThrownBy(() -> runtime.contentItemService().publish(
                    PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("item remains in DRAFT status — publish did not reach delegate")
        void itemRemainsInDraft() {
            assertThatThrownBy(() -> runtime.contentItemService().publish(
                    PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            final ContentItem item = runtime.coreRuntime().contentItemService()
                    .findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ADMIN).orElseThrow();
            assertThat(item.status()).isEqualTo(ContentItemStatus.DRAFT);
            assertThat(item.currentPublishedRevisionId()).isNull();
        }

        @Test
        @DisplayName("no new domain events after denied publish")
        void noNewEventsAfterDenied() {
            assertThatThrownBy(() -> runtime.contentItemService().publish(
                    PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.coreRuntime().recordedEvents()).hasSize(baselineEventCount);
        }

        @Test
        @DisplayName("no new Chronicon audit record after denied publish")
        void noChroniconRecordAfterDenied() {
            assertThatThrownBy(() -> runtime.contentItemService().publish(
                    PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.chroniconRuntime().repository().findAll()).hasSize(baselineAuditCount);
        }

        @Test
        @DisplayName("index not updated — no publish event emitted so no index subscriber triggered")
        void indexNotUpdatedAfterDeniedPublish() {
            assertThatThrownBy(() -> runtime.contentItemService().publish(
                    PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            // Index updates are driven exclusively by domain events via the module event dispatcher.
            // If no new events were recorded, the ContentItemPublishedIndexingSubscriber was never invoked.
            assertThat(runtime.coreRuntime().recordedEvents()).hasSize(baselineEventCount);
        }
    }

    // --- Denied content item update ---

    @Nested
    @DisplayName("Denied content item update")
    class DeniedContentItemUpdate {

        private ContentItem originalItem;
        private int baselineEventCount;
        private int baselineAuditCount;

        @BeforeEach
        void setupItemThenDeny() {
            runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), ADMIN);
            runtime.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), ADMIN);
            runtime.contentTypeService().addField(
                    AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                            Field.builder().key(FieldKey.TITLE).displayName("Title")
                                    .type(FieldType.TEXT).required(true).build()),
                    ADMIN);
            runtime.contentTypeService().activate(
                    ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), ADMIN);
            originalItem = runtime.contentItemService().create(
                    CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(FieldKey.TITLE, "Original Title")),
                    ADMIN);

            switchToUnauthorized();

            baselineEventCount = runtime.coreRuntime().recordedEvents().size();
            baselineAuditCount = runtime.chroniconRuntime().repository().findAll().size();
        }

        @Test
        @DisplayName("throws AccessDeniedException")
        void throwsAccessDeniedException() {
            assertThatThrownBy(() -> runtime.contentItemService().update(
                    UpdateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(FieldKey.TITLE, "Changed Title")),
                    UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("item working revision unchanged — update did not reach delegate")
        void itemWorkingRevisionUnchanged() {
            assertThatThrownBy(() -> runtime.contentItemService().update(
                    UpdateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(FieldKey.TITLE, "Changed Title")),
                    UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            final ContentItem itemAfterAttempt = runtime.coreRuntime().contentItemService()
                    .findByKey(SITE_KEY, CT_KEY, ITEM_KEY, ADMIN).orElseThrow();
            assertThat(itemAfterAttempt.currentWorkingRevisionId())
                    .isEqualTo(originalItem.currentWorkingRevisionId());
        }

        @Test
        @DisplayName("no new domain events after denied update")
        void noNewEventsAfterDenied() {
            assertThatThrownBy(() -> runtime.contentItemService().update(
                    UpdateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(FieldKey.TITLE, "Changed Title")),
                    UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.coreRuntime().recordedEvents()).hasSize(baselineEventCount);
        }

        @Test
        @DisplayName("no new Chronicon audit record after denied update")
        void noChroniconRecordAfterDenied() {
            assertThatThrownBy(() -> runtime.contentItemService().update(
                    UpdateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(FieldKey.TITLE, "Changed Title")),
                    UNAUTHORIZED))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.chroniconRuntime().repository().findAll()).hasSize(baselineAuditCount);
        }
    }
}
