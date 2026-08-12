package codex.concilium.internal;

import codex.chronicon.api.AuditRecord;
import codex.concilium.api.runtime.ConciliumRuntime;
import codex.concilium.api.runtime.RuntimeSecurityMode;
import codex.codex.api.model.command.ActivateContentTypeCommand;
import codex.codex.api.model.command.AddContentTypeFieldCommand;
import codex.codex.api.model.command.CreateContentItemCommand;
import codex.codex.api.model.command.CreateContentTypeCommand;
import codex.codex.api.model.command.CreateSiteCommand;
import codex.codex.api.model.command.PublishContentItemCommand;
import codex.codex.api.model.entity.ContentItem;
import codex.codex.api.model.entity.Field;
import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.FieldKey;
import codex.codex.api.model.identity.SiteKey;
import codex.codex.api.model.value.FieldType;
import codex.custos.api.exception.AccessDeniedException;
import codex.custos.api.exception.CustosAgentSuperAdminInvariantViolationException;
import codex.custos.api.model.BuiltInRoles;
import codex.custos.api.model.GlobalScope;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.RoleAssignment;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral tests for {@link ConciliumRuntime#secured(Supplier)}.
 *
 * <p>Proves security by behavior — no assertions on concrete secured implementation classes.</p>
 */
class ConciliumRuntimeSecuredTest {

    private static final Actor SUPER_ADMIN_ACTOR = Actor.human(ActorId.of("admin-1"), "Admin");
    private static final Actor UNAUTHORIZED_ACTOR = Actor.human(ActorId.of("anon-1"), "Anon");
    private static final Actor AGENT_ACTOR = Actor.agent("test-bot");

    private static final SiteKey SITE_KEY = SiteKey.of("acme");
    private static final ContentTypeKey CT_KEY = ContentTypeKey.of("article");
    private static final ContentItemKey ITEM_KEY = ContentItemKey.of("first-post");

    private static final PermissionResolutionSnapshot SUPER_ADMIN_SNAPSHOT =
            PermissionResolutionSnapshot.of(
                    List.of(RoleAssignment.of(
                            SUPER_ADMIN_ACTOR, BuiltInRoles.SUPER_ADMIN.key(), GlobalScope.INSTANCE)),
                    Map.of(BuiltInRoles.SUPER_ADMIN.key(), BuiltInRoles.SUPER_ADMIN));

    private static final PermissionResolutionSnapshot EMPTY_SNAPSHOT =
            PermissionResolutionSnapshot.of(List.of(), Map.of());

    private static final PermissionResolutionSnapshot AGENT_SUPER_ADMIN_SNAPSHOT =
            PermissionResolutionSnapshot.of(
                    List.of(RoleAssignment.of(
                            AGENT_ACTOR, BuiltInRoles.SUPER_ADMIN.key(), GlobalScope.INSTANCE)),
                    Map.of(BuiltInRoles.SUPER_ADMIN.key(), BuiltInRoles.SUPER_ADMIN));

    // --- factory ---

    @Nested
    @DisplayName("Factory: secured(snapshotProvider)")
    class Factory {

        @Test
        @DisplayName("returns a non-null runtime")
        void securedRuntimeIsNotNull() {
            assertThat(ConciliumRuntime.secured(() -> SUPER_ADMIN_SNAPSHOT)).isNotNull();
        }

        @Test
        @DisplayName("rejects null snapshotProvider")
        void rejectsNullSnapshotProvider() {
            assertThatThrownBy(() -> ConciliumRuntime.secured(null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("reports SECURED runtime metadata")
        void securedRuntimeReportsSecuredMode() {
            final ConciliumRuntime runtime = ConciliumRuntime.secured(() -> SUPER_ADMIN_SNAPSHOT);
            assertThat(runtime.securityMode())
                    .isEqualTo(RuntimeSecurityMode.SECURED);
        }
    }

    // --- authorization enforcement ---

    @Nested
    @DisplayName("Authorization: deny")
    class AuthorizationDeny {

        @Test
        @DisplayName("Unauthorized actor gets AccessDeniedException on site create")
        void unauthorizedActorCannotCreateSite() {
            final ConciliumRuntime runtime = ConciliumRuntime.secured(() -> EMPTY_SNAPSHOT);
            assertThatThrownBy(() -> runtime.siteService().create(
                    CreateSiteCommand.of(SITE_KEY, "Acme"), UNAUTHORIZED_ACTOR))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Denied site create does not mutate site state")
        void deniedSiteCreateDoesNotMutateSiteState() {
            final ConciliumRuntime runtime = ConciliumRuntime.secured(() -> EMPTY_SNAPSHOT);

            assertThatThrownBy(() -> runtime.siteService().create(
                    CreateSiteCommand.of(SITE_KEY, "Acme"), UNAUTHORIZED_ACTOR))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.coreRuntime().siteService().findAll(UNAUTHORIZED_ACTOR)).isEmpty();
        }

        @Test
        @DisplayName("Denied operation does not emit domain events to the composed pipeline")
        void deniedOperationDoesNotEmitDomainEvents() {
            final ConciliumRuntime runtime = ConciliumRuntime.secured(() -> EMPTY_SNAPSHOT);

            assertThatThrownBy(() -> runtime.siteService().create(
                    CreateSiteCommand.of(SITE_KEY, "Acme"), UNAUTHORIZED_ACTOR))
                    .isInstanceOf(AccessDeniedException.class);

            assertThat(runtime.coreRuntime().recordedEvents()).isEmpty();
        }
    }

    // --- authorized flow ---

    @Nested
    @DisplayName("Authorization: grant")
    class AuthorizationGrant {

        @Test
        @DisplayName("Authorized SUPER_ADMIN can create site, content type, and content item")
        void superAdminCanCreateSiteTypeAndItem() {
            final ConciliumRuntime runtime = ConciliumRuntime.secured(() -> SUPER_ADMIN_SNAPSHOT);

            runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), SUPER_ADMIN_ACTOR);

            runtime.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), SUPER_ADMIN_ACTOR);
            runtime.contentTypeService().addField(
                    AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                            Field.builder()
                                    .key(FieldKey.TITLE)
                                    .displayName("Title")
                                    .type(FieldType.TEXT)
                                    .required(true)
                                    .build()),
                    SUPER_ADMIN_ACTOR);
            runtime.contentTypeService().activate(
                    ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), SUPER_ADMIN_ACTOR);

            final ContentItem item = runtime.contentItemService().create(
                    CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                            Map.of(FieldKey.TITLE, "First Post")),
                    SUPER_ADMIN_ACTOR);

            assertThat(item).isNotNull();
            assertThat(item.key()).isEqualTo(ITEM_KEY);
        }

        @Test
        @DisplayName("Authorized publish flow still reaches Index and Chronicon")
        void authorizedPublishFlowReachesIndexAndChronicon() {
            final ConciliumRuntime runtime = ConciliumRuntime.secured(() -> SUPER_ADMIN_SNAPSHOT);
            setupAndPublish(runtime, SUPER_ADMIN_ACTOR);

            final List<AuditRecord> auditRecords = runtime.chroniconRuntime().repository().findAll();
            assertThat(auditRecords).isNotEmpty();
            assertThat(runtime.coreRuntime().recordedEvents()).isNotEmpty();
        }
    }

    // --- snapshot provider ---

    @Nested
    @DisplayName("Snapshot provider")
    class SnapshotProvider {

        @Test
        @DisplayName("snapshotProvider is called per secured operation, not eagerly at runtime creation")
        void snapshotProviderIsCalledPerOperation() {
            final AtomicInteger callCount = new AtomicInteger(0);
            final Supplier<PermissionResolutionSnapshot> countingProvider = () -> {
                callCount.incrementAndGet();
                return SUPER_ADMIN_SNAPSHOT;
            };

            final ConciliumRuntime runtime = ConciliumRuntime.secured(countingProvider);
            assertThat(callCount.get()).isEqualTo(0);

            runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), SUPER_ADMIN_ACTOR);
            final int countAfterFirstOp = callCount.get();
            assertThat(countAfterFirstOp).isGreaterThanOrEqualTo(1);

            runtime.contentTypeService().create(
                    CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), SUPER_ADMIN_ACTOR);
            assertThat(callCount.get()).isGreaterThan(countAfterFirstOp);
        }
    }

    // --- hard invariants ---

    @Nested
    @DisplayName("Hard invariants")
    class HardInvariants {

        @Test
        @DisplayName("AGENT actor with SUPER_ADMIN throws CustosAgentSuperAdminInvariantViolationException")
        void agentWithSuperAdminThrowsInvariantViolation() {
            final ConciliumRuntime runtime = ConciliumRuntime.secured(() -> AGENT_SUPER_ADMIN_SNAPSHOT);
            assertThatThrownBy(() -> runtime.siteService().create(
                    CreateSiteCommand.of(SITE_KEY, "Acme"), AGENT_ACTOR))
                    .isInstanceOf(CustosAgentSuperAdminInvariantViolationException.class);
        }
    }

    // --- lifecycle ---

    @Nested
    @DisplayName("Lifecycle")
    class Lifecycle {

        @Test
        @DisplayName("ConciliumRuntime.inMemory() still works as unsecured back-compat path")
        void inMemoryRuntimeStillWorksAsUnsecuredPath() {
            final ConciliumRuntime runtime = ConciliumRuntime.inMemory();
            runtime.coreRuntime().siteService().create(
                    CreateSiteCommand.of(SITE_KEY, "Acme"), UNAUTHORIZED_ACTOR);
            assertThat(runtime.chroniconRuntime().repository().findAll()).hasSize(1);
        }

        @Test
        @DisplayName("inMemory() reports UNSECURED runtime metadata")
        void inMemoryRuntimeReportsUnsecuredMode() {
            assertThat(ConciliumRuntime.inMemory().securityMode())
                    .isEqualTo(RuntimeSecurityMode.UNSECURED);
        }

        @Test
        @DisplayName("close() is idempotent on secured runtime")
        void closeIsIdempotentOnSecuredRuntime() {
            final ConciliumRuntime runtime = ConciliumRuntime.secured(() -> SUPER_ADMIN_SNAPSHOT);
            assertThatCode(runtime::close).doesNotThrowAnyException();
            assertThatCode(runtime::close).doesNotThrowAnyException();
        }
    }

    // --- private helpers ---

    private static void setupAndPublish(final ConciliumRuntime runtime, final Actor actor) {
        runtime.siteService().create(CreateSiteCommand.of(SITE_KEY, "Acme"), actor);

        runtime.contentTypeService().create(
                CreateContentTypeCommand.of(SITE_KEY, CT_KEY, "Article"), actor);
        runtime.contentTypeService().addField(
                AddContentTypeFieldCommand.of(SITE_KEY, CT_KEY,
                        Field.builder()
                                .key(FieldKey.TITLE)
                                .displayName("Title")
                                .type(FieldType.TEXT)
                                .required(true)
                                .build()),
                actor);
        runtime.contentTypeService().activate(
                ActivateContentTypeCommand.of(SITE_KEY, CT_KEY), actor);

        runtime.contentItemService().create(
                CreateContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY,
                        Map.of(FieldKey.TITLE, "First Post")),
                actor);
        runtime.contentItemService().publish(
                PublishContentItemCommand.of(SITE_KEY, CT_KEY, ITEM_KEY), actor);
    }
}
