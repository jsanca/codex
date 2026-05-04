````markdown id="task32-chronicon-event-subscribers"
# Task32: Chronicon Event Subscribers

## Objective

Introduce the first Chronicon event subscribers.

Chronicon should listen to canonical domain events from `codex-codex` and write audit records into `ChroniconRepository`.

This task proves the Chronicon projection pattern:

```text
domain event
  -> Chronicon subscriber
     -> AuditRecord
        -> ChroniconRepository
````

Chronicon does not own canonical lifecycle state.

Chronicon records audit/history projections derived from domain events.

## Decision Context

Task30 created the Chronicon audit foundation:

* `AuditRecordId`
* `AuditAction`
* `AuditSubject`
* `AuditRecord`
* `ChroniconRepository`
* `MemoryChroniconRepository`
* `RecordingChroniconRepository`

Task31 cleaned the public projection/read boundary for indexing.

Now Chronicon should start using the existing event subscriber infrastructure from `codex-fundamentum`.

This task should mirror the projection approach already used by `codex-index`:

```text
codex-index
  ContentItemPublishedEvent -> IndexDocument -> IndexWriter

codex-chronicon
  Domain event -> AuditRecord -> ChroniconRepository
```

Keep this task small.

Do not implement every possible audit event.

Start with a small representative set of events.

## Scope

Implement in `codex-chronicon`:

* event-to-audit mapper/helper if useful
* `SiteCreatedChroniconSubscriber`
* `ContentTypeCreatedChroniconSubscriber`
* `ContentItemPublishedChroniconSubscriber`
* tests
* documentation update if useful

Do not implement:

* runtime wiring
* `ChroniconRuntime`
* `CodexModuleRuntime`
* ServiceLoader
* Spring
* REST
* GraphQL
* durable persistence
* audit query service
* timeline query service
* observability/metrics
* cache invalidation
* workflow
* search/indexing changes
* all domain event subscribers

## Module Dependencies

This task will require `codex-chronicon` to depend on `codex-codex`.

Update:

```text
codex-chronicon/pom.xml
codex-chronicon/src/main/java/module-info.java
```

Expected module declaration direction:

```java
module codex.chronicon {
    requires codex.fundamentum;
    requires codex.codex;

    exports codex.chronicon.api;
}
```

Do not export internal packages.

Do not make `codex-codex` depend on `codex-chronicon`.

## Package Layout

Use existing packages:

```text
codex.chronicon.api
codex.chronicon.internal
```

Subscribers should live in:

```text
codex.chronicon.internal
```

Do not expose subscriber classes as API unless required by the current module/testing convention.

## 1. Subscriber Pattern

Each subscriber should implement:

```text
CodexEventSubscriber<E extends CodexEvent>
```

from `codex-fundamentum`.

Each subscriber should:

* declare its event type
* validate constructor dependencies
* validate event non-null
* create an `AuditRecord`
* save it to `ChroniconRepository`

Example conceptual shape:

```java
public final class ContentItemPublishedChroniconSubscriber
        implements CodexEventSubscriber<ContentItemPublishedEvent> {

    private final ChroniconRepository repository;

    @Override
    public Class<ContentItemPublishedEvent> eventType() {
        return ContentItemPublishedEvent.class;
    }

    @Override
    public void handle(final ContentItemPublishedEvent event) {
        repository.save(...);
    }
}
```

Do not use annotations.

Do not use reflection.

Do not use Spring.

Do not use ServiceLoader.

## 2. Audit Record Id Strategy

Use deterministic ids where possible.

Since domain events may not have event ids yet, derive audit ids from event data.

Suggested formats:

### Site created

```text
audit:site-created:{siteKey}:{occurredAt}
```

### Content type created

```text
audit:content-type-created:{siteKey}:{contentTypeKey}:{occurredAt}
```

### Content item published

```text
audit:content-item-published:{siteKey}:{contentTypeKey}:{contentItemKey}:{publishedRevisionId}:{occurredAt}
```

Requirements:

* use `AuditRecordId.of(...)`
* keep format simple
* do not introduce event ids in this task
* do not add UUIDs unless absolutely necessary
* if timestamps include characters that make ids awkward, use `occurredAt.toEpochMilli()` or another stable string form

Future work may introduce canonical event ids.

## 3. Audit Subject Mapping

Use `AuditSubject` to identify the resource affected.

Suggested subject types:

```text
site
content-type
content-item
```

### SiteCreatedEvent

Subject:

```text
type = "site"
id = site key value or site id if available
key = site key value
```

Use whatever the event exposes.

If event exposes only `SiteKey`, use that as both `id` and `key`.

### ContentTypeCreatedEvent

Subject:

```text
type = "content-type"
id = content type id value if available
key = content type key value
```

If event exposes site key, include it in metadata.

### ContentItemPublishedEvent

Subject:

```text
type = "content-item"
id = content item id value
key = content item key value
```

Metadata should include published revision id.

Do not create `AuditSubjectType` enum yet.

String subject types are intentional for future extensibility.

## 4. Audit Action Mapping

Map events to actions:

```text
SiteCreatedEvent -> AuditAction.CREATED
ContentTypeCreatedEvent -> AuditAction.CREATED
ContentItemPublishedEvent -> AuditAction.PUBLISHED
```

Do not add more actions unless existing enum lacks them.

Do not modify `AuditAction` unless required.

## 5. Actor Mapping

Use the actor from the domain event.

Audit records require non-null `ActorId`.

If events expose `Actor actor`, use:

```java
event.actor().id()
```

If an event does not expose actor, use a system actor id only if a stable convention already exists.

Do not make `actorId` optional.

Do not invent full user/permission logic.

Custos remains future work.

## 6. Timestamp Mapping

Use event `occurredAt`.

Audit record `occurredAt` should equal event `occurredAt`.

Do not use `Instant.now()` in subscribers.

Chronicon should preserve event time, not subscriber handling time.

## 7. Summary Mapping

Create short human-readable summaries.

Examples:

```text
Created site {siteKey}
Created content type {contentTypeKey} in site {siteKey}
Published content item {contentItemKey} in content type {contentTypeKey}
```

Requirements:

* keep summaries simple
* do not include content values
* do not include large payloads
* do not localize yet

## 8. Metadata Mapping

Add useful string metadata.

### SiteCreatedEvent metadata

Possible keys:

```text
siteKey
```

### ContentTypeCreatedEvent metadata

Possible keys:

```text
siteKey
contentTypeKey
contentTypeId
```

Use only fields available on the event.

### ContentItemPublishedEvent metadata

Suggested keys:

```text
siteKey
contentTypeKey
contentTypeVersionId
contentItemKey
contentItemId
publishedRevisionId
```

Do not include content values.

Do not include full revision payloads.

Do not include search/indexing data.

## 9. Optional Mapper Helper

If subscriber code becomes repetitive, introduce a small internal helper:

```text
AuditRecords
```

or:

```text
ChroniconAuditRecordFactory
```

Suggested package:

```text
codex.chronicon.internal
```

Requirements:

* internal only
* no framework dependency
* no runtime wiring
* no ServiceLocator
* only maps known events to `AuditRecord`

Do not overbuild a generic event-to-audit framework yet.

If simple duplication is clearer, skip the helper.

## 10. Local Dispatcher Integration Test

Add an integration-style test in `codex-chronicon` proving the subscribers work with `LocalCodexEventDispatcher`.

Example flow:

```text
RecordingChroniconRepository
  <- SiteCreatedChroniconSubscriber
  <- ContentTypeCreatedChroniconSubscriber
  <- ContentItemPublishedChroniconSubscriber
  <- LocalCodexEventDispatcher
```

Dispatch events manually.

Assert records were saved.

Do not require `CodexRuntime`.

Do not wire a full application runtime.

Do not use indexing components.

## 11. Tests

Add JUnit 5 tests.

No Spring.

No Mockito unless absolutely necessary.

Prefer real `RecordingChroniconRepository`.

### SiteCreatedChroniconSubscriber tests

* constructor rejects null repository
* `eventType()` returns `SiteCreatedEvent.class`
* `handle` rejects null event
* `handle` saves one audit record
* saved record action is `CREATED`
* subject type is `site`
* actorId matches event actor id
* occurredAt matches event occurredAt
* summary is non-blank
* metadata contains site key

### ContentTypeCreatedChroniconSubscriber tests

* constructor rejects null repository
* `eventType()` returns `ContentTypeCreatedEvent.class`
* `handle` rejects null event
* `handle` saves one audit record
* saved record action is `CREATED`
* subject type is `content-type`
* actorId matches event actor id
* occurredAt matches event occurredAt
* metadata contains content type key and site key if available

### ContentItemPublishedChroniconSubscriber tests

* constructor rejects null repository
* `eventType()` returns `ContentItemPublishedEvent.class`
* `handle` rejects null event
* `handle` saves one audit record
* saved record action is `PUBLISHED`
* subject type is `content-item`
* actorId matches event actor id
* occurredAt matches event occurredAt
* metadata contains content item id
* metadata contains content item key
* metadata contains content type key
* metadata contains content type version id
* metadata contains published revision id

### Local dispatcher integration test

* register all three subscribers with `LocalCodexEventDispatcher`
* dispatch each supported event
* assert three audit records saved
* assert unsupported event, if easy to create, is ignored
* do not assert ordering unless repository guarantees it

## 12. Documentation

Update `codex-chronicon/README.md` briefly.

Mention:

* Chronicon now has initial event subscribers
* subscribers currently cover:

    * site created
    * content type created
    * content item published
* runtime wiring is future work
* durable persistence is future work
* timeline/query APIs are future work

Optionally update:

```text
codex-docs/modules/CHRONICON-BOUNDARY-NOTES.md
```

Add a small note that initial Chronicon event subscribers now exist.

Do not rewrite the document.

## 13. Runtime Wiring

Do not wire subscribers into `CodexRuntime`.

Do not create `ChroniconRuntime`.

Do not create `codex-runtime` or `codex-assembly`.

Manual dispatcher tests are enough for this task.

Future composition may look like:

```text
ChroniconRuntime
  -> ChroniconRepository
  -> Chronicon subscribers

ApplicationRuntime / PortaRuntime
  -> composes CoreRuntime
  -> composes IndexRuntime
  -> composes ChroniconRuntime
```

Do not implement this now.

## 14. Acceptance Criteria

Task is complete when:

* `codex-chronicon` depends on `codex-codex`
* three Chronicon subscribers exist
* subscribers implement `CodexEventSubscriber`
* subscribers save `AuditRecord` entries
* tests prove direct subscriber behavior
* tests prove `LocalCodexEventDispatcher` integration
* no runtime wiring is added
* no canonical classes are moved
* no domain events are moved
* no persistence backend is added

## 15. Maven Commands

Run:

```bash
mvn test -pl codex-chronicon -am
```

If practical, also run:

```bash
mvn test -pl codex-fundamentum,codex-codex,codex-chronicon -am
```

Report command results.

## 16. Post-Task Report

After implementation, report:

* files created
* files modified
* module-info changes
* pom changes
* tests added/updated
* Maven commands run
* whether tests passed
* intentional deviations
* open questions
* recommended follow-up tasks

## 17. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Keep Chronicon as audit/history projection module.
* Do not move domain events.
* Do not move canonical entities.
* Do not move canonical services.
* Do not wire runtime.
* Do not create ChroniconRuntime yet.
* Do not create CodexModuleRuntime.
* Do not use ServiceLoader.
* Do not use Spring.
* Do not use Service Locator.
* Do not implement durable persistence.
* Do not implement audit query service.
* Do not implement timeline query service.
* Do not implement observability.
* Do not add cache behavior.
* Do not add indexing behavior.
* Do not add workflow.
* Do not modify unrelated files.
* Keep comments and JavaDoc in English.
* Prefer small, explicit, testable classes.

