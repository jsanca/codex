# Knowledge Report — CODEX-018B: AccessDecision Trace Model Proposal

## Task

`TaskCODEX-018B—AccessDecisionTraceModelProposal`

## Date

2026-08-12

## Owner

Elito

## Scope

Documentation and model proposal only.

No production code, tests, dispatcher, consumers, Chronicon security audit stream,
Observance metrics, logging sink, external API behavior, persistence, queue/outbox,
saga/orchestration, Olorin behavior, or actor boundary integration tests were implemented.

## References Reviewed

* `docs/agents/tasks/knowledge/TaskCODEX-018B—AccessDecisionTraceModelProposal.md`
* `docs/ADR-corpus/ADR-012.md`
* `docs/agents/reports/knowledge/CODEX-018A—AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting—REPORT.md`
* `docs/agents/reports/knowledge/CODEX-018—AuthorizationAuditAndDecisionTraceDiscovery—REPORT.md`
* `docs/security/CUSTOS-MODEL.md`
* `codex-custos/src/main/java/codex/custos/api/model/AccessDecision.java`
* `codex-custos/src/main/java/codex/custos/api/model/PermissionResolution.java`
* `codex-custos/src/main/java/codex/custos/api/model/PermissionResolutionRequest.java`
* `codex-custos/src/main/java/codex/custos/api/model/PermissionResolutionSnapshot.java`
* `codex-custos/src/main/java/codex/custos/api/model/RoleAssignment.java`
* `codex-custos/src/main/java/codex/custos/api/model/PermissionGrant.java`
* `codex-custos/src/main/java/codex/custos/api/model/ResourceRef.java`
* `codex-custos/src/main/java/codex/custos/api/model/ResourceScope.java`
* `codex-custos/src/main/java/codex/custos/api/model/PermissionKey.java`
* `codex-custos/src/main/java/codex/custos/internal/service/DefaultPermissionResolver.java`
* `codex-custos/src/main/java/codex/custos/internal/service/DefaultAccessDecisionService.java`

## Problem Statement

ADR-012 establishes that authorization outcomes may become bounded authorization decision
records routed to configured sinks. Before implementation, Codex needs to decide what
explanation data belongs in a trace, which parts are safe for each audience, and how to avoid
turning every authorization check into a heavyweight or sensitive object.

Current `AccessDecision` and `PermissionResolution` carry a human-readable reason. That is
useful, but free text is not enough for durable security audit, PI-safe metrics, or structured
support tooling. It is also risky to expose resolver internals such as role assignments,
scope walk paths, or implication paths without a redaction boundary.

## Terminology

| Term | Definition | Boundary |
| --- | --- | --- |
| Authorization decision record | A routable record representing an authorization outcome or security-relevant authorization fact. | It is not a domain event. |
| AccessDecision trace | Structured explanation of how authorization reached its result. | It explains; it does not choose sinks. |
| Reason category | Stable machine-readable category for why a decision was granted, denied, or stopped. | It should not depend on prose wording. |
| Human reason | Developer-readable text currently carried by `AccessDecision` and `PermissionResolution`. | Useful for diagnostics; not stable enough as the only audit reason. |
| Internal trace | Fullest available explanation for tests/support/security engineering. | May include sensitive topology and must not be externally exposed by default. |
| Redacted view | Audience-specific projection of a trace. | Each sink receives only fields appropriate to its purpose. |
| Correlation reference | Future link to request/operation context. | Not implemented today; should be allowed later. |

## Proposed Trace Fields

Conceptual authorization decision record:

```text
AuthorizationDecisionRecord
  decisionId
  occurredAt
  actorRef
  actorType
  permissionKey
  resourceRef
  resourceType
  targetScope
  targetScopeType
  outcome
  reasonCategory
  humanReason
  traceView
  redactionProfile
  correlationRef
```

Conceptual trace payload:

```text
AuthorizationDecisionTrace
  matchedRoleKey
  matchedAssignmentRef
  matchedGrantRef
  evaluatedScope
  scopeWalkPath
  implicationPath
  invariantViolationType
  privilegedBypass
  missingAssignment
  missingPermission
```

Notes:

* `matchedGrantRef` is conceptual. The current `Role` model stores `Set<PermissionKey>`;
  `PermissionGrant` remains `PermissionKey + ResourceScope` and does not carry actor or role.
* `matchedAssignmentRef` should be a stable reference if/when assignments receive identity.
  Until then, avoid logging the whole assignment object.
* `scopeWalkPath` and `implicationPath` are sensitive topology. They are valuable internally
  but should not be exposed broadly.
* `humanReason` should remain useful, but durable consumers should prefer `reasonCategory`.

## Reason Category Proposal

Start with a small stable set:

| Category | Meaning |
| --- | --- |
| `ROLE_GRANT` | A role assigned to the actor granted the requested permission. |
| `SUPER_ADMIN_BYPASS` | A valid non-agent actor was granted through scoped `SUPER_ADMIN` bypass. |
| `PERMISSION_NOT_GRANTED` | No assignment/role/permission combination granted the requested permission. |
| `SCOPE_MISMATCH` | Relevant authorization data existed, but not at a scope covering the target. |
| `MISSING_ROLE_ASSIGNMENT` | The actor had no relevant role assignment. |
| `MISSING_ROLE_BLUEPRINT` | A role assignment referenced a role key absent from the role registry. |
| `IMPLIED_PERMISSION` | The requested permission was granted through an implication rule. |
| `ACTOR_TYPE_FORBIDDEN` | The actor type is not allowed for the requested authorization path. |
| `INVARIANT_VIOLATION` | Security state is invalid and evaluation must not continue. |
| `UNKNOWN` | Fallback for legacy/uncategorized outcomes. |

These categories should be stable enough for logs, metrics, security audit, and tests. The
free-text reason can still provide local diagnostic context, but it should not be the primary
machine contract.

## Redaction Model

| Redaction level | Intended use | May include | Must exclude or minimize |
| --- | --- | --- | --- |
| `INTERNAL_FULL` | tests, privileged support tooling, security engineering | full trace, scope walk, implication path, matched role key, safe assignment/grant references | raw sensitive payloads, secrets, external actor internals |
| `SECURITY_AUDIT` | durable security audit | actor id, actor type, permission, resource type/id, target scope, outcome, reason category, invariant type, timestamp, correlation ref | full role registry, full assignment list, full scope walk unless explicitly needed |
| `DIAGNOSTIC_LOG` | developer/operator logs | actor id if domain-safe, actor type, permission, resource type/id if safe, outcome, reason category, sanitized human reason, correlation ref | full topology, full snapshot, full assignment objects, sensitive content payloads |
| `METRICS_SAFE` | Observance | outcome, reason category, actor type, permission category, resource type, latency/counts | actor id, resource id, target scope value, human reason, role keys, assignment/grant refs |
| `EXTERNAL_SAFE` | API/UI denial messages | outcome, permission category if appropriate, stable external reason category, correlation ref | actor internals, role topology, scope walk, role keys, assignment/grant refs, internal human reason |

## Consumer / Audience Matrix

| Field | Internal trace | Security audit | Logs | Observance | External API | Notes |
| --- | ---: | ---: | ---: | ---: | ---: | --- |
| actor id | Yes | Yes | Maybe | No | No | Logs may include only if actor id is domain-safe. |
| actor type | Yes | Yes | Yes | Yes | Maybe | Low-cardinality and useful. |
| permission key | Yes | Yes | Yes | Maybe | Maybe | Metrics should prefer permission category if cardinality grows. |
| resource type | Yes | Yes | Yes | Yes | Maybe | Safe and low-cardinality. |
| resource id | Yes | Yes | Maybe | No | No | Avoid in metrics; logs only if safe. |
| target scope type | Yes | Yes | Yes | Yes | Maybe | Low-cardinality. |
| target scope value | Yes | Maybe | Maybe | No | No | Can reveal tenancy/topology. |
| decision outcome | Yes | Yes | Yes | Yes | Yes | Core field. |
| reason category | Yes | Yes | Yes | Yes | Maybe | Prefer over free-text for machines. |
| human reason | Yes | Maybe | Maybe | No | No | Sanitize before logs; avoid durable reliance on prose. |
| matched role key | Yes | Maybe | No by default | No | No | Reveals role topology. |
| matched role assignment id | Yes | Maybe | No | No | No | Only after assignment identity exists. |
| matched permission grant | Yes | Maybe | No | No | No | Current model does not bind grants to actor/role. |
| scope walk path | Yes | Rare | No | No | No | Sensitive topology. |
| permission implication path | Yes | Maybe | No by default | No | No | Useful in security audit only if needed. |
| invariant violation type | Yes | Yes | Yes | Yes | Maybe | Fatal condition; still redacted externally. |
| correlation id | Yes | Yes | Yes | Maybe | Yes | Preferred future link across logs/audit/support. |
| timestamp | Yes | Yes | Yes | Maybe | Maybe | Usually safe and necessary for audit. |

## Answers To Design Questions

| Question | Recommendation |
| --- | --- |
| Should every authorization decision have a trace? | Every decision may have a minimal record. Full traces should be opt-in, sampled, or generated for selected categories to avoid cost and leakage. |
| Should normal granted reads create full traces? | No by default. Use metrics-only or minimal records; full traces only for tests/debug/sampling. |
| Minimum trace for denied writes? | actor id/type, permission, resource type/id, target scope, outcome, reason category, sanitized reason, timestamp, correlation ref if available. |
| Minimum trace for invariant violations? | actor id/type, invariant violation type, permission if available, target scope if available, timestamp, correlation ref if available. Keep exception fatal. |
| What is safe for Observance? | outcome, reason category, actor type, permission category, resource type, counts, latency. No actor id, resource id, full scope value, human reason, or topology. |
| What is safe for logs? | actor id if domain-safe, actor type, permission, resource type/id if safe, outcome, reason category, sanitized reason, correlation ref. |
| What belongs only in security audit/internal trace? | matched role key, assignment/grant references, scope walk path, implication path, invariant type details, privileged bypass details. |
| What must never be exposed externally by default? | role topology, full assignments, full scope walk, implication path, role registry, permission graph internals, sensitive resource payloads. |
| Include matched role/grant details? | Yes for internal traces; security audit may include references selectively. Logs/metrics/external views should not include them by default. |
| Should invariant violations remain exceptions and also create trace records? | Yes eventually. They remain fatal exceptions and may also emit high-priority decision records. The record must not downgrade them to normal denials. |
| How does this support future tests? | Tests can assert reason category, outcome, safe fields, and selected trace fields without parsing prose reasons. |

## Relationship To AccessDecision

Recommendation: do not embed a full trace directly in `AccessDecision` by default.

Reasons:

* `AccessDecision` is currently simple and caller-facing: actor, permission, resource, reason,
  and granted/denied behavior.
* Full trace data may include sensitive topology that many callers should never receive.
* Normal read checks may be high-volume; attaching full traces to all decisions increases
  allocation and accidental exposure risk.
* Some trace fields are resolver-internal (`scopeWalkPath`, `matchedRoleKey`,
  implication path), while `DefaultAccessDecisionService` is intentionally a thin adapter.

Preferred direction:

```text
DefaultAccessDecisionService
  -> remains pure mapper from PermissionResolution to AccessDecision

Tracing/recording decorator or observer
  -> creates AuthorizationDecisionRecord
  -> attaches minimal safe decision fields
  -> includes optional internal trace when available
  -> routes through bounded consumers/sinks from ADR-012
```

Full trace may require future resolver support. An outer observer around
`AccessDecisionService` can record minimal decision facts today, but it cannot reliably know
the complete scope walk or matched role without structured data from the resolver.

## Recommended Implementation Path

1. Define reason categories before adding durable storage.
   * This prevents audit and metrics from depending on free-text reasons.

2. Introduce a minimal authorization decision record concept.
   * Include safe fields available from `AccessDecisionRequest`, `AccessDecision`, and fatal
     invariant exceptions.

3. Add a bounded recording/dispatch boundary outside `DefaultAccessDecisionService`.
   * Keep the default service pure.
   * Preserve ADR-012's finite flow.

4. Add resolver trace support only where needed.
   * Start with selected categories: denied writes, `SUPER_ADMIN` bypass, invariant violations,
     permission administration, and tests.

5. Add redacted views per sink.
   * Metrics view first: aggregate and PI-safe.
   * Logs view second: diagnostic and sanitized.
   * Security audit view later: durable and policy-driven.

## Risks

| Risk | Impact | Mitigation |
| --- | --- | --- |
| Role topology leakage | External users or broad logs may reveal role names, assignments, or scope hierarchy. | Keep full trace internal; use redacted views per sink. |
| Metrics cardinality explosion | Actor/resource/scope values in metrics can break observability systems and leak PI. | Observance only receives aggregate low-cardinality fields. |
| Free-text reason coupling | Tests/audit/metrics become brittle if they parse human reasons. | Introduce stable reason categories. |
| Heavy decisions | Embedding full traces in every `AccessDecision` increases allocation and accidental exposure. | Keep trace separate or optional. |
| Invariant downgrade | Fatal security invariants could be treated as normal denials. | Keep exceptions fatal; records supplement but do not replace exceptions. |
| Partial trace confusion | A minimal observer may look complete even without resolver internals. | Label trace completeness and add full resolver trace only when available. |
| Audit noise | Recording every normal read can overwhelm useful security signals. | Sample, suppress, or metrics-only for low-risk high-volume decisions. |

## Proposed Follow-Up Tasks

1. `CODEX-018B.1 — ADR: AccessDecision Trace Redaction Model`
   * Promote the redaction model and field matrix to a formal ADR if Jonathan wants a durable decision before implementation.

2. `CODEX-018C — Authorization Observance Metrics Design`
   * Define PI-safe metric names and aggregation rules using this report's `METRICS_SAFE` boundary.

3. `CODEX-018D — Local Authorization Decision Record Prototype`
   * Implement a minimal local record/decorator/dispatcher path without persistence or distributed routing.

4. `CODEX-018F — Resolver Trace Support Discovery`
   * Decide how `PermissionResolver` can expose optional structured trace data without making normal resolution heavy.

5. `CODEX-KNOW — Fix stale Custos JavaDoc wording`
   * Update future-facing JavaDoc in `RoleAssignment` and `PermissionGrant` once production-code documentation cleanup is explicitly authorized.

## Validation

```bash
git diff --check -- docs
```

Result: passed.

No tests were run because this was documentation-only.
