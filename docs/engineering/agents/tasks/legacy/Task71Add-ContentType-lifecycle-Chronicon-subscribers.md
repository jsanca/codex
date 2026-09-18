Add Chronicon audit subscribers for the remaining ContentType lifecycle events.

Context:
Site lifecycle Chronicon audit coverage is now complete.

ContentType currently has:
- ContentTypeCreatedEvent → ContentTypeCreatedChroniconSubscriber → AuditAction.CREATED

Missing:
- ContentTypeActivatedEvent
- ContentTypeArchivedEvent

The inspection report confirmed:
- Both events already exist.
- Both events are already published.
- No new events are needed.
- AuditAction.ACTIVATED and AuditAction.ARCHIVED already exist.
- Existing subscribers build AuditRecord from event fields only and do not reload entities.

Goal:
Add Chronicon audit coverage for the remaining ContentType lifecycle events.

Requirements:
1. Create:
    - ContentTypeActivatedChroniconSubscriber
    - ContentTypeArchivedChroniconSubscriber

2. Map events to AuditAction:
    - ContentTypeActivatedEvent → AuditAction.ACTIVATED
    - ContentTypeArchivedEvent → AuditAction.ARCHIVED

3. Follow the style of:
    - ContentTypeCreatedChroniconSubscriber

4. Do not reload ContentType from a repository.
   Use only event fields.

5. Preserve existing AuditRecordId semantics.
   If ContentType audit IDs are duplicated, you may add a package-private helper to AuditRecordIdGenerator, for example:
    - contentTypeLifecycle(action, siteKey, contentTypeKey, occurredAt)

   The generated IDs must remain consistent with the existing ContentTypeCreatedChroniconSubscriber format.

6. Wire both subscribers into:
    - ChroniconRuntime.buildSubscribers()

7. Add focused unit tests for each subscriber:
    - constructor null guard
    - eventType if the subscriber API exposes it
    - handle(null) rejection
    - saves one AuditRecord
    - correct AuditAction
    - correct subject
    - correct actorId
    - correct occurredAt
    - expected metadata

8. Update ChroniconRuntime tests:
    - subscriber count from 13 to 15
    - dispatcher/integration test should include both new ContentType events if there is an existing pattern

9. Update ConciliumRuntime tests:
    - total subscriber count from 17 to 19

Constraints:
- Do not add events.
- Do not modify ContentTypeService or EventPublishingContentTypeService.
- Do not modify AuditAction.
- Do not change existing AuditRecordId values.
- Do not introduce repository reloads.
- Do not change ContentItem or Site audit/indexing behavior.

Expected result:
- ContentType lifecycle audit coverage includes created, activated, and archived.
- Both new subscribers are wired into ChroniconRuntime.
- Existing tests pass.
- Full reactor build passes.