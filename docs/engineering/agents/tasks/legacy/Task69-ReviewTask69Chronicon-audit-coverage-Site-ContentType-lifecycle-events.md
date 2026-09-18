Review the current Chronicon/audit coverage for Site and ContentType lifecycle events.

Context:
ContentItem lifecycle is now complete:
- lifecycle operations
- domain events
- cache invalidation
- Chronicon audit coverage
- indexing subscribers
- ADRs documenting lifecycle and indexing semantics

Now we want to understand the current audit/event coverage for:
- Site
- ContentType

Goal:
Inspect the current Codex and Chronicon code and report what already exists for Site and ContentType lifecycle events and audit subscribers.

Please check:

1. Site lifecycle/event coverage
   - Which Site operations currently exist in SiteService?
   - Which Site domain events currently exist?
   - Which Site events are published by EventPublishingSiteService or equivalent wrappers?
   - Which Site Chronicon subscribers exist?
   - Which Site subscribers are wired into ChroniconRuntime?
   - Which Site lifecycle operations are missing events or audit records?

2. ContentType lifecycle/event coverage
   - Which ContentType operations currently exist in ContentTypeService?
   - Which ContentType domain events currently exist?
   - Which ContentType events are published by EventPublishingContentTypeService or equivalent wrappers?
   - Which ContentType Chronicon subscribers exist?
   - Which ContentType subscribers are wired into ChroniconRuntime?
   - Which ContentType lifecycle operations are missing events or audit records?

3. AuditAction coverage
   - Which AuditAction enum values already exist?
   - Which values are missing for Site and ContentType lifecycle audit?
   - Avoid adding new enum values in this task; report only.

4. Existing subscriber patterns
   - Identify the existing Site or ContentType Chronicon subscriber style.
   - Confirm whether subscribers rely only on event fields or reload entities.
   - Recommend the pattern we should follow.

5. Runtime wiring
   - Check ChroniconRuntime subscriber count and registration style.
   - Check ConciliumRuntime tests if they aggregate Chronicon subscribers.

6. Recommended implementation plan
   - Propose the smallest safe next task.
   - Separate Site and ContentType if they should not be done together.
   - Identify any event-publishing gaps that must be fixed before audit subscribers can work.

Constraints:
- Inspection/report only.
- Do not implement subscribers.
- Do not add events.
- Do not modify AuditAction.
- Do not change runtime wiring.
- Do not change production code unless there is a trivial typo or broken reference.

Expected output:
- Current Site audit/event coverage table.
- Current ContentType audit/event coverage table.
- Missing coverage list.
- AuditAction gaps.
- Recommended next implementation task.