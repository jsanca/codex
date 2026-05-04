package codex.fundamentum.api.runtime;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Simple map-backed, immutable implementation of {@link CodexRuntimeContext}.
 *
 * <p>Intended for manual composition and tests. Not a global registry.
 * Not a dynamic service registry. Instances are immutable after {@link Builder#build()}.</p>
 *
 * <p>Each type may be registered at most once. Attempting to register the same type twice
 * via {@link Builder#put(Class, Object)} throws {@link IllegalStateException} to prevent
 * silent overwrites in assembly code.</p>
 *
 * <p>Example usage at the composition edge:</p>
 * <pre>
 * final CodexRuntimeContext context = MapBackedCodexRuntimeContext.builder()
 *         .put(ContentItemProjectionReader.class, core.contentItemProjectionReader())
 *         .build();
 *
 * final IndexRuntime index = indexProvider.create(context);
 * </pre>
 */
public final class MapBackedCodexRuntimeContext implements CodexRuntimeContext {

    private final Map<Class<?>, Object> registry;

    private MapBackedCodexRuntimeContext(final Map<Class<?>, Object> registry) {
        this.registry = Map.copyOf(registry);
    }

    /**
     * Returns an empty context with no registered components.
     *
     * @return an empty {@code MapBackedCodexRuntimeContext}
     */
    public static MapBackedCodexRuntimeContext empty() {
        return new MapBackedCodexRuntimeContext(Map.of());
    }

    /**
     * Returns a new {@link Builder} for constructing a context.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public <T> Optional<T> find(final Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        return Optional.ofNullable(type.cast(registry.get(type)));
    }

    @Override
    public <T> T require(final Class<T> type) {
        Objects.requireNonNull(type, "type must not be null");
        final Object value = registry.get(type);
        if (value == null) {
            throw new IllegalStateException(
                    "No runtime component registered for type: " + type.getName());
        }
        return type.cast(value);
    }

    /**
     * Builder for {@link MapBackedCodexRuntimeContext}.
     *
     * <p>Each type may be registered at most once. Duplicate registrations are rejected.</p>
     */
    public static final class Builder {

        private final Map<Class<?>, Object> entries = new LinkedHashMap<>();

        private Builder() {}

        /**
         * Registers a component instance by its type.
         *
         * @param <T>      the component type
         * @param type     the class key; must not be null
         * @param instance the component instance; must not be null
         * @return this builder
         * @throws NullPointerException  if {@code type} or {@code instance} is null
         * @throws IllegalStateException if {@code type} is already registered
         */
        public <T> Builder put(final Class<T> type, final T instance) {
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(instance, "instance must not be null");
            if (entries.containsKey(type)) {
                throw new IllegalStateException(
                        "Runtime component already registered for type: " + type.getName());
            }
            entries.put(type, instance);
            return this;
        }

        /**
         * Builds an immutable {@link MapBackedCodexRuntimeContext} from the registered entries.
         *
         * @return a new context; never null
         */
        public MapBackedCodexRuntimeContext build() {
            return new MapBackedCodexRuntimeContext(entries);
        }
    }
}
