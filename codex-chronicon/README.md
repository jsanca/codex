# codex-chronicon

> The Chronicon preserves the memory of the manuscript.

## Responsibility

`codex-chronicon` owns the audit, history, and timeline layer of the Codex CMS. It will
eventually hold:

- Audit trails
- Event history and historical timelines
- Change narratives
- Temporal projections
- Restoration views
- Explaining how resources changed over time

## Current Status

Audit foundation implemented (Task 30):

- `AuditRecordId`, `AuditAction`, `AuditSubject`, `AuditRecord` — core audit model
- `ChroniconRepository` — append-only storage contract
- `MemoryChroniconRepository` — in-memory implementation for development and testing
- `RecordingChroniconRepository` — test helper that records saved records

**Not yet implemented:**
- Event subscribers (Chronicon does not yet listen to domain events)
- Runtime wiring (Chronicon is not yet connected to the event pipeline)
- Durable persistence (records are in-memory only)
- Timeline query service and audit search APIs

## Dependency Rules

- Listens to domain events; records what happened.
- Does **not** own canonical lifecycle state.
- Does not replace domain events; consumes them.
- Does not perform indexing, search, or workflow orchestration.

## Boundary Note

Audit is a **product/domain history concern**.
Observability (metrics, traces, logs) is an **operations concern** and must remain separate.
