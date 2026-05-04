````markdown
# Task33: Chronicon Local Dispatcher Integration Test

## Objective

Add a focused integration test proving that Chronicon subscribers work correctly through `LocalCodexEventDispatcher`.

Chronicon already has initial event subscribers:

- `SiteCreatedChroniconSubscriber`
- `ContentTypeCreatedChroniconSubscriber`
- `ContentItemPublishedChroniconSubscriber`

This task should verify the subscriber pipeline as a small in-process integration flow:

```text
Domain events
  -> LocalCodexEventDispatcher
     -> Chronicon subscribers
        -> ChroniconRepository
           -> AuditRecord entries
````

This task should not introduce runtime wiring, `ChroniconRuntime`, `CodexModuleRuntime`, ServiceLoader, Spring, persistence, REST, or query APIs.

## Decision Context

Task32 introduced individual Chronicon subscribers and some dispatcher coverage.

Before designing `ChroniconRuntime` or generic runtime abstractions, we want one clean integration test that proves Chronicon can receive multiple domain events through the existing local dispatcher and produce audit records.

This should mirror the existing index projection pattern:

```text
ContentItemPublishedEvent
  -> LocalCodexEventDispatcher
     -> ContentItemPublishedIndexingSubscriber
        -> IndexWriter
```

Chronicon equivalent:

```text
SiteCreatedEvent / ContentTypeCreatedEvent / ContentItemPublishedEvent
  -> LocalCodexEventDispatcher
     -> Chronicon subscribers
        -> ChroniconRepository
```

The test should demonstrate the module-level behavior without requiring a global runtime.

## Scope

Implement or refine integration test coverage in `codex-chronicon`.

Allowed:

* Add one integration-style JUnit 5 test class.
* Use real `RecordingChroniconRepository` or `MemoryChroniconRepository`.
* Use real Chronicon subscribers.
* Use real `LocalCodexEventDispatcher`.
* Dispatch domain events manually.
* Assert saved `AuditRecord` entries.

Not allowed:

* Do not create `ChroniconRuntime`.
* Do not create `CodexModuleRuntime`.
* Do not create `CodexRuntimeContext`.
* Do not introduce ServiceLoader.
* Do not introduce Spring.
* Do not wire Chronicon into `CodexRuntime`.
* Do not create persistence adapters.
* Do not create REST/GraphQL APIs.
* Do not implement audit query services.
* Do not implement timeline query services.
* Do not add cache/index/workflow behavior.
* Do not refactor subscriber architecture unless required by tests.

## Suggested Test Class

Create or update:

```text
codex-chronicon/src/test/java/codex/chronicon/internal/ChroniconDispatcherIntegrationTest.java
```

If this class already exists from Task32, refine or extend it rather than creating a duplicate.

## Required Test: dispatching supported events records audit entries

Create a test named:

```text
dispatching supported events records audit entries
```

Flow:

1. Create a `RecordingChroniconRepository`.
2. Create:

    * `SiteCreatedChroniconSubscriber`
    * `ContentTypeCreatedChroniconSubscriber`
    * `ContentItemPublishedChroniconSubscriber`
3. Create a `LocalCodexEventDispatcher` with the three subscribers.
4. Create deterministic domain events:

    * `SiteCreatedEvent`
    * `ContentTypeCreatedEvent`
    * `ContentItemPublishedEvent`
5. Dispatch all three events.
6. Assert that three audit records were saved.
7. Assert that actions include:

    * `CREATED`
    * `PUBLISHED`
8. Assert that subjects include:

    * `site`
    * `content-type`
    * `content-item`
9. Assert that actor ids match the event actor.
10. Assert that occurredAt values match the event occurredAt values.

Do not assert exact ordering unless `RecordingChroniconRepository` or `MemoryChroniconRepository` explicitly guarantees ordering.

If ordering is guaranteed by the repository, asserting order is acceptable but not required.

## Required Test: unsupported event is ignored

Add one test proving that an event with no matching subscriber does not create audit records.

Suggested test name:

```text
dispatching unsupported event records nothing
```

Use a small local test record implementing `CodexEvent`, for example:

```java
private record UnsupportedChroniconTestEvent(Instant occurredAt) implements CodexEvent {
}
```

Flow:

1. Create repository.
2. Create dispatcher with Chronicon subscribers.
3. Dispatch unsupported event.
4. Assert no records were saved.

Do not create production event classes for this.

## Required Test: dispatcher propagates subscriber failures

Add one test if not already covered by `LocalCodexEventDispatcher` tests.

Suggested test name:

```text
subscriber failure propagates and stops dispatch
```

This can use a small local failing subscriber inside the test.

However, if `LocalCodexEventDispatcher` already has this behavior covered in `codex-fundamentum`, do not duplicate it heavily.

For Chronicon integration, it is enough to verify that if a Chronicon subscriber receives an invalid event or repository failure occurs, the exception is not swallowed.

If testing this requires too much test-only scaffolding, document it as already covered by fundamentum dispatcher tests and skip.

## Optional Test: duplicate dispatch creates duplicate audit records

Audit records are append-only projections.

If a domain event is dispatched twice, the current simple subscribers may attempt to write two audit records. Depending on deterministic audit id generation and repository behavior, this may either:

* overwrite the same id, or
* preserve only one record by id, or
* create duplicate records if ids differ.

Do not force a new behavior in this task.

If current behavior is clear, add a test documenting it.

If current behavior is not important yet, skip this test.

Future idempotency can be handled in a separate task.

## Event Construction

Use existing event constructors/factories.

Keep event data deterministic:

* fixed `Actor`
* fixed `ActorId`
* fixed `Instant`
* fixed keys/ids

Do not use `Instant.now()` in tests unless unavoidable.

Prefer `Clock.fixed(...)` or fixed `Instant.parse(...)`.

## Assertions

Prefer clear helper methods if useful:

```text
recordsOfAction(...)
recordsOfSubjectType(...)
singleRecordForSubject(...)
```

But do not overbuild.

The test should be readable.

Assert important fields:

* `AuditRecord.action()`
* `AuditRecord.subject().type()`
* `AuditRecord.subject().id()`
* `AuditRecord.subject().key()`
* `AuditRecord.actorId()`
* `AuditRecord.occurredAt()`
* relevant metadata keys

Do not assert full summaries word-for-word unless current summaries are considered stable.

It is okay to assert summaries are non-blank.

## Documentation

Update `codex-chronicon/README.md` only if useful.

A small note is enough:

```text
Chronicon subscribers are verified through LocalCodexEventDispatcher integration tests.
Runtime-level composition remains future work.
```

Do not rewrite the README.

## Acceptance Criteria

This task is complete when:

* Chronicon has a focused local dispatcher integration test.
* The test uses real Chronicon subscribers.
* The test uses `LocalCodexEventDispatcher`.
* The test proves supported events create audit records.
* The test proves unsupported events do not create audit records.
* No runtime wiring is introduced.
* No new production architecture is introduced.
* Existing subscriber behavior remains unchanged.
* Tests pass.

## Maven Commands

Run:

```bash
mvn test -pl codex-chronicon -am
```

If practical, also run:

```bash
mvn test -pl codex-fundamentum,codex-codex,codex-chronicon -am
```

Report command results.

## Post-Task Report

After implementation, report:

* files created
* files modified
* tests added or updated
* Maven commands run
* whether tests passed
* intentional deviations
* open questions
* recommended follow-up tasks

## Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Test/integration task only.
* Do not implement `ChroniconRuntime`.
* Do not implement runtime abstractions.
* Do not use ServiceLoader.
* Do not use Spring.
* Do not use Service Locator.
* Do not wire into `CodexRuntime`.
* Do not implement durable persistence.
* Do not implement audit query service.
* Do not implement timeline query service.
* Do not implement observability.
* Do not implement cache.
* Do not implement indexing.
* Do not implement workflow.
* Do not modify unrelated files.
* Keep comments and JavaDoc in English.
* Prefer small, explicit, readable tests.
