package codex.custos.internal.service;

import codex.codex.api.model.identity.ContentItemKey;
import codex.codex.api.model.identity.ContentTypeKey;
import codex.codex.api.model.identity.SiteKey;
import codex.custos.api.exception.CustosAgentSuperAdminInvariantViolationException;
import codex.custos.api.model.BuiltInRoles;
import codex.custos.api.model.ContentItemScope;
import codex.custos.api.model.ContentTypeScope;
import codex.custos.api.model.GlobalScope;
import codex.custos.api.model.PermissionResolution;
import codex.custos.api.model.PermissionResolutionRequest;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.Permissions;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

class DefaultPermissionResolverTest {

    private DefaultPermissionResolver resolver;

    private static final SiteKey SITE_A = SiteKey.of("site-a");
    private static final SiteKey SITE_B = SiteKey.of("site-b");
    private static final ContentTypeKey BLOG = ContentTypeKey.of("blog-post");
    private static final ContentTypeKey NEWS = ContentTypeKey.of("news-item");
    private static final ContentItemKey ITEM_1 = ContentItemKey.of("welcome-post");

    private static final Actor ALICE = Actor.human(ActorId.of("user-alice"), "Alice");
    private static final Actor BOB = Actor.human(ActorId.of("user-bob"), "Bob");
    private static final Actor BOT = Actor.agent("search-bot");

    @BeforeEach
    void setUp() {
        resolver = new DefaultPermissionResolver();
    }

    // -----------------------------------------------------------------------
    // Null argument guards
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("null argument rejection")
    class NullGuards {

        @Test
        @DisplayName("null request is rejected")
        void nullRequest() {
            PermissionResolutionSnapshot snap = snapshot(List.of(), Map.of());
            assertThatNullPointerException().isThrownBy(() -> resolver.resolve(null, snap));
        }

        @Test
        @DisplayName("null snapshot is rejected")
        void nullSnapshot() {
            PermissionResolutionRequest req = request(ALICE, Permissions.CONTENT_ITEM_READ, GlobalScope.INSTANCE);
            assertThatNullPointerException().isThrownBy(() -> resolver.resolve(req, null));
        }

        @Test
        @DisplayName("null actor in request is rejected at request construction")
        void nullActorInRequest() {
            assertThatNullPointerException().isThrownBy(() ->
                    PermissionResolutionRequest.of(null, Permissions.CONTENT_ITEM_READ, GlobalScope.INSTANCE));
        }

        @Test
        @DisplayName("null permission in request is rejected at request construction")
        void nullPermissionInRequest() {
            assertThatNullPointerException().isThrownBy(() ->
                    PermissionResolutionRequest.of(ALICE, null, GlobalScope.INSTANCE));
        }

        @Test
        @DisplayName("null target in request is rejected at request construction")
        void nullTargetInRequest() {
            assertThatNullPointerException().isThrownBy(() ->
                    PermissionResolutionRequest.of(ALICE, Permissions.CONTENT_ITEM_READ, null));
        }

        @Test
        @DisplayName("null assignments collection in snapshot is rejected")
        void nullAssignmentsInSnapshot() {
            assertThatNullPointerException().isThrownBy(() ->
                    PermissionResolutionSnapshot.of(null, Map.of()));
        }

        @Test
        @DisplayName("null role registry in snapshot is rejected")
        void nullRegistryInSnapshot() {
            assertThatNullPointerException().isThrownBy(() ->
                    PermissionResolutionSnapshot.of(List.of(), null));
        }

        @Test
        @DisplayName("null element in assignments collection is rejected at snapshot construction")
        void nullElementInAssignments() {
            List<RoleAssignment> withNull = new java.util.ArrayList<>();
            withNull.add(null);
            assertThatNullPointerException().isThrownBy(() ->
                    PermissionResolutionSnapshot.of(withNull, Map.of()));
        }
    }

    // -----------------------------------------------------------------------
    // Hard invariant: AGENT must never hold SUPER_ADMIN
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("AGENT + SUPER_ADMIN invariant")
    class AgentSuperAdminInvariant {

        @Test
        @DisplayName("AGENT with SUPER_ADMIN at GlobalScope throws CustosAgentSuperAdminInvariantViolationException")
        void agentGlobalSuperAdmin() {
            RoleAssignment assignment = RoleAssignment.of(BOT, SUPER_ADMIN_KEY, GlobalScope.INSTANCE);
            assertThatExceptionOfType(CustosAgentSuperAdminInvariantViolationException.class)
                    .isThrownBy(() -> resolver.resolve(
                            request(BOT, Permissions.CONTENT_ITEM_READ, GlobalScope.INSTANCE),
                            snapshot(List.of(assignment), registry(BuiltInRoles.SUPER_ADMIN))))
                    .withMessageContaining("AGENT")
                    .withMessageContaining("SUPER_ADMIN")
                    .satisfies(ex -> assertThat(ex.actor()).isEqualTo(BOT));
        }

        @Test
        @DisplayName("exception carries the offending agent actor")
        void exceptionCarriesActor() {
            RoleAssignment assignment = RoleAssignment.of(BOT, SUPER_ADMIN_KEY, new SiteScope(SITE_A));
            assertThatExceptionOfType(CustosAgentSuperAdminInvariantViolationException.class)
                    .isThrownBy(() -> resolver.resolve(
                            request(BOT, Permissions.SITE_READ, new SiteScope(SITE_A)),
                            snapshot(List.of(assignment), registry(BuiltInRoles.SUPER_ADMIN))))
                    .satisfies(ex -> {
                        assertThat(ex.actor()).isEqualTo(BOT);
                        assertThat(ex.actor().id()).isEqualTo(BOT.id());
                    });
        }

        @Test
        @DisplayName("AGENT without SUPER_ADMIN resolves normally and returns Denied when no match")
        void agentWithoutSuperAdminResolvesDenied() {
            PermissionResolution result = resolver.resolve(
                    request(BOT, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_A)),
                    snapshot(List.of(), Map.of()));
            assertThat(result).isInstanceOf(PermissionResolution.Denied.class);
        }

        @Test
        @DisplayName("AGENT with VIEWER role resolves Granted")
        void agentWithViewerResolves() {
            RoleAssignment assignment = RoleAssignment.of(BOT, BuiltInRoles.VIEWER.key(), new SiteScope(SITE_A));
            PermissionResolution result = resolver.resolve(
                    request(BOT, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.VIEWER)));
            assertThat(result).isInstanceOf(PermissionResolution.Granted.class);
        }

        @Test
        @DisplayName("hard guard only fires for the requested actor, not others in the snapshot")
        void hardGuardChecksOnlyRequestedActor() {
            RoleAssignment aliceAssignment = RoleAssignment.of(ALICE, SUPER_ADMIN_KEY, GlobalScope.INSTANCE);
            RoleAssignment botAssignment = RoleAssignment.of(BOT, BuiltInRoles.VIEWER.key(), new SiteScope(SITE_A));

            PermissionResolution result = resolver.resolve(
                    request(BOT, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_A)),
                    snapshot(List.of(aliceAssignment, botAssignment),
                            registry(BuiltInRoles.SUPER_ADMIN, BuiltInRoles.VIEWER)));
            assertThat(result).isInstanceOf(PermissionResolution.Granted.class);
        }
    }

    // -----------------------------------------------------------------------
    // SUPER_ADMIN bypass
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("SUPER_ADMIN bypass")
    class SuperAdminBypass {

        @Test
        @DisplayName("SUPER_ADMIN at GlobalScope returns Granted for any target")
        void globalSuperAdminGrantsAll() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, SUPER_ADMIN_KEY, GlobalScope.INSTANCE);
            PermissionResolutionSnapshot snap = snapshot(List.of(assignment), registry(BuiltInRoles.SUPER_ADMIN));

            assertGranted(resolver.resolve(request(ALICE, Permissions.SITE_CREATE, GlobalScope.INSTANCE), snap));
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_PUBLISH,
                            new ContentItemScope(SITE_A, BLOG, ITEM_1)), snap));
        }

        @Test
        @DisplayName("SUPER_ADMIN bypass reason is non-blank")
        void superAdminBypassReasonIsNonBlank() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, SUPER_ADMIN_KEY, GlobalScope.INSTANCE);
            PermissionResolution result = resolver.resolve(
                    request(ALICE, Permissions.SITE_CREATE, GlobalScope.INSTANCE),
                    snapshot(List.of(assignment), registry(BuiltInRoles.SUPER_ADMIN)));
            assertThat(result.reason()).isNotBlank();
        }

        @Test
        @DisplayName("SUPER_ADMIN at SiteScope bypasses for resources within that site")
        void siteSuperAdminBypassesWithinSite() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, SUPER_ADMIN_KEY, new SiteScope(SITE_A));
            PermissionResolutionSnapshot snap = snapshot(List.of(assignment), registry(BuiltInRoles.SUPER_ADMIN));

            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_PUBLISH, new ContentTypeScope(SITE_A, BLOG)), snap));
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_PUBLISH,
                            new ContentItemScope(SITE_A, BLOG, ITEM_1)), snap));
        }

        @Test
        @DisplayName("SUPER_ADMIN at SiteScope does NOT bypass for a different site")
        void siteSuperAdminDoesNotCrossToOtherSite() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, SUPER_ADMIN_KEY, new SiteScope(SITE_A));
            PermissionResolution result = resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_B)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.SUPER_ADMIN)));
            assertDenied(result);
        }

        @Test
        @DisplayName("SUPER_ADMIN at SiteScope does NOT bypass a GlobalScope target")
        void siteSuperAdminDoesNotCoverGlobal() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, SUPER_ADMIN_KEY, new SiteScope(SITE_A));
            PermissionResolution result = resolver.resolve(
                    request(ALICE, Permissions.SITE_CREATE, GlobalScope.INSTANCE),
                    snapshot(List.of(assignment), registry(BuiltInRoles.SUPER_ADMIN)));
            assertDenied(result);
        }

        @Test
        @DisplayName("SUPER_ADMIN bypass fires on key match even when registry is empty")
        void superAdminBypassDoesNotNeedRegistry() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, SUPER_ADMIN_KEY, GlobalScope.INSTANCE);
            PermissionResolution result = resolver.resolve(
                    request(ALICE, Permissions.SITE_CREATE, GlobalScope.INSTANCE),
                    snapshot(List.of(assignment), Map.of()));
            assertGranted(result);
        }
    }

    // -----------------------------------------------------------------------
    // Exact scope match
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("exact scope match")
    class ExactScopeMatch {

        @Test
        @DisplayName("role at target scope returns Granted with non-blank reason")
        void directGrantAtTargetScope() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            PermissionResolution result = resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR)));
            assertGranted(result);
            assertThat(result.reason()).isNotBlank();
        }

        @Test
        @DisplayName("permission not in role returns Denied with non-blank reason")
        void permissionNotInRole() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.VIEWER.key(), new SiteScope(SITE_A));
            PermissionResolution result = resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.VIEWER)));
            assertDenied(result);
            assertThat(result.reason()).isNotBlank();
        }

        @Test
        @DisplayName("no assignments returns Denied")
        void noAssignments() {
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_A)),
                    snapshot(List.of(), Map.of())));
        }

        @Test
        @DisplayName("assignment for a different actor is not used")
        void differentActorAssignmentIgnored() {
            RoleAssignment bobAssignment = RoleAssignment.of(BOB, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new SiteScope(SITE_A)),
                    snapshot(List.of(bobAssignment), registry(BuiltInRoles.EDITOR))));
        }

        @Test
        @DisplayName("assignment at a different scope is not used for exact match")
        void differentScopeNotUsedForExactMatch() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_B));
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR))));
        }
    }

    // -----------------------------------------------------------------------
    // Scope hierarchy walk
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("scope hierarchy walk")
    class ScopeHierarchyWalk {

        @Test
        @DisplayName("ContentItemScope falls through to ContentTypeScope")
        void contentItemFallsToContentType() {
            ContentTypeScope typeScope = new ContentTypeScope(SITE_A, BLOG);
            ContentItemScope itemTarget = new ContentItemScope(SITE_A, BLOG, ITEM_1);
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), typeScope);
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, itemTarget),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR))));
        }

        @Test
        @DisplayName("ContentItemScope falls through to SiteScope")
        void contentItemFallsToSite() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_PUBLISH, new ContentItemScope(SITE_A, BLOG, ITEM_1)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR))));
        }

        @Test
        @DisplayName("ContentItemScope falls through to GlobalScope")
        void contentItemFallsToGlobal() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), GlobalScope.INSTANCE);
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new ContentItemScope(SITE_A, BLOG, ITEM_1)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR))));
        }

        @Test
        @DisplayName("ContentTypeScope falls through to SiteScope")
        void contentTypeFallsToSite() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.COPYWRITER.key(), new SiteScope(SITE_A));
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_CREATE, new ContentTypeScope(SITE_A, BLOG)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.COPYWRITER))));
        }

        @Test
        @DisplayName("SiteScope falls through to GlobalScope")
        void siteFallsToGlobal() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.REVIEWER.key(), GlobalScope.INSTANCE);
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_PUBLISH, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.REVIEWER))));
        }

        @Test
        @DisplayName("reason includes the role key and the granting scope")
        void reasonIncludesRoleAndScope() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            PermissionResolution result = resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new ContentItemScope(SITE_A, BLOG, ITEM_1)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR)));
            assertGranted(result);
            assertThat(result.reason()).containsIgnoringCase("EDITOR");
        }

        @Test
        @DisplayName("hierarchy walk stops at the first granting scope")
        void walkStopsAtFirstMatch() {
            ContentItemScope itemScope = new ContentItemScope(SITE_A, BLOG, ITEM_1);
            RoleAssignment itemAssignment = RoleAssignment.of(ALICE, BuiltInRoles.VIEWER.key(), itemScope);
            RoleAssignment siteAssignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            PermissionResolutionSnapshot snap = snapshot(
                    List.of(itemAssignment, siteAssignment),
                    registry(BuiltInRoles.VIEWER, BuiltInRoles.EDITOR));

            // read granted at item level (VIEWER)
            assertGranted(resolver.resolve(request(ALICE, Permissions.CONTENT_ITEM_READ, itemScope), snap));
            // update not in VIEWER; walk continues to site level where EDITOR has it
            assertGranted(resolver.resolve(request(ALICE, Permissions.CONTENT_ITEM_UPDATE, itemScope), snap));
        }
    }

    // -----------------------------------------------------------------------
    // Site boundary enforcement
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("site boundary enforcement")
    class SiteBoundary {

        @Test
        @DisplayName("assignment in site-a scope does not apply to resource in site-b")
        void siteADoesNotApplyToSiteB() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_A));
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new SiteScope(SITE_B)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR))));
        }

        @Test
        @DisplayName("ContentItemScope in site-a ignores ContentTypeScope from site-b")
        void contentItemInSiteAIgnoresTypeScopeFromSiteB() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(),
                    new ContentTypeScope(SITE_B, BLOG));
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new ContentItemScope(SITE_A, BLOG, ITEM_1)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR))));
        }

        @Test
        @DisplayName("ContentItemScope in site-a ignores SiteScope from site-b")
        void contentItemInSiteAIgnoresSiteScopeFromSiteB() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), new SiteScope(SITE_B));
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new ContentItemScope(SITE_A, BLOG, ITEM_1)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR))));
        }

        @Test
        @DisplayName("ContentItemScope for blog ignores ContentTypeScope for news in same site")
        void contentItemDoesNotPickUpDifferentContentTypeScope() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(),
                    new ContentTypeScope(SITE_A, NEWS));
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new ContentItemScope(SITE_A, BLOG, ITEM_1)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR))));
        }

        @Test
        @DisplayName("GlobalScope assignment applies across all sites")
        void globalScopeAppliesToAllSites() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.EDITOR.key(), GlobalScope.INSTANCE);
            PermissionResolutionSnapshot snap = snapshot(List.of(assignment), registry(BuiltInRoles.EDITOR));

            assertGranted(resolver.resolve(request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new SiteScope(SITE_A)), snap));
            assertGranted(resolver.resolve(request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new SiteScope(SITE_B)), snap));
        }
    }

    // -----------------------------------------------------------------------
    // Permission implication rules (ADR-009)
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("permission implication (ADR-009)")
    class PermissionImplication {

        @Test
        @DisplayName("contentItem.update implies contentItem.read")
        void updateImpliesRead() {
            Role updateOnly = Role.of(RoleKey.of("UPDATE-ONLY"), Permissions.CONTENT_ITEM_UPDATE);
            RoleAssignment assignment = RoleAssignment.of(ALICE, updateOnly.key(), new SiteScope(SITE_A));
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), Map.of(updateOnly.key(), updateOnly))));
        }

        @Test
        @DisplayName("contentItem.publish implies contentItem.read")
        void publishImpliesRead() {
            Role publishOnly = Role.of(RoleKey.of("PUBLISH-ONLY"), Permissions.CONTENT_ITEM_PUBLISH);
            RoleAssignment assignment = RoleAssignment.of(ALICE, publishOnly.key(), new SiteScope(SITE_A));
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), Map.of(publishOnly.key(), publishOnly))));
        }

        @Test
        @DisplayName("contentItem.publish does NOT imply contentItem.update")
        void publishDoesNotImplyUpdate() {
            Role publishOnly = Role.of(RoleKey.of("PUBLISH-ONLY"), Permissions.CONTENT_ITEM_PUBLISH);
            RoleAssignment assignment = RoleAssignment.of(ALICE, publishOnly.key(), new SiteScope(SITE_A));
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), Map.of(publishOnly.key(), publishOnly))));
        }

        @Test
        @DisplayName("contentItem.update does NOT imply contentItem.publish")
        void updateDoesNotImplyPublish() {
            Role updateOnly = Role.of(RoleKey.of("UPDATE-ONLY"), Permissions.CONTENT_ITEM_UPDATE);
            RoleAssignment assignment = RoleAssignment.of(ALICE, updateOnly.key(), new SiteScope(SITE_A));
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_PUBLISH, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), Map.of(updateOnly.key(), updateOnly))));
        }

        @Test
        @DisplayName("implication does not apply to non-contentItem.read permissions")
        void implicationOnlyForContentItemRead() {
            Role publishOnly = Role.of(RoleKey.of("PUBLISH-ONLY"), Permissions.CONTENT_ITEM_PUBLISH);
            RoleAssignment assignment = RoleAssignment.of(ALICE, publishOnly.key(), GlobalScope.INSTANCE);
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.SITE_READ, GlobalScope.INSTANCE),
                    snapshot(List.of(assignment), Map.of(publishOnly.key(), publishOnly))));
        }
    }

    // -----------------------------------------------------------------------
    // Registry miss
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("unknown role key handling")
    class RegistryMiss {

        @Test
        @DisplayName("assignment with key absent from registry is silently skipped")
        void unknownRoleKeySkipped() {
            RoleKey unknownKey = RoleKey.of("PHANTOM-ROLE");
            RoleAssignment assignment = RoleAssignment.of(ALICE, unknownKey, new SiteScope(SITE_A));
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), Map.of())));
        }

        @Test
        @DisplayName("valid and unknown assignments coexist; valid one still grants")
        void validAndUnknownCoexist() {
            RoleAssignment phantom = RoleAssignment.of(ALICE, RoleKey.of("PHANTOM-ROLE"), new SiteScope(SITE_A));
            RoleAssignment viewer = RoleAssignment.of(ALICE, BuiltInRoles.VIEWER.key(), new SiteScope(SITE_A));
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_A)),
                    snapshot(List.of(phantom, viewer), registry(BuiltInRoles.VIEWER))));
        }
    }

    // -----------------------------------------------------------------------
    // Built-in role spot checks
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("built-in role behaviour spot checks")
    class BuiltInRoleSpotChecks {

        @Test
        @DisplayName("VIEWER grants only contentItem.read")
        void viewerGrantsRead() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.VIEWER.key(), new SiteScope(SITE_A));
            PermissionResolutionSnapshot snap = snapshot(List.of(assignment), registry(BuiltInRoles.VIEWER));

            assertGranted(resolver.resolve(request(ALICE, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_A)), snap));
            assertDenied(resolver.resolve(request(ALICE, Permissions.CONTENT_ITEM_UPDATE, new SiteScope(SITE_A)), snap));
        }

        @Test
        @DisplayName("REVIEWER read is implied by publish")
        void reviewerGrantsReadViaPublishImplication() {
            Role reviewerWithoutRead = Role.of(RoleKey.of("REVIEWER-NO-READ"),
                    Permissions.CONTENT_ITEM_PUBLISH, Permissions.CONTENT_ITEM_UNPUBLISH);
            RoleAssignment assignment = RoleAssignment.of(ALICE, reviewerWithoutRead.key(), new SiteScope(SITE_A));
            assertGranted(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_READ, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), Map.of(reviewerWithoutRead.key(), reviewerWithoutRead))));
        }

        @Test
        @DisplayName("COPYWRITER cannot publish")
        void copywriterCannotPublish() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.COPYWRITER.key(), new SiteScope(SITE_A));
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.CONTENT_ITEM_PUBLISH, new SiteScope(SITE_A)),
                    snapshot(List.of(assignment), registry(BuiltInRoles.COPYWRITER))));
        }

        @Test
        @DisplayName("SITE_ADMIN cannot create sites")
        void siteAdminCannotCreateSite() {
            RoleAssignment assignment = RoleAssignment.of(ALICE, BuiltInRoles.SITE_ADMIN.key(), GlobalScope.INSTANCE);
            assertDenied(resolver.resolve(
                    request(ALICE, Permissions.SITE_CREATE, GlobalScope.INSTANCE),
                    snapshot(List.of(assignment), registry(BuiltInRoles.SITE_ADMIN))));
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private static final RoleKey SUPER_ADMIN_KEY = BuiltInRoles.SUPER_ADMIN.key();

    private static PermissionResolutionRequest request(final Actor actor,
                                                       final codex.custos.api.model.PermissionKey permission,
                                                       final codex.custos.api.model.ResourceScope target) {
        return PermissionResolutionRequest.of(actor, permission, target);
    }

    private static PermissionResolutionSnapshot snapshot(final java.util.Collection<RoleAssignment> assignments,
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

    private static void assertGranted(final PermissionResolution resolution) {
        assertThat(resolution).isInstanceOf(PermissionResolution.Granted.class);
        assertThat(resolution.isGranted()).isTrue();
        assertThat(resolution.reason()).isNotBlank();
    }

    private static void assertDenied(final PermissionResolution resolution) {
        assertThat(resolution).isInstanceOf(PermissionResolution.Denied.class);
        assertThat(resolution.isGranted()).isFalse();
        assertThat(resolution.reason()).isNotBlank();
    }
}
