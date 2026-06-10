# AGENTS.md — Codex Project Guide

## Build & Test Commands

```bash
mvn clean verify          # full build + tests (all modules)
mvn test                   # run all tests
mvn test -pl codex-codex   # run tests in one module
mvn test -pl codex-codex -Dtest=CodexSiteServiceTest  # single test class
mvn test -DskipTests       # compile only, skip tests
```

Test framework: **JUnit 5 + AssertJ**. No CI workflows exist. No formatter/lint plugins (spotless, checkstyle) are configured.

## Module Map

Each module has a `module-info.java`, `pom.xml`, and follows `codex.<module>.api` / `codex.<module>.internal` package split.

| Module                  | Responsibility                                      |
|-------------------------|-----------------------------------------------------|
| `codex-bom`             | Bill of Materials (version management)              |
| `codex-fundamentum`     | Shared abstractions: CodexEvent, dispatchers, cache, Actor, CodexExecutor |
| `codex-codex`           | Central domain kernel: sites, content types, items, lifecycle |
| `codex-chronicon`       | Audit history, revision memory, event subscribers   |
| `codex-archivum`        | Storage abstraction (skeleton)                      |
| `codex-index`           | Search/indexing abstractions and subscribers        |
| `codex-concilium`       | Runtime composition: composes CodexRuntime + IndexRuntime + ChroniconRuntime |
| `codex-scriptorium`     | Scripting runtime (skeleton)                        |
| `codex-illuminarium`    | Enrichment & semantic enhancement (skeleton)        |
| `codex-porta`           | REST/GraphQL exposure (skeleton)                    |
| `codex-iter`            | Workflow engine (skeleton)                          |
| `codex-custos`          | Domain authorization: Phase 0 complete/hardened; early Phase 1 roles and resolver primitives |
| `codex-imaginarium`     | AI infrastructure (skeleton)                        |
| `codex-olorin`          | Agent reasoning (skeleton)                          |

**Dependency direction**: inward only. `codex-codex` depends only on `codex-fundamentum`. It must never depend on any other Codex module. `codex-concilium` composes module runtimes without making the core depend on projection/adapter modules.

## Architecture Rules

### Core purity (`codex-codex` and `codex-fundamentum`)

Must never import: Spring Boot, JPA, JDBC, HTTP, REST controllers, persistence annotations, or auth mechanisms. Infrastructure belongs in adapters or runtime modules.

### Package conventions

- Public contracts: `codex.<module>.api`
- Implementation: `codex.<module>.internal`
- Never export `internal` packages in `module-info.java`
- Qualified exports (`exports X.internal.Y to codex.Z`) are accepted as controlled architectural debt
- Use `requires transitive` only when the module's own API surface exposes the depended-on module's types (e.g., if `codex.index.api` exposes `codex-codex` types, `requires transitive codex.codex` is correct). Otherwise prefer plain `requires`.

### Decoration, not inheritance

Services grow through decorator composition (e.g. `TransactionalSiteService -> LockingSiteService -> AuditingSiteService -> EventPublishingSiteService -> CodexSiteService`). Each wrapper adds one concern.

### Events vs Hooks

- **Events** = facts that already happened (`SiteCreatedEvent`, `ContentItemPublished`). Must implement `CodexEvent`.
- **Hooks** = extension points around something happening (`beforeSave`, `afterPublish`). Keep them conceptually separate.

## Java Conventions

### Target version
Java 25. Prefer virtual threads, `ScopedValue` over `ThreadLocal`, primitives over wrappers.

### Records and immutability
Always use `record` for value objects, identities, commands, and events. Defensive copies (`List.copyOf()`, `Set.copyOf()`) on all collections received from outside.

### Entity pattern
Every non-trivial entity needs: `static Builder builder()`, `static Builder copyOf(T source)`, `static T of(...)` for simple types. Canonical constructors do validation.

### Identity types
```java
public record SiteId(String value) {
    public SiteId { /* validate, trim */ }
    public static SiteId of(String value) { ... }
    public static SiteId generate() { ... }
}
```
Prefer deterministic identity (`UUID.nameUUIDFromBytes`) where possible.

### Enums for closed sets
Lifecycle statuses, field types, actor types — any domain concept with a fixed set of values.

### Validation
Validate at boundaries (constructors, command handlers, service entry points). Use `Objects.requireNonNull(value, "message")`. Throw domain exceptions (unchecked, extend `RuntimeException`, live in `codex.<module>.api.exception`). `Optional<T>` for valid absence, not error signaling.

### Logging
`private static final Logger LOGGER = LoggerFactory.getLogger(MyClass.class)` using SLF4J. Log at service boundaries and state transitions.

### Method size
≤20 lines of logic (log statements don't count). Extract well-named private methods if exceeding.

### Idempotent methods
If a method is idempotent (e.g., `activate` when the entity is already active), do not update `updatedAt`/`updatedBy`. Only state-changing operations advance the audit timestamps.

### Schema field collections
Prefer `Map<FieldKey, Field>` over `List<Field>` for schema field collections.

## Concurrency Conventions

- When using `Collections.synchronizedList`, always synchronize explicitly on the list when iterating or copying: `synchronized (list) { return List.copyOf(list); }`. Individual `add()` calls are safe without explicit sync, but `List.copyOf()` iterates internally and is not protected by the wrapper's per-method lock.
- Same rule applies to `clearRecording()`-style bulk clears: `synchronized (list) { list.clear(); }`.

## Repository & Service Conventions

- **Repositories are dumb** — CRUD only, no business logic, never call other repositories. Lives in `codex.<module>.internal.repository` (not exported).
- **Services are smart** — business logic, validation, orchestration. Interface in `codex.<module>.api.model.service` (exported), implementation in `codex.<module>.internal.service`.
- Every public service method receives an `Actor` for audit context.
- Constructor injection always; `Objects.requireNonNull` on every constructor parameter.
- Start with `MemoryXxxRepository` before adding real persistence.

## State Machines

State transitions must be explicit and validated in a dedicated method. Current `SiteStatus` machine:
```
STARTED ⟷ SUSPENDED ⟷ ARCHIVED
```
No skipping steps. `unarchive` returns to `SUSPENDED`, not `STARTED`.

## Code Quality Constraints

### Never
- Add Spring/JPA/Hibernate annotations to core domain classes (`codex-codex`, `codex-fundamentum`)
- Use `ThreadLocal` when `ScopedValue` is available
- Use wrapper types (`Integer`, `Long`) when primitives suffice
- Put business logic in repositories
- Skip validation in constructors or service entry points
- Create mutable domain objects
- Use inheritance where composition works
- Modify existing behavior or perform broad refactors unless explicitly asked
- Use single-letter variable names (except `i` in loops)

### Always
- Read existing code before writing new code
- Write JavaDoc for all public types and methods
- Add `Objects.requireNonNull` for every required parameter
- Log at service entry points and state transitions
- Ask before making architectural decisions that affect module boundaries
- Surface follow-up tasks in a post-task report instead of implementing them opportunistically

## Exception Style

- Unchecked exceptions, extend `RuntimeException`
- Domain exceptions in `codex.<module>.api.exception` (exported). No common base class — direct subtypes with specific names.
- Constructors: `(String message)` + `(String message, Throwable cause)`. Optional domain-specific factory.
- No `throws` declarations for domain validation errors
- `IllegalStateException` for subscriber/projection failures (system invariant violation)

## Custos / Authorization Rules

`codex-custos` owns domain authorization — not HTTP security. The core question it answers:
```
May this Actor perform this PermissionKey on this Codex resource under this Context?
```

### Key types (Phase 0 + early Phase 1)
- `PermissionKey` — domain permission name (e.g., `contentItem.publish`)
- `Permissions` — catalog of built-in domain permission keys
- `ResourceRef` — sealed hierarchy: `GlobalResourceRef`, `SiteResourceRef`, `ContentTypeResourceRef`, `ContentItemResourceRef`
- `ResourceScope` — where a grant is assigned; mirrors `ResourceRef` hierarchy
- `AccessDecision` — sealed: `Granted` / `Denied`; always carries actor, permission, resource, reason
- `AccessDeniedException` — thrown by `AccessDecision.Denied.requireGranted()`
- `SecurityEvaluationContext` — request-level context passed to evaluators
- `PermissionEvaluator` — low-level evaluation port (interface)
- `AccessDecisionService` — application-level service wrapping the evaluator
- `RoleKey` — role identifier
- `Role` — permission blueprint; it does not contain actors or scopes
- `PermissionGrant` — pairs a permission with a resource scope
- `RoleAssignment` — pairs an `Actor` with a `RoleKey` at a `ResourceScope`
- `BuiltInRoles` — catalog of role blueprints
- `PermissionResolver` — computes effective permissions from role assignments, role registry, scopes, and implication rules
- `DefaultPermissionResolver` — internal implementation of `PermissionResolver`

### Hard rules
- `AgentActor` must never be granted permission-management capabilities (hardcoded, not configurable).
- Permission lookup never crosses site boundaries.
- Authorization evaluates domain operations, not endpoints. Prefer `contentItemPermissionsService.canPublish(actor, resource)` over checking roles directly.
- `SUPER_ADMIN` includes all built-in permissions for introspection and blueprint purposes.
- `SUPER_ADMIN` bypass logic is not encoded in `BuiltInRoles`; it belongs in resolver/evaluator behavior.
- Hard invariants must run before any `SUPER_ADMIN` bypass.
- `PermissionResolver` does not produce `AccessDecision` yet; `AccessDecisionService` wiring is still pending.
- Direct actor `PermissionGrant` support and explanation trace are still pending.

### ADR
Full specification in `docs/future-forward/ADR-009.md`.

## Key Documentation Files

- `docs/agents/AGENT-CALIBRATION.md` — accumulated agent feedback, corrections, and task-specific conventions
- `docs/modules/MODULE-RESPONSIBILITIES.md` — detailed responsibility boundaries and cross-module matrix
- `CODING_IDENTITY.md` — broader design fingerprint
- `CLAUDE.md` — snapshot of this file for Claude Code; update both when conventions change

## Fundamentum Rule

A type belongs in `codex-fundamentum` only if it is generic, framework-agnostic, reusable by multiple modules, free of CMS/domain concepts, and small enough to remain stable. If a type mentions Site, ContentItem, ContentType, IndexDocument, AuditRecord, Workflow, User, Role, REST, AI, or persistence backend details, it does not belong in fundamentum.

## Backlog Classification

- **Active**: may implement when explicitly tasked
- **Near-future**: be aware, do not implement unless task says so
- **Future-forward**: document only, do not add code
