# Codex Module Responsibilities

This document defines the responsibility boundary of each Codex module.

It exists to make the architectural map explicit before code is moved across module boundaries,
to prevent dependency cycles, god modules, and premature coupling.

> **Skeleton status (Task 27):** Maven/Jigsaw skeletons now exist for `codex-index`,
> `codex-archivum`, and `codex-chronicon`. Implementation migration is future work.

---

## Dependency Direction

The fundamental rule is that modules depend **inward** toward the core. The core must never depend
on adapter or projection modules.

```
codex-fundamentum
  ↑
  used by all modules

codex-codex
  → canonical core
  → depends on codex-fundamentum
  → must not depend on any module below

codex-index
codex-archivum
codex-chronicon
codex-custos
codex-porta
codex-iter
codex-nuntius
codex-speculum
codex-scriptorium
codex-illuminarium
codex-imaginarium
codex-olorin
  → may depend on codex-codex and codex-fundamentum as needed
  → core must never depend on them
```

**Composition and runtime wiring** may eventually live in a dedicated `codex-runtime` or
`codex-assembly` module once multiple modules need to be wired together from outside the core.
That module does not exist yet and must not be created until the need is concrete.

---

## codex-fundamentum

> The Fundamentum is the foundation beneath the manuscript.

### Responsibilities

- Shared primitives used across all modules
- `CodexEvent` interface and `CodexEventDispatcher` abstraction
- `CodexEventSubscriber` interface and `LocalCodexEventDispatcher` / `CompositeCodexEventDispatcher`
- `DeferredEventDispatcher` (buffering, transactional flush)
- `TransactionContext`
- `CacheEntry`, `CacheRegion`, `NoOpCacheRegion`, `ConcurrentMapCacheRegion`, `RecordingCacheRegion`
- `CodexExecutor` (virtual thread concurrency utilities)
- `Actor`, `ActorId`, `ActorType`
- Common exceptions (`NotFoundException`, `InvalidStateTransitionException`)
- `LifecycleParticipation` primitives

### Rules

- Must remain small and stable.
- Must not depend on any other Codex module.
- Must not contain CMS-domain concepts (sites, content types, content items).
- Must not contain content-specific logic.
- Provides reusable building blocks that any module can safely import.

---

## codex-codex

> The Codex core is the canonical manuscript model.

### Responsibilities

- Canonical domain model: `Site`, `ContentType`, `ContentTypeVersion`, `ContentItem`,
  `ContentRevision`, field definitions
- Commands: `CreateSiteCommand`, `CreateContentTypeCommand`, `CreateContentItemCommand`,
  `PublishContentItemCommand`, and all other lifecycle commands
- Lifecycle services: `SiteService`, `ContentTypeService`, `ContentItemService`,
  `ContentRevisionService`
- Domain events: `SiteCreatedEvent`, `ContentTypeCreatedEvent`, `ContentItemPublishedEvent`, etc.
- Domain validation and semantic exceptions (in `codex.codex.api.exception`)
- In-memory MVP repositories (`MemorySiteRepository`, `MemoryContentItemRepository`, etc.)
  — located here during MVP; will move to `codex-archivum` when durable persistence is introduced
- Event publishing decorators around canonical services
- Indexing foundation (`IndexDocument`, `IndexWriter`, `IndexDocumentId`, `IndexResourceType`,
  `ContentItemIndexDocumentMapper`, `ContentItemPublishedIndexingSubscriber`,
  `ContentItemProjectionSource`) — located here during MVP; will move to `codex-index` when
  module boundaries are finalized
- `CodexRuntime`: wires the in-memory MVP pipeline

### Rules

- Owns the **source of truth** for the CMS domain model and lifecycle semantics.
- Emits domain events; does not perform indexing directly.
- Does not perform search.
- Does not persist to specific durable backends directly.
- Does not expose REST or GraphQL.
- Does not own workflow orchestration.
- Does not own full identity or permission logic.
- Knows only minimal `Actor` information from `codex-fundamentum`.
- Must not depend on `codex-index`, `codex-archivum`, `codex-chronicon`, `codex-porta`,
  `codex-iter`, or any other adapter/projection module.

---

## codex-index

> The Index makes knowledge discoverable.

### Responsibilities

- Indexing abstractions: `IndexDocument`, `IndexDocumentId`, `IndexResourceType`, `IndexWriter`
- Search services and query abstractions
- Indexing projection subscribers (mapping domain events into index documents)
- Ranking and retrieval strategies
- Future adapters: Lucene, OpenSearch, Elasticsearch, myIR, vector indexes, hybrid search

### Rules

- Does not own canonical content; receives projections derived from domain events.
- May depend on `codex-codex` and `codex-fundamentum`.
- `codex-codex` must **never** depend on `codex-index`.
- Canonical services must not call indexing directly.
- Public content indexing should be driven by published-content events, not by lifecycle services.

> **Current boundary note:** The indexing foundation currently lives inside `codex-codex` during
> MVP. A future task will move indexing abstractions and subscribers into `codex-index` once module
> boundaries are finalized.

---

## codex-archivum

> The Archivum stores the manuscripts.

### Responsibilities

- Durable repository implementations (PostgreSQL, filesystem, object storage)
- Relational database adapters
- S3/MinIO-style binary asset storage
- Transaction adapters for durable backends
- Migration support
- Outbox storage (future)
- Storage of canonical structures, binary assets, generated artifacts, and large objects

### Rules

- Implements persistence; does not own domain lifecycle.
- Does not own search, cache, audit, workflow, rendering, or AI enrichment.
- Depends inward on core repository interfaces and entity contracts.
- Repository implementations must not contain business logic.

---

## codex-chronicon

> The Chronicon preserves the memory of the manuscript.

### Responsibilities

- Audit trails
- Event history and historical timelines
- Change narratives
- Temporal projections
- Restoration views
- Explaining how resources changed over time

### Rules

- Listens to domain events; records what happened.
- Does not own canonical lifecycle state.
- Does not replace domain events; consumes them.
- Does not perform indexing or search.
- Does not perform workflow orchestration.

> **Boundary note:** Audit is a **product/domain history concern**. Observability (metrics, traces,
> logs) is an **operations concern** and must remain separate.

---

## codex-custos

> The Custos guards the manuscript.

### Responsibilities

- Identity: users, groups, and teams
- Roles, permissions, and access policies
- Authorization decisions
- Future integration with external identity providers (LDAP, OAuth, SAML)

### Rules

- `codex-codex` should know only minimal `Actor` information (id, type, display name).
- Full user, role, and permission logic belongs in Custos.
- Canonical services may eventually consult authorization boundaries through explicit ports —
  never through direct Custos dependencies in the core.
- Do not leak external auth frameworks into `codex-codex` or `codex-fundamentum`.

---

## codex-porta

> The Porta is the gate.

### Responsibilities

- REST adapters and controllers
- GraphQL adapters
- WebSocket and SSE endpoints
- Request/response mapping
- Authentication boundary integration
- External API exposure (human, system, and agent-facing)

### Rules

- Depends on core services; core does not depend on Porta.
- Does not own domain behavior or canonical lifecycle.
- Does not own persistence.
- Maps external protocols to commands and services; must not bypass canonical services.

---

## codex-iter

> The Iter is the path through the manuscript.

### Responsibilities

- Workflow orchestration and process state
- Workflow steps, conditional branching, and waiting states
- Background task orchestration
- Continuation after domain events
- Actions triggered by workflow transitions

### Rules

- Listens to domain events; may execute commands through canonical services.
- May enter `WAITING_FOR_INPUT` states pending human or agent approval.
- Does not own canonical content state; `codex-codex` remains the source of truth.
- Does not bypass lifecycle services.
- Does not become the audit system.

> Iter orchestrates the path. Codex core records where the manuscript actually is.

---

## codex-nuntius

> The Nuntius carries messages.

### Responsibilities

- Human and agent interaction messages
- Prompts, replies, and approval requests
- Notifications and conversational state around operations
- Interaction channels between users, agents, and workflows

### Rules

- Transports interaction; does not own canonical content.
- Does not own workflow orchestration or AI reasoning.
- May support `codex-iter` when workflows wait for human or agent input.

> **Module relationships:**
> Nuntius transports interaction.
> Iter orchestrates process.
> Olórin reasons and proposes actions.
> Porta exposes external channels.

---

## codex-speculum

> The Speculum is the mirror.

### Responsibilities

- Synchronization with external editorial and storage surfaces
- Projections between Codex and: Google Drive, Git repositories, local filesystem folders,
  external document systems, static site projections
- Controlled ingestion of external changes into canonical Codex state
- Controlled reflection of Codex state outward to external surfaces
- Future sync conflict handling and idempotent adapter commands

### Rules

- Does not replace Codex as canonical model; external systems are reflections or controlled inputs.
- Must use canonical commands and services when ingesting changes — never write directly to
  repositories or bypass lifecycle.
- Should not weaken domain invariants.

> Speculum may eventually require strong idempotency semantics for commands invoked by external
> adapters.

---

## codex-scriptorium

> The Scriptorium is where custom rules are written.

### Responsibilities

- Scripting extensions and extension points
- Custom business rules and controlled automation
- Future lifecycle hooks
- Sandboxed extension execution (JavaScript or other scripting runtimes)

### Rules

- Extensions must not compromise the canonical model.
- Core prefers explicit services and event subscribers over scripting.
- Scripting is an **extension layer**, not the foundation of domain behavior.
- Dynamic execution must be controlled and sandboxed.

---

## codex-illuminarium

> The Illuminarium enriches the manuscript.

### Responsibilities

- Enrichment pipelines: embeddings, classification, metadata extraction
- Content analysis, summarization, and semantic enrichment
- AI-assisted annotations
- Feeding index, search, and RAG projections

### Rules

- Does not own canonical content.
- May write enrichment projections or metadata through controlled services.
- Should not perform lifecycle operations directly; must go through commands and services.

---

## codex-imaginarium

> The Imaginarium is the AI integration space.

### Responsibilities

- AI provider connectors and model integration
- Tool orchestration infrastructure
- Prompt and runtime abstractions
- Agent runtime concepts
- LLM-based capabilities shared by higher-level agents (Olórin, Illuminarium)

### Rules

- Provides AI infrastructure; does not own Codex domain lifecycle.
- Must not bypass canonical services.
- May support `codex-olorin` and `codex-illuminarium`.

---

## codex-olorin

> Olórin is the guide.

### Responsibilities

- Guided Codex-aware agent reasoning
- Proposing actions to users and asking for approval
- Executing tools through controlled interfaces
- Helping users create sites, content types, content items, workflows, and other resources
- Coordinating with Nuntius, Iter, Imaginarium, and Porta

### Rules

- Uses tools and services; does not own canonical state.
- Must respect permissions and workflow gates.
- Should ask for approval where required before executing consequential actions.

---

## Cross-Module Responsibility Matrix

| Module          | Owns Canonical State?    | Listens to Events?    | Exposes APIs?            | Uses AI? | Provides Infrastructure? |
|-----------------|--------------------------|-----------------------|--------------------------|----------|--------------------------|
| fundamentum     | No                       | Provides primitives   | No                       | No       | Yes                      |
| codex-codex     | Yes                      | Emits events          | Internal services only   | No       | Minimal                  |
| codex-index     | No                       | Yes                   | Search APIs (future)     | Possibly | Yes                      |
| codex-archivum  | No                       | Maybe                 | No                       | No       | Yes                      |
| codex-chronicon | No                       | Yes                   | History APIs (future)    | No       | Yes                      |
| codex-custos    | Identity state           | Maybe                 | Auth APIs (future)       | No       | Yes                      |
| codex-porta     | No                       | No / Maybe            | Yes                      | No       | Yes                      |
| codex-iter      | Workflow state           | Yes                   | Workflow APIs (future)   | No       | Yes                      |
| codex-nuntius   | Interaction state        | Maybe                 | Messaging APIs (future)  | Maybe    | Yes                      |
| codex-speculum  | Sync state               | Yes / Maybe           | Adapter APIs (future)    | Maybe    | Yes                      |
| codex-scriptorium | Extension state        | Maybe                 | Extension APIs (future)  | Maybe    | Yes                      |
| codex-illuminarium | Enrichment projections | Yes / Maybe          | Enrichment APIs (future) | Yes      | Yes                      |
| codex-imaginarium | AI runtime state       | Maybe                 | AI APIs (future)         | Yes      | Yes                      |
| codex-olorin    | Agent session state      | Yes / Maybe           | Agent interaction APIs   | Yes      | Yes                      |

---

## Implementation Classification

### Active / Current

The following responsibilities are implemented or actively being built:

- Canonical core: `Site`, `ContentType`, `ContentItem`, `ContentRevision`, lifecycle services
- Event pipeline: `DeferredEventDispatcher`, `CompositeCodexEventDispatcher`, `LocalCodexEventDispatcher`
- Content type / item / revision lifecycle
- Indexing foundation: `IndexDocument`, `IndexWriter`, `ContentItemPublishedIndexingSubscriber`,
  `ContentItemProjectionSource` (currently in `codex-codex`, will migrate to `codex-index`)
- Cache foundation: `CacheEntry`, `CacheRegion`, `ConcurrentMapCacheRegion`, `RecordingCacheRegion`
  (in `codex-fundamentum`)
- Local event subscribers for indexing projection

### Near-Future

The following should be kept in mind but not implemented without an explicit task:

- Moving indexing classes from `codex-codex` into `codex-index`
- `ContentSearchService`
- Cache decorator services (`CachingContentItemService`, `CachingContentItemProjectionSource`)
- Cache invalidation subscribers (`ContentItemCreatedEvent → CacheRegion.evict(...)`)
- Caffeine cache adapter
- Module skeleton creation (Maven modules for `codex-index`, `codex-archivum`, etc.)
- Runtime/assembly module discussion

### Future-Forward

Document only. Do not add code unless a task explicitly says so:

- Dynamic subscriber registry
- Tenant-aware index ids
- `PublishedPointer` as a separate entity
- Full `_system` site / multi-tenant registry
- Speculum sync with external surfaces
- Scriptorium scripting runtime (GraalJS)
- Iter workflow engine
- Nuntius messaging and interaction channels
- Illuminarium enrichment pipelines
- Imaginarium AI runtime and provider connectors
- Olórin agent
- Concordia cluster coordination

---

## Open Questions

These questions are documented here for awareness. Do not resolve them in code until a task
explicitly addresses each one.

1. **Runtime assembly module**: Should runtime composition eventually move to a dedicated
   `codex-runtime` or `codex-assembly` module as the number of wired modules grows?

2. **Indexing migration timing**: When should indexing abstractions and subscribers move from their
   current MVP location in `codex-codex` to `codex-index`?

3. **`IndexDocumentIdFactory`**: Should this abstraction wait until tenant-aware indexing is
   required, or should it be introduced as part of the indexing migration?

4. **`PublishedPointer`**: If it becomes a real entity, which module owns it — `codex-codex` or
   `codex-chronicon`?

5. **Cache invalidation ownership**: Which module owns cache invalidation subscribers — the core,
   a cache infrastructure module, or the specific projection modules that own the cached data?

6. **Custos integration**: How will `codex-custos` integrate with `codex-codex` without leaking
   full user and permission models into the core?

7. **Concordia**: Which module will own cluster coordination if it becomes necessary — a dedicated
   `codex-concordia` module, or a responsibility of `codex-fundamentum`?
