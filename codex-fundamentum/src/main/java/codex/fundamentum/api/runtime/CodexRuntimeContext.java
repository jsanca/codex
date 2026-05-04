package codex.fundamentum.api.runtime;

import java.util.Optional;

/**
 * Read-only context used during module runtime construction and assembly.
 *
 * <p>A {@code CodexRuntimeContext} holds the set of components that have been registered
 * at the composition edge. Providers use it to look up cross-module dependencies before
 * constructing their {@link CodexModuleRuntime}.</p>
 *
 * <p><strong>Usage rules (must not be violated):</strong></p>
 * <ul>
 *   <li>Allowed: use this context while constructing module runtimes at the composition edge.</li>
 *   <li>Allowed: use {@link java.util.ServiceLoader} later to discover {@link CodexModuleRuntimeProvider}.</li>
 *   <li>Allowed: pass explicit dependencies into constructors after resolving them at the composition edge.</li>
 *   <li><em>Not allowed</em>: pass this context into domain services, entities, or repositories.</li>
 *   <li><em>Not allowed</em>: call {@link #require(Class)} inside canonical lifecycle logic.</li>
 *   <li><em>Not allowed</em>: store this context in a static or global field.</li>
 *   <li><em>Not allowed</em>: use this as a hidden dependency lookup mechanism (Service Locator).</li>
 * </ul>
 *
 * <p>Violations of these rules introduce hidden dependencies, weaken testability,
 * and blur module boundaries.</p>
 */
public interface CodexRuntimeContext {

    /**
     * Finds a registered component by type.
     *
     * @param <T>  the expected type
     * @param type the component type; must not be null
     * @return the component wrapped in an {@link Optional}, or empty if not registered
     * @throws NullPointerException if {@code type} is null
     */
    <T> Optional<T> find(Class<T> type);

    /**
     * Returns a required component by type, throwing if absent.
     *
     * @param <T>  the expected type
     * @param type the component type; must not be null
     * @return the registered component; never null
     * @throws NullPointerException  if {@code type} is null
     * @throws IllegalStateException if no component is registered for {@code type}
     */
    <T> T require(Class<T> type);
}
