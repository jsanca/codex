# Task35: ChroniconRuntime

## Objective

Create the first module runtime for `codex-chronicon`.

`ChroniconRuntime` should assemble the internal Chronicon audit components and expose them through the generic runtime abstraction introduced in `codex-fundamentum`.

This task validates the module runtime pattern on a small module before applying it to `codex-index`.

## Decision Context

Task34 introduced generic runtime abstractions in `codex-fundamentum`:

- `CodexModuleRuntime`
- `CodexModuleRuntimeProvider`
- `CodexRuntimeContext`
- `MapBackedCodexRuntimeContext`

Chronicon already has:

- `ChroniconRepository`
- `MemoryChroniconRepository`
- `RecordingChroniconRepository`
- `SiteCreatedChroniconSubscriber`
- `ContentTypeCreatedChroniconSubscriber`
- `ContentItemPublishedChroniconSubscriber`

Now Chronicon should expose a runtime that assembles its repository and subscribers.

Expected conceptual shape:

```text
ChroniconRuntime
  -> ChroniconRepository
  -> Chronicon subscribers
  -> CodexModuleRuntime
```

This task should not wire Chronicon into `CodexRuntime`.
This task should not create a global application runtime.

## Scope

Implement:
* `ChroniconRuntime`
* tests
* README update if useful

Do not implement:
* `IndexRuntime`, `CoreRuntime`, `ApplicationRuntime`, `PortaRuntime`
* ServiceLoader provider declarations
* `ChroniconRuntimeProvider`
* Spring integration
* runtime assembly module
* dynamic registry
* durable persistence
* audit/timeline query services
* REST/GraphQL/Workflow/Cache
* indexing changes

## Location

Create:
`codex-chronicon/src/main/java/codex/chronicon/internal/ChroniconRuntime.java`

The class may stay internal for now. Do not export `codex.chronicon.internal`.

## 1. ChroniconRuntime Shape

`ChroniconRuntime` should implement `CodexModuleRuntime`.

Suggested API:
```java
public final class ChroniconRuntime implements CodexModuleRuntime {
    public static ChroniconRuntime inMemory()
    public static ChroniconRuntime withRepository(ChroniconRepository repository)
    public ChroniconRepository repository()

    @Override
    public String moduleName()

    @Override
    public List<CodexEventSubscriber<? CodexEvent extends>> subscribers()

    @Override
    public void close()
}
```

Requirements:
* `moduleName()` returns `"codex-chronicon"`
* `subscribers()` returns immutable list
* `repository()` exposes the configured `ChroniconRepository`
* `close()` is idempotent and no-op for now
* validate null repository in factory/constructor
* No ServiceLoader, Spring, global registry, or dynamic registration.

## 2. inMemory Factory
Implement `public static ChroniconRuntime inMemory()`.
It should create:
* `MemoryChroniconRepository`
* `SiteCreatedChroniconSubscriber`
* `ContentTypeCreatedChroniconSubscriber`
* `ContentItemPublishedChroniconSubscriber`

## 3. withRepository Factory
Implement `public static ChroniconRuntime withRepository(ChroniconRepository repository)`.
It should create the same subscriber list using the provided repository. Useful for tests with `RecordingChroniconRepository`.

## 4. Subscriber List
The runtime should expose all current Chronicon subscribers. Do not include unsupported/future subscribers or create generic discovery.

## 5. Tests
Add JUnit 5 tests in:
`codex-chronicon/src/test/java/codex/chronicon/internal/ChroniconRuntimeTest.java`

Test cases:
* `inMemory` creates runtime
* `moduleName` returns `codex-chronicon`
* `repository` is not null
* `subscribers` contains three subscribers (immutable snapshot)
* `withRepository` rejects null and uses provided repository
* `close` is idempotent and safe
* runtime subscribers work with `LocalCodexEventDispatcher`

## 6. Documentation
Briefly update `codex-chronicon/README.md` to mention:
* Chronicon now has a module runtime.
* `ChroniconRuntime.inMemory()` usage.
* Future work: assembly with core/index and ServiceLoader provider.

## 7. Module Info
No changes needed unless compilation strictly requires it. Do not export internal packages.

## 8. Acceptance Criteria
* `ChroniconRuntime` exists and implements `CodexModuleRuntime`.
* Exposes current subscribers and allows repository injection.
* Tests prove composition works with `LocalCodexEventDispatcher`.
* No global assembly, ServiceLoader, or Spring introduced.

## 9. Maven Commands
Run: `mvn test -pl codex-chronicon -am`

## 10. Post-Task Report
Report: files created/modified, tests added, Maven results, deviations, and follow-up tasks.

## 11. Constraints
* Follow CLAUDE.md, CODING_IDENTITY.md, and AGENT-CALIBRATION.md.
* Keep runtime explicit and small.
* Do not wire into `CodexRuntime` or create other Runtimes.
* Keep comments and JavaDoc in English.
