package codex.custos.api.service;

import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.PermissionKey;
import codex.custos.api.model.ResourceRef;
import codex.custos.api.model.SecurityEvaluationContext;
import codex.fundamentum.api.model.Actor;

/**
 * Application-level authorization service.
 * <p>
 * Wraps {@link PermissionEvaluator} with logging, policy enforcement, hardcoded actor-type
 * restrictions (e.g., agents cannot manage permissions), and observability. Domain code should
 * call this service rather than {@code PermissionEvaluator} directly.
 * <p>
 * Phase 2 will introduce resource-specific sub-services ({@code SitePermissionsService},
 * {@code ContentItemPermissionsService}) that delegate to this service for more expressive
 * authorization calls.
 */
public interface AccessDecisionService {

    /**
     * Evaluates whether the given actor may perform the specified permission on the given resource.
     *
     * @param actor      the actor requesting the operation
     * @param permission the domain permission being checked
     * @param resource   the resource the operation targets
     * @param context    the request-level evaluation context
     * @return a {@link AccessDecision.Granted} or {@link AccessDecision.Denied} with full details
     */
    AccessDecision evaluate(Actor actor, PermissionKey permission, ResourceRef resource,
                            SecurityEvaluationContext context);

    /**
     * Convenience overload using an {@link SecurityEvaluationContext#empty() empty context}.
     *
     * @param actor      the actor requesting the operation
     * @param permission the domain permission being checked
     * @param resource   the resource the operation targets
     * @return a {@link AccessDecision.Granted} or {@link AccessDecision.Denied} with full details
     */
    default AccessDecision evaluate(final Actor actor, final PermissionKey permission, final ResourceRef resource) {
        return evaluate(actor, permission, resource, SecurityEvaluationContext.empty());
    }
}
