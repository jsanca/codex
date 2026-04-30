# CLAUDE.md — Codex Project Guide

## What is Codex

Codex is a headless, multi-tenant, multi-language CMS designed as a disciplined modular monolith.
It is not a page-centric CMS. The center is the domain: structured content, revision-aware lifecycle,
workflow-driven operations, and extensibility points that allow the system to evolve without bloating the core.

The system is organized around a lore-inspired module map:

| Module                  | Responsibility                                      |
|-------------------------|-----------------------------------------------------|
| `codex-fundamentum`     | Foundational shared abstractions and base types     |
| `codex-codex`           | Central domain/kernel: content model and lifecycle  |
| `codex-chronicon`       | History, revision memory, publication narrative     |
| `codex-archivum`        | Storage abstraction layer (pluggable)               |
| `codex-index`           | Search and discoverability (pluggable)              |
| `codex-scriptorium`     | Scripting runtime and dynamic customization         |
| `codex-illuminarium`    | Enrichment and semantic enhancement                 |
| `codex-porta`           | External exposure: REST, GraphQL, integrations      |
| `codex-iter`            | Workflow and orchestration engine                   |
| `codex-custos`          | Identity, roles, permissions, access control        |
| `codex-imaginarium`     | AI integration infrastructure                       |
| `codex-olorin`          | Agent-oriented reasoning (named after Gandalf's Maiar name) |

---

## Architecture Principles

### Modular Monolith with Jigsaw

Codex uses Java Module System (Jigsaw). Every module has a `module-info.java`.

- Public contracts live in `codex.<module>.api`
- Implementation details live in `codex.<module>.internal`
- Never expose internal packages in `module-info.java`
- The goal: future extraction to microservices by swapping the binding behind the facade

```
module codex.codex {
    requires transitive codex.fundamentum;
    requires org.slf4j;
    exports codex.codex.api;
    exports codex.codex.api.model.identity;
    exports codex.codex.api.model.value;
    exports codex.codex.api.model.entity;
}
```

### Core Must Stay Clean

`codex-codex` must not import:
- Spring Boot, JPA, JDBC, HTTP, or any infrastructure framework
- Persistence annotations or storage mappings
- REST controllers or servlet concerns
- Authentication mechanisms

Infrastructure belongs in adapters or runtime modules, never in the core.

### Decorator Pipeline for Cross-Cutting Concerns

Services grow through composition, not inheritance:

```
TransactionalSiteService
  -> LockingSiteService
    -> AuditingSiteService
      -> EventPublishingSiteService
        -> CodexSiteService
```

Each decorator wraps the interface and adds one concern. The core service stays focused on business logic.

### Events vs Hooks

- **Events** = facts that already happened (`SiteCreatedEvent`, `ContentItemPublished`)
- **Hooks** = extension points around something happening (`beforeSave`, `afterPublish`)

Keep them conceptually separate. Both are first-class extensibility mechanisms.

---

## Java Style and Coding Standards

### Target: Java 25

- Use Java 25 features where appropriate
- Prefer **virtual threads** over platform threads
- Use platform threads only when the task is CPU-bound and would waste a carrier thread
- Use `ScopedValue` instead of `ThreadLocal` whenever possible

### Immutability First

- **Always prefer `record`** for domain objects, value objects, commands, events, and identities
- Use `List.copyOf()`, `Map.copyOf()`, `Set.copyOf()` in canonical constructors
- Defensive copies on all collections received from outside
- Immutable objects do not need synchronization

```java
public record SiteKey(String value) {
    public SiteKey {
        Objects.requireNonNull(value, "site key cannot be null");
        value = value.trim().toLowerCase();
        if (value.isBlank()) throw new IllegalArgumentException("site key cannot be blank");
    }

    public static SiteKey of(final String value) {
        return new SiteKey(value);
    }
}
```

### Builder + copyOf + from Pattern

Every non-trivial entity or record should provide:
- `static Builder builder()` — fluent construction
- `static T copyOf(T source)` — returns a builder pre-populated from an existing instance
- `static T from(...)` — named factory methods where semantically useful
- `static T of(...)` — for simple value types

```java
public static Builder copyOf(final Site site) {
    Objects.requireNonNull(site, "site cannot be null");
    return builder()
            .id(site.id())
            .key(site.key())
            .displayName(site.displayName())
            .status(site.status());
}
```

### Primitives Over Wrappers

- Use `int`, `long`, `boolean`, `double` — not `Integer`, `Long`, `Boolean`, `Double`
- Exception: when `null` is semantically meaningful or required by a generic type
- This prepares the codebase for Project Valhalla and avoids the Object Tax

### Interfaces Always

- Every service must be an interface
- Every repository must be an interface
- Exception: utility classes (pure static methods, no state)
- Composition over inheritance — use inheritance only when the real-world relationship is IS-A (e.g., `Animal → Dog`)

### Naming

- **No single-letter variable names** except `i` in for loops
- Variable names must express intent: `siteKey`, `createSiteCommand`, `targetStatus`
- Method names must express semantics: `findByKey`, `changeStatus`, `throwSiteAlreadyExistException`
- Enum values when the domain has a closed set of states

### Enums

Use enums for:
- Lifecycle statuses (`SiteStatus`, `EditorialState`, `ContentTypeVersionStatus`)
- Field types (`FieldType`, `FieldConstraintType`)
- Actor types (`ActorType`)
- Any closed set of domain values

### Validation

- Validate at the boundary: constructors, command handlers, service entry points
- Use `Objects.requireNonNull(value, "meaningful message")` for required references
- Throw domain exceptions, not raw `IllegalArgumentException`, for state violations
- Use `Optional<T>` when absence is a valid domain outcome (not for error signaling)

```java
public Site create(final CreateSiteCommand createSiteCommand, final Actor actor) {
    Objects.requireNonNull(createSiteCommand, "createSiteCommand must not be null");
    Objects.requireNonNull(actor, "actor must not be null");
    // ...
}
```

### Method Size

- **Methods should not exceed ~20 lines of logic** (excluding log statements)
- If a method grows beyond that, extract a well-named private method
- Log statements are encouraged and do not count toward the limit

### Logging

- **Always log** at service boundaries and state transitions
- Use `LOGGER.debug(...)` for operational detail
- Use `LOGGER.info(...)` for significant state changes
- Use `LOGGER.warn(...)` for recoverable anomalies
- Use `LOGGER.error(...)` for failures
- Logger must be `private static final` using SLF4J

```java
private static final Logger LOGGER = LoggerFactory.getLogger(CodexSiteService.class);
```

### JavaDoc

- All public interfaces, records, and classes must have JavaDoc
- Document intent, not implementation
- Document parameters and return values for public methods
- Keep JavaDoc in English

---

## Domain Model Conventions

### Identity Types

Every entity has a strongly typed identity record. Pattern:

```java
public record SiteId(String value) {
    public SiteId {
        Objects.requireNonNull(value, "SiteId value cannot be null");
        value = value.trim();
        if (value.isBlank()) throw new IllegalArgumentException("SiteId value cannot be blank");
    }
    public static SiteId of(String value) { return new SiteId(value); }
    public static SiteId generate() { return new SiteId(UUID.randomUUID().toString()); }
}
```

Prefer **deterministic identity** where possible (e.g., `UUID.nameUUIDFromBytes`):

```java
// SiteIdentityGenerator: same SiteKey always produces same SiteId
final UUID uuid = UUID.nameUUIDFromBytes(("site:" + source.key().value())
        .getBytes(StandardCharsets.UTF_8));
```

### Commands

Commands are immutable records expressing intent. Not GoF Command pattern — they carry data only:

```java
public record CreateSiteCommand(SiteKey key, String displayName, SiteStatus status, Set<SiteAlias> aliases) {
    public CreateSiteCommand {
        Objects.requireNonNull(key, "key cannot be null");
        aliases = Set.copyOf(aliases);
    }
    public static CreateSiteCommand of(final SiteKey key, final String displayName) { ... }
}
```

### Events

Events are immutable records describing facts that already happened. Must implement `CodexEvent`:

```java
public record SiteCreatedEvent(SiteId id, SiteKey key, Actor actor, Instant instant) implements CodexEvent {
    @Override
    public Instant occurredAt() { return instant; }
}
```

### Entities

Entities are records with a builder, `copyOf`, and validation in the canonical constructor.
All collections are defensive copies. Timestamps default to `Instant.now()` if null.

### State Machines

State transitions must be explicit and validated. Use a dedicated method:

```java
private boolean isValidTransition(final SiteStatus current, final SiteStatus target) {
    return switch (current) {
        case STARTED  -> target == SiteStatus.SUSPENDED;
        case SUSPENDED -> target == SiteStatus.STARTED || target == SiteStatus.ARCHIVED;
        case ARCHIVED  -> target == SiteStatus.SUSPENDED;
    };
}
```

Current `SiteStatus` machine:
```
STARTED ⟷ SUSPENDED ⟷ ARCHIVED
```
No skipping steps. `unarchive` returns to `SUSPENDED`, not `STARTED`.

---

## Repository Conventions

- Repositories are **dumb** — CRUD only, no business logic
- Repositories never call other repositories
- Services are **smart** — business logic, validation, orchestration
- Services may call repositories and other services
- Start with `MemoryXxxRepository` before adding real persistence
- Repository interface lives in `codex.<module>.internal.repository` (internal, not exported)

```java
public interface SiteRepository {
    Site save(Site site);
    Optional<Site> findByKey(SiteKey siteKey);
    Optional<Site> findByAlias(SiteAlias alias);
    boolean existsByKey(SiteKey siteKey);
    List<Site> findAll();
    boolean deleteByKey(SiteKey siteKey);
}
```

---

## Service Conventions

- Service interface lives in `codex.<module>.api.model.service` (exported)
- Implementation lives in `codex.<module>.internal.service` (not exported)
- Every public method receives an `Actor` for audit context
- Constructor injection always, no field injection
- `Objects.requireNonNull` for every constructor parameter

```java
public final class CodexSiteService implements SiteService {
    private final SiteRepository siteRepository;
    private final Clock clock;
    private final IdentityGenerator<CreateSiteCommand, SiteId> siteIdentityGenerator;

    protected CodexSiteService(final SiteRepository siteRepository, final Clock clock) {
        this.siteRepository = Objects.requireNonNull(siteRepository, "siteRepository must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.siteIdentityGenerator = new SiteIdentityGenerator();
    }
}
```

---

## Cross-Cutting Values

Every design decision should consider these properties:

| Value               | What it means in practice                                      |
|---------------------|----------------------------------------------------------------|
| Scalable            | Stateless services, cluster-safe operations                    |
| Extensible          | Extension points over modification, open/closed principle      |
| Customizable        | Hooks, events, scripting (Scriptorium) for custom behavior     |
| Observable          | SLF4J logs, metrics hooks, structured events                   |
| Auditable           | Every state change carries an `Actor` and a timestamp          |
| Cluster Aware       | No JVM-only assumptions, ShedLock or distributed locks         |
| Event Aware         | Domain events as first-class citizens via `CodexEventDispatcher` |
| Cacheable           | Caffeine (L1) + Valkey (L2), event-driven invalidation         |
| Treeable            | Content navigable through site/folder/item paths               |
| Transactional       | Transactional by default, Sagas where distributed consistency needed |

---

## Technology Stack (MVP)

| Layer            | Technology                                      |
|------------------|-------------------------------------------------|
| Framework        | Spring Boot (disciplined, adapters only)        |
| Database         | PostgreSQL (source of truth)                    |
| Vector search    | pgvector (initial, flexible)                    |
| Search           | OpenSearch (lexical search)                     |
| Assets           | S3-compatible object storage                    |
| Local cache      | Caffeine                                        |
| Distributed cache| Valkey                                          |
| Scripting        | GraalJS (future, Scriptorium)                   |
| Concurrency      | Virtual threads (Java 25)                       |

Spring Boot annotations (`@Transactional`, `@Component`, `@Autowired`) must never appear in `codex-codex` or `codex-fundamentum`. They belong in adapter/runtime modules only.

---

## What Claude Should Never Do

- Add Spring annotations to core domain classes
- Add persistence annotations (JPA, Hibernate) to domain entities
- Use `ThreadLocal` when `ScopedValue` is available
- Use wrapper types (`Integer`, `Long`) when primitives suffice
- Write repositories with business logic
- Skip validation in constructors or service entry points
- Use single-letter variable names (except `i` in loops)
- Create mutable domain objects
- Introduce inheritance where composition works
- Skip logging on state-changing operations
- Modify existing behavior without being explicitly asked
- Perform broad refactors unless explicitly requested

---

## What Claude Should Always Do

- Read existing code before writing new code
- Follow the existing package and naming conventions
- Write JavaDoc for all public types and methods
- Add `Objects.requireNonNull` for every required parameter
- Use `record` for new value objects, commands, events, and identities
- Provide builder + `copyOf` + factory methods for entities
- Write focused methods (≤20 lines of logic)
- Log at service entry points and state transitions
- Prefer `ScopedValue` over `ThreadLocal`
- Prefer primitives over wrappers
- Use enums for closed sets of domain values
- Keep changes small and focused
- Ask before making architectural decisions that affect module boundaries