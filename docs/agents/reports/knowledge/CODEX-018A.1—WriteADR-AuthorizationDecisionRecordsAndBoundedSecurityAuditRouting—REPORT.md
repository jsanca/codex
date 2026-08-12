# Knowledge Report — CODEX-018A.1: Write ADR

## Task

`TaskCODEX-018A.1—WriteADR:AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting`

## Date

2026-08-09

## Owner

Elito

## Scope

Documentation-only. Converted the CODEX-018A ADR-prep report into a formal Codex ADR.

No production code, tests, event classes, dispatcher, consumers, Chronicon changes,
Observance metrics, persistence, queues, outbox/retry/DLQ, Porta behavior, Olorin behavior,
or saga orchestration were implemented.

## Files Changed

* `docs/ADR-corpus/ADR-012.md`
* `docs/agents/reports/knowledge/CODEX-018A.1—WriteADR-AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting—REPORT.md`

## ADR Location Decision

The ADR was created at:

```text
docs/ADR-corpus/ADR-012.md
```

Reasoning:

* `docs/ADR-corpus/` is the active Codex ADR location for recent accepted/proposed system ADRs such as ADR-010 and ADR-011.
* `docs/adr/` does not exist.
* Creating a new ADR tree would introduce documentation structure drift.
* `docs/future-forward/` exists, but this task asked for a formal Codex ADR rather than another future-forward note.

`docs/future-forward/ADR-012-Custos-Permission-Decision-Caching.md` already exists, but it is in a separate future-forward corpus. The new ADR uses `ADR-012.md` inside `docs/ADR-corpus/`, following that folder's local numbering.

## Sources Reviewed

* `docs/tasks/knowledge/TaskCODEX-018A.1—WriteADR:AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting.md`
* `docs/agents/reports/knowledge/CODEX-018A—AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting—REPORT.md`
* `docs/agents/reports/knowledge/CODEX-018—AuthorizationAuditAndDecisionTraceDiscovery—REPORT.md`
* `docs/security/CUSTOS-MODEL.md`
* `docs/ADR-corpus/ADR-010.md`
* `docs/ADR-corpus/ADR-011.md`
* `docs/future-forward/ADR-012-Custos-Permission-Decision-Caching.md`

## ADR Content Created

The ADR documents:

* authorization decision records are not domain lifecycle events
* Chronicon domain audit and future security audit are semantically separate
* domain audit records applied business/domain facts
* security audit records security facts such as denied writes, privileged use, role assignment changes, grant/revoke, invariant violations, and future step-up outcomes
* bounded routing is the default: `producer -> dispatcher/router -> final consumer/sink -> done`
* uncontrolled reactive event chains are rejected unless a future ADR introduces orchestration
* Custos remains low-coupled and does not directly depend on Chronicon, Observance, logs, queues, or alerting
* runtime composition owns consumer/sink selection
* first implementation should be local and simple
* future async executor, outbox, external queue, EventBridge-style routing, or dedicated security audit service remain deferred options

## Alternatives Documented

The ADR rejects or defers:

* putting denied authorization attempts into Chronicon domain audit
* letting Custos call Chronicon/Observance/logs directly
* full event-driven choreography for authorization visibility
* doing nothing / logs only
* distributed queue/outbox immediately

## Validation

```bash
git diff --check -- docs
```

Result: passed.

No tests were run because this was documentation-only.

## Stale Docs Found

No additional stale current-state docs were changed.

Known stale production JavaDoc wording in `RoleAssignment` and `PermissionGrant` remains out
of scope, as instructed by CODEX-018A.

## Follow-Up Work

* `CODEX-018B — AccessDecision Trace Model Proposal`
* `CODEX-018C — Authorization Observance Metrics Design`
* `CODEX-018D — Local Authorization Decision Record Prototype`
* `CODEX-018E — Security Audit Stream Boundary Discovery`

## No Commits

No commits were created.
