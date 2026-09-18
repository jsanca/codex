# Architecture Decision Records

## Purpose

Store durable decisions whose alternatives and consequences matter to future engineers.

## What belongs here

ADRs that record context, decision, and consequences for architectural or long-lived technical choices, such as persistence, boundaries, communication, security, deployment, or orchestration strategy.

**Example:** “Choose PostgreSQL instead of Provider X” with alternatives and consequences is an ADR. “The project uses PostgreSQL” is current knowledge in `../knowledge/`.

## Canonical Corpus

This directory is Codex's only canonical ADR location. Current ADR identifiers are:

| ADR | Decision |
| --- | --- |
| [ADR-001](ADR-001-external-workspaces-as-adapters.md) | External workspaces are adapters |
| [ADR-002](ADR-002-knowledge-units-not-pages.md) | Codex manages knowledge units, not pages |
| [ADR-003](ADR-003-projections-as-transformation-pipelines.md) | Projections are transformation pipelines |
| [ADR-004](ADR-004-content-model-ownership.md) | Content-model ownership |
| [ADR-005](ADR-005-tenant-aware-index-document-identity.md) | Tenant-aware index document identity |
| [ADR-006](ADR-006-logging-redaction-direction.md) | Logging redaction direction |
| [ADR-007](ADR-007-canonical-services-cache-indexing.md) | Canonical services, cache, indexing, and search boundaries |
| [ADR-008](ADR-008-content-item-lifecycle-semantics.md) | ContentItem lifecycle semantics |
| [ADR-009](ADR-009-content-item-published-only-indexing.md) | ContentItem published-only indexing |
| [ADR-010](ADR-010-chronicon-audit-coverage.md) | Chronicon audit coverage |
| [ADR-011](ADR-011-observance-metrics-foundation.md) | Observance metrics foundation |
| [ADR-012](ADR-012-authorization-decision-records.md) | Authorization decision records and bounded security-audit routing |

The former `future-forward/` documents that reused ADR numbers are preserved as proposals under
[`../roadmap/future/`](../roadmap/future/); they are not duplicate canonical ADRs.

## What does not belong here

Task reports, general documentation, or a restatement of the current architecture. Keep current system understanding in `../knowledge/`.
