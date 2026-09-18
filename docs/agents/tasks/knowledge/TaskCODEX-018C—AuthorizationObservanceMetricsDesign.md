# Task CODEX-018C — Authorization Observance Metrics Design

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

This is documentation / metrics design only.

Create report under:

* `docs/agents/reports/knowledge/`

## Objective

Design the PI-safe Observance metrics model for authorization decisions before implementing authorization decision records, dispatchers, or consumers.

The goal is to define what authorization metrics Codex should collect, how to avoid sensitive data leakage, how to avoid high-cardinality labels, and how these metrics relate to `AuthorizationDecisionRecord`, `AccessDecisionTrace`, and future security audit streams.

## Context

ADR-012 established:

* authorization outcomes may become bounded authorization decision records
* authorization decision records are not domain events
* domain audit and security audit remain semantically separate
* Custos does not depend directly on Chronicon, Observance, logs, queues, or alerting
* runtime composition chooses configured consumers/sinks
* routing must stay bounded:

    * producer -> dispatcher/router -> final consumer/sink -> done

CODEX-018B established the AccessDecision trace model proposal.

For Observance, CODEX-018B recommends the `METRICS_SAFE` boundary.

Observance may include:

* decision outcome
* reason category
* actor type
* permission category
* resource type
* counts
* latency/timers

Observance must exclude:

* actor id
* resource id
* target scope value
* human reason
* role keys
* assignment references
* permission grant references
* scope walk path
* permission implication path
* full permission topology

## Scope

Review and synthesize:

* `docs/ADR-corpus/ADR-012.md`
* `docs/agents/reports/knowledge/CODEX-018A—AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting—REPORT.md`
* `docs/agents/reports/knowledge/CODEX-018B—AccessDecisionTraceModelProposal—REPORT.md`
* `docs/security/CUSTOS-MODEL.md`
* existing Observance docs/code, if useful
* current Custos authorization model

Produce a metrics design report.

Do not implement metrics in this task.

## Key Questions

1. Which authorization facts should produce Observance metrics?
2. Which labels/tags are safe?
3. Which labels/tags are forbidden due to PI, topology leakage, or cardinality risk?
4. Should normal granted reads be counted?
5. Should denied writes always be counted?
6. Should invariant violations always be counted?
7. Should privileged `SUPER_ADMIN` use be counted?
8. Should permission administration operations be counted?
9. How should latency be measured?
10. Where should metrics be emitted from in a future implementation?
11. Should metrics be emitted by:

    * `AccessDecisionService`
    * a decorator around `AccessDecisionService`
    * `AuthorizationDecisionDispatcher`
    * an Observance consumer/sink
12. How do metrics remain bounded and avoid becoming a source of sensitive audit data?

## Required Design Principles

### 1. Metrics are aggregate operational telemetry

Observance metrics answer operational questions:

* Is authorization working?
* Are denials increasing?
* Are invariant violations happening?
* Are authorization checks slow?
* Which permission/resource categories are noisy?
* Is privileged access being used frequently?

Observance does not answer full forensic audit questions.

### 2. Metrics are not durable security audit

Observance must not become the security audit source of truth.

Security audit belongs to a future security audit stream/sink.

### 3. Metrics must be PI-safe

Do not include:

* actor id
* user id
* email
* resource id
* content item key
* site key value
* content type key value
* target scope value
* human reason
* role assignment id
* permission grant ref
* scope walk path
* implication path

### 4. Metrics must be low-cardinality

Avoid labels that can grow unbounded.

Prefer categories:

* actor type
* outcome
* reason category
* permission category
* resource type
* runtime security mode
* maybe operation family

Avoid identifiers.

### 5. Metrics are produced by consumers, not by Custos core

Custos should not directly depend on Observance.

Preferred future architecture:

```text
AuthorizationDecisionRecord
  -> AuthorizationDecisionDispatcher
  -> ObservanceAuthorizationMetricsConsumer
  -> Observance
```

### 6. Metrics do not create additional authorization decision events

The Observance consumer is a final sink.

It should not emit more authorization decision records.

Bounded flow remains:

```text
producer -> dispatcher/router -> final consumer/sink -> done
```

## Metric Categories To Propose

Propose concrete metric names and safe tags for at least:

### 1. Authorization decisions total

Example conceptual metric:

```text
codex.authorization.decisions.total
```

Safe tags may include:

* outcome
* reason_category
* actor_type
* permission_category
* resource_type
* runtime_security_mode

### 2. Authorization denied total

```text
codex.authorization.denied.total
```

Safe tags:

* reason_category
* actor_type
* permission_category
* resource_type
* runtime_security_mode

### 3. Authorization invariant violations total

```text
codex.authorization.invariant_violations.total
```

Safe tags:

* invariant_type
* actor_type
* runtime_security_mode

### 4. Authorization decision latency

```text
codex.authorization.decision.duration
```

or equivalent Observance naming convention.

Safe tags:

* outcome
* actor_type
* permission_category
* resource_type
* runtime_security_mode

### 5. Privileged authorization use total

```text
codex.authorization.privileged_use.total
```

Safe tags:

* actor_type
* permission_category
* resource_type
* runtime_security_mode

### 6. Permission administration total

Future-oriented metric for role/grant changes.

```text
codex.authorization.admin_operations.total
```

Safe tags:

* operation_category
* outcome
* actor_type
* runtime_security_mode

## Labels / Tags Matrix

Include a table:

| Field | Allowed as metric tag? | Reason |
| ----- | ---------------------: | ------ |

Cover at least:

* outcome
* reason category
* actor type
* actor id
* permission key
* permission category
* resource type
* resource id
* site key
* content type key
* content item key
* target scope type
* target scope value
* runtime security mode
* invariant violation type
* matched role key
* matched role assignment id
* human reason
* correlation id

## Recommendation Matrix

Include a table:

| Authorization fact | Metric? | Metric name/category | Tags | Notes |
| ------------------ | ------: | -------------------- | ---- | ----- |

Cover at least:

* normal granted read
* normal granted write
* denied read
* denied write
* privileged `SUPER_ADMIN` operation
* `AGENT + SUPER_ADMIN` invariant violation
* role assignment created
* role assignment revoked
* permission grant created
* permission grant revoked
* step-up approval required
* step-up approval accepted
* step-up approval denied

## Relationship To AccessDecision Trace

Explain:

* Observance should consume redacted metric-safe views.
* Observance should not receive full traces.
* Observance should not receive role topology.
* Observance should not receive human reasons.
* Observance may consume reason categories.
* Observance may consume runtime mode, actor type, permission category, resource type, and outcome.

## Relationship To Future Security Audit

Explain:

* security audit may persist actor/resource-level facts
* Observance should not
* Observance can show aggregate trends
* security audit can support investigation
* logs can support diagnostics
* traces can support explanation

## Out of Scope

Do not implement:

* Java code
* tests
* Observance counters
* timers
* metric registry changes
* authorization decision record classes
* dispatcher
* consumers
* Chronicon changes
* security audit stream
* persistence
* queues
* outbox/retry/DLQ
* REST/Porta behavior
* Olorin behavior

## Deliverables

Create:

* `docs/agents/reports/knowledge/CODEX-018C—AuthorizationObservanceMetricsDesign—REPORT.md`

The report should include:

1. Problem statement
2. Observance role in authorization visibility
3. Metric categories
4. Proposed metric names
5. Safe/forbidden tag matrix
6. Authorization fact recommendation matrix
7. Relationship to AccessDecision trace
8. Relationship to future security audit
9. Risks
10. Recommended implementation path
11. Follow-up tasks

## Risks To Address

At minimum address:

* PI leakage
* high-cardinality metrics
* exposing permission topology
* treating metrics as audit source of truth
* metric noise from high-volume reads
* coupling Custos directly to Observance
* over-instrumenting before decision records exist

## Acceptance Criteria

* The report defines PI-safe authorization metrics.
* The report defines forbidden metric tags.
* The report avoids actor/resource identifiers in metrics.
* The report proposes concrete metric names or naming patterns.
* The report preserves the distinction:

    * Observance = aggregate operational telemetry
    * security audit = durable investigation trail
    * logs = diagnostics
    * trace = explanation
* The report recommends where metrics should be emitted from in future implementation.
* The report warns against high-cardinality labels.
* The report proposes follow-up tasks.
* `git diff --check -- docs` passes.
* No commits performed.

## Validation

Run:

```bash
git diff --check -- docs
```

No tests required because this is documentation/design only.
