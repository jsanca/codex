# ADR Prep Report — CODEX-018A: Authorization Decision Records and Bounded Security Audit Routing

## Task

`TaskCODEX-018A—ADRPrep:AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting`

## Date

2026-08-08

## Owner

Elito

## Scope

Documentation and ADR preparation only. No production code, tests, event classes, dispatcher,
consumers, Chronicon changes, Observance metrics, persistence, queues, or orchestration were
implemented.

This report prepares the future ADR. It does not create the ADR itself because the task only
allowed a draft under `docs/adr/` if that ADR location already existed as a ready project
pattern. The current repository uses other ADR locations, so creating a new `docs/adr/` tree
would introduce documentation structure drift.

## References Reviewed

* `docs/tasks/knowledge/TaskCODEX-018A—ADRPrep:AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting.md`
* `docs/agents/reports/knowledge/CODEX-018—AuthorizationAuditAndDecisionTraceDiscovery—REPORT.md`
* `docs/security/CUSTOS-MODEL.md`
* `docs/CODEX-ROADMAP-PHASES.md`
* `docs/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md`
* `docs/engineering/ENGINEERING_LOG.md`
* `docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md`
* `docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-RUNTIME-COMPOSITION.md`

## Problem Statement

Codex now fails closed for denied operations in the secured runtime: denied operations do not
mutate domain state, emit lifecycle events, invalidate cache, update index projections, or
create Chronicon audit records accidentally.

That correctness leaves a visibility question:

```text
When Custos authorizes, denies, or detects a security invariant violation, what durable or
diagnostic fact should Codex produce, and where should it go?
```

The answer must preserve visibility without turning authorization into an uncontrolled
reactive graph where decision facts bounce from consumer to consumer. Authorization visibility
does not currently require Saga orchestration, retry/DLQ/outbox, or distributed choreography.

## Terminology

| Term | Meaning | Boundary |
| --- | --- | --- |
| Authorization decision record | A structured record representing an authorization decision or security-relevant authorization outcome. | It is not a domain lifecycle event. |
| Security audit stream | A stream/category for security facts such as denied writes, privileged use, grant/revoke, invariant violations, and step-up outcomes. | It is semantically separate from Chronicon's domain audit stream. |
| Bounded decision routing | A finite routing model where a decision record moves from producer to configured final sink(s), then stops. | It is not open-ended event choreography. |
| Consumer / sink | A final processor of authorization decision records, such as diagnostic logging, Observance metrics, security audit, alerting, or no-op. | A sink should not emit more authorization decision events unless a future ADR explicitly permits it. |
| Domain audit | Chronicon's current audit stream for applied business/domain facts derived from domain events. | It should not record denied authorization attempts by default. |
| Security audit | Durable security-relevant record keeping. | It may eventually use Chronicon-like infrastructure, but its meaning remains separate from domain audit. |

## Proposed Bounded Routing Model

Preferred conceptual flow:

```text
Custos / AccessDecisionService
  -> AuthorizationDecisionRecord
  -> AuthorizationDecisionDispatcher
  -> configured final consumers/sinks
  -> done
```

Minimal direct form, still acceptable for the first local implementation:

```text
producer -> configured consumer/sink -> done
```

Default rule:

```text
producer -> dispatcher/router -> final consumer/sink -> done
```

Avoid:

```text
event -> consumer -> event -> consumer -> event -> consumer
```

The producer of an authorization decision must not know whether a decision is logged, counted,
persisted, ignored, queued, or alerted on. Runtime composition chooses active consumers.

## Chronicon Domain/Security Stream Separation

Chronicon should remain focused on business/domain audit.

A denied authorization attempt is not a business/domain fact because the protected domain
operation did not happen. It is a security fact.

Future architecture may still reuse or extend Chronicon-like audit infrastructure:

```text
Chronicon
  ├── domain audit stream
  └── security audit stream
```

The important rule is separation of meaning:

* domain audit = applied business/domain facts
* security audit = authorization/security-relevant facts

The storage choice is intentionally deferred. The future security audit stream may become a
dedicated stream, a dedicated category, a Chronicon-backed security audit store, or a pluggable
sink/consumer.

## Consumer/Sink Model

Potential consumers:

| Consumer / sink | Role | Notes |
| --- | --- | --- |
| Diagnostic logging sink | Writes useful diagnostic facts for developers/operators. | Not durable audit; avoid sensitive payload and full topology leakage. |
| Observance metrics sink | Records aggregate PI-safe counters/timers. | Avoid actor ids, resource ids, full reasons, or high-cardinality metric names. |
| Chronicon security audit sink | Persists selected security facts if Chronicon-like infrastructure is selected later. | Must remain separate from domain audit semantics. |
| Alerting sink | Emits high-priority signals for invariant violations or suspicious patterns. | Should be carefully bounded to avoid noise. |
| No-op sink | Discards records for tests/dev or deliberately quiet runtimes. | Useful default while the model matures. |
| External queue publisher | Future distributed sink. | Allowed later without changing the producer contract. |

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

## Explicit Non-Saga Decision

Codex does not need Saga orchestration for authorization decision visibility.

Authorization decision records should not introduce saga-style choreography, retry/DLQ/outbox,
or distributed process management by default. Arbitrieri remains the correct laboratory and
reference space for saga, retry, outbox, DLQ, and complex orchestration patterns.

Future distributed delivery may be useful, but the current decision shape must remain finite:

```text
producer -> dispatcher/router -> final consumer/sink -> done
```

## Questions Answered For The Future ADR

| Question | ADR-prep answer |
| --- | --- |
| Should records be created inside `AccessDecisionService`, secured decorators, or an observer/decorator? | Prefer a separate observer/decorator around `AccessDecisionService` or an explicit decision-record producer boundary. Keep `DefaultAccessDecisionService` pure and focused on mapping `PermissionResolution` to `AccessDecision`. |
| Emit both granted and denied decisions, or only selected categories? | Model the record type broadly, but route selectively. Denied writes, invariant violations, privileged use, and permission administration are stronger candidates than normal reads. |
| Which records are always logged? | Invariant violations should be warn-level. Normal grants/denials should be debug-level by default unless deployment policy raises selected categories. |
| Which records become Observance metrics? | Aggregate PI-safe counts and durations: decisions total, granted total, denied total, invariant violations total, decision latency, denied by safe permission category/resource type. |
| Which records go to the future security audit stream? | Denied writes, privileged reads if sensitive, privileged SUPER_ADMIN operations, role assignment changes, permission grant/revoke, invariant violations, and step-up outcomes. |
| Which records should be sampled or suppressed? | Normal granted reads and high-volume low-risk denials may be sampled, suppressed, or metrics-only to avoid audit noise. |
| How should PI/sensitive data be redacted? | Use actor id only if domain-safe; avoid full actor internals, sensitive resource payloads, full role registries, full assignment lists, and internal topology in external-facing output. |
| What correlation identifiers are needed? | Future records should allow correlation with operation/request context once Codex has a correlation model. Do not invent one in this task. |
| Should invariant violations be records, exceptions, or both? | Both eventually: they remain fatal exceptions and may also produce a high-priority security decision record. The record must not convert the invariant into a normal denial. |
| Minimal local implementation path? | Add a local decision-record model and bounded dispatcher/decorator with no-op/logging/metrics consumers first; keep persistence and distributed delivery deferred. |

## Recommendation Matrix

| Authorization fact | Record? | Logs | Observance | Security audit | Domain audit | Notes |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| Normal granted read | Optional | Debug | Aggregate count/timer only | Usually no | No | High volume; avoid durable audit noise. |
| Normal granted write | Optional/yes if sensitive | Debug/info | Aggregate count/timer | Optional by policy | Yes, if it emits a domain event | Domain audit records the applied business fact, not the authorization decision. |
| Denied read | Optional | Debug/info by policy | Aggregate denied count if PI-safe | Optional for sensitive resources | No | Avoid logging full resource payload or topology. |
| Denied write | Yes | Info/debug by policy | Aggregate denied count/timer | Yes, future candidate | No | Strong security fact because a mutation was attempted and blocked. |
| Privileged SUPER_ADMIN operation | Yes | Debug/info | Aggregate privileged count | Yes, future candidate | Yes, if operation changes domain state | Do not treat legitimate privileged use as warning by default. |
| AGENT + SUPER_ADMIN invariant violation | Yes | Warn | Invariant violation count | Yes, high priority | No | Fatal corrupted security state; not a normal denial. |
| Role assignment created | Yes | Info/debug | Admin operation count | Yes | No by default | Security administration fact, not ordinary domain lifecycle audit. |
| Role assignment revoked | Yes | Info/debug | Admin operation count | Yes | No by default | Same boundary as assignment creation. |
| Permission grant created | Yes | Info/debug | Admin operation count | Yes | No by default | Direct actor grants remain deferred; role/grant model may evolve. |
| Permission grant revoked | Yes | Info/debug | Admin operation count | Yes | No by default | Security audit should capture grant lifecycle once implemented. |
| Step-up approval required | Yes | Info/debug | Step-up required count | Yes | No | Future workflow/security boundary; not implemented now. |
| Step-up approval accepted | Yes | Info/debug | Step-up accepted count | Yes | No | May later relate to Olorin proposal flow. |
| Step-up approval denied | Yes | Info/debug | Step-up denied count | Yes | No | Redaction required before external exposure. |

## Required Principles For ADR

1. Low coupling
   * Custos produces or exposes authorization decision records.
   * Custos does not know active sinks.

2. Runtime-configured consumers
   * Runtime composition chooses no-op, logs, metrics, audit, alerting, or external queue sinks.

3. Bounded event flow
   * Decision records terminate at final consumers by default.
   * Chained consumer-generated decision events are not allowed without a future ADR.

4. Not domain events
   * Authorization decision records are not `SiteCreated`, `ContentTypeArchived`,
     `ContentItemPublished`, or any other domain lifecycle event.

5. Separate audit meanings
   * Domain audit records applied business/domain facts.
   * Security audit records authorization/security facts.

6. Future scalability
   * The first implementation can be local and simple.
   * The producer contract should survive future async executor, outbox, external queue,
     EventBridge-style routing, or dedicated security audit service.

7. No direct hard dependency from Custos
   * Custos does not directly depend on Chronicon, Observance, logging implementation,
     queue implementation, or alerting implementation.

## Risks

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Uncontrolled reactive event graph | Authorization visibility becomes hard to reason about and debug. | Enforce bounded routing: producer -> dispatcher/router -> final sink -> done. |
| Mixing domain and security audit | Chronicon domain history becomes polluted with facts about attempts that did not happen. | Keep domain audit and security audit semantically separate. |
| Sensitive data leakage | Logs/metrics/audit may expose actor internals, resource data, or permission topology. | Use PI-safe metrics, redaction policy, and internal/external trace boundaries. |
| Audit noise | Durable audit stream becomes too noisy to investigate. | Route selected categories; sample or suppress low-risk high-volume records. |
| Coupling Custos to infrastructure | Authorization correctness depends on audit/metrics/log sinks. | Use producer/decorator/dispatcher boundary; runtime wires consumers. |
| Invariants softened into denials | Fatal corrupted security state is misreported as normal access denial. | Keep invariant exceptions fatal; optionally also emit high-priority decision records. |
| Premature distributed design | Queue/retry/outbox complexity appears before the local boundary is proven. | Keep v1 local and finite; defer Saga and distributed delivery to future ADRs. |

## Proposed ADR

Suggested title:

```text
ADR — Authorization Decision Records and Bounded Security Audit Routing
```

Suggested decision statement:

```text
Codex will represent authorization outcomes as bounded authorization decision records that
may be routed by runtime-configured consumers/sinks. These records are not domain events.
Domain audit and security audit remain semantically separate. Custos does not depend directly
on Chronicon, Observance, logs, queues, or alerting.
```

Suggested ADR status: `Proposed`.

## Proposed Next Tasks

1. `CODEX-018A.1 — Write ADR: Authorization Decision Records and Bounded Security Audit Routing`
   * Convert this prep report into the canonical ADR once Jonathan confirms the ADR location.

2. `CODEX-018B — AccessDecision Trace Model Proposal`
   * Define structured trace fields, reason categories, redaction levels, and internal/external exposure rules.

3. `CODEX-018C — Authorization Observance Metrics Design`
   * Define PI-safe authorization counters/timers without implementing them yet.

4. `CODEX-018D — Local Authorization Decision Record Prototype Task`
   * Slice a future implementation that keeps `DefaultAccessDecisionService` pure and uses a bounded decorator/dispatcher.

5. `CODEX-018E — Security Audit Stream Boundary Discovery`
   * Decide whether the future security audit stream is Chronicon-backed, separate, or pluggable.

## Out Of Scope Preserved

* No production code changed.
* No tests changed.
* No event classes, dispatcher, consumers, or sinks implemented.
* No Chronicon changes.
* No Observance metrics added.
* No persistence, queue, outbox, retry, DLQ, or saga behavior introduced.
* No Porta or Olorin behavior changed.
* Stale JavaDoc wording in `RoleAssignment` and `PermissionGrant` was not modified.

## Validation

```bash
git diff --check -- docs
```

Result: passed.

No tests were run because this was documentation-only.
