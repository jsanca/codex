# Task CODEX-018 — Authorization Audit and Decision Trace Discovery

Status: Planned
Owner: Elito
Role: Knowledge Curator
Target: 30–45 minutes
Hard Stop: 60 minutes

## Execution Requirements

Apply, if available:

* `.opencode/skills/osk-architecture-review/SKILL.md`
* `.opencode/skills/osk-knowledge-curation/SKILL.md`
* `.opencode/skills/osk-engineering-reporting/SKILL.md`

No commits.

Create a knowledge/report artifact under:

* `docs/agents/reports/knowledge/`

This is discovery and architecture framing only.

## Objective

Clarify where authorization-related facts should live in Codex before implementing audit behavior.

Specifically, decide how to distinguish:

* domain audit
* operational observability
* diagnostic logs
* security audit
* access decision explanation / trace

## Context

Codex now has:

* Custos secured decorators complete
* secured runtime composition complete
* denied-operation side-effect consistency tests accepted
* denied operations do not mutate state
* denied operations do not emit domain events
* denied operations do not accidentally create Chronicon audit records
* denied operations do not update index projections

This means denied authorization attempts are currently blocked correctly, but Codex has not yet decided how, where, or whether to record them.

## Key Question

When Custos denies an operation, what should Codex record, and where?

Possible destinations:

1. Chronicon

Current intuition:
Chronicon records domain/audit history for applied domain changes.

Open question:
Should denied authorization attempts appear in Chronicon, or would that pollute domain history with non-domain facts?

2. Observance

Current intuition:
Observance records operational metrics and telemetry.

Possible examples:

* authorization denied count
* authorization granted count
* invariant violation count
* authorization decision latency

Open question:
Which authorization facts are safe and useful as metrics?

3. Logs

Current intuition:
Logs are always useful for diagnostics but should not be treated as durable audit.

Open question:
What should be logged on denied decisions, and at what level?

4. Security audit stream

Definition to explore:
A security audit stream is a specialized record of security-relevant facts, separate from domain history.

Possible examples:

* denied access attempt
* privileged permission use
* role assignment change
* permission grant/revoke
* AGENT + SUPER_ADMIN invariant violation
* step-up approval requested/applied/denied

Open question:
Does Codex need a dedicated security audit concept, or can this be deferred?

5. AccessDecision trace

Definition to explore:
An AccessDecision trace explains how a decision was reached.

Possible fields:

* actor
* actor type
* permission key
* resource reference
* target scope
* granted/denied
* reason
* matched role assignment
* matched permission grant
* implication path, if any
* scope walk path, if any

Open question:
Should traces be:

* always created?
* only available in tests/debug?
* optionally attached to AccessDecision?
* persisted only for security audit?
* redacted before external exposure?

## Scope

Review current docs and code concepts related to:

* Custos
* AccessDecision
* PermissionResolution
* Chronicon
* Observance
* logging
* roadmap Phase 3.2
* denied-operation side-effect consistency
* CODEX-016 / CODEX-017 reports

Produce a recommendation, not code.

## Out of Scope

Do not implement:

* new audit events
* new event stream
* new persistence model
* AccessDecision trace fields
* Chronicon changes
* Observance counters
* logging changes
* REST/Porta behavior
* Olorin behavior
* StepUpApproval

Do not modify production code or tests.

## Deliverables

Create:

* `docs/agents/reports/knowledge/CODEX-018—AuthorizationAuditAndDecisionTraceDiscovery—REPORT.md`

The report should include:

1. Terminology

Define:

* domain event
* domain audit
* operational metric
* diagnostic log
* security audit event
* access decision trace

2. Current-state assessment

Summarize what Codex currently does:

* denied operations throw `AccessDeniedException`
* denied operations do not reach delegate
* denied operations do not emit domain events
* denied operations do not mutate state
* denied operations do not create Chronicon audit records accidentally
* denied operations are not yet intentionally security-audited

3. Recommendation matrix

Provide a table:

| Authorization fact | Chronicon | Observance | Logs | Security audit | AccessDecision trace | Recommendation |
| ------------------ | --------- | ---------- | ---- | -------------- | -------------------- | -------------- |

Include at least:

* granted normal domain operation
* denied read
* denied write
* AGENT + SUPER_ADMIN invariant violation
* role assignment created
* role assignment revoked
* permission grant created
* permission grant revoked
* privileged SUPER_ADMIN operation
* step-up approval required
* step-up approval accepted
* step-up approval denied

4. Proposed architecture direction

Answer:

* Should Chronicon remain focused on applied domain facts?
* Should denied attempts avoid Chronicon by default?
* Should Observance receive aggregate authorization metrics?
* Should logs always capture diagnostic denied decisions?
* Should a dedicated security audit stream exist later?
* Should AccessDecision trace be added before security audit persistence?

5. Proposed next tasks

Suggest the next 2–4 tasks after this discovery.

Possible examples:

* CODEX-018A — ADR: Authorization Audit Boundaries
* CODEX-018B — AccessDecision trace model proposal
* CODEX-018C — Observance authorization counters
* CODEX-018D — Security audit event model

6. Risks

Call out risks such as:

* leaking sensitive authorization details
* mixing denied attempts into domain history
* creating too much audit noise
* making AccessDecision too heavy
* coupling Custos to Chronicon or Observance directly
* exposing internal permission topology externally

## Acceptance Criteria

* No code changed.
* Terms are clearly separated.
* Recommendation does not force all denied attempts into Chronicon.
* Recommendation preserves the distinction:

    * Chronicon = domain/audit history
    * Observance = operational metrics
    * logs = diagnostics
    * security audit = security-relevant facts
    * AccessDecision trace = explanation of authorization reasoning
* Next implementation/discovery tasks are proposed.
* `git diff --check -- docs` passes.

## Validation

Run:

* `git diff --check -- docs`

No tests required because this is documentation/discovery only.

## Definition of Done

* Report created.
* Recommendation matrix completed.
* Risks documented.
* Follow-up tasks proposed.
* No commits performed.
