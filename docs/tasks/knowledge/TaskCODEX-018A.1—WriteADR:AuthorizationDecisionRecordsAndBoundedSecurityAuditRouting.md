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

Convert the CODEX-018A ADR-prep report into a formal ADR for Codex.

## Context

CODEX-018A produced:

* `docs/agents/reports/knowledge/CODEX-018A—AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting—REPORT.md`

The report recommends:

* authorization outcomes should be represented as bounded authorization decision records
* these records are not domain events
* domain audit and security audit must remain semantically separate
* Chronicon may eventually host both:

    * domain audit stream
    * security audit stream
* Custos must not depend directly on Chronicon, Observance, logs, queues, or alerting
* runtime composition chooses active consumers/sinks
* Codex should avoid uncontrolled reactive event graphs
* Codex does not need Saga orchestration for authorization decision visibility

## Scope

1. Locate the existing Codex ADR convention.

Check for existing ADR locations such as:

* `docs/adr/`
* `docs/security/ADR-*.md`
* `docs/architecture/adr/`
* any existing ADR pattern referenced by docs

Do not create a new ADR tree if a project convention already exists.

2. Create a formal ADR using the existing Codex ADR convention.

Suggested ADR title:

* `ADR — Authorization Decision Records and Bounded Security Audit Routing`

Suggested filename depends on the existing ADR numbering/location.

3. The ADR should include:

* Status: Proposed or Accepted, depending on current project ADR convention
* Context
* Decision
* Consequences
* Alternatives considered
* Follow-up work

## Decision Statement

Use this decision statement as the base:

Codex will represent authorization outcomes as bounded authorization decision records that may be routed by runtime-configured consumers/sinks. These records are not domain events. Domain audit and security audit remain semantically separate. Custos does not depend directly on Chronicon, Observance, logs, queues, or alerting.

## Required Content

The ADR must explicitly state:

1. Authorization decision records are not domain lifecycle events.

They must not be mixed with events such as:

* `SiteCreated`
* `ContentTypeArchived`
* `ContentItemPublished`

2. Chronicon audit streams are semantically separated.

Conceptual model:

```text
Chronicon
  ├── domain audit stream
  └── security audit stream
```

3. Domain audit records applied business/domain facts.

4. Security audit records security facts:

* denied writes
* privileged use
* role assignment changes
* permission grant/revoke
* invariant violations
* future step-up approval outcomes

5. Bounded routing is the default.

Preferred flow:

```text
producer -> dispatcher/router -> final consumer/sink -> done
```

Avoid uncontrolled chains:

```text
event -> consumer -> event -> consumer -> event -> consumer
```

unless a future ADR explicitly introduces orchestration.

6. Custos remains low-coupled.

Custos must not directly depend on:

* Chronicon
* Observance
* logging implementation
* queue implementation
* alerting implementation

7. Runtime composition owns consumer selection.

Example consumers/sinks:

* no-op sink
* diagnostic logging sink
* Observance metrics sink
* Chronicon security audit sink
* alerting sink
* future external queue publisher

8. First implementation should be local and simple.

Future implementations may use:

* async executor
* outbox
* external queue
* EventBridge-style routing
* dedicated security audit service

But those are deferred.

## Alternatives To Include

Document and reject or defer:

1. Put denied authorization attempts into Chronicon domain audit.
   Reason to reject:

* pollutes business/domain audit with non-domain facts.

2. Let Custos call Chronicon/Observance/logs directly.
   Reason to reject:

* couples authorization core to infrastructure/visibility sinks.

3. Full event-driven choreography.
   Reason to reject:

* creates hard-to-track reactive graph; Codex does not need saga behavior here.

4. Do nothing / logs only.
   Reason to reject:

* logs are useful diagnostics but not reliable durable audit or metrics source of truth.

5. Distributed queue/outbox immediately.
   Reason to defer:

* premature for current local runtime; producer contract should allow this later.

## Consequences

Positive:

* security visibility without coupling
* domain audit remains clean
* future security audit stream is possible
* Observance can stay PI-safe
* logs remain useful for developers
* future queue/service extraction remains possible

Trade-offs:

* requires new model/dispatcher later
* requires redaction policy
* requires careful routing configuration
* requires avoiding audit noise
* requires clear tests so decision records do not become domain events

## Follow-up Tasks

Recommend:

1. `CODEX-018B — AccessDecision Trace Model Proposal`
2. `CODEX-018C — Authorization Observance Metrics Design`
3. `CODEX-018D — Local Authorization Decision Record Prototype`
4. `CODEX-018E — Security Audit Stream Boundary Discovery`

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
* Porta or Olorin behavior

## Validation

Run:

* `git diff --check -- docs`

No tests required.

## Deliverables

* ADR created in the correct existing ADR location
* report created under `docs/agents/reports/knowledge/`
* ADR location decision documented
* any stale docs found reported but not corrected unless directly necessary
* no commits
