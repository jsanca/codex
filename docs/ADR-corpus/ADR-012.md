# ADR-012: Authorization Decision Records and Bounded Security Audit Routing

## Status

Proposed

## Context

Custos now protects Codex domain services through secured decorators and secured runtime
composition. In the secured runtime, denied operations fail before delegation: they do not
mutate domain state, emit domain lifecycle events, update index projections, or create
Chronicon domain audit records accidentally.

That behavior is correct, but it leaves a visibility question:

```text
When authorization is granted, denied, or fails because of a security invariant violation,
what should Codex record, and where should that fact go?
```

Codex already separates several kinds of system knowledge:

| Layer | Meaning |
| --- | --- |
| Domain events | Facts about successful domain operations. |
| Chronicon domain audit | Business/domain history projected from domain events. |
| Observance | Aggregate counts and timings. |
| Logs | Diagnostic narrative. |
| Security audit | Future durable record of security-relevant facts. |

A denied authorization attempt is not a business/domain fact. The protected operation did
not happen. It is a security fact.

At the same time, Codex should not become an uncontrolled reactive system where events bounce
across consumers and produce a graph that is hard to reason about. Authorization visibility
does not currently require Saga orchestration, retry/DLQ/outbox, or distributed choreography.
Arbitrieri remains the appropriate laboratory for saga and complex orchestration patterns.

## Decision

Codex will represent authorization outcomes as bounded authorization decision records that
may be routed by runtime-configured consumers/sinks.

These records are not domain events. Domain audit and security audit remain semantically
separate. Custos does not depend directly on Chronicon, Observance, logs, queues, or alerting.

The default routing shape is finite:

```text
producer -> dispatcher/router -> final consumer/sink -> done
```

The direct form is also acceptable for a first local implementation:

```text
producer -> configured consumer/sink -> done
```

Codex should avoid uncontrolled chains:

```text
event -> consumer -> event -> consumer -> event -> consumer
```

unless a future ADR explicitly introduces orchestration for that boundary.

## Authorization Decision Records

An authorization decision record is a structured representation of an authorization outcome
or security-relevant authorization fact.

Examples include:

* authorization granted
* authorization denied
* invariant violation
* privileged authorization used
* step-up approval required
* step-up approval accepted
* step-up approval denied

Authorization decision records are not domain lifecycle events. They must not be mixed with
events such as:

* `SiteCreated`
* `ContentTypeArchived`
* `ContentItemPublished`

Domain lifecycle events describe business/domain facts that happened. Authorization decision
records describe authorization/security facts about whether execution was allowed, denied,
or stopped because security state was invalid.

## Audit Stream Separation

Chronicon's current audit stream records applied business/domain facts. It should remain clean.

Conceptually, future audit infrastructure may support separate streams:

```text
Chronicon
  ├── domain audit stream
  └── security audit stream
```

The important separation is meaning, not storage technology:

* domain audit records applied business/domain facts
* security audit records security facts

Security audit candidates include:

* denied writes
* privileged reads, if classified as sensitive
* privileged `SUPER_ADMIN` operations
* role assignment created
* role assignment revoked
* permission grant created
* permission grant revoked
* `AGENT` + `SUPER_ADMIN` invariant violations
* future step-up approval required/accepted/denied outcomes

The security audit stream may later be implemented as a dedicated stream, a dedicated
category, a Chronicon-backed security audit store, or a pluggable sink/consumer. This ADR
does not choose storage.

## Consumer / Sink Model

Runtime composition owns consumer selection.

Possible consumers/sinks:

| Consumer / sink | Role |
| --- | --- |
| No-op sink | Discards decision records for tests, development, or deliberately quiet runtimes. |
| Diagnostic logging sink | Writes useful diagnostic facts without becoming durable audit. |
| Observance metrics sink | Records PI-safe aggregate counters and timers. |
| Chronicon security audit sink | Persists selected security facts if Chronicon-like infrastructure is selected later. |
| Alerting sink | Emits high-priority signals for invariant violations or suspicious patterns. |
| External queue publisher | Future distributed delivery path. |

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

A sink should not normally emit additional authorization decision records. If a future
consumer must produce follow-up messages, that behavior needs a separate ADR because it
changes bounded routing into orchestration.

## Low Coupling

Custos may produce or expose authorization decision records, but it must not directly depend
on:

* Chronicon
* Observance
* logging implementation
* queue implementation
* alerting implementation

The producer of an authorization decision must not know whether the record is logged, counted
as a metric, persisted to security audit, ignored, sent to a queue, or alerted on.

Runtime composition wires consumers.

## First Implementation Direction

The first implementation should be local and simple.

Preferred future shape:

```text
AccessDecisionService decorator / observer
  -> AuthorizationDecisionRecord
  -> AuthorizationDecisionDispatcher
  -> configured final consumers/sinks
  -> done
```

`DefaultAccessDecisionService` should remain focused on translating `PermissionResolution`
into `AccessDecision`. It should not own security audit persistence, metrics, logging sinks,
or queue publishing.

Future implementations may replace or extend the local dispatcher with:

* async executor
* outbox
* external queue
* EventBridge-style routing
* dedicated security audit service
* implementation in another language

The producer contract should survive those changes.

## Alternatives Considered

### Put denied authorization attempts into Chronicon domain audit

Rejected.

Denied attempts are not applied business/domain facts. Writing them into the same Chronicon
domain audit stream would pollute business history with operations that did not happen.

### Let Custos call Chronicon, Observance, or logs directly

Rejected.

This would couple the authorization core to infrastructure and visibility sinks. Custos
should remain focused on authorization semantics. Runtime composition should choose sinks.

### Full event-driven choreography

Rejected for this boundary.

Authorization decision visibility needs finite routing, not saga-style choreography.
Unbounded chains make the system hard to track and debug. A future ADR may introduce
orchestration where it is genuinely needed.

### Do nothing / logs only

Rejected.

Logs are useful diagnostics, but they are not reliable durable audit and they are not an
aggregate metrics source of truth. Codex needs a path toward security audit and PI-safe
operational metrics.

### Distributed queue/outbox immediately

Deferred.

Distributed delivery may be useful later, but it is premature for the current local runtime.
The first design should preserve a producer contract that can later route to an outbox,
queue, or external service without changing authorization semantics.

## Consequences

### Positive

* Security visibility can grow without coupling Custos to infrastructure sinks.
* Chronicon domain audit remains focused on applied business/domain facts.
* A future security audit stream remains possible.
* Observance can receive PI-safe aggregate metrics without storing sensitive details.
* Logs remain useful for developers and operators without becoming durable audit.
* Future queue, outbox, or service extraction remains possible.
* Authorization visibility stays bounded and understandable.

### Trade-offs

* A new decision-record model and routing boundary will be needed later.
* Redaction policy must be designed before records are exposed externally or persisted.
* Runtime configuration must be careful to avoid missing required security sinks.
* Audit noise must be managed through selective routing, sampling, or suppression.
* Tests must preserve the distinction between authorization decision records and domain events.

## Follow-Up Work

1. `CODEX-018B — AccessDecision Trace Model Proposal`
   * Define structured trace fields, reason categories, redaction levels, and internal/external exposure rules.

2. `CODEX-018C — Authorization Observance Metrics Design`
   * Define PI-safe authorization counters/timers without changing authorization semantics.

3. `CODEX-018D — Local Authorization Decision Record Prototype`
   * Slice a first implementation that keeps `DefaultAccessDecisionService` pure and uses a bounded decorator/dispatcher.

4. `CODEX-018E — Security Audit Stream Boundary Discovery`
   * Decide whether the future security audit stream is Chronicon-backed, separate, or pluggable.

## Non-Goals

This ADR does not implement or define concrete APIs for:

* Java classes
* tests
* event classes
* dispatchers
* consumers
* Chronicon changes
* Observance metrics
* persistence
* queues
* outbox/retry/DLQ
* Porta behavior
* Olorin behavior
* saga orchestration
