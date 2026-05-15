Review the current Chronicon/audit support for ContentItem lifecycle events.

Context:
ADR-008 documents the current ContentItem lifecycle semantics.

ContentItemService now supports:
- create
- update
- publish
- unpublish
- archive
- restore
- delete

EventPublishingContentItemService dispatches one lifecycle event per successful operation.

Known issue:
delete is currently a hard delete from ARCHIVED only. After deletion, subscribers cannot reload the item from storage. ADR-008 notes that future Chronicon/audit support may need either:
- an enriched ContentItemDeletedEvent with a pre-delete snapshot, or
- a pre-delete archival hook.

Goal:
Inspect the current Chronicon/audit infrastructure and report what is already implemented for ContentItem lifecycle events.

Please check:
1. Which ContentItem lifecycle events currently have Chronicon/audit subscribers.
2. Which lifecycle events are missing audit coverage.
3. Whether existing audit subscribers reload the entity after the event or rely only on event data.
4. Whether ContentItemDeletedEvent currently carries enough data for a useful deletion audit record.
5. What the smallest safe next task should be.

Constraints:
- Inspection/report only.
- Do not implement new subscribers yet.
- Do not modify ContentItemDeletedEvent yet.
- Do not introduce a snapshot model yet.
- Do not change production code unless there is a trivial typo or broken reference.

Expected output:
- Current Chronicon coverage summary.
- Missing coverage list.
- Risk assessment for hard-delete audit.
- Recommended next implementation task.