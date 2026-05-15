# ADR-008: ContentItem Lifecycle Semantics

## Status

Accepted

## Context

`ContentItemService` now exposes a complete set of lifecycle operations covering creation,
editorial updates, publication, and retirement. The operations touch both `ContentItem` status
and `ContentRevision` status, and they drive event-based cache invalidation. This ADR captures
the rules governing each transition so future tasks do not accidentally change the semantics.

---

## Lifecycle State Machine

```
                  ┌──────────┐
       create ──► │  DRAFT   │◄── restore
                  └────┬─────┘
                       │ publish
                       ▼
                  ┌──────────┐
                  │PUBLISHED │
                  └────┬─────┘
                       │ unpublish
                       ▼
                  ┌──────────┐
       archive ──►│ ARCHIVED │──── delete (hard delete, permanent)
  (from DRAFT     └──────────┘
   or PUBLISHED)
```

---

## Decision

### create

- Produces a `ContentItem` in `DRAFT` status.
- Creates a `ContentRevision` in `WORKING` status.
- Validates field values against the latest published `ContentTypeVersion`.
- `currentPublishedRevisionId` is `null` on creation.

### update

- Valid only on `DRAFT` items (the working revision is `WORKING`).
- Updates field values on the existing `ContentRevision` (no new revision created at this stage).
- Updates `updatedBy` / `updatedAt`.
- Does not change item status.

### publish

- Transitions `ContentItem` from `DRAFT` → `PUBLISHED`.
- Sets the working `ContentRevision` to `ContentRevisionStatus.PUBLISHED`.
- Sets `currentPublishedRevisionId` to point to that revision (same as `currentWorkingRevisionId`).
- Idempotent: re-publishing an already-published item whose pointers already match returns the
  existing item without mutation.
- Rejects `ARCHIVED` items. Rejects items whose working revision is already `ARCHIVED`.

### unpublish

- Valid only from `PUBLISHED`.
- Transitions `ContentItem` back to `DRAFT`.
- Reverts the published `ContentRevision` to `ContentRevisionStatus.WORKING`.
- Clears `currentPublishedRevisionId`.

### archive

- Valid from `DRAFT` or `PUBLISHED`. Rejects `ARCHIVED` items.
- Transitions `ContentItem` to `ARCHIVED`.
- If the item was `PUBLISHED`: sets the published `ContentRevision` to
  `ContentRevisionStatus.ARCHIVED` and clears `currentPublishedRevisionId`.
- If the item was `DRAFT`: the working `ContentRevision` is left as `WORKING`; no revision
  status change is needed.

### restore

- Valid only from `ARCHIVED`. Rejects `DRAFT` and `PUBLISHED` items.
- Transitions `ContentItem` back to `DRAFT`.
- If the working `ContentRevision` is `ARCHIVED` (produced by the PUBLISHED → ARCHIVED path),
  reverts it to `ContentRevisionStatus.WORKING`.
- Does **not** republish the item. `currentPublishedRevisionId` remains `null`.
- The full PUBLISHED → ARCHIVED → RESTORED → PUBLISHED cycle is valid.

### delete

- Valid **only from `ARCHIVED`**. Rejects `DRAFT` and `PUBLISHED` items.
- **Hard delete**: permanently removes the `ContentItem` record from storage.
- After deletion, `findByKey` returns `Optional.empty()` for the same identity.
- Associated `ContentRevision` records are currently **not** deleted (revision orphans are
  accepted; see Known Limitations).

---

## Event Publishing

`CodexContentItemService` does **not** dispatch domain events directly. It owns domain validation,
lifecycle transitions, and repository writes only.

`EventPublishingContentItemService` wraps it and dispatches one event per successful operation:

| Operation  | Event                        |
|------------|------------------------------|
| create     | `ContentItemCreatedEvent`    |
| update     | `ContentItemUpdatedEvent`    |
| publish    | `ContentItemPublishedEvent`  |
| unpublish  | `ContentItemUnpublishedEvent`|
| archive    | `ContentItemArchivedEvent`   |
| restore    | `ContentItemRestoredEvent`   |
| delete     | `ContentItemDeletedEvent`    |

Events are dispatched **only** after the delegate succeeds. If the delegate throws, no event is
dispatched. In production the dispatcher is `DeferredEventDispatcher`, which buffers events inside
a transaction and delivers them on commit; a rollback discards all buffered events.

For `delete`, `EventPublishingContentItemService` performs a `findByKey` before delegating so it
can capture the identity fields needed for the event — the item no longer exists after the
hard delete completes.

---

## Cache Invalidation

Cache invalidation is **event-driven**. A `ContentItemCacheInvalidationSubscriber` is registered
for every lifecycle event. When the event fires, the subscriber evicts the corresponding
`ContentItemCacheKey` from the `CacheRegion`.

All seven subscribers are wired into `CodexRuntime` via a dedicated `LocalCodexEventDispatcher`
that sits between the `EventRecorder` and any external dispatcher in the composite:

```
CompositeCodexEventDispatcher
  [EventRecorder, cacheDispatcher, externalDispatcher]
```

This ordering ensures cache evictions happen before external modules receive the event.

---

## Known Limitations and Future Follow-ups

### Hard delete and audit snapshots

Because `delete` is a hard delete, any Chronicon/audit subscriber that reacts to
`ContentItemDeletedEvent` and attempts to reload the item will find nothing — the item is already
gone by the time the event fires.

Two mitigation strategies are under consideration (neither is implemented):

1. **Enrich `ContentItemDeletedEvent`** with a pre-delete identity/metadata snapshot so
   subscribers do not need to reload the item.
2. **Pre-delete archival hook** — persist a deletion snapshot to Chronicon or Archivum before
   the hard delete executes.

Neither strategy should be added until the Chronicon deletion subscriber is tasked.

### Revision orphans

Hard-deleting a `ContentItem` leaves its associated `ContentRevision` records in storage.
The orphans are unreachable (no item holds their IDs) and will be cleaned up when a real
persistence layer with cascading deletes is introduced. This is out of scope for the current
in-memory model.

### Unpublish-after-restore semantics

After a PUBLISHED → ARCHIVED → RESTORED cycle the working revision is `WORKING` and
`currentPublishedRevisionId` is `null`. The item can be re-published normally. No further
revision reconciliation is needed.

### Future lifecycle extensions

The following operations may be added in future tasks but are **not** part of this ADR:

- Workflow-gated publish (requires `codex-iter`)
- Soft delete / tombstone status
- Bulk lifecycle operations
- Schedule-based publish / unpublish
