# codex-concilium

> The Concilium is the council of module runtimes.

## Meaning

*Concilium* means council, assembly, or gathering.

In Codex, `codex-concilium` is the place where module runtimes meet and are composed into a
coherent local application runtime.

## Responsibility

`codex-concilium` will be the local runtime composition layer. It will eventually compose:

```
CodexRuntime         (canonical core)
  + IndexRuntime     (indexing projection)
  + ChroniconRuntime (audit-history projection)
  + future ArchivumRuntime
  + future CustosRuntime
  + future IterRuntime
  -> Concilium application runtime
```

Its responsibilities will include:

- Composing module runtimes into a single local runtime
- Collecting all module subscribers into a unified event dispatcher
- Coordinating lifecycle shutdown across all modules
- Exposing an assembled runtime to edge modules such as `codex-porta`
- Potentially supporting ServiceLoader-based runtime provider discovery in the future

## What Concilium Is Not

- It is **not** the canonical core (`codex-codex` owns domain lifecycle).
- It is **not** `codex-porta` (Porta exposes external protocols — REST, GraphQL, WebSocket).
- It is **not** cluster coordination (a future `codex-concordia` may handle that).
- It is **not** a Service Locator or dependency injection container.
- It is **not** ServiceLoader-based discovery yet.
- It is **not** Spring configuration.
- It does **not** own domain behavior.

## Relationship to Porta

```
codex-concilium  → composes local module runtimes
codex-porta      → exposes external API protocols
```

Porta may eventually depend on Concilium to obtain a composed runtime.
Concilium must not depend on Porta.

## Relationship to Concordia

```
codex-concilium         → local runtime composition
future codex-concordia  → possible cluster / distributed coordination
```

`codex-concordia` does not exist yet and is future-forward.

## Current Status

`ConciliumRuntime` is implemented (Task 38):

- `ConciliumRuntime.inMemory()` — assembles `CodexRuntime` + `IndexRuntime` +
  `ChroniconRuntime` into a fully wired local runtime. Domain events from core services are
  automatically routed to both the indexing and audit/history subscribers.
- `ConciliumRuntime.compose(core, index, chronicon)` — explicit composition for tests or
  future adapters that need to provide custom child runtimes.

Usage:

```java
// In-memory: suitable for tests and early development
ConciliumRuntime runtime = ConciliumRuntime.inMemory();
runtime.coreRuntime().siteService().create(createSiteCommand, actor);
// SiteCreatedEvent reaches ChroniconRuntime subscribers automatically
```

**Not yet implemented:**
- Assembly with `PortaRuntime` or other edge modules
- `ConciliumRuntimeProvider` (ServiceLoader-based provider declaration)
- Spring integration
- Cluster coordination (future `codex-concordia`)

## Dependency Rules

- Depends on `codex-fundamentum`, `codex-codex`, `codex-index`, and `codex-chronicon`.
- Edge modules such as `codex-porta` may depend on `codex-concilium`.
- `codex-codex` must **never** depend on `codex-concilium`.
