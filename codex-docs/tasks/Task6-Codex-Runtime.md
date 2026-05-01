# Task: Implement CodexRuntime as the composition root for codex-codex

## Objective
Create `CodexRuntime` as the manual composition root for `codex-codex`.
It wires all real components together and exposes them through a clean API.
The client never sees how the pipeline is assembled internally.

## Location
`codex.codex.internal.runtime.CodexRuntime`

## API

```java
public final class CodexRuntime {

    public static CodexRuntime inMemory() { ... }

    public SiteService siteService() { ... }

    public CodexEventDispatcher eventDispatcher() { ... }

    public Clock clock() { ... }

    public void shutdown() { ... }
}
```

## Internal wiring for inMemory()

The full pipeline must be assembled in this order:

MemorySiteRepository
Clock.systemUTC()
SiteIdentityGenerator
CodexSiteService(repository, clock, identityGenerator)
RecordingCodexEventDispatcher (or LoggerCodexEventDispatcher as delegate)
CodexExecutor via CodexExecutorConfig.of(50)
DeferredEventDispatcher(delegate, asyncExecutor)
EventPublishingSiteService(codexSiteService, deferredDispatcher, clock)


`siteService()` returns the outermost decorator — `EventPublishingSiteService`.
`eventDispatcher()` returns the `DeferredEventDispatcher`.

## Lifecycle

- `shutdown()` must call `asyncExecutor.shutdown()`
- `CodexRuntime` should implement `AutoCloseable` so it can be used
  in try-with-resources

## Simplify TestCodexContext

Once `CodexRuntime` exists, update `TestCodexContext` to delegate to it:

```java
public final class TestCodexContext {

    private final CodexRuntime runtime;

    public static TestCodexContext create() {
        return new TestCodexContext(CodexRuntime.inMemory());
    }

    public SiteService siteService() {
        return runtime.siteService();
    }

    public List<CodexEvent> events() { ... }

    public void assertNoEvents() { ... }

    public <E extends CodexEvent> E assertSingleEvent(Class<E> type) { ... }

    public void clearEvents() { ... }
}
```

## Constraints
- Follow CLAUDE.md conventions
- No Spring annotations anywhere
- Constructor injection throughout
- `CodexRuntime` is not exported in module-info.java
- `shutdown()` must be idempotent
- Proper JavaDoc on all public methods
- Logs on startup and shutdown