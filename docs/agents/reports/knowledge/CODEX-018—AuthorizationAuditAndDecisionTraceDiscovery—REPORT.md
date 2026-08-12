# Knowledge Report — CODEX-018: Authorization Audit and Decision Trace Discovery

## Task

`TaskCODEX-018—AuthorizationAuditAndDecisionTraceDiscovery`

## Date

2026-08-08

## Owner

Elito

## Scope

Discovery and architecture framing only. This report clarifies where authorization-related
facts should live before Codex implements durable security audit or decision tracing.

No production code, tests, runtime wiring, Chronicon subscribers, Observance counters, or
logging behavior were changed.

## References Reviewed

* `docs/tasks/knowledge/TaskCODEX-018—AuthorizationAuditAndDecisionTraceDiscovery.md`
* `docs/agents/reports/runtime/CODEX-016-SecuredRuntimeComposition-REPORT.md`
* `docs/agents/reports/runtime/CODEX-017-DeniedSideEffectConsistency-REPORT.md`
* `docs/CODEX-ROADMAP-PHASES.md`
* `docs/ADR-corpus/ADR-010.md`
* `docs/ADR-corpus/ADR-011.md`
* `docs/future-forward/ADR-009.md`
* `docs/security/CUSTOS-MODEL.md`
* `codex-custos/src/main/java/codex/custos/api/model/AccessDecision.java`
* `codex-custos/src/main/java/codex/custos/api/model/PermissionResolution.java`
* `codex-custos/src/main/java/codex/custos/internal/service/DefaultAccessDecisionService.java`
* `codex-custos/src/main/java/codex/custos/internal/service/DefaultPermissionResolver.java`
* `codex-custos/src/main/java/codex/custos/api/exception/CustosAgentSuperAdminInvariantViolationException.java`

## Terminology

| Term | Definition | Boundary |
| --- | --- | --- |
| Domain event | A fact emitted after a successful domain operation, such as a lifecycle state change. | It is not an attempted operation and should not describe a rejected authorization request. |
| Domain audit | Chronicon's projection of domain events into business/history audit records. | It should describe applied domain facts, not operational timing or every failed access attempt. |
| Operational metric | Aggregate count or duration recorded through Observance. | It should be low-cardinality and should not carry rich actor/resource topology. |
| Diagnostic log | Technical narrative for developers, operators, or security responders. | It is not durable audit and should not be treated as the system of record. |
| Security audit event | A durable security-relevant fact, such as denied access, privileged permission use, grant/revoke, invariant violation, or step-up outcome. | It is distinct from Chronicon domain history unless Codex explicitly decides to merge them later. |
| Access decision trace | Structured explanation of how an authorization result was reached: actor, permission, resource, target scope, matched assignment/grant, implication path, and scope walk. | It is explanation data, not necessarily a persisted audit record. |
| Authorization decision record | A normalized representation of an authorization outcome that can be routed to configured consumers. | It is a source fact for logs, metrics, alerts, or security audit; it should not force Custos to depend on any sink. |
| Consumer / sink | A runtime-configured destination for a decision record, such as security audit, Observance metrics, diagnostic logging, alerting, or no-op. | It is not part of the core permission computation. |

## Current-State Assessment

Current secured runtime behavior is already fail-before-delegate:

* Denied operations throw `AccessDeniedException`.
* Secured decorators call permission services and `AccessDecision.requireGranted()` before delegating.
* Denied operations do not reach the raw domain service delegate.
* Denied operations do not mutate state.
* Denied operations do not emit domain lifecycle events.
* Denied operations do not accidentally create Chronicon audit records.
* Denied operations do not update index projections because no event is emitted.
* `CustosAgentSuperAdminInvariantViolationException` propagates as a fatal invariant violation, not as a denied decision.

What Codex does not yet have:

* No intentional durable security audit for denied authorization attempts.
* No authorization-specific Observance counters.
* No structured `AccessDecision` trace beyond the current human-readable reason.
* No dedicated security audit stream or repository.
* No persistence or redaction policy for authorization explanation data.

## Recommendation Matrix

| Authorization fact | Chronicon | Observance | Logs | Security audit | AccessDecision trace | Recommendation |
| --- | --- | --- | --- | --- | --- | --- |
| Granted normal domain operation | Yes, indirectly through resulting domain event if the operation changes domain state | Count/timer may be useful at authorization layer | Debug only if needed | Usually no separate security record unless operation is sensitive | Optional, normally ephemeral | Keep Chronicon focused on the applied domain fact. Do not duplicate every ordinary grant into security audit by default. |
| Denied read | No by default | Count denied reads by permission/resource type, avoid per-resource cardinality | Debug or info depending on deployment policy | Future security audit may record if the system needs access-attempt history | Useful for explainability and tests | Do not write to Chronicon. Prefer metrics + bounded logs now; model trace before durable security audit. |
| Denied write | No by default | Count denied writes by permission/resource type | Info or debug; warn only for suspicious patterns, not every denial | Strong candidate for future security audit | Useful and likely important | Keep out of Chronicon. Future security audit should capture durable denied writes with redaction. |
| AGENT + SUPER_ADMIN invariant violation | No | Count invariant violations | Warn is appropriate and safe if actor id is domain-safe; do not log full internals | Yes, high-priority security audit event later | Trace should identify invariant rule, not expose full topology externally | Treat as fatal security invariant, not denied access. Security audit should record this once the stream exists. |
| Role assignment created | Possibly not Chronicon unless role administration is modeled as a domain event accepted by Chronicon | Count admin operations | Info/debug | Yes | Trace not applicable except for authorizing the admin operation | Prefer future security audit stream for permission administration facts. Do not overload existing domain lifecycle Chronicon records. |
| Role assignment revoked | Possibly not Chronicon unless role administration is modeled as a domain event accepted by Chronicon | Count admin operations | Info/debug | Yes | Trace not applicable except for authorizing the admin operation | Same as assignment creation; security audit is the natural durable destination. |
| Permission grant created | Possibly not Chronicon unless grant administration becomes a domain event accepted by Chronicon | Count admin operations | Info/debug | Yes | Trace not applicable except for authorizing the admin operation | Security audit should capture grant/revoke history. Current direct actor grant support is still deferred. |
| Permission grant revoked | Possibly not Chronicon unless grant administration becomes a domain event accepted by Chronicon | Count admin operations | Info/debug | Yes | Trace not applicable except for authorizing the admin operation | Security audit later; avoid adding grant facts to existing lifecycle audit prematurely. |
| Privileged SUPER_ADMIN operation | Domain result appears in Chronicon if it changes domain state | Count privileged decisions separately if low-cardinality | Debug/info; avoid noisy warn for legitimate use | Yes, for sensitive operations | Trace should indicate bypass reason internally | Do not cache or over-log. Future security audit should record privileged use with minimal safe identifiers. |
| Step-up approval required | No unless step-up becomes a domain workflow event | Count required step-ups | Info/debug | Yes | Trace may explain why step-up was required | Future security audit or workflow audit, not current Chronicon lifecycle audit. |
| Step-up approval accepted | No unless step-up becomes a domain workflow event | Count accepted step-ups | Info/debug | Yes | Trace may link approval to original decision | Future security audit; may later integrate with Olorin proposal flow. |
| Step-up approval denied | No unless step-up becomes a domain workflow event | Count denied step-ups | Info/debug | Yes | Trace may explain denial reason | Future security audit; should be redacted for external exposure. |

## Proposed Architecture Direction

### 1. Keep Chronicon focused on applied domain facts

Chronicon's current ADR says it projects domain events into audit records. That boundary is
healthy. A denied authorization attempt is not an applied domain fact: the protected domain
operation did not happen, no lifecycle event was emitted, and no aggregate changed.

Recommendation: denied attempts should avoid Chronicon by default. If Codex later decides
to use Chronicon-like infrastructure for security audit, the meaning should stay separated:

```text
Chronicon / audit infrastructure
  ├── domain audit stream
  └── security audit stream
```

The key rule is separation of meaning:

* domain audit = applied business/domain facts
* security audit = authorization/security-relevant facts

This could become a Chronicon-backed security audit store, a dedicated stream, or a pluggable
consumer. Storage should not be decided before the boundary is clear.

### 2. Use Observance for aggregate authorization telemetry

Observance is the right destination for counters and timers:

* authorization decisions evaluated
* authorization granted/denied counts
* invariant violation counts
* authorization decision latency
* security audit write failures, once that stream exists

Metrics should avoid high-cardinality names that embed actor ids, resource keys, or full
decision reasons. Prefer dimensions that can be kept bounded, or delay labels until the
Observance model supports them intentionally.

Safe metric candidates:

* authorization decisions total
* authorization granted total
* authorization denied total
* authorization invariant violations total
* authorization decision latency
* denied decisions by permission category, if safe
* denied decisions by resource type, if safe

### 3. Keep logs diagnostic and bounded

Current Custos DEBUG logs include actor id, permission, target scope, granted flag, and
reason. That is useful for local diagnosis, but logs must not become durable audit.

Recommendation:

* normal grants/denials: debug-level by default
* suspicious repeated denials: future policy, not current scope
* `AGENT + SUPER_ADMIN`: warn-level is appropriate because it is corrupted security state
* avoid logging full role registries, all assignments, or permission topology
* actor id is acceptable if the domain considers it safe, but no richer actor internals
* correlation identifiers are preferred once Codex has a correlation model

### 4. Add AccessDecision trace before security audit persistence

Security audit records need stable fields. Today `AccessDecision` and `PermissionResolution`
carry only a human-readable reason, not a structured trace. Persisting the current reason
string would make audit semantics depend on prose.

Recommendation: define a small, internal trace model before durable security audit. The
trace should explain resolver behavior without forcing `AccessDecision` itself to become
heavy for every call.

Likely trace concepts:

* evaluated actor id and actor type
* permission key
* resource ref
* target scope
* outcome
* reason code or reason category
* matched role assignment, if any
* matched role key, if any
* implication path, if any
* scope walk path, if any
* hard invariant rule, if any
* redaction/exposure level

### 5. Treat AccessDecision as a future event/record source

An authorization decision can be treated like a routable fact. The important design idea is
not a specific broker, queue, or event bus. The important idea is routing by meaning:

```text
event/category/payload
  -> route to appropriate audit/log/metric sink
```

Possible authorization decision records:

```text
AuthorizationDecisionRecorded
AuthorizationDenied
AuthorizationGranted
AuthorizationInvariantViolated
PrivilegedAuthorizationUsed
```

Possible consumers:

* security audit consumer
* Observance metrics consumer
* diagnostic logging consumer
* alerting consumer
* no-op consumer for tests/dev

Consumers should be enabled or disabled by runtime composition/configuration. This keeps
the authorization core independent while allowing production deployments to choose the
right security posture.

### 6. Introduce a dedicated security audit stream later

Codex likely needs a durable security audit concept, but not by coupling Custos directly
to Chronicon or Observance.

Preferred direction:

```text
Custos
  -> emits or exposes authorization decision event/record
  -> configured consumers process it
```

This keeps Custos transport-agnostic and avoids making the default decision service own
persistence, metrics, or audit side effects.

## Recommendation Summary

* Chronicon should remain focused on applied domain facts.
* Denied authorization attempts should avoid Chronicon by default.
* Observance should receive aggregate authorization metrics in a future task.
* Logs should remain diagnostic; normal denials should not be warning-level by default.
* A dedicated security audit stream is likely needed later.
* AccessDecision should be treated as a possible event/record source.
* Authorization decision consumers should be runtime-configurable and may include security
  audit, Observance, logs, alerting, or no-op.
* AccessDecision trace should be designed before durable security audit persistence.
* Custos should not directly depend on Chronicon or Observance for security audit.

## Proposed Next Tasks

1. `CODEX-018A — ADR: Authorization Audit Boundaries`
   * Decide and document the permanent boundary between Chronicon domain audit,
     Observance metrics, diagnostic logs, security audit, and decision trace.

2. `CODEX-018B — AccessDecision Trace Model Proposal`
   * Propose structured trace fields, redaction levels, and whether trace is always
     computed, optionally computed, or debug/test-only.

3. `CODEX-018C — Observance Authorization Counters`
   * Add aggregate counters/timers around authorization evaluation without changing
     authorization semantics.

4. `CODEX-018D — Security Audit Event Model`
   * Design a future durable event model for denied attempts, privileged operations,
     role assignment changes, grant/revoke, invariant violations, and step-up outcomes.

## Risks

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Leaking sensitive authorization topology | Logs or traces may expose role names, scope paths, or permission graph details to the wrong audience. | Define redaction rules before external exposure or durable security audit. |
| Polluting domain history with denied attempts | Chronicon queries would mix actual domain changes with operations that never happened. | Keep denied attempts out of Chronicon unless an ADR explicitly changes Chronicon's scope. |
| Audit noise | Persisting every normal denial could overwhelm useful security signals. | Start with metrics and selected security audit categories; add sampling or policy later. |
| Heavy `AccessDecision` objects | Adding full trace to every decision may increase allocation and API complexity. | Keep trace optional or separate until its consumers are clear. |
| Coupling Custos to Chronicon/Observance | Authorization logic could become dependent on audit/telemetry availability. | Use decorators or outer-layer observers; default resolver/decision service stays pure. |
| Exposing internal permission topology externally | API clients may learn role assignment or implication internals from denial details. | Separate internal trace from externally safe denial messages. |
| Treating fatal invariants as denials | Corrupted security state could be hidden as ordinary access denial. | Keep invariant exceptions fatal and uncaught by `AccessDecisionService`. |

## Stale Or Conflicting Documentation Found

`docs/security/CUSTOS-MODEL.md` still states that `AccessDecisionService` wiring is pending
and that the resolver does not produce `AccessDecision` yet. Repository evidence shows
`DefaultAccessDecisionService` exists and maps `PermissionResolution` into `AccessDecision`.

This report does not change that file because CODEX-018 is a discovery artifact, not a
general documentation reconciliation task. Suggested follow-up: update `CUSTOS-MODEL.md`
in a focused docs sync.

## Validation

No tests were run because this task is documentation-only.

Recommended validation for this report:

```bash
git diff --check -- docs/agents/reports/knowledge/CODEX-018—AuthorizationAuditAndDecisionTraceDiscovery—REPORT.md
```

## Acceptance Criteria

* No production code changed.
* Terms are clearly separated.
* Recommendation does not force denied attempts into Chronicon.
* Chronicon remains domain/audit history.
* Observance remains operational metrics.
* Logs remain diagnostic.
* Security audit and AccessDecision trace are framed as future architecture work.
