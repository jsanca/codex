package codex.custos.internal.service;

import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.AccessDecisionRequest;
import codex.custos.api.model.PermissionResolution;
import codex.custos.api.model.PermissionResolutionRequest;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.service.AccessDecisionService;
import codex.custos.api.service.PermissionResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Default implementation of {@link AccessDecisionService}.
 * <p>
 * Delegates scope-based permission resolution to an injected {@link PermissionResolver},
 * then translates the {@link PermissionResolution} into an {@link AccessDecision} that
 * carries the original {@link codex.custos.api.model.ResourceRef} from the request.
 *
 * <p>{@link codex.custos.api.exception.CustosAgentSuperAdminInvariantViolationException}
 * propagates from the resolver uncaught — callers must treat it as a fatal error.
 */
public final class DefaultAccessDecisionService implements AccessDecisionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultAccessDecisionService.class);

    private final PermissionResolver resolver;

    /**
     * @param resolver the permission resolver to delegate scope evaluation to; must not be null
     */
    public DefaultAccessDecisionService(final PermissionResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    @Override
    public AccessDecision evaluate(final AccessDecisionRequest request,
                                   final PermissionResolutionSnapshot snapshot) {
        Objects.requireNonNull(request, "request must not be null");
        Objects.requireNonNull(snapshot, "snapshot must not be null");

        LOGGER.debug("evaluate: actor=[{}] permission=[{}] targetScope=[{}]",
                request.actor().id().value(), request.permission().value(), request.targetScope());

        final PermissionResolutionRequest resolutionRequest = PermissionResolutionRequest.of(
                request.actor(), request.permission(), request.targetScope());

        final PermissionResolution resolution = resolver.resolve(resolutionRequest, snapshot);

        final AccessDecision decision = switch (resolution) {
            case PermissionResolution.Granted granted ->
                    AccessDecision.granted(request.actor(), request.permission(),
                            request.resource(), granted.reason());
            case PermissionResolution.Denied denied ->
                    AccessDecision.denied(request.actor(), request.permission(),
                            request.resource(), denied.reason());
        };

        LOGGER.debug("evaluate result: actor=[{}] permission=[{}] granted=[{}] reason=[{}]",
                request.actor().id().value(), request.permission().value(),
                decision.isGranted(), decision.reason());

        return decision;
    }
}
