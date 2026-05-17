Add Chronicon audit subscribers for the remaining Site lifecycle events.

Context:
The current Chronicon coverage for Site is incomplete.

Already covered:
- SiteCreatedEvent → SiteCreatedChroniconSubscriber → AuditAction.CREATED

Missing:
- SiteStartedEvent
- SiteSuspendedEvent
- SiteArchivedEvent
- SiteUnarchivedEvent

The inspection report confirmed:
- All four events already exist.
- All four events are already published.
- No new events are needed.
- Existing subscribers build AuditRecord from event fields only and do not reload entities.
- Site AuditRecordId format is currently:
  audit:site-{action}:{siteKey}:{epochMilli}

Goal:
Add Chronicon audit coverage for the remaining Site lifecycle events.

Requirements:
1. Create:
    - SiteStartedChroniconSubscriber
    - SiteSuspendedChroniconSubscriber
    - SiteArchivedChroniconSubscriber
    - SiteUnarchivedChroniconSubscriber

2. Map events to AuditAction:
    - SiteStartedEvent → AuditAction.STARTED
    - SiteSuspendedEvent → AuditAction.SUSPENDED
    - SiteArchivedEvent → AuditAction.ARCHIVED
    - SiteUnarchivedEvent → AuditAction.RESTORED

3. Follow the style of:
    - SiteCreatedChroniconSubscriber

4. Do not reload Site from a repository.
   Use only event fields.

5. Preserve the existing Site AuditRecordId format:
   audit:site-{action}:{siteKey}:{epochMilli}

6. If the Site AuditRecordId string construction is duplicated, you may introduce a small package-private Chronicon helper/factory for Site audit ids.
   If you do, it must preserve the generated ID values exactly.

7. Wire all four subscribers into:
    - ChroniconRuntime.buildSubscribers()

8. Add focused unit tests for each subscriber:
    - constructor null guard
    - eventType if the subscriber API exposes it
    - handle(null) rejection
    - saves one AuditRecord
    - correct AuditAction
    - correct subject
    - correct actorId
    - correct occurredAt
    - expected metadata

9. Update ChroniconRuntime tests:
    - subscriber count from 9 to 13
    - dispatcher/integration test should include the four new Site events if there is an existing pattern

10. Update ConciliumRuntime tests:
- total subscriber count from 13 to 17

Constraints:
- Do not implement ContentType subscribers in this task.
- Do not add new events.
- Do not modify SiteService or EventPublishingSiteService.
- Do not modify AuditAction.
- Do not change existing AuditRecordId values.
- Do not introduce repository reloads.
- Do not change ContentItem audit/indexing behavior.

Expected result:
- Site lifecycle audit coverage includes created, started, suspended, archived, and unarchived.
- All Site subscribers are wired into ChroniconRuntime.
- Existing tests pass.
- Full reactor build passes.