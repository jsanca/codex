package codex.custos.api.service;

import codex.custos.api.exception.CustosAgentSuperAdminInvariantViolationException;
import codex.custos.api.model.PermissionResolution;
import codex.custos.api.model.PermissionResolutionRequest;
import codex.custos.api.model.PermissionResolutionSnapshot;

/**
 * Computes whether an actor effectively has a permission at a given resource scope.
 * <p>
 * The resolver accepts a {@link PermissionResolutionRequest} (who wants to do what on which
 * scope) and a {@link PermissionResolutionSnapshot} (available assignments and role blueprints),
 * then returns an explainable {@link PermissionResolution}.
 *
 * <h2>Evaluation order</h2>
 * <ol>
 *   <li><strong>Hard safety rules</strong> — {@code AGENT} actors may never hold {@code SUPER_ADMIN}.
 *       If this invariant is violated, implementations must throw
 *       {@link CustosAgentSuperAdminInvariantViolationException}.</li>
 *   <li><strong>SUPER_ADMIN bypass</strong> — if a non-{@code AGENT} actor holds a
 *       {@code SUPER_ADMIN} assignment whose scope covers the target, return
 *       {@link PermissionResolution.Granted} immediately.</li>
 *   <li><strong>Scope hierarchy walk</strong> — check assignments at the target scope, then walk
 *       upward ({@code ContentItemScope} → {@code ContentTypeScope} → {@code SiteScope} →
 *       {@code GlobalScope}), never crossing site boundaries.</li>
 * </ol>
 *
 * <h2>Implication rules (ADR-009)</h2>
 * <ul>
 *   <li>{@code contentItem.update} implies {@code contentItem.read}</li>
 *   <li>{@code contentItem.publish} implies {@code contentItem.read}</li>
 *   <li>{@code contentItem.publish} does <em>not</em> imply {@code contentItem.update}</li>
 * </ul>
 *
 * <p>This resolver does not mutate assignments or roles, does not access any persistent store,
 * and does not produce transport artefacts.
 */
public interface PermissionResolver {

    /**
     * Resolves whether the actor in {@code request} effectively has the requested permission
     * at the target scope, using the data in {@code snapshot}.
     *
     * @param request  who wants to do what on which scope; must not be null
     * @param snapshot available role assignments and role blueprints; must not be null
     * @return a {@link PermissionResolution.Granted} or {@link PermissionResolution.Denied}
     *         with full details including a human-readable reason
     * @throws CustosAgentSuperAdminInvariantViolationException if an {@code AGENT} actor holds
     *         a {@code SUPER_ADMIN} assignment (hard invariant violation — not a normal denial)
     */
    PermissionResolution resolve(PermissionResolutionRequest request, PermissionResolutionSnapshot snapshot);
}
