# AGENTS.md — Codex Project Guide

> **`AGENTS.md` is canonical. `CLAUDE.md` is its mirror.** When conventions change, update both.

## Build & Test

No Maven wrapper — `mvn` must be on PATH. Java 25. All commands run from repo root.

```bash
mvn clean verify                                          # full build + tests (all modules)
mvn test                                                  # run all tests
mvn test -pl codex-codex                                  # tests in one module
mvn test -pl codex-codex -Dtest=CodexSiteServiceTest      # single test class
mvn test -pl codex-custos -Dtest=DefaultPermissionResolverTest  # single test in custos
mvn test -DskipTests                                      # compile only, skip tests
```

JUnit 5 + AssertJ. Use `assertThatThrownBy` / `assertThatNullPointerException`; do not use JUnit's `assertThrows`.
No CI workflows, no formatter/lint plugins (spotless, checkstyle) — nothing to run beyond `mvn`.

`spring-boot-starter-test` in `<scope>test</scope>` is allowed even in `codex-codex` — test scope never leaks into production, so it does not violate core purity.

## Module Map

Maven + Jigsaw. Each module has `pom.xml` + `module-info.java`, with `codex.<module>.api` (public) / `codex.<module>.internal` (hidden) split. Root `src/` is unused — all source lives in modules.

| Module | Responsibility | State |
|---|---|---|
| `codex-bom` | Version management | — |
| `codex-fundamentum` | Shared primitives: events, dispatchers, cache, `Actor`, `CodexExecutor`, Observance, `TransactionContext` | ✓ |
| `codex-codex` | Domain kernel: sites, content types, items, revisions, lifecycle | ✓ |
| `codex-chronicon` | Audit history, revision memory, event subscribers | ✓ |
| `codex-index` | Search/indexing abstractions and subscribers | ✓ |
| `codex-concilium` | Runtime composition: `ConciliumRuntime` assembles `CodexRuntime` + `IndexRuntime` + `ChroniconRuntime` | ✓ |
| `codex-custos` | Domain authorization (kernel + roles + resolver + domain permission services + secured decorators; Phase 4 workflow pending) | ✓ |
| `codex-archivum`, `codex-scriptorium`, `codex-illuminarium`, `codex-porta`, `codex-iter`, `codex-imaginarium`, `codex-olorin` | Skeletons — no implementation | — |

**Dependency direction: inward only.** `codex-codex` depends only on `codex-fundamentum` and must never depend on any other Codex module. Composition lives in `codex-concilium`, never in the core. Module runtimes live in public `*.api.runtime` packages (moving a runtime there removes the need for qualified exports).

## Architecture Rules

### Core purity (`codex-codex`, `codex-fundamentum`)

Never import Spring Boot, JPA, JDBC, HTTP, REST controllers, persistence annotations, or auth mechanisms. Infrastructure belongs in adapters/runtime modules.

### Fundamentum rule

A type belongs in `codex-fundamentum` only if generic, framework-agnostic, reusable by multiple modules, free of CMS/domain concepts, and small enough to stay stable. Anything mentioning Site, ContentItem, ContentType, IndexDocument, AuditRecord, Workflow, User, Role, REST, AI, or persistence backends does not belong there.

### Package / module-info

- Never export `internal` packages. Qualified exports (`exports X.internal.Y to codex.Z`) are accepted only as documented transitional debt pointing at a future public read/projection API.
- `requires transitive` only when the module's own API surface exposes the depended-on module's types (e.g. `codex.index.api` exposing `codex-codex` types). Otherwise plain `requires`.
- For skeleton tasks keep `module-info` and dependencies minimal — only what compiles. No placeholder classes, no `.gitkeep`.

### Decoration, not inheritance

Services grow by wrapping: e.g. `TimedSiteService → Caching… → SecuredSiteService (custos) → EventPublishingSiteService → CodexSiteService`. Each decorator adds one concern. Cross-cutting metrics go through Observance (`Timed*Service`, `*MetricNames`); do not invent new metric plumbing.

### Deferred event dispatch

New service methods that emit events must follow the defer-then-flush pattern in `EventPublishingSiteService` / `EventPublishingContentItemService`: accumulate via `DeferredEventDispatcher` during the operation, flush as one batch at the end.

### Events vs Hooks

- **Events** = facts already happened (`SiteCreatedEvent`, `ContentItemPublishedEvent`). Must implement `CodexEvent`. Never put content values in events.
- **Hooks** = extension points around something happening (`beforeSave`). Keep the two conceptually separate.

### Subscribers read projections, not repositories

Indexing/Chronicon subscribers must not access repositories or lifecycle services directly — use projection sources (e.g. `ContentItemProjectionSource`). Subscribers stay single-responsibility; no cache, transactions, or runtime wiring inside feature/subscriber tasks unless asked.

## Java Conventions

- Records for value objects, identities, commands, events. Defensive copies (`List.copyOf()`, `Set.copyOf()`) on all inbound collections.
- Entities: `static Builder builder()`, `static Builder copyOf(T source)`, `static T of(...)` for simple types. Validation in canonical constructors.
- Identity types validate + trim in the canonical constructor, with `of(...)` and `generate()`; prefer deterministic identity (`UUID.nameUUIDFromBytes`) where possible.
- Sealed interfaces + record permits for discriminated unions (e.g. `AccessDecision.Granted/ Denied`); pattern-match at call sites, never `instanceof` chains.
- `Optional<T>` for valid absence, never for error signaling. `Objects.requireNonNull` on every required parameter (constructors, injection, service entry points).
- SLF4J via `private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class)`; log at service boundaries and state transitions.
- ≤20 lines of logic per method (log lines don't count); primitives over wrappers; `ScopedValue`, never `ThreadLocal`; virtual threads for concurrency.
- Idempotent operations (e.g. `activate` on an already-active entity) must not touch `updatedAt`/`updatedBy`.
- Schema fields: `Map<FieldKey, Field>`, not `List<Field>`.
- Domain lambda names: no one-letter names for domain concepts — `roleAssignment`, `permission`, `scope`, `actor`.
- JavaDoc on all public types and methods.

## Repositories & Services

- **Repositories are dumb**: CRUD only, no business logic, never call other repositories. `codex.<module>.internal.repository`, unexported.
- **Services are smart**: validation, orchestration, business logic. Interface in `codex.<module>.api…service`, impl in `codex.<module>.internal.service`.
- Every public service method takes an `Actor` for audit context. Constructor injection always.
- Start with `MemoryXxxRepository`; real persistence comes later via `codex-archivum` and must not contain business logic.

## State Machines

Transitions are explicit, validated in a dedicated method, no skipped steps.

- **SiteStatus**: `STARTED → SUSPENDED → STARTED`, `SUSPENDED → ARCHIVED`, `ARCHIVED → SUSPENDED`. `unarchive` returns to `SUSPENDED`, not `STARTED`.
- **ContentTypeStatus**: `DRAFT → ACTIVE → ARCHIVED`, plus `DRAFT → ARCHIVED`. One `ACTIVE` version per `(siteId, key)`.
- **ContentItemStatus**: `DRAFT → PUBLISHED → ARCHIVED`, `PUBLISHED → DRAFT` (unpublish), `ARCHIVED → DRAFT` (restore). Delete requires `ARCHIVED`.

## Exception Style

Unchecked, extend `RuntimeException`, no common base class, no `throws` for domain validation errors. Constructors: `(String message)` + `(String message, Throwable cause)`; never the legacy four-arg constructors. Domain exceptions live in `codex.<module>.api.exception` (exported) — **except `codex-codex`**, whose service exceptions (e.g. `SiteAlreadyExistException`, `InvalidContentTypeStatusTransitionException`) still live unexported in `codex.codex.internal.service`. Do not add new exceptions there; use `api.exception`. `IllegalStateException` for subscriber/projection invariant violations. Keep `NotFoundException` generic in fundamentum; add typed subclasses only when a caller discriminates on them.

## Custos (authorization)

Custos answers `Actor + Permission + Resource + Context → AccessDecision` for **domain operations, not endpoints**. Prefer `contentItemPermissionsService.canPublish(actor, resource)` over role checks.

- Scope walk is bottom-up (`ContentItem → ContentType → Site → Global`); first grant wins; never crosses site boundaries.
- Implications live in `DefaultPermissionImplicationRules`, not role blueprints: `update` ⇒ `read`, `publish` ⇒ `read`, `publish` ⇏ `update`.
- `SUPER_ADMIN` bypass belongs in resolver/evaluator behavior, never in `BuiltInRoles`; hard invariants run **before** any bypass. AGENT + SUPER_ADMIN is a fatal invariant violation (`CustosAgentSuperAdminInvariantViolationException`) — never convert to `Denied`, always propagate.
- `Role` is a permission blueprint only — no actors, no scopes. `PermissionGrant` = permission + scope (no actor/role). `RoleAssignment` = actor + role + scope.
- Current fail-closed / pass-through edges (do not "fix" opportunistically): item delete/restore, site unarchive are fail-closed; `findAll`/`findBy*` list reads are pass-through pending a read-filtering strategy.
- Use `ConciliumRuntime.secured(snapshotProvider)` for authorization-sensitive paths; `inMemory()` is unsecured (tests/back-compat). Callers use `runtime.siteService()` etc. — never `runtime.coreRuntime().siteService()`. Security mode is diagnostic; enforcement is in the service graph.

## Concurrency (recording/test helpers)

With `Collections.synchronizedList`, synchronize explicitly when iterating/copying/clearing: `synchronized (list) { return List.copyOf(list); }`, `synchronized (list) { list.clear(); }`. Bare `add()` is safe; `List.copyOf()` iterates internally and is not covered by the wrapper lock.

## Test Conventions

Follow `codex-custos` style (flatter `codex-codex` tests predate it — do not copy them).

- `@Nested` per scenario family with its own `@DisplayName`; `@DisplayName` on every test naming observable behaviour.
- Fixtures as `private static final UPPER_SNAKE_CASE` constants (`ALICE`, `SITE_A`). Test classes package-private.
- AssertJ only for exception assertions. Never rely on unordered repository results in tests.
- New abstractions get their own focused tests (null args, success, missing entity) — not just coverage through higher-level tests.
- Check `git status` before finishing: new files must be tracked.

## Workflow Constraints

- Skeleton or documentation-only tasks: zero code changes — no Java, pom.xml, module-info, wiring, or tests.
- Backlog labels: **Active** = may implement when tasked; **Near-future** = do not implement unless asked; **Future-forward** = document only, never code.
- Complete partially-built modules minimally; do not recreate or broaden scope. Match existing patterns closely; no dynamic runtime behavior unless requested. No refactors beyond the task.
- Surface follow-ups in the post-task report (files changed, tests run + result, deviations, open questions) instead of implementing them.

## Docs That Matter

The docs follow an OSK information model — placement is by purpose, not by the task that produced it. Start at `docs/PROJECT.md`, then `docs/OSK.md`.

- `docs/knowledge/modules/MODULE-RESPONSIBILITIES.md` — boundary map (still lists uncreated `nuntius`/`speculum` as conceptual; trust code over prose).
- `docs/knowledge/security/CUSTOS-MODEL.md` + `docs/knowledge/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md` — authorization model and phase status.
- `docs/engineering/roadmap/ROADMAP.md` — committed direction; non-committed ideas stay in `docs/engineering/roadmap/future/`.
- `docs/engineering/AGENT-CALIBRATION.md` — accumulated corrections (check here before repeating a known pattern).
- `CODING_IDENTITY.md` — broader design fingerprint.
- Decisions → `docs/engineering/adr/`; durable facts → `docs/knowledge/`; task evidence → `docs/engineering/`.
