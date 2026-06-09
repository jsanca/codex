package codex.custos.api.model;

/**
 * Assigns a permission or role at the platform-wide global scope.
 * A grant at this scope applies across all sites unless overridden by site-specific policy.
 */
public record GlobalScope() implements ResourceScope {

    /** Singleton instance — {@code GlobalScope} carries no state. */
    public static final GlobalScope INSTANCE = new GlobalScope();
}
