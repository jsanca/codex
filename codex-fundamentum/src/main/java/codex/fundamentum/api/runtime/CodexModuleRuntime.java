package codex.fundamentum.api.runtime;

import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;

import java.util.List;

/**
 * Contract for a module-level runtime composition unit.
 *
 * <p>A {@code CodexModuleRuntime} knows how to construct its module's internal objects
 * and exposes any event subscribers that the module contributes to the event pipeline.
 * It is intended to be created at the composition edge — not inside domain services,
 * entities, repositories, or subscribers.</p>
 *
 * <p>Typical module runtimes:</p>
 * <ul>
 *   <li>{@code CoreRuntime} — canonical services, repositories, core event pipeline</li>
 *   <li>{@code IndexRuntime} — index writers, mappers, indexing subscribers</li>
 *   <li>{@code ChroniconRuntime} — audit repositories, audit subscribers</li>
 * </ul>
 *
 * <p>A leaf module or assembly layer may compose several module runtimes into a running
 * application. Composition must be explicit — do not use a Service Locator inside domain
 * code to resolve runtime dependencies.</p>
 *
 * <p>Module runtimes implement {@link AutoCloseable} so they can clean up owned resources
 * (executors, index writers, caches, file watchers, etc.) when the application shuts down.</p>
 */
public interface CodexModuleRuntime extends AutoCloseable {

    /**
     * Returns the name identifying this module runtime.
     *
     * @return a non-null, non-blank module name
     */
    String moduleName();

    /**
     * Returns the event subscribers contributed by this module to the event pipeline.
     *
     * <p>Default implementation returns an empty list. Module runtimes that contribute
     * subscribers should override this method.</p>
     *
     * @return immutable list of subscribers; never null
     */
    default List<CodexEventSubscriber<? extends CodexEvent>> subscribers() {
        return List.of();
    }

    /**
     * Releases resources owned by this module runtime. Default implementation is a no-op.
     *
     * <p>Override to shut down executors, close writers, release caches, or stop
     * background workers owned by this module.</p>
     */
    @Override
    default void close() {
    }
}
