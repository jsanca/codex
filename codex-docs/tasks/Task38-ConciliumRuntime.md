# Task38: ConciliumRuntime

## Objective

Implement the first local application runtime composition in `codex-concilium`.

`ConciliumRuntime` should compose the existing module runtimes:

- `CodexRuntime` from `codex-codex`
- `IndexRuntime` from `codex-index`
- `ChroniconRuntime` from `codex-chronicon`

It should build a unified event dispatcher pipeline so domain events emitted by the core can be delivered to projection modules such as Index and Chronicon.

This task is a major milestone:

```text
CodexRuntime
  -> canonical core

IndexRuntime
  -> indexing projection subscribers

ChroniconRuntime
  -> audit/history subscribers

ConciliumRuntime
  -> local runtime council that composes them
````

Do not introduce ServiceLoader yet.

Do not introduce Spring.

Do not introduce REST/GraphQL.

Do not create Porta integration yet.

## Decision Context

Codex now has:

```text
codex-codex
  CodexRuntime

codex-index
  IndexRuntime

codex-chronicon
  ChroniconRuntime

codex-fundamentum
  CodexModuleRuntime
  CodexEventSubscriber
  LocalCodexEventDispatcher
  CompositeCodexEventDispatcher
  DeferredEventDispatcher
```

`codex-concilium` exists as a module skeleton.

The role of Concilium is to compose local module runtimes without forcing the canonical core to depend on projection or adapter modules.

This keeps the dependency direction clean:

```text
codex-codex
  must not depend on codex-index
  must not depend on codex-chronicon
  must not depend on codex-concilium

codex-concilium
  may depend on codex-codex
  may depend on codex-index
  may depend on codex-chronicon
  may depend on codex-fundamentum
```

Concilium is not Porta.

Concilium is not REST/GraphQL.

Concilium is not distributed coordination.

Concilium is local runtime composition.

## Scope

Implement:

* `ConciliumRuntime`
* tests
* README update
* module-info / pom updates as needed

Do not implement:

* ServiceLoader discovery
* `CodexModuleRuntimeProvider` implementations
* Spring configuration
* REST/GraphQL/WebSocket/SSE
* Porta integration
* CLI
* distributed coordination
* OSGi-like registry
* dynamic runtime registry
* persistence adapters
* OpenSearch/Lucene/myIR adapters
* cache wiring
* workflow wiring
* AI/agent wiring

## Location

Create:

```text
codex-concilium/src/main/java/codex/concilium/internal/ConciliumRuntime.java
```

Keep it internal for now.

Do not export `codex.concilium.internal`.

If a public API package becomes necessary later, create it in a separate task.

## 1. Module Dependencies

Update `codex-concilium/pom.xml`.

Add dependencies on:

* `codex-fundamentum`
* `codex-codex`
* `codex-index`
* `codex-chronicon`

Add test dependencies consistent with the existing module style.

Update `module-info.java`.

Expected shape:

```java
module codex.concilium {
    requires codex.fundamentum;
    requires codex.codex;
    requires codex.index;
    requires codex.chronicon;
}
```

Do not export packages yet.

Do not add `provides` declarations.

Do not add `requires transitive` unless required by public exported APIs.

## 2. ConciliumRuntime Shape

`ConciliumRuntime` should implement:

```text
CodexModuleRuntime
```

Suggested API:

```java
public final class ConciliumRuntime implements CodexModuleRuntime {

    public static ConciliumRuntime inMemory()

    public static ConciliumRuntime compose(
            CodexRuntime coreRuntime,
            IndexRuntime indexRuntime,
            ChroniconRuntime chroniconRuntime
    )

    public CodexRuntime coreRuntime()

    public IndexRuntime indexRuntime()

    public ChroniconRuntime chroniconRuntime()

    public CodexEventDispatcher eventDispatcher()

    @Override
    public String moduleName()

    @Override
    public List<CodexEventSubscriber<? extends CodexEvent>> subscribers()

    @Override
    public void close()
}
```

Requirements:

* `moduleName()` should return `"codex-concilium"`
* `subscribers()` should return the combined immutable subscriber list from child module runtimes
* `eventDispatcher()` should expose the composed dispatcher used by the runtime
* `close()` should be idempotent
* `close()` should close child runtimes in reverse order:

    * Chronicon
    * Index
    * Core
* validate null runtimes in `compose(...)`
* no ServiceLoader
* no Spring
* no global registry
* no dynamic registration

## 3. inMemory Factory

Implement:

```java
public static ConciliumRuntime inMemory()
```

It should create:

```text
CodexRuntime core = CodexRuntime.inMemory();

IndexRuntime index = IndexRuntime.inMemory(
    core.contentItemProjectionReader()
);

ChroniconRuntime chronicon = ChroniconRuntime.inMemory();

ConciliumRuntime runtime = ConciliumRuntime.compose(core, index, chronicon);
```

This gives Codex a simple full local in-memory runtime.

No external infrastructure required.

Indexing should use the default no-op index writer from `IndexRuntime.inMemory(...)`.

Chronicon should use the memory repository from `ChroniconRuntime.inMemory()`.

## 4. compose Factory

Implement:

```java
public static ConciliumRuntime compose(
        CodexRuntime coreRuntime,
        IndexRuntime indexRuntime,
        ChroniconRuntime chroniconRuntime
)
```

This is the explicit composition entry point.

It should:

1. validate all runtimes are non-null
2. collect subscribers from `indexRuntime` and `chroniconRuntime`
3. build a `LocalCodexEventDispatcher` with those subscribers
4. optionally build a `CompositeCodexEventDispatcher` if useful
5. create a transaction-aware dispatcher if needed
6. ensure core event publishing uses the composed dispatcher if possible

Important design constraint:

If current `CodexRuntime` already creates its own internal `DeferredEventDispatcher`, do not hack around it.

Prefer the smallest clean change required to allow `ConciliumRuntime` to supply or compose the event dispatcher.

If `CodexRuntime` cannot yet accept an external dispatcher, update `CodexRuntime` with a narrow factory overload if needed:

```java
public static CodexRuntime inMemory(CodexEventDispatcher eventDispatcher)
```

or another clean internal construction method.

But preserve:

```java
CodexRuntime.inMemory()
```

as the simple core-only default.

Do not make `codex-codex` depend on `codex-concilium`, `codex-index`, or `codex-chronicon`.

The dependency must remain one-way:

```text
codex-concilium -> codex-codex
```

## 5. Event Dispatcher Composition

The intended event path is:

```text
Core domain operation
  -> EventPublishing*Service
     -> DeferredEventDispatcher
        -> CompositeCodexEventDispatcher
           -> core EventRecorder
           -> LocalCodexEventDispatcher
              -> Index subscribers
              -> Chronicon subscribers
```

If `CodexRuntime` owns the `DeferredEventDispatcher`, Concilium should provide the delegate dispatcher into `CodexRuntime`.

Preferred shape:

```text
Concilium creates:
  LocalCodexEventDispatcher(index + chronicon subscribers)

Concilium creates or provides:
  CompositeCodexEventDispatcher(core recorder + local dispatcher)

CodexRuntime wraps it with:
  DeferredEventDispatcher
```

If the current core runtime structure makes this difficult, choose the smallest clean refactor.

Do not duplicate event dispatchers in a way that causes events to be delivered twice.

Do not dispatch directly to index/chronicon outside the core event pipeline.

## 6. Event Recording

If `CodexRuntime` has an internal event recorder, preserve it.

`ConciliumRuntime` should not replace Chronicon with EventRecorder.

EventRecorder is an in-memory observation/testing helper.

Chronicon is product audit/history.

Both may receive the same events:

```text
EventRecorder
  -> test/observation helper

Chronicon
  -> audit/history projection
```

Do not remove EventRecorder.

Do not turn EventRecorder into Chronicon.

## 7. Runtime Accessors

Expose child runtimes:

```java
public CodexRuntime coreRuntime()

public IndexRuntime indexRuntime()

public ChroniconRuntime chroniconRuntime()
```

Reason:

Tests and future edge modules may need access to:

* core services
* index writer
* chronicon repository
* recorded events

Do not expose low-level internal repositories directly from Concilium unless already exposed by child runtimes.

## 8. subscribers()

`ConciliumRuntime.subscribers()` should return the subscribers contributed by child modules.

Expected:

```text
indexRuntime.subscribers()
chroniconRuntime.subscribers()
```

Do not include core event recorder as a subscriber unless it already is one.

Do not include unsupported/future modules.

Return immutable list.

## 9. close()

`ConciliumRuntime` should implement `close()`.

Requirements:

* idempotent
* close child runtimes in reverse dependency/composition order:

    1. `chroniconRuntime.close()`
    2. `indexRuntime.close()`
    3. `coreRuntime.close()`
* if a child close throws, decide a simple behavior:

    * propagate the first exception
    * optionally attempt remaining closes
* keep it simple
* document behavior
* no lifecycle manager abstraction yet

Recommendation:

For now, close sequentially and propagate exceptions. Since current child runtimes are no-op, this is enough.

If implementing idempotency requires an `AtomicBoolean`, use it.

## 10. Tests

Add JUnit 5 tests.

Suggested test class:

```text
codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeTest.java
```

No Mockito.

### Construction tests

* `inMemory` creates runtime
* `moduleName` returns `codex-concilium`
* `coreRuntime` is not null
* `indexRuntime` is not null
* `chroniconRuntime` is not null
* `eventDispatcher` is not null
* `subscribers` contains index + chronicon subscribers
* `subscribers` snapshot is immutable

### compose tests

* `compose` rejects null core runtime
* `compose` rejects null index runtime
* `compose` rejects null chronicon runtime
* `compose` uses provided runtimes

### Event integration test

Add a test proving that a core operation emits an event that reaches both Index and Chronicon.

This may require composing with custom child runtimes:

* `CodexRuntime` with configurable dispatcher/delegate
* `IndexRuntime.withWriter(...)` using `RecordingIndexWriter`
* `ChroniconRuntime.withRepository(...)` using `RecordingChroniconRepository`

Flow:

1. create core runtime
2. create recording index writer
3. create index runtime using core projection reader
4. create recording chronicon repository
5. create chronicon runtime using recording repository
6. create concilium runtime from these runtimes
7. perform a core operation that emits an event supported by Chronicon and/or Index
8. assert:

    * core event recorder recorded event if available
    * Chronicon repository saved audit record
    * Index writer upserted document if publishing content item

Recommended full flow if existing services support it:

```text
create site
create content type
add fields
activate content type
create content item
publish content item
```

Expected after publish:

```text
ContentItemPublishedEvent
  -> IndexRuntime subscriber upserts IndexDocument
  -> ChroniconRuntime subscriber saves AuditRecord
```

If full authoring flow is too large for this task, use a smaller supported event such as `SiteCreatedEvent` for Chronicon only and document that Index publish integration remains covered by `codex-index` tests.

Prefer one meaningful integration test over many brittle tests.

### close tests

* `close` does not throw
* `close` is idempotent

If testing close order requires test runtime stubs, keep it simple.

## 11. README Update

Update `codex-concilium/README.md`.

Mention:

* `ConciliumRuntime` now exists
* `inMemory()` composes core, index, and chronicon runtimes
* Concilium builds a local runtime council
* Porta may use Concilium later
* ServiceLoader discovery is future work
* Spring integration is future work
* this is not cluster coordination

Do not rewrite the whole README.

## 12. Documentation Update

Update `codex-docs/modules/MODULE-RESPONSIBILITIES.md` if useful.

Add a short note:

```text
codex-concilium now contains the first ConciliumRuntime, composing core, index, and chronicon runtimes locally.
```

Do not rewrite the document.

## 13. Acceptance Criteria

Task is complete when:

* `ConciliumRuntime` exists
* it implements `CodexModuleRuntime`
* it can create an in-memory composed runtime
* it can compose provided core/index/chronicon runtimes
* it exposes child runtimes
* it exposes combined subscribers
* it exposes the composed event dispatcher
* events from core can reach projection module subscribers
* no ServiceLoader is implemented
* no Spring is introduced
* no Porta integration is introduced
* tests pass
* dependency direction remains clean

## 14. Maven Commands

Run:

```bash
mvn test -pl codex-concilium -am
```

If practical, also run:

```bash
mvn test -pl codex-fundamentum,codex-codex,codex-index,codex-chronicon,codex-concilium -am
```

Then run:

```bash
mvn compile
```

Report command results.

## 15. Post-Task Report

After implementation, report:

* files created
* files modified
* module-info changes
* pom changes
* tests added/updated
* Maven commands run
* whether tests passed
* whether any CodexRuntime factory was added
* event pipeline shape implemented
* intentional deviations
* open questions
* recommended follow-up tasks

## 16. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Keep Concilium as local runtime composition.
* Do not implement ServiceLoader.
* Do not implement Spring.
* Do not implement Service Locator.
* Do not wire Porta.
* Do not introduce REST/GraphQL.
* Do not introduce CLI.
* Do not introduce distributed coordination.
* Do not create Concordia.
* Do not modify domain behavior.
* Do not make codex-codex depend on codex-concilium.
* Do not make codex-codex depend on codex-index or codex-chronicon.
* Do not add external infrastructure dependencies.
* Do not implement persistence adapters.
* Do not implement search service.
* Do not implement cache integration.
* Do not implement workflow.
* Keep comments and JavaDoc in English.
* Prefer explicit construction.
* Prefer small, testable runtime composition.
