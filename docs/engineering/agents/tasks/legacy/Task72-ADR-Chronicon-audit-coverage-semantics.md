Create an ADR documenting Chronicon audit coverage semantics for Site, ContentType, and ContentItem.

Context:
Chronicon audit coverage is now complete for the current lifecycle operations of:
- Site
- ContentType
- ContentItem

Current coverage:
Site:
- created
- started
- suspended
- archived
- unarchived

ContentType:
- created
- activated
- archived

ContentItem:
- created
- updated
- published
- unpublished
- archived
- restored
- deleted

Goal:
Document the audit semantics so future work does not accidentally bypass event-based audit or introduce repository reloads inside subscribers.

Please include:
1. Status: Accepted.
2. Context:
    - Core services own validation and state transitions.
    - EventPublishing services publish events after successful operations.
    - Chronicon subscribers consume events and write AuditRecord entries.
3. Decision:
    - Chronicon audit is event-driven.
    - Subscribers build AuditRecord from event fields only.
    - Subscribers should not reload domain entities.
    - Delete subscribers must not reload deleted entities because hard delete has already completed.
4. Coverage tables for Site, ContentType, and ContentItem.
5. AuditRecordId generation:
    - AuditRecordIdGenerator centralizes content-item, site, and content-type lifecycle audit IDs where applicable.
    - Some special cases may remain inline if they include extra semantic identity, such as published revision ID.
6. Runtime wiring:
    - ChroniconRuntime wires all lifecycle audit subscribers.
    - ConciliumRuntime composes index and Chronicon subscribers.
7. Known limitations / future follow-ups:
    - no rich pre-delete snapshot yet
    - no field-level ContentType schema mutation audit yet for addField/removeField
    - no workflow-level audit yet
    - no transaction/persistence-backed audit storage yet
    - no eventId-based AuditRecordId yet
8. Constraints:
    - documentation only
    - do not change production code
    - do not change tests