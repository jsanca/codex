package codex.custos.api.model;

/**
 * Carries request-level context available to {@link codex.custos.api.service.PermissionEvaluator}
 * implementations and authorization policies.
 * <p>
 * The context is intentionally open-ended. Phase 0 provides only an {@link #empty()} baseline.
 * Future implementations may carry tenant context, request timestamps, IP address, or
 * step-up approval tokens.
 */
public interface SecurityEvaluationContext {

    /**
     * Returns a minimal context with no additional metadata.
     * Suitable for internal, system-initiated, or test authorization calls.
     */
    static SecurityEvaluationContext empty() {
        return EmptyContext.INSTANCE;
    }

    /** Minimal no-op implementation. */
    record EmptyContext() implements SecurityEvaluationContext {
        private static final EmptyContext INSTANCE = new EmptyContext();
    }
}
