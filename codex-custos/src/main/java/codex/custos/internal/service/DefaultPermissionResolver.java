package codex.custos.internal.service;

import codex.custos.api.exception.CustosAgentSuperAdminInvariantViolationException;
import codex.custos.api.model.BuiltInRoles;
import codex.custos.api.model.PermissionResolution;
import codex.custos.api.model.PermissionResolutionRequest;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.model.ResourceScope;
import codex.custos.api.model.Role;
import codex.custos.api.model.RoleAssignment;
import codex.custos.api.model.PermissionKey;
import codex.custos.api.model.RoleKey;
import codex.custos.api.service.PermissionResolver;
import codex.fundamentum.api.model.Actor;
import codex.fundamentum.api.model.ActorType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;

/**
 * Default in-memory implementation of {@link PermissionResolver}.
 *
 * <p>Evaluation order:
 * <ol>
 *   <li>Hard guard: {@code AGENT} + {@code SUPER_ADMIN} →
 *       {@link CustosAgentSuperAdminInvariantViolationException}</li>
 *   <li>{@code SUPER_ADMIN} bypass for non-agent actors whose assignment covers the target</li>
 *   <li>Scope hierarchy walk: target → parent → … → {@code GlobalScope} with role lookup
 *       and permission implication</li>
 * </ol>
 * @author jsanca/elo/clio
 */
public final class DefaultPermissionResolver implements PermissionResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultPermissionResolver.class);
    private static final RoleKey SUPER_ADMIN_KEY = BuiltInRoles.SUPER_ADMIN.key();

    private final ResourceScopeHierarchy scopeHierarchy;
    private final PermissionImplicationRules implicationRules;

    /** Constructs the resolver with the default scope hierarchy and implication rules. */
    public DefaultPermissionResolver() {
        this(new DefaultResourceScopeHierarchy(), new DefaultPermissionImplicationRules());
    }

    DefaultPermissionResolver(final ResourceScopeHierarchy scopeHierarchy,
                               final PermissionImplicationRules implicationRules) {
        this.scopeHierarchy = Objects.requireNonNull(scopeHierarchy, "scopeHierarchy must not be null");
        this.implicationRules = Objects.requireNonNull(implicationRules, "implicationRules must not be null");
    }

    @Override
    public PermissionResolution resolve(final PermissionResolutionRequest request,
                                        final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");

        final Actor actor = request.actor();

        enforceAgentSuperAdminInvariant(actor, snapshot);

        if (actor.type() != ActorType.AGENT && hasCoveringSuperAdmin(actor, request.target(), snapshot)) {
            return PermissionResolution.granted(actor, request.permission(), request.target(),
                    "SUPER_ADMIN bypass");
        }

        return resolveByScope(request, snapshot);
    }

    private void enforceAgentSuperAdminInvariant(final Actor actor,
                                                 final PermissionResolutionSnapshot snapshot) {
        if (actor.type() != ActorType.AGENT) {
            return;
        }
        final boolean agentHoldsSuperAdmin = snapshot.assignments().stream()
                .filter(roleAssignment -> roleAssignment.actor().equals(actor))
                .anyMatch(roleAssignment -> SUPER_ADMIN_KEY.equals(roleAssignment.role()));
        if (agentHoldsSuperAdmin) {
            LOGGER.warn("Security invariant violation: AGENT actor [{}] holds SUPER_ADMIN",
                    actor.id().value());
            throw new CustosAgentSuperAdminInvariantViolationException(actor);
        }
    }

    private boolean hasCoveringSuperAdmin(final Actor actor, final ResourceScope target,
                                          final PermissionResolutionSnapshot snapshot) {
        return snapshot.assignments().stream()
                .filter(roleAssignment -> roleAssignment.actor().equals(actor))
                .filter(roleAssignment -> SUPER_ADMIN_KEY.equals(roleAssignment.role()))
                .anyMatch(roleAssignment -> scopeHierarchy.covers(roleAssignment.scope(), target));
    }

    private PermissionResolution resolveByScope(final PermissionResolutionRequest request,
                                                final PermissionResolutionSnapshot snapshot) {
        ResourceScope current = request.target();
        while (current != null) {
            final Optional<Role> grantingRole = findGrantingRoleAtScope(request.actor(), request.permission(),
                    current, snapshot);
            if (grantingRole.isPresent()) {
                return PermissionResolution.granted(request.actor(), request.permission(),
                        request.target(),
                        "role [" + grantingRole.get().key().value() + "] at [" + current + "]");
            }
            current = scopeHierarchy.parentOf(current);
        }

        return PermissionResolution.denied(request.actor(), request.permission(), request.target(),
                "no assignment grants [" + request.permission().value() + "] at or above ["
                        + request.target() + "]");
    }

    private Optional<Role> findGrantingRoleAtScope(final Actor actor, final PermissionKey permission,
                                                    final ResourceScope scope,
                                                    final PermissionResolutionSnapshot snapshot) {
        return snapshot.assignments().stream()
                .filter(roleAssignment -> roleAssignment.actor().equals(actor))
                .filter(roleAssignment -> roleAssignment.scope().equals(scope))
                .map(roleAssignment -> snapshot.roleRegistry().get(roleAssignment.role()))
                .filter(Objects::nonNull)
                .filter(role -> implicationRules.permits(role.permissions(), permission))
                .findFirst();
    }
}
