package codex.custos.api.service;

import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.PermissionKey;
import codex.custos.api.model.ResourceRef;
import codex.custos.api.model.SecurityEvaluationContext;
import codex.fundamentum.api.model.Actor;

/**
 * Low-level authorization port.
 * <p>
 * Evaluates a single ({@link Actor}, {@link PermissionKey}, {@link ResourceRef}) triple and
 * returns a self-describing {@link AccessDecision}. Implementations may consult an in-memory
 * grant store, a database, a policy engine, or a combination.
 * <p>
 * Domain services should prefer the expressive higher-level methods on
 * {@link AccessDecisionService} rather than calling this evaluator directly.
 */
public interface PermissionEvaluator {

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
}
