package codex.custos.internal.service;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.exception.CustosAgentSuperAdminInvariantViolationException;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.AccessDecisionRequest;
import codex.custos.api.model.BuiltInRoles;
import codex.custos.api.model.ContentItemResourceRef;
import codex.custos.api.model.ContentItemScope;
import codex.custos.api.model.GlobalScope;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.Permissions;
import codex.custos.api.model.Role;
import codex.custos.api.model.RoleAssignment;
import codex.custos.api.model.RoleKey;
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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DefaultAccessDecisionServiceTest {

    private DefaultAccessDecisionService service;

    private static final SiteKey SITE_A = SiteKey.of("site-a");
    private static final ContentTypeKey BLOG = ContentTypeKey.of("blog-post");
    private static final ContentItemKey ITEM_1 = ContentItemKey.of("welcome-post");

    private static final Actor ALICE = Actor.human(ActorId.of("user-alice"), "Alice");
    private static final Actor BOT = Actor.agent("indexer");

    private static final SiteResourceRef SITE_A_REF = new SiteResourceRef(SITE_A);
    private static final ContentItemResourceRef ITEM_REF =
            new ContentItemResourceRef(SITE_A, BLOG, ITEM_1);

    @BeforeEach
    void setUp() {
        service = new DefaultAccessDecisionService(new DefaultPermissionResolver());
    }

    // -----------------------------------------------------------------------
    // Null guards
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("null argument rejection")
    class NullGuards {

        @Test
        @DisplayName("null request is rejected")
        void nullRequest() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.evaluate(null, snapshot(List.of(), Map.of())));
        }

        @Test
        @DisplayName("null snapshot is rejected")
        void nullSnapshot() {
            assertThatNullPointerException().isThrownBy(() ->
                    service.evaluate(
                            AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_READ,
                                    SITE_A_REF, new SiteScope(SITE_A)),
                            null));
        }

        @Test
        @DisplayName("null actor in AccessDecisionRequest is rejected at construction")
        void nullActorInRequest() {
            assertThatNullPointerException().isThrownBy(() ->
                    AccessDecisionRequest.of(null, Permissions.CONTENT_ITEM_READ,
                            SITE_A_REF, new SiteScope(SITE_A)));
        }

        @Test
        @DisplayName("null permission in AccessDecisionRequest is rejected at construction")
        void nullPermissionInRequest() {
            assertThatNullPointerException().isThrownBy(() ->
                    AccessDecisionRequest.of(ALICE, null, SITE_A_REF, new SiteScope(SITE_A)));
        }

        @Test
        @DisplayName("null resource in AccessDecisionRequest is rejected at construction")
        void nullResourceInRequest() {
            assertThatNullPointerException().isThrownBy(() ->
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_READ,
                            null, new SiteScope(SITE_A)));
        }

        @Test
        @DisplayName("null targetScope in AccessDecisionRequest is rejected at construction")
        void nullTargetScopeInRequest() {
            assertThatNullPointerException().isThrownBy(() ->
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_READ, SITE_A_REF, null));
        }

        @Test
        @DisplayName("null resolver in constructor is rejected")
        void nullResolverInConstructor() {
            assertThatNullPointerException().isThrownBy(() ->
                    new DefaultAccessDecisionService(null));
        }
    }

    // -----------------------------------------------------------------------
    // Granted resolution → AccessDecision.Granted
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("granted resolution")
    class GrantedResolution {

        @Test
        @DisplayName("returns AccessDecision.Granted when resolution is Granted")
        void grantedDecision() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR)));

            assertThat(decision).isInstanceOf(AccessDecision.Granted.class);
            assertThat(decision.isGranted()).isTrue();
        }

        @Test
        @DisplayName("actor is preserved from request in the decision")
        void actorPreserved() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR)));

            assertThat(decision.actor()).isEqualTo(ALICE);
        }

        @Test
        @DisplayName("permission is preserved from request in the decision")
        void permissionPreserved() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR)));

            assertThat(decision.permission()).isEqualTo(Permissions.CONTENT_ITEM_UPDATE);
        }

        @Test
        @DisplayName("ResourceRef is preserved from request in the decision")
        void resourceRefPreserved() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR)));

            assertThat(decision.resource()).isEqualTo(ITEM_REF);
        }

        @Test
        @DisplayName("reason from PermissionResolution is preserved in the decision")
        void reasonPreserved() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR)));

            assertThat(decision.reason()).isNotBlank();
        }

        @Test
        @DisplayName("SUPER_ADMIN bypass propagates as Granted with SUPER_ADMIN reason")
        void superAdminBypassGranted() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.SUPER_ADMIN.key(), GlobalScope.INSTANCE);
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.SITE_CREATE,
                            SITE_A_REF, GlobalScope.INSTANCE),
                    snapshot(List.of(assignment), registry(BuiltInRoles.SUPER_ADMIN)));

            assertThat(decision).isInstanceOf(AccessDecision.Granted.class);
            assertThat(decision.reason()).containsIgnoringCase("SUPER_ADMIN");
        }
    }

    // -----------------------------------------------------------------------
    // Denied resolution → AccessDecision.Denied
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("denied resolution")
    class DeniedResolution {

        @Test
        @DisplayName("returns AccessDecision.Denied when resolution is Denied")
        void deniedDecision() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.VIEWER.key(), new SiteScope(SITE_A));
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.VIEWER)));

            assertThat(decision).isInstanceOf(AccessDecision.Denied.class);
            assertThat(decision.isGranted()).isFalse();
        }

        @Test
        @DisplayName("actor is preserved in denied decision")
        void actorPreservedOnDeny() {
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(), Map.of()));

            assertThat(decision.actor()).isEqualTo(ALICE);
        }

        @Test
        @DisplayName("permission is preserved in denied decision")
        void permissionPreservedOnDeny() {
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(), Map.of()));

            assertThat(decision.permission()).isEqualTo(Permissions.CONTENT_ITEM_UPDATE);
        }

        @Test
        @DisplayName("ResourceRef is preserved in denied decision")
        void resourceRefPreservedOnDeny() {
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(), Map.of()));

            assertThat(decision.resource()).isEqualTo(ITEM_REF);
        }

        @Test
        @DisplayName("reason is non-blank in denied decision")
        void reasonNonBlankOnDeny() {
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(), Map.of()));

            assertThat(decision.reason()).isNotBlank();
        }
    }

    // -----------------------------------------------------------------------
    // ResourceRef / ResourceScope separation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("ResourceRef and ResourceScope separation")
    class RefScopeSeparation {

        @Test
        @DisplayName("ResourceRef in the decision is the one from the request, not derived from targetScope")
        void resourceRefFromRequest() {
            // The request carries ITEM_REF (ContentItemResourceRef) but the scope is SiteScope.
            // The decision must carry ITEM_REF, not a resource derived from the scope.
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR)));

            assertThat(decision.resource()).isEqualTo(ITEM_REF);
            assertThat(decision.resource()).isInstanceOf(ContentItemResourceRef.class);
        }

        @Test
        @DisplayName("targetScope is used for resolver walk, not preserved in AccessDecision")
        void targetScopeUsedForResolutionOnly() {
            // Assignment at ContentItemScope grants via exact scope match.
            ContentItemScope exactScope = new ContentItemScope(SITE_A, BLOG, ITEM_1);
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), exactScope);

            AccessDecision decision = service.evaluate(
                    AccessDecisionRequest.of(ALICE, Permissions.CONTENT_ITEM_UPDATE,
                            ITEM_REF, exactScope),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR)));

            // Decision is Granted (scope walk worked); decision.resource() is ITEM_REF (not the scope)
            assertThat(decision).isInstanceOf(AccessDecision.Granted.class);
            assertThat(decision.resource()).isEqualTo(ITEM_REF);
        }
    }

    // -----------------------------------------------------------------------
    // AGENT + SUPER_ADMIN hard invariant propagation
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("hard invariant propagation")
    class InvariantPropagation {

        @Test
        @DisplayName("CustosAgentSuperAdminInvariantViolationException propagates from resolver")
        void agentSuperAdminExceptionPropagates() {
            RoleAssignment assignment = RoleAssignment.of(BOT, BuiltInRoles.SUPER_ADMIN.key(), GlobalScope.INSTANCE);
            assertThatExceptionOfType(CustosAgentSuperAdminInvariantViolationException.class)
                    .isThrownBy(() -> service.evaluate(
                            AccessDecisionRequest.of(BOT, Permissions.CONTENT_ITEM_READ,
                                    ITEM_REF, new SiteScope(SITE_A)),
                            snapshot(List.of(assignment), registry(BuiltInRoles.SUPER_ADMIN))))
                    .satisfies(ex -> assertThat(ex.actor()).isEqualTo(BOT));
        }

        @Test
        @DisplayName("invariant exception is not converted to Denied — it propagates as-is")
        void invariantExceptionIsNotDenied() {
            RoleAssignment assignment = RoleAssignment.of(BOT, BuiltInRoles.SUPER_ADMIN.key(), GlobalScope.INSTANCE);
            assertThatExceptionOfType(CustosAgentSuperAdminInvariantViolationException.class)
                    .isThrownBy(() -> service.evaluate(
                            AccessDecisionRequest.of(BOT, Permissions.CONTENT_ITEM_READ,
                                    ITEM_REF, new SiteScope(SITE_A)),
                            snapshot(List.of(assignment), registry(BuiltInRoles.SUPER_ADMIN))))
                    .isNotInstanceOf(codex.custos.api.exception.AccessDeniedException.class);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static PermissionResolutionSnapshot snapshot(
            final java.util.Collection<RoleAssignment> assignments,
            final Map<RoleKey, Role> registry) {
        return PermissionResolutionSnapshot.of(assignments, registry);
    }

    private static Map<RoleKey, Role> registry(final Role... roles) {
        Map<RoleKey, Role> map = new java.util.HashMap<>();
        for (Role role : roles) {
            map.put(role.key(), role);
        }
        return Map.copyOf(map);
    }
}
