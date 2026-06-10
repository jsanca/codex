# ADR-010: Chronicon Audit Coverage Semantics

## Status

Accepted

## Context

`ContentItem` lifecycle is event-driven (see ADR-008). `codex-chronicon` extends that
event-driven model to all three core domain aggregates — `Site`, `ContentType`, and
`ContentItem` — by consuming domain events and projecting them into `AuditRecord` entries
for the audit trail.

The audit layer sits outside the domain core. Core services (`CodexSiteService`,
`CodexContentTypeService`, `CodexContentItemService`) own validation and state transitions.
`EventPublishing` decorators (`EventPublishingSiteService`,
`EventPublishingContentTypeService`, `EventPublishingContentItemService`) fire the
corresponding domain events after each successful operation. Chronicon subscribers
then consume those events and write audit records.

This ADR establishes which lifecycle operations are audited, what data each audit record
carries, and the rules subscribers must follow, so future work does not accidentally bypass
event-based audit, introduce repository reloads inside subscribers, or add audit logic
directly to core services.

---

## Decision

**Chronicon audit is event-driven. Subscribers build `AuditRecord` entries from event
fields only and must not reload domain entities from repositories.**

### Rationale

- Events carry all identity and contextual data needed for an audit record (`id`, `key`,
  `actor`, `occurredAt`).
- Reloading from a repository inside a subscriber would couple Chronicon to the core
  domain's persistence boundary and introduce hidden transaction dependencies.
- Deleted-entity subscribers must not reload because the hard delete has already completed
  when the event fires. The entity no longer exists in the repository.

---

## Site Audit Coverage

| Event                 | `AuditAction` | Subscriber                           |
|-----------------------|---------------|--------------------------------------|
| `SiteCreatedEvent`    | `CREATED`     | `SiteCreatedChroniconSubscriber`     |
| `SiteStartedEvent`    | `STARTED`     | `SiteStartedChroniconSubscriber`     |
| `SiteSuspendedEvent`  | `SUSPENDED`   | `SiteSuspendedChroniconSubscriber`   |
| `SiteArchivedEvent`   | `ARCHIVED`    | `SiteArchivedChroniconSubscriber`    |
| `SiteUnarchivedEvent` | `RESTORED`    | `SiteUnarchivedChroniconSubscriber`  |

All five Site operations that change state are audited. Read-only operations (`findByKey`,
`findByAlias`, `findAll`) do not emit events and are not audited.

---

## ContentType Audit Coverage

| Event                      | `AuditAction` | Subscriber                                |
|----------------------------|---------------|-------------------------------------------|
| `ContentTypeCreatedEvent`  | `CREATED`     | `ContentTypeCreatedChroniconSubscriber`   |
| `ContentTypeActivatedEvent`| `ACTIVATED`   | `ContentTypeActivatedChroniconSubscriber` |
| `ContentTypeArchivedEvent` | `ARCHIVED`    | `ContentTypeArchivedChroniconSubscriber`  |

`addField` and `removeField` operations do not currently emit domain events and are
therefore not audited. See Known Limitations below.

---

## ContentItem Audit Coverage

| Event                          | `AuditAction` | Subscriber                                    |
|--------------------------------|---------------|-----------------------------------------------|
| `ContentItemCreatedEvent`      | `CREATED`     | `ContentItemCreatedChroniconSubscriber`       |
| `ContentItemUpdatedEvent`      | `UPDATED`     | `ContentItemUpdatedChroniconSubscriber`       |
| `ContentItemPublishedEvent`    | `PUBLISHED`   | `ContentItemPublishedChroniconSubscriber`     |
| `ContentItemUnpublishedEvent`  | `UNPUBLISHED` | `ContentItemUnpublishedChroniconSubscriber`   |
| `ContentItemArchivedEvent`     | `ARCHIVED`    | `ContentItemArchivedChroniconSubscriber`      |
| `ContentItemRestoredEvent`     | `RESTORED`    | `ContentItemRestoredChroniconSubscriber`      |
| `ContentItemDeletedEvent`      | `DELETED`     | `ContentItemDeletedChroniconSubscriber`       |

All seven ContentItem lifecycle events are audited. The deleted subscriber explicitly
must not reload the entity — the hard delete has already completed when the event fires.

---

## AuditRecord Structure

Every audit record contains:

| Field        | Source                                    |
|--------------|-------------------------------------------|
| `id`         | Deterministic string from event fields (see below) |
| `action`     | Mapped from the event type                |
| `subject`    | Built from entity type, entity id, and entity key |
| `actorId`    | `event.actor().id()`                      |
| `occurredAt` | `event.occurredAt()`                      |
| `summary`    | Human-readable description of the transition |
| `metadata`   | Key/value map of identity fields          |

Subscribers never write `null` to any field. All fields are derived from event data
at the time of dispatch.

---

## AuditRecordId Generation

`AuditRecordIdGenerator` (package-private, `codex.chronicon.internal`) centralizes
deterministic id generation for Site, ContentType, and ContentItem lifecycle events.

| Method                                                       | Format                                                                       |
|--------------------------------------------------------------|------------------------------------------------------------------------------|
| `siteLifecycle(action, siteKey, occurredAt)`                 | `audit:site-{action}:{siteKey}:{epochMilli}`                                 |
| `contentTypeLifecycle(action, siteKey, ctKey, occurredAt)`   | `audit:content-type-{action}:{siteKey}:{ctKey}:{epochMilli}`                 |
| `contentItemLifecycle(action, siteKey, ctKey, itemKey, occurredAt)` | `audit:content-item-{action}:{siteKey}:{ctKey}:{itemKey}:{epochMilli}` |

**Exception:** `ContentItemPublishedChroniconSubscriber` inlines its id because the
publish event carries a `publishedRevisionId` that is included as an extra segment.
This makes the published id format unique and incompatible with the standard five-argument
helper:

```
audit:content-item-published:{siteKey}:{contentTypeKey}:{contentItemKey}:{revisionId}:{epochMilli}
```

All other ContentItem, ContentType, and Site lifecycle subscribers use
`AuditRecordIdGenerator`. The generated ids are deterministic: the same event fields
always produce the same id value.

---

## Runtime Wiring

### ChroniconRuntime

`ChroniconRuntime.buildSubscribers()` composes all current Chronicon audit subscribers
in a single `List.of(...)` call. No ServiceLoader, Spring context, or dynamic registration
is used.

Current subscriber count: **15**

- Site: 5 (created, started, suspended, archived, unarchived)
- ContentType: 3 (created, activated, archived)
- ContentItem: 7 (created, updated, published, unpublished, archived, restored, deleted)

### ConciliumRuntime

`ConciliumRuntime` composes `IndexRuntime` and `ChroniconRuntime` subscribers into a
single flat list registered with a shared `LocalCodexEventDispatcher`.

Current total: **19** (4 index + 15 Chronicon)

---

## Known Limitations and Future Follow-ups

### No pre-delete snapshot

`ContentItemDeletedChroniconSubscriber` records that a deletion occurred but cannot
capture the item's last known field values. A future snapshot strategy (e.g. a
pre-delete snapshot emitted as part of the event payload, or a snapshotting step before
deletion) would be needed to produce a rich deleted audit record.

### No ContentType schema mutation audit

`ContentTypeService.addField()` and `removeField()` do not emit domain events and
therefore produce no audit records. Once `ContentTypeFieldAddedEvent` and
`ContentTypeFieldRemovedEvent` are introduced, corresponding Chronicon subscribers
should be added.

### No workflow-level audit

The `codex-iter` workflow module is not yet implemented. When workflow operations
(approval, rejection, state machine transitions) are introduced, they should follow the
same event-driven audit pattern.

### No persistence-backed audit storage

`ChroniconRepository` currently has an in-memory (`MemoryChroniconRepository`)
implementation only. Audit records are lost on restart. A durable implementation backed
by PostgreSQL or an append-only event store is required before Chronicon is production-ready.

### No eventId-based AuditRecordId

Current ids are generated from event fields and a millisecond timestamp. Two events for
the same resource at the same millisecond would produce a collision. A future improvement
would use a stable event identity (e.g. a UUID assigned to each event at dispatch time)
as the authoritative id source, removing the timestamp dependency entirely.

### No multi-tenant or per-site audit isolation

All audit records are stored in a single flat repository with no per-site or per-tenant
partition. A future permission or isolation layer may need to scope `findBySubject` and
`findByActor` queries to a site context.
