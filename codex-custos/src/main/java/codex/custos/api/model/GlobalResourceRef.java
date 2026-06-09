package codex.custos.api.model;

/**
 * Refers to the global Codex platform scope, not tied to any specific site.
 * Used for platform-wide permissions such as {@code site.create} or {@code permission.grant}.
 */
public record GlobalResourceRef() implements ResourceRef {

    /** Singleton instance — {@code GlobalResourceRef} carries no state. */
    public static final GlobalResourceRef INSTANCE = new GlobalResourceRef();
}
