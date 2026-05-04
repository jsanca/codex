package codex.fundamentum.api.runtime;

import codex.fundamentum.api.event.CodexEvent;
import codex.fundamentum.api.event.CodexEventSubscriber;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link CodexModuleRuntime} defaults and {@link CodexModuleRuntimeProvider}.
 */
class CodexModuleRuntimeTest {

    // --- CodexModuleRuntime ---

    @Test
    void defaultSubscribersReturnsEmptyList() {
        final CodexModuleRuntime runtime = new MinimalTestRuntime("test-module");
        assertEquals(List.of(), runtime.subscribers());
    }

    @Test
    void defaultCloseDoesNotThrow() {
        final CodexModuleRuntime runtime = new MinimalTestRuntime("test-module");
        assertDoesNotThrow(runtime::close);
    }

    @Test
    void moduleNameIsReturned() {
        final CodexModuleRuntime runtime = new MinimalTestRuntime("my-module");
        assertEquals("my-module", runtime.moduleName());
    }

    @Test
    void subscribersOverrideReturnsConfiguredList() {
        final CodexEventSubscriber<TestEvent> sub = new TestEventSubscriber();
        final CodexModuleRuntime runtime = new RuntimeWithSubscriber("module-with-sub", sub);
        assertEquals(1, runtime.subscribers().size());
        assertEquals(sub, runtime.subscribers().get(0));
    }

    @Test
    void runtimeIsAutoCloseable() {
        // verifies the interface contract compiles and works with try-with-resources
        assertDoesNotThrow(() -> {
            try (final CodexModuleRuntime runtime = new MinimalTestRuntime("closeable-test")) {
                assertEquals("closeable-test", runtime.moduleName());
            }
        });
    }

    // --- CodexModuleRuntimeProvider ---

    @Test
    void providerReturnsModuleName() {
        final CodexModuleRuntimeProvider provider = new TestRuntimeProvider();
        assertEquals("test-provider-module", provider.moduleName());
    }

    @Test
    void providerCreatesRuntimeFromContext() {
        final CodexModuleRuntimeProvider provider = new TestRuntimeProvider();
        final CodexRuntimeContext context = MapBackedCodexRuntimeContext.empty();
        final CodexModuleRuntime runtime = provider.create(context);
        assertNotNull(runtime);
        assertEquals("test-provider-module", runtime.moduleName());
    }

    // --- inner test implementations ---

    private record TestEvent(Instant occurredAt) implements CodexEvent {}

    private static final class TestEventSubscriber implements CodexEventSubscriber<TestEvent> {
        @Override
        public Class<TestEvent> eventType() { return TestEvent.class; }

        @Override
        public void handle(final TestEvent event) {}
    }

    private static final class MinimalTestRuntime implements CodexModuleRuntime {
        private final String name;

        MinimalTestRuntime(final String name) { this.name = name; }

        @Override
        public String moduleName() { return name; }
    }

    private static final class RuntimeWithSubscriber implements CodexModuleRuntime {
        private final String name;
        private final CodexEventSubscriber<? extends CodexEvent> subscriber;

        RuntimeWithSubscriber(final String name,
                              final CodexEventSubscriber<? extends CodexEvent> subscriber) {
            this.name = name;
            this.subscriber = subscriber;
        }

        @Override
        public String moduleName() { return name; }

        @Override
        public List<CodexEventSubscriber<? extends CodexEvent>> subscribers() {
            return List.of(subscriber);
        }
    }

    private static final class TestRuntimeProvider implements CodexModuleRuntimeProvider {
        @Override
        public String moduleName() { return "test-provider-module"; }

        @Override
        public CodexModuleRuntime create(final CodexRuntimeContext context) {
            return new MinimalTestRuntime(moduleName());
        }
    }
}
