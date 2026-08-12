package codex.concilium.api.runtime;

/**
 * Indicates whether a {@link ConciliumRuntime} enforces Custos domain authorization.
 *
 * <p>This is diagnostic metadata only — security is enforced by the service graph
 * (secured vs. unsecured decorators), not by this value alone.</p>
 */
public enum RuntimeSecurityMode {
    /** Services enforce Custos authorization on every domain operation. */
    SECURED,
    /** Services have no authorization enforcement; suitable for tests and back-compat paths. */
    UNSECURED
}
