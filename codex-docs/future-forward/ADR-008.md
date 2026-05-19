# ADR-008 — Observance, Auditability and Explainable Security Foundations

## Status

Proposed

---

## Context

Codex aims to become a modular, observable, auditable, and explainable CMS platform.

As the system grows, cross-cutting concerns such as observability, auditing, transactions, security, caching, indexing, and event dispatching must remain:

* explicit
* composable
* testable
* implementation-agnostic

The project intentionally avoids premature coupling to frameworks such as Spring AOP, Micrometer annotations, proxies, reflection-based interception, or runtime magic.

Instead, Codex favors explicit wrappers/decorators and semantic domain instrumentation.

The project has already introduced:

* `Observance`
* `NoOpObservance`
* explicit forwarding/decorator patterns
* deferred transactional event dispatching
* runtime composition through layered services

The architecture now requires a formal baseline for:

* Observance
* future auditing
* future explainable security/permissions

---

# Decision

## 1. Observance is a first-class architectural capability

Observability is not considered "logging attached later".

Observance represents semantic instrumentation across:

* domain services
* event dispatching
* caching
* persistence
* indexing
* infrastructure pipelines

The goal is to make Codex introspectable without invasive code changes.

---

## 2. Explicit decorators are preferred over AOP/proxy magic

Codex standardizes on explicit wrappers/decorators.

Example:

```text
TimedSiteService
  -> AuditingSiteService
      -> TransactionalSiteService
          -> LockingSiteService
              -> CoreSiteService
```

This approach preserves:

* readability
* testability
* deterministic composition
* implementation transparency

The project intentionally avoids:

* runtime proxies
* reflection-based interception
* annotation-driven hidden behavior
* implicit framework instrumentation

---

## 3. Observance must remain implementation-agnostic

Core modules must not depend directly on:

* Prometheus
* Micrometer
* OpenTelemetry
* Grafana
* vendor-specific telemetry systems

Adapters may exist externally.

The baseline API should remain lightweight and portable.

---

## 4. Low-cardinality metrics are preferred

Observance should avoid high-cardinality dimensions.

Allowed examples:

* operation names
* event type
* dispatch mode
* cache region
* service name

Disallowed examples:

* content ids
* site ids
* actor ids
* request ids
* arbitrary user input

The goal is operational stability and predictable metric storage costs.

---

## 5. Dispatcher observance and service observance are separate concerns

Event pipeline observance measures:

* dispatch counts
* failures
* dispatch latency
* async vs sync behavior

Service observance measures:

* caller-visible operation latency
* failures
* retries
* lock contention
* transactional costs

These responsibilities must remain conceptually separated.

---

## 6. NoOp implementations are mandatory baseline components

Cross-cutting capabilities should always provide no-op implementations.

Examples:

* `NoOpObservance`
* future `NoOpAuditSink`
* future `NoOpPermissionExplainer`

This guarantees:

* simpler composition
* easier testing
* reduced null handling
* implementation optionality

---

## 7. Auditability is a distinct future capability

Observance answers:

> What happened technically?

Auditing answers:

> Who performed which operation, against which resource, and when?

Auditing is intentionally modeled separately from metrics.

Future auditing components may include:

* `AuditEntry`
* `AuditSink`
* `AuditTrail`
* `AuditingSiteService`

---

## 8. Security must eventually become explainable

Future Codex security should not only evaluate permissions, but also explain decisions.

The intended direction is a hierarchical permission model with inheritance and explicit overrides.

Potential hierarchy:

```text
System
  -> Site
      -> ContentType
          -> ContentItem
              -> ContentRevision
```

Planned evaluation philosophy:

* more specific rules override inherited rules
* explicit deny overrides allow at the same scope
* absence of rules implies inherited evaluation
* final fallback is deny

The system should eventually support explanations such as:

```text
ALLOW because actor inherited UPDATE_CONTENT
from Site "acme" and no explicit deny exists.
```

or:

```text
DENY because ContentItem "homepage"
contains an explicit deny override.
```

---

## 9. Anonymous and Super Admin are special actors

The project anticipates two special security actors:

### Anonymous

Represents unauthenticated/public access.

Anonymous participates in normal permission evaluation.

### Super Admin

Represents platform-level privileged access.

Super admin may bypass normal permission evaluation, but decisions should still remain auditable and explainable.

---

## 10. Current non-goals

The following are intentionally out of scope for the current phase:

* distributed tracing
* OpenTelemetry integration
* dashboards
* Prometheus exporters
* workflow-aware permission transitions
* policy engines
* dynamic scripting permissions
* external IAM integration

These may be introduced incrementally later.

---

# Consequences

## Positive

* highly testable architecture
* framework independence
* explicit operational semantics
* easier future instrumentation
* predictable behavior
* explainable permission direction
* strong architectural consistency

## Negative

* more boilerplate than annotation-driven frameworks
* additional wrapper composition
* manual metric instrumentation
* slower short-term development speed

The project explicitly accepts these tradeoffs in favor of long-term clarity and maintainability.
