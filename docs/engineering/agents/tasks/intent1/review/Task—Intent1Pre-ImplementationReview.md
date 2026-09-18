# Task — Intent 1 Pre-Implementation Review

Status: Planned
Owner: Deep
Role: Architecture Reviewer / Adversarial Reviewer
Scope: Custos C1 — Complete Secured Mutation Enforcement
Target: 45–60 minutes
Hard Stop: 90 minutes

## Objective

Perform a pre-implementation architecture and plan review for Custos C1.

Review the complete Intent 1 package as one coherent specification:

```text
Intent
  -> Use Cases
  -> Canonical Test Cases
  -> Engineering Plan
```

Determine whether the package is internally consistent, architecturally sound, sufficiently testable, and safe to advance to Design.

This review is the approval gate before implementation design begins.

## Sources of Truth

Read all of the following before reaching a verdict:

### Intent

Canonical Intent 1 document for:

* Custos C1 — Complete Secured Mutation Enforcement

### Use Cases

Under:

```text
docs/knowledge/use-cases/intent1/
```

Including:

* Site unarchive under secured runtime
* ContentItem delete under secured runtime
* ContentItem restore under secured runtime

### Canonical Test Cases

Under:

```text
docs/knowledge/test-cases/intent1/
```

Including:

* TC-01 through TC-17
* traceability/index README

### Engineering Plan

```text
docs/engineering/plan/intent1/engineering-plan.md
```

### Supporting Context

Review as needed:

* `docs/PROJECT.md`
* `docs/OSK.md`
* `docs/engineering/roadmap/ROADMAP.md`
* relevant Custos ADRs
* relevant Custos knowledge docs
* CODEX-OSK-001 reality check
* current Custos source and tests where necessary to validate plan feasibility

Do not trust documentation claims blindly where repository inspection can verify them.

## Review Goal

Answer:

> Is Intent 1 sufficiently coherent and correctly planned to proceed to Design without carrying hidden product, security, architecture, or test contradictions forward?

## Review Dimensions

### 1. Intent Alignment

Verify that:

* the plan solves the Intent and does not broaden it
* no deferred concern is accidentally pulled into C1
* the use cases remain faithful to the Intent
* the test cases verify the Intent rather than implementation details

Identify any behavior present in plan/tests that has no source in Intent or Use Cases.

### 2. Use Case Completeness

Verify all three use cases:

* Site unarchive
* ContentItem delete
* ContentItem restore

Check:

* main authorized flow
* unauthorized flow
* domain-precondition behavior
* hard invariant propagation
* scope boundaries
* downstream side-effect semantics
* explicit no-UOE goal

Identify missing, redundant, or contradictory behavior.

### 3. Canonical Test Case Quality

Review TC-01 through TC-17.

Verify:

* every observable use-case requirement is represented
* tests are behavioral and implementation-agnostic
* no important requirement is only implied
* no test accidentally freezes an implementation choice
* stateful test scenarios are independently executable where necessary
* role expectations marked provisional are not being treated as immutable product truth

Specifically review:

#### TC-13

The restore/publish independence scenario currently performs multiple actions.

Determine whether each assertion should use independent fixtures to avoid state coupling.

#### TC-08 and TC-14

Review wording around Site boundary isolation vs legitimate broader scopes.

Ensure the specification means:

```text
Site-A authority must not authorize Site-B resources.
```

without incorrectly implying:

```text
Global authority cannot cover multiple Sites.
```

#### TC-17

Determine whether the "3 operations × authorized/unauthorized" no-fallback scenario should explicitly require independent initial fixtures per invocation.

### 4. Permission Vocabulary

Review the proposed three distinct permissions:

```text
site.unarchive
contentItem.delete
contentItem.restore
```

Determine whether:

* each deserves independent semantics
* reuse of archive/update/publish permissions would be incorrect
* no implication rule should be added
* naming is consistent with existing Custos vocabulary

Flag any semantic ambiguity.

### 5. Built-In Role Mapping

Adversarially review the plan's recommendation:

| Permission          | SUPER_ADMIN | SITE_ADMIN | EDITOR | COPYWRITER | REVIEWER | VIEWER |
| ------------------- | ----------: | ---------: | -----: | ---------: | -------: | -----: |
| Site unarchive      |         yes |        yes |     no |         no |       no |     no |
| ContentItem delete  |         yes |        yes |     no |         no |       no |     no |
| ContentItem restore |         yes |        yes |    yes |         no |       no |     no |

Particularly assess:

* whether `EDITOR -> restore` is justified
* whether `SITE_ADMIN -> delete` is too broad or appropriately aligned
* whether delete should require stronger authority than archive
* whether lifecycle symmetry is a sufficient reason for restore/unarchive permissions
* whether any role gains an accidental privilege escalation

Do not approve mappings merely because they are convenient.

If role semantics remain product-ambiguous, identify exactly what needs an explicit maintainer decision before Design.

### 6. Scope Semantics

Review proposed scope behavior.

Site unarchive:

```text
Site -> Global
```

ContentItem delete / restore:

```text
ContentItem
  -> ContentType
  -> Site
  -> Global
```

Verify:

* this matches current scope hierarchy
* no cross-Site leakage occurs
* broader scope remains legitimate where currently supported
* no new scope layer is needed
* no operation should require intentionally narrower scope than proposed

Explicitly assess the QA finding that ContentType/Global coverage is not fully enumerated by canonical test cases.

Recommend whether this must be added before Design or whether existing boundary cases are sufficient for C1.

### 7. Authorization vs Domain Semantics

Verify the proposed ordering:

```text
authorize first
  -> if granted
      invoke domain operation
        -> domain validates lifecycle/state
```

Confirm:

* Custos remains state-blind
* invalid domain state is not transformed into authorization denial
* authorization grant does not bypass domain invariants
* denial prevents domain invocation entirely

Review TC-07 and TC-12 against this rule.

### 8. Hard Invariant Semantics

Verify that:

```text
AGENT + SUPER_ADMIN
```

continues to be a fatal invariant and is never transformed into a standard denial.

Review:

* TC-04
* TC-09
* TC-15
* engineering plan assumptions

Confirm the planned implementation does not need to alter invariant code.

### 9. Denied Side-Effect Safety

Review TC-16 and Slice 7.

Verify that a denied operation guarantees:

```text
no domain mutation
no domain event
no projection update
no Chronicon domain-audit record
```

Determine whether current runtime architecture makes those assertions valid and testable for all three operations.

Identify any side effect that the plan fails to observe.

### 10. Engineering Slice Quality

Review the seven proposed slices:

1. Permission vocabulary
2. Built-in roles
3. Permission-service API/defaults
4. Secured Site unarchive
5. Secured ContentItem delete/restore
6. Authorization matrix / scope coverage
7. Runtime denied-side-effect verification

Assess:

* dependency order
* slice size
* reviewability
* whether any slice combines unrelated decisions
* whether any required step is missing
* whether any slice should move earlier/later

Check the textual inconsistency where the plan refers to "six bounded slices" but defines seven.

### 11. Test Layering

Review the plan's test-layer responsibilities:

```text
permission-service unit
secured-decorator unit
role blueprint tests
authorization matrix
Concilium runtime integration
```

Verify:

* each layer has a clear purpose
* unnecessary duplication is avoided
* end-to-end confirmation does not contradict the "one responsibility per layer" principle
* the traceability matrix accurately maps behavioral tests into implementation-level verification

If necessary, recommend wording such as:

```text
one primary assertion layer per responsibility,
with selective end-to-end confirmation
```

rather than an absolute "exactly one test layer."

### 12. Architecture Boundaries

Confirm C1 does not accidentally introduce:

* Chronicon changes
* Observance changes
* decision records
* structured traces
* role administration
* direct grants
* list filtering
* persistence
* Porta/API concerns
* Olorin/AI concerns
* new module dependencies
* new event choreography

## Required Findings Classification

Classify findings as:

* BLOCKER
* MAJOR
* MINOR
* NOTE

A BLOCKER means Design should not begin.

A MAJOR means a material plan/spec correction is required before Design.

A MINOR may be corrected during reconciliation without reopening the objective.

## Verdict

Use one of:

```text
PASS
PASS WITH MINOR CHANGES
CHANGES REQUIRED
```

Do not use PASS if unresolved role/scope semantics could materially change implementation or canonical test behavior.

## Required Output

Create review under the canonical OSK review location, for example:

```text
docs/engineering/agents/reviews/intent1/
  Intent1PreImplementationReview.md
```

Follow installed OSK naming convention if different.

The review must contain:

1. Executive verdict
2. Evidence reviewed
3. Intent alignment findings
4. Use-case findings
5. Test-case findings
6. Permission vocabulary assessment
7. Role-mapping assessment
8. Scope assessment
9. Authorization/domain ordering assessment
10. Hard-invariant assessment
11. Side-effect safety assessment
12. Engineering-slice assessment
13. Test-layer assessment
14. Architecture-boundary assessment
15. Required changes before Design
16. Optional improvements
17. Final recommendation

## Acceptance Criteria

The review is complete when:

* all four artifact layers are reviewed together
* the 17 canonical test cases are considered
* provisional role mappings are explicitly assessed
* scope semantics are explicitly assessed
* TC-13, TC-08/14, and TC-17 concerns are addressed
* engineering slice ordering is assessed
* test layering is assessed
* architecture boundaries are verified
* findings are severity-classified
* a clear go/no-go verdict for Design is issued
* no production code is modified
* no tests are modified
* no commits are made

## Validation

If code inspection is sufficient, documentation validation is enough.

Run:

```bash
git diff --check -- docs
```

Run focused repository searches/tests only if needed to validate architectural feasibility claims.

No implementation changes.
