# Module Runtime Composition Decision

## Status

Accepted as architectural direction.

## Context

Codex is becoming a multi-module system.

Several modules may eventually provide their own runtime components:

- `codex-codex` provides canonical services, repositories, domain lifecycle, and core events.
- `codex-index` provides indexing writers, mappers, and indexing subscribers.
- `codex-chronicon` provides audit/history repositories and subscribers.
- `codex-archivum` may provide durable repositories and transaction adapters.
- `codex-porta` may expose REST, GraphQL, WebSocket, or SSE interfaces.
- `codex-iter`, `codex-nuntius`, `codex-speculum`, `codex-illuminarium`, `codex-imaginarium`, and `codex-olorin` may provide their own runtime pieces later.

As module boundaries become stricter, the core module must not know about every projection, adapter, or integration module.

For example:

```text
codex-codex
  must not depend on codex-index

codex-index
  may depend on codex-codex and codex-fundamentum
 ```

This means a single monolithic runtime inside codex-codex cannot be responsible for wiring the whole system forever.

Decision

Codex will move toward composable module runtimes.

Each module may define its own runtime or runtime provider.

A module runtime may:

construct that module’s internal objects
expose subscribers
expose public services or ports
hold infrastructure adapters
manage lifecycle
close owned resources

A module runtime may contain internal runtimes or compose smaller runtime pieces.

Global application runtimes, such as a future codex-porta application or a future codex-assembly, may compose multiple module runtimes into a running application.

Runtime Composition Model

The intended direction is:

CoreRuntime
-> canonical services
-> canonical repositories
-> core event publishing

IndexRuntime
-> index writer
-> indexing subscribers
-> index document mappers

ChroniconRuntime
-> chronicon repository
-> audit subscribers

PortaRuntime / ApplicationRuntime
-> composes CoreRuntime
-> composes IndexRuntime
-> composes ChroniconRuntime
-> exposes external APIs

A leaf module, such as codex-porta, may act as a composition root because it sits at the edge of the dependency tree.

A future neutral module such as codex-runtime or codex-assembly may also become the composition root if Codex needs a runtime independent from REST/GraphQL/API concerns.

Do not create this module yet unless explicitly tasked.

Explicit Construction First

The preferred first implementation style is explicit construction.

Example conceptual shape:

var core = CoreRuntime.inMemory();

var index = IndexRuntime.inMemory(
core.contentItemProjectionReader()
);

var chronicon = ChroniconRuntime.inMemory();

var application = ApplicationRuntime.compose(core, index, chronicon);

This keeps dependencies visible and easy to test.

Internal objects should still be constructed through explicit constructors or factories.

ServiceLoader-Based Discovery Later

Codex may later support runtime discovery using Java ServiceLoader.

This is for discovering module runtime providers, not for resolving arbitrary domain dependencies.

Conceptual shape:

public interface CodexModuleRuntimeProvider {

    String moduleName();

    CodexModuleRuntime create(CodexRuntimeContext context);
}

A module may provide:

provides CodexModuleRuntimeProvider
with codex.index.internal.IndexRuntimeProvider;

An application/runtime assembly may discover providers:

ServiceLoader.load(CodexModuleRuntimeProvider.class)

This allows optional modules to participate in composition without hardcoding every module in the application.

Runtime Context

A future runtime composition context may exist.

Conceptual shape:

public interface CodexRuntimeContext {

    <T> Optional<T> find(Class<T> type);

    <T> T require(Class<T> type);
}

This context is only for runtime construction and module assembly.

It must not leak into domain services, domain entities, repositories, subscribers, or canonical business logic.

AutoCloseable Runtime Lifecycle

Module runtimes should be able to close resources they own.

Future runtime interfaces should likely extend AutoCloseable.

Conceptual shape:

public interface CodexModuleRuntime extends AutoCloseable {

    String moduleName();

    List<CodexEventSubscriber<? extends CodexEvent>> subscribers();

    @Override
    default void close() {
    }
}

This allows modules to own resources such as:

executors
caches
index writers
database pools
file watchers
external clients
background workers

without forcing a framework-specific lifecycle.

No Service Locator in the Domain

Codex must not use a Service Locator pattern inside the domain.

Do not do this inside services, entities, subscribers, repositories, or domain logic:

locator.get(ContentItemService.class)
locator.get(IndexWriter.class)
locator.get(ChroniconRepository.class)

Reason:

hidden dependencies
weaker testability
unclear module boundaries
accidental god runtime
framework-like behavior without framework clarity

Domain and application components should receive dependencies explicitly through constructors.

Allowed:

ServiceLoader for discovering runtime providers at the composition layer
runtime context for assembling module runtimes
explicit constructors/factories for internal module objects

Not allowed:

domain components pulling dependencies from a global locator
static global registries for core domain behavior
hidden dependency lookup inside canonical services
OSGi Note

OSGi-style service registries are only a metaphor at this stage.

Codex is not adopting OSGi.

Dynamic service registration, plugin lifecycle, service ranking, unregistering services at runtime, and classloader-level module dynamics are future-forward concepts only.

Do not implement OSGi-like dynamic runtime behavior unless explicitly tasked.

Spring Note

Spring may eventually be used in an adapter or application layer.

If Spring is introduced later, it should act as one possible composition mechanism.

Spring must not become required by the Codex core.

The same module runtime/provider concepts should remain useful with or without Spring.

Possible future:

Manual composition
-> uses explicit factories/module runtimes

ServiceLoader composition
-> discovers module runtime providers

Spring composition
-> creates equivalent beans/configurations

The architectural boundary remains the same:

Frameworks may compose Codex.
Codex core must not depend on frameworks.
Consequences
Positive
Keeps codex-codex independent from projection/adapter modules.
Allows modules to own their internal wiring.
Avoids a monolithic god runtime.
Keeps runtime composition testable.
Allows manual composition for alpha.
Allows ServiceLoader-based discovery later.
Leaves room for Spring without requiring it.
Supports resource lifecycle through AutoCloseable.
Tradeoffs
More explicit wiring is needed at first.
A future assembly/composition layer will be necessary.
Some module interactions require public contracts instead of internal repository access.
Runtime provider discovery must be carefully scoped to avoid becoming a Service Locator.
Clarifications (resolved 2026-05-04)

1. Package location for runtime abstractions

CodexModuleRuntime, CodexModuleRuntimeProvider, and CodexRuntimeContext belong in codex-fundamentum.

Target package: codex.fundamentum.api.runtime

Rationale: these are generic module-composition primitives, not CMS domain concepts.
CodexEventSubscriber already lives in fundamentum; the runtime types are a natural sibling.

Rule: fundamentum may own generic runtime composition primitives; it must not gain CMS domain types.

2. CodexRuntime vs CoreRuntime

CoreRuntime is the architectural name for the role that CodexRuntime currently plays.
The existing CodexRuntime class in codex-codex is not replaced or renamed yet.

Do not introduce a second competing runtime.
Treat "CoreRuntime" as the conceptual label; CodexRuntime is the current implementation.

CodexRuntime must not wire codex-index, codex-chronicon, codex-porta, or other leaf/projection modules directly.
It may expose core services and future public projection/read contracts.

3. Chronicon subscribers

Task30 created the Chronicon audit foundation (AuditRecord, AuditSubject, AuditAction, ChroniconRepository, Memory/Recording repositories).

The next Chronicon implementation task is event subscribers. Candidates:

- SiteCreatedChroniconSubscriber
- ContentTypeCreatedChroniconSubscriber
- ContentItemPublishedChroniconSubscriber

Those subscribers listen to domain events from codex-codex and write AuditRecord entries to ChroniconRepository.
This will require codex-chronicon to depend on codex-codex.

Do not implement until explicitly tasked.

4. ContentItemProjectionReader — public core API contract

Do not use RepositoryContentItemProjectionSource as the public contract.
RepositoryContentItemProjectionSource is an index-side internal adapter and must not become the core public API.

A new public read contract belongs in codex-codex.api:

Target package: codex.codex.api.projection (or codex.codex.api.read — to be confirmed in the task)

Candidate interface shape:

public interface ContentItemProjectionReader {
    Optional<ContentItem> findContentItem(SiteKey siteKey, ContentTypeKey contentTypeKey, ContentItemKey key);
    Optional<ContentRevision> findContentRevision(ContentRevisionId revisionId);
}

This contract is the intended replacement for the current qualified export:

exports codex.codex.internal.repository to codex.index;

When ContentItemProjectionReader is introduced in codex-codex.api and implemented behind CodexRuntime,
codex-index may depend on it without needing direct repository access.

Do not implement until explicitly tasked.

Guidance for Clio

When implementing future runtime or module-boundary tasks:

Do not make codex-codex depend on adapter/projection modules.
Prefer explicit construction first.
If runtime discovery is required, use ServiceLoader only for module runtime providers.
Do not introduce a global Service Locator.
Do not pass runtime contexts into domain services or entities.
Keep dependencies explicit in constructors.
Module runtimes may implement AutoCloseable.
A module runtime may compose smaller internal runtimes.
Leaf modules such as codex-porta may become composition roots.
A neutral codex-runtime or codex-assembly module is possible later but should not be created unless explicitly tasked.
OSGi is only a metaphor for now; do not implement dynamic registry behavior.
Runtime abstraction types (CodexModuleRuntime, CodexModuleRuntimeProvider, CodexRuntimeContext) belong in codex.fundamentum.api.runtime.
ContentItemProjectionReader is the intended clean replacement for the qualified repository export to codex-index.