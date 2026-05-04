package codex.fundamentum.api.runtime;

/**
 * Factory contract for creating a {@link CodexModuleRuntime} from a {@link CodexRuntimeContext}.
 *
 * <p>This interface is designed for future discovery via {@link java.util.ServiceLoader}.
 * A module may declare:</p>
 * <pre>
 * provides CodexModuleRuntimeProvider
 *     with codex.index.internal.IndexRuntimeProvider;
 * </pre>
 *
 * <p>An assembly layer may then discover all providers:</p>
 * <pre>
 * ServiceLoader.load(CodexModuleRuntimeProvider.class)
 *     .stream()
 *     .map(ServiceLoader.Provider::get)
 *     .map(p -> p.create(context))
 *     .toList();
 * </pre>
 *
 * <p>This interface is for module runtime construction only — not for resolving arbitrary
 * domain dependencies. Domain services, entities, repositories, and subscribers must
 * receive their dependencies explicitly through constructors.</p>
 *
 * <p>ServiceLoader usage is not implemented in this task.</p>
 */
public interface CodexModuleRuntimeProvider {

    /**
     * Returns the name of the module whose runtime this provider creates.
     *
     * @return a non-null, non-blank module name
     */
    String moduleName();

    /**
     * Creates and returns a fully configured {@link CodexModuleRuntime} for this module.
     *
     * <p>The provider may use the {@link CodexRuntimeContext} to resolve cross-module
     * dependencies that were registered by other modules at the composition edge.
     * The context must not be stored or passed into domain objects.</p>
     *
     * @param context the runtime composition context; must not be null
     * @return a ready-to-use module runtime; never null
     */
    CodexModuleRuntime create(CodexRuntimeContext context);
}
