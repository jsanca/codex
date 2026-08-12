    # Task CODEX-018A.1 — Write ADR: Authorization Decision Records and Bounded Security Audit Routing

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

This is documentation-only.

Create an engineering report under:

* `docs/agents/reports/knowledge/`

## Objective

Convert the CODEX-018A ADR-prep report into a formal Codex ADR.

The ADR should capture the architectural decision for authorization decision records, bounded routing, Chronicon domain/security stream separation, and runtime-configured consumers/sinks.

## Context

CODEX-018A produced:

* `docs/agents/reports/knowledge/CODEX-018A—AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting—REPORT.md`

That report established the following direction:

* Authorization outcomes should be represented as bounded authorization decision records.
* Authorization decision records are not domain lifecycle events.
* Domain audit and security audit must remain semantically separate.
* Chronicon may eventually support both a domain audit stream and a security audit stream.
* Custos must not directly depend on Chronicon, Observance, logs, queues, or alerting.
* Runtime composition decides which consumers/sinks are active.
* Codex should avoid uncontrolled reactive event graphs.
* Codex does not need Saga orchestration for authorization visibility.
* Arbitrieri remains the laboratory for saga, retry, outbox, DLQ, and complex orchestration patterns.

## Scope

1. Locate the existing Codex ADR convention.

Check for existing ADR locations such as:

* `docs/adr/`
* `docs/security/ADR-*.md`
* `docs/architecture/adr/`
* any ADR pattern referenced by current docs

Do not create a new ADR tree if an existing project convention already exists.

2. Create a formal ADR using the existing Codex ADR convention.

Suggested ADR title:

* `ADR — Authorization Decision Records and Bounded Security Audit Routing`

The filename should follow the existing Codex ADR numbering and location convention.

3. The ADR should include:

* Status
* Context
* Decision
* Consequences
* Alternatives considered
* Follow-up work

Use `Proposed` if the repository convention requires ADRs to be proposed first. Use `Accepted` only if the existing project convention and Jonathan's current direction make that appropriate.

## Decision Statement

Use this as the base decision statement:

Codex will represent authorization outcomes as bounded authorization decision records that may be routed by runtime-configured consumers/sinks. These records are not domain events. Domain audit and security audit remain semantically separate. Custos does not depend directly on Chronicon, Observance, logs, queues, or alerting.

## Required Content

### 1. Authorization decision records are not domain events

The ADR must explicitly state that authorization decision records are not domain lifecycle events.

They must not be mixed with events such as:

* `SiteCreated`
* `ContentTypeArchived`
* `ContentItemPublished`

### 2. Chronicon domain/security stream separation

The ADR must document the conceptual model:

```text
Chronicon
  ├── domain audit stream
  └── security audit stream
```

Domain audit records applied business/domain facts.

Security audit records security facts, such as:

* denied writes
* privileged use
* role assignment changes
* permission grant/revoke
* invariant violations
* future step-up approval outcomes

### 3. Bounded routing is the default

Preferred flow:

```text
producer -> dispatcher/router -> final consumer/sink -> done
```

Avoid uncontrolled chains:

```text
event -> consumer -> event -> consumer -> event -> consumer
```

unless a future ADR explicitly introduces orchestration.

### 4. This is not Saga orchestration

The ADR must explicitly state:

* Codex does not currently need saga-style orchestration for authorization visibility.
* Authorization decision visibility should remain finite and local first.
* Arbitrieri remains the reference/lab for saga, retry, outbox, DLQ, and complex orchestration patterns.

### 5. Custos remains low-coupled

Custos must not directly depend on:

* Chronicon
* Observance
* logging implementation
* queue implementation
* alerting implementation

Custos may produce or expose authorization decision records. Runtime composition wires consumers/sinks.

### 6. Runtime composition owns consumer selection

Example consumers/sinks:

* no-op sink
* diagnostic logging sink
* Observance metrics sink
* Chronicon security audit sink
* alerting sink
* future external queue publisher

Example runtime profiles:

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

### 7. First implementation should be local and simple

The ADR should allow a future implementation path:

* local in-memory dispatcher first
* optional async executor later
* optional outbox later
* optional external queue later
* optional dedicated security audit service later

The producer contract should survive those changes.

## Alternatives To Include

Document and reject or defer:

### Alternative 1 — Put denied authorization attempts into Chronicon domain audit

Reject because:

* denied attempts are not applied business/domain facts
* this would pollute domain history with security facts

### Alternative 2 — Let Custos call Chronicon, Observance, or logs directly

Reject because:

* it couples authorization core to visibility infrastructure
* it weakens modular boundaries

### Alternative 3 — Full event-driven choreography

Reject because:

* it creates a hard-to-track reactive graph
* Codex does not need saga behavior for authorization visibility

### Alternative 4 — Logs only

Reject because:

* logs are useful diagnostics
* logs are not durable audit or metrics source of truth

### Alternative 5 — Distributed queue/outbox immediately

Defer because:

* it is premature for current local runtime
* the local producer contract should allow future distributed delivery

## Consequences

### Positive consequences

* security visibility without coupling
* domain audit remains clean
* future security audit stream is possible
* Observance can stay PI-safe
* logs remain useful for developers
* future queue/service extraction remains possible
* Codex avoids uncontrolled event graph complexity

### Trade-offs

* requires new authorization decision record model later
* requires dispatcher/consumer boundary later
* requires redaction policy
* requires careful routing configuration
* requires avoiding audit noise
* requires tests proving decision records do not become domain events

## Future Actor/Boundary Scenario Note

Add a short future-work note that Codex should later validate Custos with richer actor and boundary integration scenarios.

Example future scenario:

```text
Admin creates Site A.
Admin creates ContentTypes.
Admin assigns limited permissions to other users.
Users operate inside and outside their allowed boundaries.
Custos verifies role/scope constraints.
Denied operations produce no domain side effects.
Future authorization decision records provide visibility into granted/denied outcomes.
```

This scenario is not part of this ADR implementation. It is a future integration test direction after the decision-record/audit boundary is formalized.

## Follow-up Tasks

Recommend:

1. `CODEX-018B — AccessDecision Trace Model Proposal`
2. `CODEX-018C — Authorization Observance Metrics Design`
3. `CODEX-018D — Local Authorization Decision Record Prototype`
4. `CODEX-018E — Security Audit Stream Boundary Discovery`
5. `CODEX-019 — Actor Boundary Integration Scenario`

## Out of Scope

Do not implement:

* Java code
* tests
* event classes
* dispatcher
* consumers
* Chronicon changes
* Observance metrics
* persistence
* queues
* outbox/retry/DLQ
* Porta behavior
* Olorin behavior
* actor boundary integration tests

## Validation

Run:

```bash
git diff --check -- docs
```

No tests required.

## Deliverables

* ADR created in the correct existing ADR location
* report created under `docs/agents/reports/knowledge/`
* ADR location decision documented
* future actor/boundary scenario noted as follow-up
* any stale docs found reported but not corrected unless directly necessary
* no commits
