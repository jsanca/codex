# Task CODEX-018B — AccessDecision Trace Model Proposal

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

This is documentation / model proposal only.

Create report under:

* `docs/agents/reports/knowledge/`

## Objective

Define the proposed trace model for authorization decisions before implementing `AuthorizationDecisionRecord`.

The goal is to decide what information should be captured, redacted, logged, counted, audited, or kept internal when Custos grants, denies, or detects an invariant violation.

## Context

ADR-012 has been created:

* `docs/ADR-corpus/ADR-012.md`

ADR-012 establishes:

* authorization decision records are not domain events
* domain audit and security audit remain semantically separate
* authorization decision records use bounded routing
* Custos does not directly depend on Chronicon, Observance, logs, queues, or alerting
* runtime composition selects consumers/sinks
* Codex avoids saga-style event choreography for authorization visibility

Before implementation, Codex needs a trace model.

## Key Question

What should an authorization decision trace contain?

The model must support:

* diagnostic logs
* PI-safe Observance metrics
* future security audit stream
* support/debug investigation
* future Olorin / agent capability visibility
* redaction boundaries
* internal vs external exposure

## Scope

Review and synthesize:

* `docs/ADR-corpus/ADR-012.md`
* `docs/agents/reports/knowledge/CODEX-018A—AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting—REPORT.md`
* `docs/agents/reports/knowledge/CODEX-018—AuthorizationAuditAndDecisionTraceDiscovery—REPORT.md`
* `docs/security/CUSTOS-MODEL.md`
* current Custos model classes:

    * `AccessDecision`
    * `AccessDecisionService`
    * `PermissionResolution`
    * `PermissionResolutionRequest`
    * `PermissionResolutionSnapshot`
    * `RoleAssignment`
    * `PermissionGrant`
    * `ResourceRef`
    * `ResourceScope`
    * `PermissionKey`
    * `Actor`
    * `ActorType`

No Java implementation in this task.

## Proposed Concepts To Define

### 1. AccessDecision trace

A structured explanation of how an authorization decision was reached.

It may include:

* actor reference
* actor type
* permission key
* resource reference
* target scope
* decision outcome
* reason category
* human-readable reason
* matched role assignment
* matched permission grant
* matched role key
* scope walk path
* permission implication path
* invariant violation marker
* correlation id, if available later
* timestamp, if appropriate
* redaction level

### 2. Reason category

Define stable categories instead of relying only on free-text reasons.

Possible categories:

* `EXPLICIT_GRANT`
* `ROLE_GRANT`
* `SCOPE_MATCH`
* `SCOPE_MISMATCH`
* `PERMISSION_NOT_GRANTED`
* `ACTOR_TYPE_FORBIDDEN`
* `INVARIANT_VIOLATION`
* `MISSING_ROLE_ASSIGNMENT`
* `MISSING_PERMISSION_GRANT`
* `UNKNOWN`

Do not force these exact names if better names exist, but propose a small stable set.

### 3. Redaction level

Define what trace data can be used where.

Possible levels:

* `INTERNAL_FULL`
* `SECURITY_AUDIT`
* `DIAGNOSTIC_LOG`
* `METRICS_SAFE`
* `EXTERNAL_SAFE`

Explain what each level may include or exclude.

### 4. Trace audience

Different consumers need different views.

Examples:

* logs need diagnostic but not full topology leakage
* Observance needs low-cardinality PI-safe aggregates
* security audit needs durable security facts
* external API users should receive minimal denial explanation
* tests may inspect fuller traces
* support tooling may need privileged internal trace access

### 5. Relationship to AccessDecision

Answer whether trace should be:

* embedded directly inside `AccessDecision`
* attached as optional metadata
* produced by a separate observer/decorator
* generated only for selected categories
* generated lazily on demand

Recommendation should preserve current `AccessDecision` simplicity unless there is a strong reason to change it.

## Questions To Answer

1. Should every authorization decision have a trace?
2. Should normal granted reads create full traces, sampled traces, or no durable traces?
3. What is the minimum trace needed for denied writes?
4. What is the minimum trace needed for invariant violations?
5. What trace fields are safe for Observance metrics?
6. What trace fields are safe for logs?
7. What trace fields belong only in security audit?
8. What trace fields must never be exposed externally?
9. How do we avoid leaking role topology or permission hierarchy?
10. Should the trace include matched role/grant details, or only reason categories?
11. Should invariant violations remain exceptions and also create trace records?
12. How does this model support future actor/boundary integration tests?

## Recommendation Matrix

Include a table:

| Field | Internal trace | Security audit | Logs | Observance | External API | Notes |
| ----- | -------------: | -------------: | ---: | ---------: | -----------: | ----- |

Cover at least:

* actor id
* actor type
* permission key
* resource type
* resource id
* target scope type
* target scope value
* decision outcome
* reason category
* human reason
* matched role key
* matched role assignment id
* matched permission grant
* scope walk path
* permission implication path
* invariant violation type
* correlation id
* timestamp

## Proposed Output Model

Propose a conceptual model, not Java code.

Example shape:

```text
AuthorizationDecisionRecord
  decisionId
  occurredAt
  actorRef
  actorType
  permissionKey
  resourceRef
  targetScope
  outcome
  reasonCategory
  reason
  trace
  redactionProfile
  correlationRef
```

And:

```text
AuthorizationDecisionTrace
  matchedRoleKey
  matchedGrantRef
  matchedAssignmentRef
  scopeWalkPath
  implicationPath
  invariantViolation
```

Adjust names as needed.

## Out of Scope

Do not implement:

* Java classes
* tests
* dispatcher
* consumers
* Chronicon security audit stream
* Observance metrics
* logging sink
* external API behavior
* persistence
* queue/outbox
* saga/orchestration
* Olorin behavior
* actor boundary integration tests

## Deliverables

Create:

* `docs/agents/reports/knowledge/CODEX-018B—AccessDecisionTraceModelProposal—REPORT.md`

The report should include:

1. Problem statement
2. Terminology
3. Proposed trace fields
4. Reason category proposal
5. Redaction model
6. Consumer/audience matrix
7. Relationship to `AccessDecision`
8. Risks
9. Recommended implementation path
10. Follow-up tasks

## Acceptance Criteria

* The report clearly separates trace, decision record, logs, metrics, and audit.
* The report defines which data is safe for Observance.
* The report defines which data is safe for logs.
* The report defines which data belongs only in security audit/internal trace.
* The report warns against leaking role topology or permission hierarchy.
* The report keeps invariant violations fatal while allowing future trace/security audit visibility.
* The report recommends whether trace should be embedded in `AccessDecision` or produced separately.
* The report proposes next implementation tasks.
* `git diff --check -- docs` passes.
* No commits performed.

## Validation

Run:

```bash
git diff --check -- docs
```

No tests required because this is documentation/model proposal only.
