# Task CODEX-018A — ADR Prep: Authorization Decision Records and Bounded Security Audit Routing

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

This is documentation / ADR-prep only.

Create report under:

* `docs/agents/reports/knowledge/`

Optionally create an ADR draft under:

* `docs/adr/`

Only create the ADR draft if the project already has an ADR pattern ready for Codex. Otherwise, produce the report only and recommend the ADR as the next task.

## Objective

Prepare the architectural decision for how Codex should represent and route authorization decision records without turning the system into an uncontrolled reactive event graph.

The goal is to preserve visibility and auditability while keeping the flow finite, understandable, and low-coupled.

## Context

CODEX-018 discovery established:

* denied authorization attempts are security facts, not business/domain facts
* Chronicon should keep the domain audit stream clean
* Chronicon may eventually host or support a separate security audit stream
* Observance may receive aggregate PI-safe metrics
* logs are always useful for diagnostics but are not the durable source of truth
* AccessDecision / authorization decision information may become a future record/event source
* Custos should not depend directly on Chronicon, Observance, logs, or alerting

Jonathan clarified an important architectural principle:

Codex should not become an uncontrolled event-driven system where messages bounce across many consumers and create a graph that is hard to track.

Unless true orchestration is needed, messages should generally have a finite path:

```text
producer -> configured consumer/sink -> done
```

or at most:

```text
producer -> dispatcher/router -> final consumer/sink -> done
```

Codex does not currently need Saga orchestration. Arbitrieri remains the laboratory for saga, retry, outbox, DLQ, and complex orchestration patterns.

## Scope

Review and synthesize:

* `docs/agents/reports/knowledge/CODEX-018—AuthorizationAuditAndDecisionTraceDiscovery—REPORT.md`
* `docs/security/CUSTOS-MODEL.md`
* `docs/CODEX-ROADMAP-PHASES.md`
* `docs/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md`
* `docs/engineering/ENGINEERING_LOG.md`
* relevant checkpoint docs for Custos secured decorators and secured runtime composition

Produce a report that frames the future ADR.

## Core Concepts to Define

Define the following clearly:

1. Authorization decision record

A structured record representing an authorization decision or security-relevant authorization outcome.

Possible examples:

* authorization granted
* authorization denied
* invariant violation
* privileged authorization used
* step-up approval required
* step-up approval accepted
* step-up approval denied

2. Security audit stream

A separate audit stream for security facts.

It may eventually be hosted by Chronicon-like infrastructure, but it must remain semantically separate from the domain audit stream.

Suggested conceptual model:

```text
Chronicon
  ├── domain audit stream
  └── security audit stream
```

3. Bounded decision routing

A finite routing model for authorization decision records.

Preferred shape:

```text
Custos / AccessDecisionService
  -> AuthorizationDecisionRecord
  -> AuthorizationDecisionDispatcher
  -> configured final consumers/sinks
```

Configured consumers/sinks may include:

* diagnostic logging sink
* Observance metrics sink
* Chronicon security audit sink
* alerting sink
* no-op sink for tests/dev

4. Consumer / sink

A final processor of authorization decision records.

A sink should not normally emit additional authorization decision events unless explicitly approved.

5. Difference from saga/orchestration

Clarify that this is not Saga orchestration.

Codex should not introduce saga-style choreography for authorization visibility.

Arbitrieri remains the correct reference/lab for saga patterns.

## Required Principles

Document these principles:

1. Low coupling

The producer of an authorization decision must not know whether the decision is:

* logged
* counted as a metric
* persisted to security audit
* ignored
* sent to an external queue
* alerted on

2. Runtime-configured consumers

The runtime decides which consumers/sinks are active.

Example profiles:

```text
test:
  no-op or in-memory recorder

dev:
  diagnostic logs + optional in-memory recorder

default secured runtime:
  logs + PI-safe Observance metrics

security/audit runtime:
  logs + PI-safe Observance metrics + security audit sink

future distributed runtime:
  logs + metrics + external queue publisher
```

3. Bounded event flow

Authorization decision records must not create an uncontrolled event graph.

Default rule:

```text
producer -> dispatcher/router -> final consumer/sink -> done
```

Avoid:

```text
event -> consumer -> event -> consumer -> event -> consumer
```

unless a future ADR explicitly introduces orchestration.

4. Not domain events

Authorization decision records are not domain events.

They must not be mixed with domain lifecycle events such as:

* SiteCreated
* ContentTypeArchived
* ContentItemPublished

5. Separate audit meanings

Chronicon domain audit stream records business/domain facts that happened.

Security audit stream records security facts, attempts, denials, privileged actions, and invariants.

6. Future scalability

The first implementation can be local and simple.

Future implementations may replace or extend the dispatcher with:

* async executor
* outbox
* external queue
* EventBridge-style routing
* dedicated security audit service
* service implemented in another language if appropriate

The producer contract should survive those changes.

7. No direct hard dependency from Custos

Custos should not directly depend on:

* Chronicon
* Observance
* logging implementation
* queue implementation
* alerting implementation

Custos may produce or expose authorization decision records; runtime composition wires consumers.

## Questions to Answer

1. Should authorization decision records be created inside AccessDecisionService, secured decorators, or a separate observer/decorator around AccessDecisionService?
2. Should records be emitted for both granted and denied decisions, or only selected categories?
3. Which records are always logged?
4. Which records become Observance metrics?
5. Which records go to the future security audit stream?
6. Which records should be sampled or suppressed to avoid noise?
7. How should PI/sensitive data be redacted?
8. What correlation identifiers are needed?
9. Should invariant violations be modeled as decision records, exceptions, or both?
10. What is the minimal local implementation path that preserves the future distributed path?

## Recommendation Matrix

Include a table:

| Authorization fact | Record? | Logs | Observance | Security audit | Domain audit | Notes |
| ------------------ | ------: | ---: | ---------: | -------------: | -----------: | ----- |

Cover at least:

* normal granted read
* normal granted write
* denied read
* denied write
* privileged SUPER_ADMIN operation
* AGENT + SUPER_ADMIN invariant violation
* role assignment created
* role assignment revoked
* permission grant created
* permission grant revoked
* step-up approval required
* step-up approval accepted
* step-up approval denied

## Out of Scope

Do not implement:

* production code
* tests
* event classes
* dispatcher
* consumers
* Chronicon changes
* Observance metrics
* persistence
* external queues
* Saga orchestration
* retry/DLQ/outbox
* Porta behavior
* Olorin behavior

Do not modify JavaDoc stale wording in `RoleAssignment` or `PermissionGrant` in this task. Report it as a separate cleanup if still relevant.

## Deliverables

Create:

* `docs/agents/reports/knowledge/CODEX-018A—AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting—REPORT.md`

The report should include:

1. Problem statement
2. Terminology
3. Proposed bounded routing model
4. Chronicon domain/security stream separation
5. Consumer/sink model
6. Explicit non-saga decision
7. Recommendation matrix
8. Risks
9. Proposed ADR title and decision statement
10. Proposed next implementation/discovery tasks

Suggested ADR title:

* `ADR — Authorization Decision Records and Bounded Security Audit Routing`

Suggested decision statement:

Codex will represent authorization outcomes as bounded authorization decision records that may be routed by runtime-configured consumers/sinks. These records are not domain events. Domain audit and security audit remain semantically separate. Custos does not depend directly on Chronicon, Observance, logs, queues, or alerting.

## Acceptance Criteria

* The report clearly separates domain audit from security audit.
* The report explicitly states that Codex does not need Saga orchestration for authorization decision visibility.
* The report defines the bounded flow rule:

    * producer -> dispatcher/router -> final consumer/sink -> done
* The report preserves low coupling between producer and consumers.
* The report allows future external queue/service evolution.
* The report warns against uncontrolled reactive event graphs.
* The recommendation matrix is completed.
* Follow-up tasks are proposed.
* `git diff --check -- docs` passes.
* No commits performed.
