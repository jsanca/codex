package codex.custos.api.service;

import codex.custos.api.exception.CustosAgentSuperAdminInvariantViolationException;
import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.AccessDecisionRequest;
import codex.custos.api.model.PermissionResolutionSnapshot;

/**
 * Application-level authorization service.
 * <p>
 * Evaluates an {@link AccessDecisionRequest} against a {@link PermissionResolutionSnapshot}
 * and returns an explainable {@link AccessDecision}. Internally delegates to
 * {@link PermissionResolver} for scoped permission resolution and translates the result
 * into an {@code AccessDecision} that carries the original {@link codex.custos.api.model.ResourceRef}.
 *
 * <h2>ResourceRef vs ResourceScope</h2>
 * {@link AccessDecision} carries a {@link codex.custos.api.model.ResourceRef} — the concrete
 * resource the decision is about. {@link PermissionResolver} operates on
 * {@link codex.custos.api.model.ResourceScope} — the scope level for the permission walk.
 * Both are present in {@link AccessDecisionRequest}. This distinction is intentional and explicit:
 * future implementations may use the {@code ResourceRef} to load resource data while the
 * {@code ResourceScope} drives the hierarchy walk.
 *
 * <h2>Hard invariants</h2>
 * {@link CustosAgentSuperAdminInvariantViolationException} propagates uncaught. It signals
 * corrupted security state, not a normal denial, and must not be swallowed.
 *
 * <p>Phase 2 will introduce resource-specific sub-services ({@code SitePermissionsService},
 * {@code ContentItemPermissionsService}) that delegate to this service.
 */
public interface AccessDecisionService {

    /**
     * Evaluates whether the actor in {@code request} may perform the requested permission
     * on the given resource, using the role assignments and blueprints in {@code snapshot}.
     *
     * @param request  the actor, permission, resource, and target scope; must not be null
     * @param snapshot available role assignments and role registry; must not be null
     * @return {@link AccessDecision.Granted} or {@link AccessDecision.Denied} with full details
     * @throws CustosAgentSuperAdminInvariantViolationException if an {@code AGENT} actor holds
     *         {@code SUPER_ADMIN} (hard invariant — not a normal denial)
     */
    AccessDecision evaluate(AccessDecisionRequest request, PermissionResolutionSnapshot snapshot);
}
