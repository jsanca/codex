# Codex Roadmap — Working Phases

This roadmap is a living guide for Codex. It is not intended to freeze the architecture or predict every implementation detail. Codex is being built through small, reviewed slices, and the roadmap should be updated as the project learns.

The purpose of this document is to preserve direction, reduce drift, and provide a shared map for Jonathan, Elo, Clio, Deep, and Elito.

## Operating Model

Codex development uses a triad of agents with distinct responsibilities:

* **Clio** implements production code and tests.
* **Deep** reviews architecture, behavior preservation, and risk without writing code.
* **Elito** documents, diagrams, and keeps the architecture understandable.
* **Elo** helps coordinate direction, task slicing, reviews, and architectural judgment.
* **Brio** Deep Research.
* **Jsanca** owns final decisions.

Core rule:

```text
Clio builds.
Deep audits.
Elito explains.
Jonathan decides.
Elo keeps the thread.
```

## Phase 1 — Finish Custos Core Authorization Loop

Goal: complete the first usable internal authorization loop for domain operations.

Current Custos foundation already includes:

* Actor / ActorType
* PermissionKey
* ResourceRef
* ResourceScope
* AccessDecision
* AccessDeniedException
* Permissions catalog
* RoleKey / Role
* PermissionGrant
* RoleAssignment
* BuiltInRoles
* PermissionResolver
* PermissionResolutionRequest
* PermissionResolutionSnapshot
* PermissionResolution
* DefaultPermissionResolver
* AccessDecisionRequest
* DefaultAccessDecisionService

Remaining work:

### 1.1 Review AccessDecisionService wiring DONE

Confirm that:

* AccessDecisionService delegates resolution to PermissionResolver.
* PermissionResolution is translated into AccessDecision.
* ResourceRef is preserved in AccessDecision.
* ResourceScope is used explicitly for resolution.
* ResourceRef and ResourceScope are not hidden behind magic mapping.
* CustosAgentSuperAdminInvariantViolationException propagates uncaught.
* No REST, persistence, JWT, SAML, OIDC, servlet, or Spring Security concerns leak into Custos.

### 1.2 Add or verify AccessDecisionService tests DONE

Expected tests:

* granted resolution becomes AccessDecision.Granted
* denied resolution becomes AccessDecision.Denied
* actor is preserved
* permission is preserved
* ResourceRef is preserved
* reason is preserved
* targetScope is used by resolver
* AGENT + SUPER_ADMIN invariant propagates
* null guards are covered

### 1.3 Define domain-specific permission services DONE

Introduce small service-level authorization helpers only if they reduce duplication and clarify intent.

Possible services:

* SitePermissionsService
* ContentTypePermissionsService
* ContentItemPermissionsService

Purpose:

```text
Domain operation intent
    -> PermissionKey + ResourceRef + ResourceScope
    -> AccessDecisionService
```

These services should not perform persistence, authentication, REST mapping, or workflow execution.

### 1.3b Align ContentType lifecycle permission vocabulary DONE

ContentType authorization now follows the domain lifecycle vocabulary:

* CONTENT_TYPE_DELETE removed
* contentType.delete removed
* canDeleteContentType removed
* CONTENT_TYPE_ARCHIVE added
* contentType.archive added
* canArchiveContentType added

### 1.4 Implement secured service decorators DONE

Add secured decorators around core domain services.

Shared rules:

* decorators call permission services or AccessDecisionService
* decorators fail before domain mutation
* denied decisions throw AccessDeniedException
* invariant violations propagate
* secured decorators remain transport-agnostic
* no HTTP status codes
* no framework security concepts

### 1.4A SecuredContentItemService DONE

Current behavior:

* create/findByKey/update/publish/unpublish/archive are gated
* delete/restore fail closed until permission semantics exist
* findByContentType/findAll remain pass-through pending filtering strategy

### 1.4A.1 Fail closed delete/restore DONE

SecuredContentItemService refuses delete and restore instead of delegating until permission semantics exist.

### 1.4A.2 Service-level authorization matrix test DONE

SecuredContentItemServiceAuthorizationMatrixTest uses the full real Custos chain with BuiltInRoles, RoleAssignment, PermissionResolver, AccessDecisionService, ContentItemPermissionsService, and SecuredContentItemService. It covers viewer, copywriter, reviewer, editor, site boundary, and AGENT + SUPER_ADMIN invariant scenarios.

### 1.4B SecuredContentTypeService NOT STARTED / NEXT

Add a secured decorator for ContentTypeService using archive vocabulary.

### 1.4C SecuredSiteService NOT STARTED

Add a secured decorator for SiteService lifecycle and read operations.

### 1.4D Secure runtime composition FUTURE / likely Phase 3

Wire secured decorators into runtime composition deliberately after the secured service surface is complete.

### 1.5 Decide direct actor grants later

Direct actor PermissionGrant support is not part of the immediate Custos core loop.

Current model stays:

```text
Actor -> RoleAssignment -> Role -> PermissionKey
```

Future direct grants may be considered only if role-based resolution proves insufficient.

## Phase 2 — Close Triad-Reported Gaps

Goal: align code, documentation, and project conventions.

Known gaps from the triad:

### 2.1 Blueprint polish

Update docs/CODEX-BLUEPRINT.md with:

* lifecycle state machines:

    * SiteStatus
    * ContentTypeStatus
    * ContentItemStatus
* AGENTS.md and CLAUDE.md reading path for coding agents
* direct actor grants moved to Deferred / Future
* AccessDecisionService wording verified against actual code
* optional note on internal exceptions in codex-codex

### 2.2 README update

Update root README so it no longer describes Codex as skeleton-only.

README should say:

* modular Maven/Jigsaw foundation exists
* Codex is in early implementation phase
* core domain, events, decorators, Observance, cache/index, and Custos have real implementation
* REST and persistence remain deferred
* docs live in docs/

### 2.3 CLAUDE.md / AGENTS.md sync

Keep both agent instruction files aligned.

Known convention updates:

* no Maven wrapper; mvn must be on PATH
* test conventions
* AssertJ preference
* sealed interface convention
* meaningful lambda names in domain/security code
* module map with test reference notes
* exception style
* Custos scope hierarchy and implication rules
* RoleAssignment must appear in key types

### 2.4 Exception placement review

Document or resolve the difference between:

```text
preferred convention: api.exception
current codex-codex reality: some exceptions live under internal.service
```

Do not move exceptions prematurely. First decide whether they are public contracts or internal implementation details.

### 2.5 DeferredEventDispatcher documentation

Ensure the event buffering pattern is clearly documented:

```text
service operation
    -> collect/buffer domain events
    -> complete operation
    -> dispatch events
    -> subscribers update cache/index/audit/observance
```

This is important for future contributors adding new operations.

## Phase 3 — Immediate Post-Custos Work

Goal: use Custos to harden the existing domain service layer before exposing Codex externally.

### 3.1 Secured runtime composition

Update runtime composition so secured decorators can be included deliberately.

Questions to answer:

* Is secure runtime the default?
* Is there a test/noop authorization mode?
* How are role assignments and role registry supplied?
* Does Concilium own composition only, or policy wiring too?

### 3.2 Audit and authorization integration

Decide how denied decisions, granted operations, and permission-related actions are recorded.

Important distinction:

* Observance = operational metrics
* Chronicon = domain/audit history
* logs = diagnostics

Avoid mixing them.

### 3.3 Cache/index consistency after secured operations

Verify that secured operations still preserve current behavior:

* denied operations do not mutate state
* denied operations do not emit lifecycle events
* denied operations do not invalidate cache
* denied operations do not update index
* successful operations preserve existing event/cache/index behavior

### 3.4 Permission-aware tests at service level

Add tests that prove domain service authorization behavior, not just resolver behavior.

Examples:

* viewer can read but not update
* copywriter can create/update draft but not publish
* reviewer can publish but not update
* site admin operates inside site boundary
* permissions do not cross site boundaries
* agent with super admin assignment triggers invariant failure

## Phase 4 — Stabilize Core CMS Slice

Goal: produce a coherent internal CMS slice before REST or persistence.

Target capabilities:

* create/manage sites
* create/manage content types
* version content type definitions
* create/update content items
* publish/unpublish/archive content items; delete/restore/purge semantics deferred to retention/archive policy work.
* emit domain lifecycle events
* observe service/cache/index behavior
* record Chronicon audit projections
* enforce Custos authorization
* index published content only

This phase is still internal/in-memory if needed.

Success criteria:

```text
A domain-level scenario can run end-to-end without REST or persistence.
```

Example scenario:

```text
Actor with EDITOR role on Site A
    -> creates content type
    -> creates content item
    -> updates draft
    -> publishes item
    -> event emitted
    -> cache invalidated
    -> index updated
    -> Chronicon records audit
    -> Observance records counters/timers
```

## Phase 5 — Archivum / Persistence Direction

Goal: decide and implement the first durable persistence strategy.

This is intentionally after domain and authorization hardening.

Questions:

* JPA, JDBC, jOOQ, or custom repositories?
* PostgreSQL JSONB usage?
* event/audit persistence shape?
* content type version persistence?
* content item/revision persistence?
* role assignment and permission persistence?
* transaction boundaries?
* migration strategy?

Deliverable:

* persistence ADR
* first Archivum-backed repositories
* integration tests
* domain behavior preserved

## Phase 6 — Porta / External Boundary

Goal: expose Codex through external APIs only after the core model is stable.

Possible scope:

* REST or GraphQL decision
* request/response DTOs
* error mapping
* authentication adapter boundary
* authorization context construction
* API tests
* no domain logic in controllers

Rules:

* Porta adapts external requests to domain operations.
* Porta does not own authorization semantics.
* Porta does not bypass Custos.
* Porta does not become the domain model.

## Phase 7 — Iter / Workflow

Goal: introduce workflow and orchestration after lifecycle, authorization, and persistence are stable.

Possible capabilities:

* review workflows
* approval states
* scheduled publishing
* editorial tasks
* workflow events
* workflow-aware permissions

Keep distinction:

```text
Lifecycle = domain state of content.
Workflow = process around changing that state.
```

## Phase 8 — Imaginarium and Olorin

Goal: add AI infrastructure and agent planning without weakening domain control.

Imaginarium:

* model integration
* retrieval helpers
* prompt/rendering infrastructure
* orchestration helpers
* AI interoperability

Olorin:

* domain-aware planning
* proposed operations
* permission change proposals
* explanation of intended actions
* capability-based execution

Rules:

* agents propose through explicit plans
* agents execute only through approved capabilities
* Custos remains the authorization gate
* StepUpApproval required for privileged security changes
* Chronicon records important applied changes

## Phase 9 — MVP Definition

Goal: define the smallest externally useful Codex MVP.

Possible MVP shape:

* headless structured content
* multi-site support
* content type + content item lifecycle
* persistence
* basic API through Porta
* authorization through Custos
* audit/history through Chronicon
* published-content indexing
* basic Observance
* minimal admin or API-first workflows

Not MVP-critical yet:

* full AI agent layer
* full workflow engine
* scripting
* semantic enrichment
* distributed infrastructure
* advanced UI
* production-grade marketplace/plugin system

## Phase 10 — Future Product Capabilities

Longer-term Codex direction:

* multi-language content modeling
* workflow-driven publishing
* AI-assisted editorial operations
* permission proposal and approval flows
* semantic enrichment
* scripting/customization
* richer search and discovery
* import/export pipelines
* content federation
* integration adapters
* operational dashboards
* agent-safe CMS automation

## Roadmap Rules

This roadmap should remain alive.

Update it when:

* a phase is completed
* a major design decision changes
* a triad report discovers a gap
* a future item becomes immediate
* implementation proves a planned path wrong

Do not update it for every tiny task.

The roadmap is meant to keep Codex aligned, not to prevent organic architectural evolution.
