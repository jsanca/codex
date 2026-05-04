# Chronicon Boundary Notes

> Discovery document for Task 29. No code was changed.

---

## 1. Purpose of This Document

Define what belongs to `codex-chronicon`, what must stay outside it, and what
the next implementation task should scope. This is boundary discovery, not implementation.

---

## 2. Current State of codex-chronicon

> Updated after Task 30 — Chronicon Audit Foundation.

| Aspect                | Status                                                                 |
|-----------------------|------------------------------------------------------------------------|
| Module exists         | Yes                                                                    |
| README exists         | Yes — updated to reflect current implementation                        |
| `module-info.java`    | Yes — `requires codex.fundamentum` only (no codex-codex dependency yet) |
| Placeholder classes   | Deleted in Task 30                                                     |
| Audit model           | `AuditRecordId`, `AuditAction`, `AuditSubject`, `AuditRecord`          |
| Repository contract   | `ChroniconRepository`                                                  |
| Implementations       | `MemoryChroniconRepository`, `RecordingChroniconRepository`            |
| Tests                 | Full coverage for all of the above                                     |
| Maven dependencies    | `codex-fundamentum` + JUnit Jupiter                                    |
| Event subscribers     | None yet — Task 31                                                     |
| Runtime wiring        | None yet — Task 34                                                     |

The README and module-info already express the correct boundary:
Chronicon listens to domain events and records audit/history projections.
It does not own the canonical lifecycle.

The placeholder classes (`ChroniconApiMarker`, `DefaultChroniconModule`) are inert
and can be deleted when the first real implementation task begins.

---

## 3. Search Results — Chronicon Candidates in the Codebase

### 3.1 Search terms used

`audit`, `Audit`, `history`, `History`, `timeline`, `Timeline`, `Chronicon`, `chronicon`,
`ChangeLog`, `EventHistory`, `ActivityLog`, `LogEntry`, `EventRecorder`, `recorder`.

### 3.2 Candidates found

**`CodexRuntime.EventRecorder` (private inner class)**

| Field          | Value |
|----------------|-------|
| File           | `codex-codex/src/main/java/codex/codex/internal/runtime/CodexRuntime.java` |
| Current module | `codex-codex` |
| Why it might belong to Chronicon | Records every dispatched domain event; is the seed of an audit/history store |
| Why it might not | It is currently a test/observation helper, not a durable store; it lives inside `CodexRuntime` as a private implementation detail; it does not persist, query, or project |
| Recommendation | **Keep where it is for now.** Replace later with a proper `ChroniconEventSubscriber` in `codex-chronicon` that writes `AuditRecord`s to a `ChroniconRepository`. The `EventRecorder` can be deleted or reduced to its test-observation role once Chronicon is real. |

**`ContentRevision` — revision history of content values**

| Field          | Value |
|----------------|-------|
| File           | `codex-codex/src/main/java/codex/codex/api/model/entity/ContentRevision.java` |
| Current module | `codex-codex` |
| Why it might seem Chronicon-related | Its JavaDoc mentions "revision history operations" as future work |
| Why it might not | `ContentRevision` is a canonical entity — it is the source of truth for content values at a point in time. Chronicon would project *from* it, not own it. |
| Recommendation | **Keep in `codex-codex`.** Chronicon may eventually build timeline views by listening to `ContentItemPublishedEvent` and reading revision metadata, but it does not own the revision entity. |

**Actor fields on canonical entities (`createdBy`, `updatedBy`, `owner`)**

| Field          | Value |
|----------------|-------|
| Files          | `ContentItem`, `ContentRevision`, `ContentType`, `ContentTypeVersion`, `Site` |
| Current module | `codex-codex` |
| Why it might seem Chronicon-related | These are the who/when data points that form an audit trail |
| Why it might not | They are canonical entity fields required by the domain, not Chronicon projections |
| Recommendation | **Keep in `codex-codex`.** Chronicon will *read* these values when building audit records from events, but does not own or move them. |

**`mvp-spec.md` § 10 — "History, chronicle, and events"**

| Field          | Value |
|----------------|-------|
| File           | `codex-docs/mvp-spec.md` |
| Content        | Describes what/who/when/action/transition chronicle as a strategic concern |
| Recommendation | This is the product intent that Chronicon implements. No code to move — it is documentation. |

**JavaDoc mentions of "audit context" in service interfaces**

| Field          | Value |
|----------------|-------|
| Files          | `ContentItemService`, `SiteService` |
| Content        | References `Actor` parameter as "audit context" |
| Recommendation | **Keep in `codex-codex`.** These are documentation comments describing why `Actor` is passed, not Chronicon logic. Chronicon subscribes downstream; it does not change the service interface. |

---

## 4. What Must Stay Outside Chronicon

### Domain events — stay in `codex-codex`

| Class | Reason |
|-------|--------|
| `SiteCreatedEvent` | Canonical fact emitted by codex-codex lifecycle |
| `SiteStartedEvent` / `SiteSuspendedEvent` / `SiteArchivedEvent` / `SiteUnarchivedEvent` | Same |
| `ContentTypeCreatedEvent` / `ContentTypeActivatedEvent` / `ContentTypeArchivedEvent` | Same |
| `ContentItemCreatedEvent` / `ContentItemPublishedEvent` | Same |

Chronicon listens to these events. It does not own or move them.

### Event infrastructure — stay in `codex-fundamentum`

| Class | Reason |
|-------|--------|
| `CodexEvent` | Shared domain event marker |
| `CodexEventDispatcher` | Generic dispatch contract |
| `CodexEventSubscriber` | Generic subscriber contract |
| `LocalCodexEventDispatcher` | Generic synchronous routing |
| `DeferredEventDispatcher` | Transaction-aware deferral — not Chronicon-specific |
| `CompositeCodexEventDispatcher` | Fan-out — not Chronicon-specific |
| `TransactionContext` | Transaction primitive |

### Actor primitives — stay in `codex-fundamentum`

`Actor` and `ActorId` are shared primitives used across all modules. Full identity and permissions belong to `codex-custos` when that module matures.

### Canonical timestamps — stay in `codex-codex` entities

`createdAt`, `updatedAt`, `occurredAt` belong to the entities and events that carry them. Chronicon records *derived* who/when projections, not canonical timestamps.

### Canonical lifecycle services — stay in `codex-codex`

`CodexSiteService`, `CodexContentTypeService`, `CodexContentItemService`, and all `EventPublishing*Service` decorators are the canonical lifecycle. Chronicon is a downstream observer, not a lifecycle owner.

---

## 5. What Chronicon Should Eventually Own

These do not exist yet. Listed here to inform the next task.

### Core model

| Type | Purpose |
|------|---------|
| `AuditRecord` | Immutable record of a domain event: who, what, when, on which resource |
| `AuditRecordId` | Strongly typed identity for `AuditRecord` |
| `AuditAction` | Enum or value object: CREATED, UPDATED, PUBLISHED, ARCHIVED, etc. |
| `AuditSubject` | The resource the action was performed on (typed reference: site, content type, content item) |
| `AuditMetadata` | Optional structured payload — transition, context fields |

### Repository

| Type | Purpose |
|------|---------|
| `ChroniconRepository` | Interface: append `AuditRecord`, query by subject/actor/time range |
| `MemoryChroniconRepository` | In-memory implementation for tests and early development |
| `RecordingChroniconRepository` | Test helper that captures records for assertion |

### Subscribers

| Type | Listens to | Purpose |
|------|-----------|---------|
| `SiteAuditSubscriber` | `SiteCreatedEvent`, `SiteStartedEvent`, `SiteSuspendedEvent`, `SiteArchivedEvent`, `SiteUnarchivedEvent` | Records site lifecycle audit trail |
| `ContentTypeAuditSubscriber` | `ContentTypeCreatedEvent`, `ContentTypeActivatedEvent`, `ContentTypeArchivedEvent` | Records content type lifecycle audit trail |
| `ContentItemAuditSubscriber` | `ContentItemCreatedEvent`, `ContentItemPublishedEvent` | Records content item audit trail |

### Future query model (near-future, not Task 30)

| Type | Purpose |
|------|---------|
| `TimelineEntry` | Projection of ordered audit events for a given subject |
| `AuditQuery` | Value object for filtering: by subject, actor, action, time range |
| `ChroniconQueryService` | Read model service for audit search and timeline views |

---

## 6. Audit vs Observability

These are separate concerns and must not be conflated.

| Concern | Owner | Audience | Durability |
|---------|-------|----------|------------|
| **Audit** | `codex-chronicon` | Users, admins, compliance | Durable, queryable, user-visible |
| **Observability** | Separate infra concern (future) | Operators, on-call engineers | Operational, not user-visible |

Audit answers: *"Who published this article, and when?"*  
Observability answers: *"Why did the publish request take 800ms?"*

Chronicon does not own observability. Observability may eventually live in a
`codex-telemetry` module or a `codex-fundamentum` telemetry abstraction.
That decision is future-forward.

---

## 7. Module Dependency Direction

When Chronicon is implemented, its dependency graph will be:

```
codex-chronicon
  requires codex.codex        (to consume domain events and read entity identity types)
  requires codex.fundamentum  (for CodexEventSubscriber, Actor, etc.)
```

Chronicon should **not** require `codex-index`, `codex-archivum`, or `codex-scriptorium`.
Domain events from `codex-codex` are the only input.

The `module-info.java` will need `requires codex.codex` added when Task 30 begins.

---

## 8. Recommended Follow-up Tasks

### Task 30 — Chronicon Audit Foundation (Active)

Suggested scope:

- `AuditRecord`, `AuditRecordId`, `AuditAction`, `AuditSubject`, `AuditMetadata`
- `ChroniconRepository` interface
- `MemoryChroniconRepository`
- `RecordingChroniconRepository`
- First subscriber: `ContentItemAuditSubscriber` for `ContentItemPublishedEvent`
- Tests for all of the above
- Update `module-info.java`: add `requires codex.codex`
- Update `pom.xml`: add `codex-codex` dependency
- Delete placeholder classes `ChroniconApiMarker` and `DefaultChroniconModule`

### Task 31 — Full Audit Subscriber Coverage (Near-future)

Extend subscribers to cover site and content type lifecycle events.

### Task 32 — Chronicon Timeline Query Model (Near-future)

`TimelineEntry`, `AuditQuery`, `ChroniconQueryService`.

### Task 33 — Observability Boundary ADR (Near-future)

Define where observability concerns live and whether `codex-fundamentum` provides
any telemetry abstraction.

---

## 9. Backlog Classification

`codex-chronicon` is classified as **Active**:
the event pipeline in `codex-codex` is mature enough to support audit subscribers now,
and the boundary is clearly defined.

Assembly wiring (how Chronicon subscribers connect to the runtime) remains an open question
consistent with the broader assembly discussion — see `CHRONICON-BOUNDARY-NOTES.md § 7`.
