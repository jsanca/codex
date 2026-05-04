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

Skeleton only. No audit or history implementation exists yet.

## Dependency Rules

- Listens to domain events; records what happened.
- Does **not** own canonical lifecycle state.
- Does not replace domain events; consumes them.
- Does not perform indexing, search, or workflow orchestration.

## Boundary Note

Audit is a **product/domain history concern**.
Observability (metrics, traces, logs) is an **operations concern** and must remain separate.
