# Intent — Custos C1: Complete Secured Mutation Enforcement

Status: Draft
Objective: Custos C1 — Complete Secured Mutation Enforcement

## Intent

Complete the authorization semantics for state-changing operations that already exist in the Codex domain API but are not yet executable through the secured runtime because Custos lacks explicit permission vocabulary for them.

A secured Codex runtime should be able to make an explicit authorization decision for every currently exposed mutation that it allows callers to invoke.

The secured boundary should not depend on `UnsupportedOperationException` as a permanent substitute for authorization semantics.

## Why This Matters

Custos already provides a functioning authorization chain:

```text
Actor
  -> RoleAssignment
  -> Role
  -> PermissionResolver
  -> PermissionResolution
  -> AccessDecisionService
  -> domain permission service
  -> secured service decorator
  -> domain operation
```

Most current mutations pass through this chain successfully.

However, the repository-grounded reality check identified three existing state-changing operations whose secured decorators remain intentionally fail-closed because corresponding permission semantics have not yet been defined:

```text
SiteService.unarchive(...)
ContentItemService.delete(...)
ContentItemService.restore(...)
```

The current behavior is safe, but incomplete.

These operations cannot currently participate in the same grant/deny model as the rest of the secured runtime.

## Desired Outcome

After this objective is complete:

```text
secured mutation
  -> explicit permission
  -> permission resolution
  -> granted or denied AccessDecision
  -> delegate executes only when granted
```

The three currently blocked operations should behave consistently with the rest of Custos:

```text
site.unarchive
contentItem.delete
contentItem.restore
```

A caller with sufficient authority can execute the operation.

A caller without sufficient authority receives the normal Custos denial behavior.

A denied operation must not reach the domain delegate or produce domain side effects.

## User / System Value

This objective improves Custos in three ways.

### Completeness

The secured runtime can govern all currently exposed state-changing operations instead of supporting only a subset.

### Consistency

Authorization failure is represented through the normal Custos decision model rather than through operation-specific unsupported behavior.

### Safety

The existing fail-closed posture is preserved while replacing temporary blocking with explicit authorization semantics.

## Behavioral Principles

### 1. Explicit authority

Every protected mutation must have an explicit permission meaning.

No state-changing operation should become authorized simply because another permission appears similar.

### 2. Fail closed

Missing, invalid, or insufficient authority must never cause the delegate operation to execute.

### 3. Preserve domain semantics

Custos controls whether an operation may execute.

Custos does not redefine what `unarchive`, `delete`, or `restore` mean to the domain.

### 4. Preserve the existing authorization chain

These operations should use the same authorization architecture already used by existing Site and ContentItem mutations.

Do not introduce a parallel authorization mechanism.

### 5. Scope matters

Authorization must respect the appropriate resource and scope boundaries for each operation.

Authority over one Site or ContentItem must not implicitly grant authority over unrelated resources.

### 6. No accidental privilege expansion

Existing built-in roles should receive new permissions only where their current semantic responsibilities justify them.

Adding permission vocabulary must not silently broaden roles beyond their intended authority.

## Operations In Scope

### Site unarchive

Codex already exposes a Site unarchive domain operation.

Custos currently blocks it in the secured decorator because no explicit authorization vocabulary exists.

The objective is to define what authority permits unarchiving a Site and enforce it through the normal Custos chain.

### ContentItem delete

Codex already exposes ContentItem deletion.

The secured runtime currently blocks the operation because delete authorization semantics are undefined.

The objective is to establish explicit permission semantics and normal grant/deny behavior.

### ContentItem restore

Codex already exposes ContentItem restoration.

The secured runtime currently blocks the operation because restore authorization semantics are undefined.

The objective is to establish explicit permission semantics and normal grant/deny behavior.

## Expected Authorization Behavior

For each operation:

```text
authorized actor
  -> operation executes normally

unauthorized actor
  -> AccessDeniedException
  -> delegate not invoked
  -> domain state unchanged
  -> no domain event emitted
  -> no projection update
  -> no Chronicon domain audit record caused by the denied operation
```

This should remain consistent with the denied-operation side-effect guarantees already established elsewhere in the secured runtime.

## Permission Vocabulary Direction

The objective expects explicit permission concepts equivalent to:

```text
site.unarchive
contentItem.delete
contentItem.restore
```

Exact constants, API method names, and role mappings are design/engineering concerns and should be finalized later in the OSK lifecycle.

The Intent establishes only that these operations require distinct explicit authorization semantics.

## Role Semantics

The objective does not assume that every existing administrator/editor role automatically receives every new permission.

During use-case and design work, existing role responsibilities must be reviewed deliberately.

Questions to resolve later include:

```text
Which built-in roles may unarchive a Site?

Which built-in roles may delete a ContentItem?

Which built-in roles may restore a ContentItem?

Should destructive or retention-related operations require stronger authority than ordinary update/archive operations?
```

These questions belong to use cases and design, not to this Intent.

## Success Conditions

This objective is successful when:

* every currently exposed mutation covered by this objective has explicit Custos permission semantics
* the secured runtime can grant or deny each operation normally
* authorized calls reach the underlying domain service
* denied calls never reach the delegate
* denied calls produce no unintended domain side effects
* scope boundaries remain intact
* existing authorization behavior remains compatible
* no unrelated permission semantics are changed

## Non-Goals

This objective does not attempt to solve:

* RoleAssignment management
* permission grant/revoke management
* direct actor permission grants
* collection-read authorization/filtering
* alias-to-SiteKey authorization
* authorization decision records
* structured AccessDecision traces
* Observance authorization metrics
* Aegis/security audit stream
* permission decision caching
* step-up approval
* workflow
* Porta/API exposure
* persistence
* Olorin/AI authorization

Those belong to subsequent Custos objectives.

## Relationship To Future Custos Work

This objective is the first completeness milestone after the repository reality check.

Expected progression:

```text
C1 — Complete Secured Mutation Enforcement
  ↓
C2 — Authorization Administration
  ↓
C3 — Explainable Authorization Decisions
  ↓
C4 — Authorization Observability
  ↓
C5 — Security Audit
  ↓
C6 — Multi-Actor Boundary Integration
  ↓
C7 — Secured Collection Reads
```

The exact future roadmap may evolve, but this objective should not absorb those concerns.

## Core Intent Statement

> Custos should explicitly authorize every state-changing operation exposed through the secured Codex runtime. Existing fail-closed placeholders for Site unarchive and ContentItem delete/restore should become normal authorization decisions without weakening scope isolation, side-effect safety, or existing domain semantics.
