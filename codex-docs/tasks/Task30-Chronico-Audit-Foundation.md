# Task30: Chronicon Audit Foundation

## Objective

Introduce the first audit/history foundation in `codex-chronicon`.

Chronicon is the memory of the manuscript.

It does not own canonical lifecycle state.

It listens to domain events and records durable/queryable audit or history projections.

This task creates the minimal audit model and in-memory repositories needed for future Chronicon event subscribers.

Do not implement subscribers yet unless explicitly stated in this task.

Do not wire Chronicon into runtime yet.

Do not move domain events.

Do not move canonical entities.

## Decision Context

Task29 confirmed that there are no real Chronicon/audit pieces to move yet.

`CodexRuntime.EventRecorder` is only an in-memory helper for observation/testing. It is not Chronicon.

`ContentRevision`, `createdBy`, `updatedBy`, `owner`, and domain event timestamps remain in `codex-codex`.

Chronicon will build projections from events.

The current task creates the foundation for that projection model:

```text
Domain event
  -> future Chronicon subscriber
     -> AuditRecord
        -> ChroniconRepository
```
This task is intentionally small.

It creates the audit model and repository contracts, but does not yet implement event subscribers.

Scope

Implement in codex-chronicon:

AuditRecordId
AuditAction
AuditSubject
AuditRecord
ChroniconRepository
MemoryChroniconRepository
RecordingChroniconRepository
tests
README/documentation update if useful

Do not implement:

event subscribers
runtime wiring
audit search service
timeline query service
persistence adapters
database storage
outbox
REST
GraphQL
UI
workflow
observability
metrics
OpenTelemetry
Spring
JPA
migration tools
Module Dependencies

codex-chronicon should depend on:

codex-fundamentum

It should not depend on codex-index.

It should not depend on codex-porta.

It may not need to depend on codex-codex yet because this task does not create event subscribers.

If the audit model uses only generic strings/value objects plus ActorId from fundamentum, avoid adding a codex-codex dependency in this task.

Do not add codex-codex dependency unless absolutely necessary.

Package Layout

Suggested packages:

codex.chronicon.api
codex.chronicon.internal

API package:

AuditRecordId
AuditAction
AuditSubject
AuditRecord
ChroniconRepository

Internal package:

MemoryChroniconRepository
RecordingChroniconRepository

If existing module conventions differ, follow existing conventions, but preserve the API/internal separation.

Update module-info.java to export only the API package.

Do not export internal packages.

1. Create AuditRecordId

Create:

codex.chronicon.api.AuditRecordId

Use a Java record wrapping String value.

Requirements:

validate non-null
trim value
reject blank
static factory:
of(String value)
deterministic factory if simple:
forEvent(String eventId) only if domain events already expose event ids
otherwise do not invent event ids yet
no random UUIDs unless no deterministic input exists and tests require it

For now, of(String value) is enough.

2. Create AuditAction

Create:

codex.chronicon.api.AuditAction

Use an enum.

Initial values:

CREATED
UPDATED
PUBLISHED
ARCHIVED
STARTED
SUSPENDED
ACTIVATED
DEACTIVATED
DELETED
UNKNOWN

Requirements:

keep generic enough for multiple resource types
do not create content-specific actions yet unless needed
UNKNOWN is allowed for future generic event ingestion

Do not implement action mapping from domain events in this task.

3. Create AuditSubject

Create:

codex.chronicon.api.AuditSubject

Use a Java record.

Suggested shape:

String type
String id
String key

Requirements:

type must not be null, trim, reject blank
id may be null or blank? Prefer:
require non-null/non-blank if available
key may be nullable because not every subject has a human key
trim values
provide static factory:
of(String type, String id, String key)
of(String type, String id)
add JavaDoc

Suggested subject types for future use:

site
content-type
content-type-version
content-item
content-revision
workflow
user

Do not create an enum for subject type yet unless it clearly helps.

Reason:

Chronicon may eventually record external/sync/workflow subjects that are not known today.

4. Create AuditRecord

Create:

codex.chronicon.api.AuditRecord

Use a Java record.

Suggested shape:

AuditRecordId id
AuditAction action
AuditSubject subject
ActorId actorId
Instant occurredAt
String summary
Map<String, String> metadata

Requirements:

id must not be null
action must not be null
subject must not be null
actorId may be null only if system/anonymous events are possible; prefer requiring non-null if ActorId.system(...) exists
occurredAt must not be null
summary may be null and default to empty string
trim summary
metadata may be null and default to Map.of()
metadata keys must not be null or blank
metadata values must not be null
trim metadata keys
defensively copy metadata with Map.copyOf(...)
builder support if consistent with project style
copyOf(...) if consistent with project style
toString() should not dump huge metadata; include metadata count

Do not include full before/after diffs yet.

Do not include payload snapshots yet.

Do not include event body serialization yet.

5. Create ChroniconRepository

Create:

codex.chronicon.api.ChroniconRepository

Contract:

AuditRecord save(AuditRecord record)

Optional<AuditRecord> findById(AuditRecordId id)

List<AuditRecord> findBySubject(AuditSubject subject)

List<AuditRecord> findByActor(ActorId actorId)

List<AuditRecord> findByAction(AuditAction action)

List<AuditRecord> findAll()

Requirements:

use value objects
validate null arguments in implementations
returned lists should be immutable snapshots
if ordering is provided, prefer chronological order by occurredAt
no delete
no update
audit records are append-only

Do not add generic query DSL.

Do not add pagination yet.

Do not add timeline query service yet.

6. Create MemoryChroniconRepository

Create:

codex.chronicon.internal.MemoryChroniconRepository

Requirements:

implements ChroniconRepository
backed by ConcurrentHashMap<AuditRecordId, AuditRecord> or a small internal store
save(...) stores by id and returns the record
findById(...) exact lookup
findBySubject(...) filters by subject
findByActor(...) filters by actorId
findByAction(...) filters by action
findAll(...) returns immutable snapshot
query list results should be sorted by occurredAt, then id value for deterministic tests
validate null arguments
no delete/update
no TTL
no persistence
no external dependencies
7. Create RecordingChroniconRepository

Create:

codex.chronicon.internal.RecordingChroniconRepository

Purpose:

test helper
records saved audit records
delegates storage behavior to MemoryChroniconRepository

Requirements:

implements ChroniconRepository
delegates normal repository behavior to MemoryChroniconRepository
records calls to save(...)
exposes immutable snapshot:
List<AuditRecord> savedRecords()
exposes:
void clearRecording()
does not clear stored records when clearRecording() is called
no Mockito
validate nulls consistently
8. Tests

Add JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

AuditRecordId tests
rejects null
rejects blank
trims value
of(...) works
equality by value
AuditAction tests
enum contains expected values
AuditSubject tests
rejects null/blank type
rejects null/blank id if id is required
trims type/id/key
allows missing key if designed that way
factories work
equality by value
AuditRecord tests
rejects null id
rejects null action
rejects null subject
rejects null actorId if actorId is required
rejects null occurredAt
defaults null summary to empty string
trims summary
defaults null metadata to empty map
rejects null metadata key
rejects blank metadata key
rejects null metadata value
trims metadata keys
metadata accessor is immutable
builder works if implemented
copyOf preserves fields if implemented
toString includes metadata count, not full metadata dump
MemoryChroniconRepository tests
save returns record
findById returns saved record
findById returns empty when missing
findBySubject returns matching records
findByActor returns matching records
findByAction returns matching records
findAll returns all records
query results sorted by occurredAt then id
snapshots are immutable
null arguments rejected
save overwrites same id or preserves latest record; document whichever behavior is chosen
RecordingChroniconRepository tests
save records saved record
savedRecords snapshot immutable
clearRecording clears recording but not stored records
normal find behavior still works through delegate
null save rejected
9. Documentation

Update codex-chronicon/README.md if useful.

Mention:

Chronicon now has an audit foundation
it does not yet listen to events
audit subscribers are future work
durable persistence is future work
query/timeline APIs are future work

Optionally update:

codex-docs/modules/CHRONICON-BOUNDARY-NOTES.md

Add a small note that the audit foundation now exists.

Do not rewrite the whole document.

10. Future Work

Recommended future task:

Task31: Chronicon Event Subscribers

Potential scope:

SiteCreatedChroniconSubscriber
ContentItemPublishedChroniconSubscriber
generic domain-event-to-audit mapper if useful
local dispatcher integration tests

Other future tasks:

Task32: Chronicon Timeline Query Model
Task33: Chronicon Durable Persistence Adapter
Task34: Chronicon Runtime Assembly Wiring

Do not implement these in Task30.

11. Post-Task Report

After implementation, report:

files created
files modified
module-info changes
pom changes if any
tests added
Maven commands run
whether tests passed
intentional deviations
open questions
recommended follow-up tasks
12. Constraints
    Follow CLAUDE.md.
    Follow CODING_IDENTITY.md.
    Follow AGENT-CALIBRATION.md.
    Keep Chronicon separate from canonical core.
    Do not move domain events.
    Do not move canonical entities.
    Do not depend on codex-index.
    Avoid codex-codex dependency unless absolutely necessary.
    Do not implement subscribers yet.
    Do not wire runtime yet.
    Do not implement durable persistence.
    Do not implement search/query service.
    Do not implement observability.
    Do not add Spring.
    Do not add JPA.
    Do not add REST.
    Do not add workflow.
    Do not modify unrelated files.
    Keep comments and JavaDoc in English.
    Prefer small, explicit, testable classes.
    Run the smallest relevant Maven test command.