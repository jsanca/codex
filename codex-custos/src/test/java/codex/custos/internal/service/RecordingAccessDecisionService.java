package codex.custos.internal.service;

import codex.custos.api.model.AccessDecision;
import codex.custos.api.model.AccessDecisionRequest;
import codex.custos.api.model.PermissionResolutionSnapshot;
import codex.custos.api.service.AccessDecisionService;

/**
 * Test double for {@link AccessDecisionService}.
 * <p>
 * Captures the last request and snapshot passed to {@link #evaluate} and returns a
 * pre-configured {@link AccessDecision}. Used to verify that domain-specific permission
 * services build and delegate the correct request without re-testing resolver logic.
 */
final class RecordingAccessDecisionService implements AccessDecisionService {

    private final AccessDecision stubbedDecision;
    private AccessDecisionRequest lastRequest;
    private PermissionResolutionSnapshot lastSnapshot;

    RecordingAccessDecisionService(final AccessDecision stubbedDecision) {
        this.stubbedDecision = stubbedDecision;
    }

    @Override
    public AccessDecision evaluate(final AccessDecisionRequest request,
                                   final PermissionResolutionSnapshot snapshot) {
        this.lastRequest = request;
        this.lastSnapshot = snapshot;
        return stubbedDecision;
    }

    AccessDecisionRequest lastRequest() {
        return lastRequest;
    }

    PermissionResolutionSnapshot lastSnapshot() {
        return lastSnapshot;
    }
}
