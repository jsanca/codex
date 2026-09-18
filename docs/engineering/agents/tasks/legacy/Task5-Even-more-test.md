# Task: Integration tests for the Site event pipeline

## Objective
Verify the full pipeline end-to-end using real production components,
no mocks, no Spring, no external infrastructure.

The full chain under test:
TransactionContext
→ EventPublishingSiteService
→ CodexSiteService
→ MemorySiteRepository
→ DeferredEventDispatcher
→ RecordingCodexEventDispatcher

## TestCodexContext

Create a reusable test fixture `TestCodexContext` that wires the full
pipeline and exposes helpers for assertions.

```java
public final class TestCodexContext {

    public static TestCodexContext create() { ... }

    public SiteService siteService() { ... }

    public List<CodexEvent> events() { ... }

    public void assertNoEvents() { ... }

    public <E extends CodexEvent> E assertSingleEvent(Class<E> type) { ... }

    public void clearEvents() { ... }
}
```

All tests use `TransactionContext.runInTransaction(...)` to simulate
the transaction boundary. Use `Actor.system("demo")` as the actor.
Use `Clock.fixed(...)` for deterministic timestamps.

## Test cases

### create
1. `create inside transaction does not dispatch before commit`
   - inside runInTransaction(), call create()
   - before the lambda returns assert zero events

2. `create inside transaction dispatches SiteCreatedEvent after commit`
   - call create() inside runInTransaction()
   - after commit assert exactly one SiteCreatedEvent with correct siteKey

3. `create rollback does not dispatch event`
   - inside runInTransaction(), call create(), then throw RuntimeException
   - after catching the exception assert zero events

### outside transaction
4. `outside transaction dispatches immediately`
   - call findByKey() outside any transaction
   - assert the call completes without accumulating events
   - call create() outside any transaction
   - assert SiteCreatedEvent is dispatched immediately without commit

### suspend
5. `suspend dispatches SiteSuspendedEvent only when status changes`
   - create a STARTED site inside a transaction
   - call suspend() inside a new transaction
   - assert exactly one SiteSuspendedEvent

6. `idempotent suspend does not dispatch event`
   - create and suspend a site (two separate transactions)
   - call suspend() again on the already SUSPENDED site
   - assert zero new events after the second suspend

7. `rollback after suspend does not dispatch SiteSuspendedEvent`
   - create a STARTED site inside a transaction
   - inside a new transaction call suspend() then throw RuntimeException
   - assert zero SiteSuspendedEvent dispatched

### state machine transitions
8. `archive after suspend dispatches SiteArchivedEvent`
   - create and suspend a site
   - call archive() inside a transaction
   - assert exactly one SiteArchivedEvent

9. `unarchive archived dispatches SiteUnarchivedEvent`
   - create, suspend, and archive a site
   - call unarchive() inside a transaction
   - assert exactly one SiteUnarchivedEvent

10. `start suspended dispatches SiteStartedEvent`
    - create and suspend a site
    - call start() inside a transaction
    - assert exactly one SiteStartedEvent

### invalid transition
11. `invalid transition does not dispatch event`
    - create a STARTED site
    - attempt to archive() directly (invalid: STARTED → ARCHIVED)
    - assert InvalidSiteStatusTransitionException is thrown
    - assert zero events dispatched

## Constraints
- Follow CLAUDE.md conventions
- JUnit 5, no Spring, no mocks
- Test names must match the descriptions above exactly
- Each test must be independent — fresh TestCodexContext per test
- Use MemorySiteRepository, not any persistence layer