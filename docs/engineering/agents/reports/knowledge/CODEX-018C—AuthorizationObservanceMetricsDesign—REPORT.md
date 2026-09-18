# Knowledge Report — CODEX-018C: Authorization Observance Metrics Design

## Task

`TaskCODEX-018C—AuthorizationObservanceMetricsDesign`

## Date

2026-08-12

## Owner

Elito

## Scope

Documentation and metrics design only.

No Java code, tests, Observance counters, timers, metric registry changes, authorization
decision record classes, dispatcher, consumers, Chronicon changes, security audit stream,
persistence, queues, outbox/retry/DLQ, REST/Porta behavior, or Olorin behavior were
implemented.

## References Reviewed

* `docs/engineering/agents/tasks/knowledge/TaskCODEX-018C—AuthorizationObservanceMetricsDesign.md`
* `docs/adr/ADR-012-authorization-decision-records.md`
* `docs/adr/ADR-011-observance-metrics-foundation.md`
* `docs/engineering/agents/reports/knowledge/CODEX-018A—AuthorizationDecisionRecordsAndBoundedSecurityAuditRouting—REPORT.md`
* `docs/engineering/agents/reports/knowledge/CODEX-018B—AccessDecisionTraceModelProposal—REPORT.md`
* `docs/security/CUSTOS-MODEL.md`

## Problem Statement

Authorization will become a high-signal operational surface once Codex exposes secured
runtimes to adapters and external entrypoints. Operators need to know whether authorization
is working, whether denials are increasing, whether invariant violations occur, and whether
decision evaluation is slow.

At the same time, authorization metrics are security-sensitive. Metrics must not leak actor
identity, resource identity, site topology, role topology, permission graph details, or free
text reasons. Observance should provide aggregate operational telemetry, not durable security
audit or forensic trace data.

## Observance Role In Authorization Visibility

Observance answers operational questions:

* Are authorization decisions being evaluated?
* Are denied decisions increasing?
* Are invariant violations happening?
* Are authorization checks slow?
* Which broad permission/resource categories are noisy?
* Is privileged access being used frequently?

Observance does not answer forensic audit questions:

* Which exact actor touched which exact resource?
* Which role assignment matched?
* Which scope walk path was followed?
* Which permission implication path granted access?
* What sensitive payload was being protected?

Those questions belong to internal trace, diagnostic logs, or a future security audit stream,
depending on redaction and durability requirements.

## Metric Production Boundary

Metrics should be produced by a final consumer/sink, not by Custos core.

Preferred future shape:

```text
AuthorizationDecisionRecord
  -> AuthorizationDecisionDispatcher
  -> ObservanceAuthorizationMetricsConsumer
  -> Observance
  -> done
```

Rules:

* `DefaultAccessDecisionService` remains pure.
* Custos does not directly depend on Observance.
* The metrics consumer receives only a redacted `METRICS_SAFE` view.
* The metrics consumer is a final sink and does not emit additional authorization decision records.
* The bounded flow from ADR-012 remains intact.

The current Observance API uses metric names only and does not define tags/labels. This report
uses "tags" as conceptual dimensions for future adapters or encoded metric-name policy. If
Codex keeps name-only Observance for v1, these dimensions should be folded into bounded
metric-name segments without including identifiers.

## Metric Categories

### Authorization Decisions Total

Conceptual metric:

```text
codex.authorization.decisions.total
```

Purpose: count all authorization decisions that reach the authorization decision record layer.

Safe dimensions:

* `outcome`
* `reason_category`
* `actor_type`
* `permission_category`
* `resource_type`
* `runtime_security_mode`

### Authorization Denied Total

Conceptual metric:

```text
codex.authorization.denied.total
```

Purpose: track blocked access attempts without storing actor/resource identity.

Safe dimensions:

* `reason_category`
* `actor_type`
* `permission_category`
* `resource_type`
* `runtime_security_mode`

### Authorization Invariant Violations Total

Conceptual metric:

```text
codex.authorization.invariant_violations.total
```

Purpose: count fatal security invariant violations, such as `AGENT + SUPER_ADMIN`.

Safe dimensions:

* `invariant_type`
* `actor_type`
* `runtime_security_mode`

### Authorization Decision Duration

Conceptual metric:

```text
codex.authorization.decision.duration
```

Purpose: measure authorization decision latency.

Safe dimensions:

* `outcome`
* `actor_type`
* `permission_category`
* `resource_type`
* `runtime_security_mode`

Latency should be measured around the authorization evaluation boundary that creates the
decision record. It should include resolver/decision-service time, but not downstream sink
delivery time. Sink delivery can have separate metrics later.

### Privileged Authorization Use Total

Conceptual metric:

```text
codex.authorization.privileged_use.total
```

Purpose: detect aggregate use of privileged bypass paths.

Safe dimensions:

* `actor_type`
* `permission_category`
* `resource_type`
* `runtime_security_mode`

Do not include actor id or scope value. Security audit may later persist exact privileged
operation details; Observance should only show aggregate trend.

### Permission Administration Total

Conceptual metric:

```text
codex.authorization.admin_operations.total
```

Purpose: future metric for role/grant/permission administration operations.

Safe dimensions:

* `operation_category`
* `outcome`
* `actor_type`
* `runtime_security_mode`

This metric is future-oriented. It should not imply direct actor grants are implemented.

## Proposed Metric Names

| Metric | Type | Purpose |
| --- | --- | --- |
| `codex.authorization.decisions.total` | Counter | Total authorization decision volume. |
| `codex.authorization.denied.total` | Counter | Denied authorization volume. |
| `codex.authorization.invariant_violations.total` | Counter | Fatal invariant violation volume. |
| `codex.authorization.decision.duration` | Timer | Authorization evaluation latency. |
| `codex.authorization.privileged_use.total` | Counter | Aggregate privileged bypass/use volume. |
| `codex.authorization.admin_operations.total` | Counter | Future role/grant administration operation volume. |

If v1 must use name-only Observance, use bounded suffixes only from approved categories, for
example:

```text
codex.authorization.denied.total.write.contentItem.USER.SECURED
```

Do not encode actor ids, site keys, content item keys, role keys, human reasons, or scope
values into metric names.

## Safe / Forbidden Tag Matrix

| Field | Allowed as metric tag? | Reason |
| --- | ---: | --- |
| outcome | Yes | Low-cardinality: granted/denied/invariant. |
| reason category | Yes | Stable machine category; avoid free-text reasons. |
| actor type | Yes | Low-cardinality enum-like value. |
| actor id | No | Identifies a person/client/system actor; security/PI risk. |
| permission key | Maybe | Safe only if built-in bounded catalog; prefer permission category. |
| permission category | Yes | Low-cardinality grouping such as read/write/admin/privileged. |
| resource type | Yes | Low-cardinality type such as site/contentType/contentItem/global. |
| resource id | No | Identifies concrete protected resource. |
| site key | No | Tenant/site topology and possibly customer identity. |
| content type key | No | Domain topology and potentially sensitive naming. |
| content item key | No | Concrete content identity; high-cardinality. |
| target scope type | Yes | Low-cardinality scope kind. |
| target scope value | No | Reveals topology and identifiers. |
| runtime security mode | Yes | Low-cardinality `SECURED`/`UNSECURED`; useful diagnostics. |
| invariant violation type | Yes | Low-cardinality security category. |
| matched role key | No | Reveals role topology and may be high-cardinality with custom roles. |
| matched role assignment id | No | Concrete authorization data identity. |
| human reason | No | Free text, unstable, potentially sensitive. |
| correlation id | No | High-cardinality identifier; use logs/audit/trace, not metrics. |

## Authorization Fact Recommendation Matrix

| Authorization fact | Metric? | Metric name/category | Tags | Notes |
| --- | ---: | --- | --- | --- |
| Normal granted read | Maybe | `codex.authorization.decisions.total` | outcome, actor_type, permission_category, resource_type, runtime_security_mode | High volume; count aggregate only or sample by runtime policy. |
| Normal granted write | Yes | `codex.authorization.decisions.total` | outcome, actor_type, permission_category, resource_type, runtime_security_mode | Useful success baseline for secured mutations. |
| Denied read | Maybe | `codex.authorization.denied.total` | reason_category, actor_type, permission_category, resource_type, runtime_security_mode | Count if useful; avoid audit-like identity detail. |
| Denied write | Yes | `codex.authorization.denied.total` | reason_category, actor_type, permission_category, resource_type, runtime_security_mode | Strong operational/security signal. |
| Privileged `SUPER_ADMIN` operation | Yes | `codex.authorization.privileged_use.total` | actor_type, permission_category, resource_type, runtime_security_mode | Aggregate only; exact actor/resource belongs to security audit. |
| `AGENT + SUPER_ADMIN` invariant violation | Yes | `codex.authorization.invariant_violations.total` | invariant_type, actor_type, runtime_security_mode | Always count; fatal invariant remains exception. |
| Role assignment created | Future | `codex.authorization.admin_operations.total` | operation_category, outcome, actor_type, runtime_security_mode | Security administration metric once operations exist. |
| Role assignment revoked | Future | `codex.authorization.admin_operations.total` | operation_category, outcome, actor_type, runtime_security_mode | Same as created. |
| Permission grant created | Future | `codex.authorization.admin_operations.total` | operation_category, outcome, actor_type, runtime_security_mode | Direct actor grants remain deferred. |
| Permission grant revoked | Future | `codex.authorization.admin_operations.total` | operation_category, outcome, actor_type, runtime_security_mode | Same as created. |
| Step-up approval required | Future | `codex.authorization.admin_operations.total` or future step-up metric | operation_category, outcome, actor_type, runtime_security_mode | Step-up semantics not implemented. |
| Step-up approval accepted | Future | `codex.authorization.admin_operations.total` or future step-up metric | operation_category, outcome, actor_type, runtime_security_mode | Keep aggregate only. |
| Step-up approval denied | Future | `codex.authorization.admin_operations.total` or future step-up metric | operation_category, outcome, actor_type, runtime_security_mode | Keep aggregate only. |

## Permission Category Guidance

Prefer categories over raw permission keys when cardinality or semantics are unclear.

Initial categories:

| Category | Examples |
| --- | --- |
| `read` | `site.read`, `contentType.read`, `contentItem.read` |
| `create` | `site.create`, `contentType.create`, `contentItem.create` |
| `update` | `contentType.update`, `contentItem.update` |
| `publish` | `contentItem.publish`, `contentItem.unpublish` |
| `archive` | `site.archive`, `contentType.archive`, `contentItem.archive` |
| `admin` | `permission.grant`, `permission.revoke`, `role.assign`, `role.revoke` |
| `privileged` | `SUPER_ADMIN` bypass/use categories |
| `unknown` | fallback for uncategorized permission keys |

If future custom permission keys become unbounded, raw permission key must not be a metric tag.

## Relationship To AccessDecision Trace

Observance should consume redacted metric-safe views derived from authorization decision
records or traces.

Observance should not receive:

* full traces
* role topology
* scope walk paths
* permission implication paths
* actor ids
* resource ids
* target scope values
* human reasons

Observance may receive:

* outcome
* reason category
* actor type
* permission category
* resource type
* runtime security mode
* invariant violation type
* latency/counts

The trace layer may know much more than Observance. The metrics consumer must intentionally
discard that extra detail.

## Relationship To Future Security Audit

Security audit may persist actor/resource-level facts for investigation. Observance should not.

Boundary:

| Layer | What it can answer |
| --- | --- |
| Observance | "Are denials increasing for content item writes?" |
| Security audit | "Which actor was denied updating which content item, and why?" |
| Logs | "What happened around this request while debugging?" |
| Trace | "Which resolver path, role, scope, or implication produced this outcome?" |

Observance is aggregate operational telemetry. It is not a durable investigation trail and
must not be treated as the security audit source of truth.

## Risks

| Risk | Impact | Mitigation |
| --- | --- | --- |
| PI leakage | Metrics could expose actor/customer/resource identities. | Forbid identifiers as tags or metric-name segments. |
| High-cardinality metrics | Metrics backend becomes expensive/noisy or unusable. | Use bounded categories only; avoid keys, ids, reasons, correlation ids. |
| Permission topology exposure | Role/scope/implication internals leak through labels. | Exclude matched role, assignment, grant, scope walk, and implication path. |
| Metrics treated as audit | Operators may rely on aggregate counters for forensic questions. | Document Observance as telemetry only; security audit remains separate. |
| Read-volume noise | Normal granted reads can dominate authorization metrics. | Make granted-read counting optional, sampled, or aggregate-only by runtime policy. |
| Custos-to-Observance coupling | Authorization core depends on metrics infrastructure. | Emit from future final consumer/sink, not `DefaultAccessDecisionService`. |
| Premature instrumentation | Metrics are added before decision records/redaction are stable. | Implement reason categories and metric-safe views first. |

## Recommended Implementation Path

1. Define permission categories and reason categories as stable bounded values.
2. Define the `METRICS_SAFE` projection of `AuthorizationDecisionRecord`.
3. Implement metrics as an `ObservanceAuthorizationMetricsConsumer` final sink.
4. Wire the consumer through runtime composition, not through Custos core.
5. Start with counters for denied decisions and invariant violations, plus decision duration.
6. Add privileged-use and admin-operation metrics after the corresponding decision records exist.
7. Add tests proving forbidden fields never appear in metric names or dimensions.

## Follow-Up Tasks

1. `CODEX-018C.1 — ADR: Authorization Observance Metrics Boundary`
   * Promote this metrics boundary to an ADR if Jonathan wants a durable decision before code.

2. `CODEX-018D — Local Authorization Decision Record Prototype`
   * Implement local bounded decision records and dispatching.

3. `CODEX-018D.1 — ObservanceAuthorizationMetricsConsumer`
   * Add the final-sink metrics consumer once records exist.

4. `CODEX-018D.2 — Forbidden Metric Field Tests`
   * Prove actor ids, resource ids, scope values, role keys, human reasons, and correlation ids are not used as metric labels or name segments.

5. `CODEX-018E — Security Audit Stream Boundary Discovery`
   * Design durable security audit separately from Observance.

## Validation

```bash
git diff --check -- docs
```

Result: passed.

No tests were run because this was documentation-only.
