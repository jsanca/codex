# Task — Intent 1 Engineering Plan

Status: Planned
Owner: Clio
Operating Role: Product Architect
Scope: Custos C1 — Complete Secured Mutation Enforcement
Target: 45–60 minutes
Hard Stop: 90 minutes

## Objective

Create the engineering plan for Intent 1 from the accepted intent and use cases.

The plan must translate the desired behavior into a sequenced, reviewable implementation strategy without writing production code.

The plan should be specific enough that implementation can later be executed in bounded slices by engineering agents.

## Sources of Truth

Read first:

* `docs/knowledge/use-cases/intent1/`
* the canonical `intent.md` for Intent 1
* `docs/PROJECT.md`
* `docs/OSK.md`
* `docs/engineering/roadmap/ROADMAP.md`
* relevant Custos ADRs
* relevant Custos knowledge under:

    * `docs/knowledge/security/`
    * `docs/knowledge/architecture/`

Use the three use cases as behavioral authority:

* Site unarchive under secured runtime
* ContentItem delete under secured runtime
* ContentItem restore under secured runtime

Do not reinterpret them into different product behavior.

## Operating Role

Act as Product Architect while creating this plan.

That means:

* preserve product/security intent
* reason from behavior before code
* make architectural boundaries explicit
* surface unresolved decisions
* avoid implementation-first thinking
* do not collapse product decisions into arbitrary class design
* define bounded engineering slices

## Output Location

Create the engineering plan under:

```text
docs/engineering/plan/intent1/
```

Suggested filename:

```text
engineering-plan.md
```

If OSK requires a different canonical filename convention, follow OSK.

## Plan Goal

Plan the work required so that these currently fail-closed secured mutations become normal Custos-authorized operations:

```text
Site unarchive
ContentItem delete
ContentItem restore
```

The end state should be:

```text
secured call
  -> explicit authorization semantics
  -> Custos evaluation
  -> granted or denied
  -> delegate executes only when granted
```

The plan must preserve:

* domain lifecycle semantics
* fail-closed behavior
* scope isolation
* hard invariant propagation
* denied-operation side-effect safety
* existing unsecured runtime behavior
* existing module boundaries

## Required Analysis

Before proposing slices, inspect current code relevant to:

### Permission vocabulary

* current permission keys
* built-in roles
* role permission sets
* permission implications if any

### Site authorization

* `SitePermissionsService`
* default implementation
* secured decorator
* current `unarchive` behavior

### ContentItem authorization

* `ContentItemPermissionsService`
* default implementation
* secured decorator
* current `delete` behavior
* current `restore` behavior

### Domain semantics

Confirm existing lifecycle behavior for:

```text
Site:
ARCHIVED -> SUSPENDED

ContentItem delete:
requires ARCHIVED

ContentItem restore:
ARCHIVED -> DRAFT
```

Do not alter those semantics.

### Existing tests

Review:

* permission service tests
* secured decorator tests
* authorization matrix tests
* denied-side-effect tests
* runtime integration tests

Prefer extending existing patterns rather than creating parallel testing styles.

## Decisions The Plan Must Resolve

The engineering plan must explicitly propose a recommendation for each of these.

### 1. Permission vocabulary

Determine the exact permission concepts needed for:

```text
site.unarchive
contentItem.delete
contentItem.restore
```

Explain why each should be distinct or, if proposing reuse, why reuse is semantically safe.

Do not reuse another permission merely to avoid adding vocabulary.

### 2. Built-in role mappings

Determine which existing built-in roles should receive each permission.

For each proposed role mapping explain the semantic reason.

Do not silently give all admin/editor-like roles every new permission.

Especially consider:

* destructive nature of delete
* restorative nature of restore
* lifecycle authority of unarchive

### 3. Scope semantics

Determine valid authorization scope behavior for each permission.

At minimum reason about:

```text
Site unarchive:
  Site
  Global

ContentItem delete/restore:
  ContentItem
  ContentType
  Site
  Global
```

Preserve existing scope-walk semantics unless a strong reason exists not to.

### 4. Domain precondition ordering

For delete and restore, clarify expected control flow between:

```text
authorization
domain state validation
```

The use cases establish:

* Custos decides whether the caller may attempt the operation.
* Domain state machine decides whether the operation is valid for current state.

The plan must preserve this separation.

### 5. Hard invariant propagation

Confirm that:

```text
AGENT + SUPER_ADMIN invariant
```

continues to propagate fatally and is never converted into an ordinary denial.

## Required Engineering Slices

Produce bounded implementation slices.

A likely shape is:

```text
Slice 1
Permission vocabulary + built-in role updates

Slice 2
Domain permission-service API + implementations

Slice 3
SecuredSiteService unarchive gating

Slice 4
SecuredContentItemService delete/restore gating

Slice 5
Authorization matrix / scope-boundary coverage

Slice 6
Denied side-effect runtime verification
```

This is only a suggested decomposition.

Adjust it according to repository evidence.

Each slice must include:

* objective
* files/areas likely affected
* behavior introduced
* tests required
* dependencies on previous slices
* acceptance criteria
* explicit out-of-scope items

## Test Strategy

Map each use case to planned test coverage.

At minimum include:

### Site unarchive

* authorized actor succeeds
* unauthorized actor denied
* denied delegate not invoked
* Site remains `ARCHIVED` after denial
* authorized transition reaches `SUSPENDED`
* cross-Site authority does not leak
* invariant violation propagates

### ContentItem delete

* authorized delete of archived item succeeds
* unauthorized delete denied
* denied item remains present
* denied operation creates no domain side effects
* authorized caller against non-ARCHIVED item receives domain precondition failure
* scope boundaries respected
* invariant violation propagates

### ContentItem restore

* authorized restore succeeds
* item becomes `DRAFT`
* unauthorized restore denied
* denied item remains `ARCHIVED`
* granted caller against invalid domain state receives domain precondition failure
* restore authority does not imply publish authority
* scope boundaries respected
* invariant violation propagates

## Test Layering

Recommend what belongs in:

```text
unit tests
permission-service mapping tests
secured decorator tests
authorization matrix tests
Concilium runtime integration tests
```

Avoid duplicating every assertion at every layer.

State which layer proves which responsibility.

## Non-Functional Constraints

The plan must preserve:

* Java 25 / JPMS module boundaries
* no framework/security-provider coupling in Custos
* no REST/auth-provider assumptions
* no Chronicon changes
* no Observance authorization metrics work
* no AuthorizationDecisionRecord implementation
* no list filtering work
* no role administration work
* no direct actor grants
* no persistence
* no saga / queue / distributed workflow

## Risks To Identify

At minimum assess:

* accidental privilege expansion in built-in roles
* delete authority being too broad
* cross-Site leakage
* permission vocabulary inconsistency
* authorization/domain-precondition conflation
* regressions in fail-closed behavior
* unintended event/audit/index side effects on denial

## Required Plan Structure

The final plan should contain:

1. Objective and source documents
2. Current-state summary
3. Decisions required
4. Recommended decisions
5. Architecture impact
6. Engineering slices
7. Test strategy
8. Use-case-to-test traceability matrix
9. Risks and mitigations
10. Out of scope
11. Definition of done
12. Recommended implementation/review sequence

## Traceability Matrix

Include a table similar to:

| Use Case | Behavioral Requirement | Planned Test Layer | Planned Slice |
| -------- | ---------------------- | ------------------ | ------------- |

Every observable verification from the three use cases should map to planned validation.

## Definition of Done

The engineering plan is complete when:

* all three use cases are covered
* no observable behavior is orphaned
* permission vocabulary is explicitly proposed
* role mappings are explicitly proposed
* scope semantics are explicit
* authorization/domain-state ordering is explicit
* implementation is decomposed into bounded slices
* test strategy is layered and non-redundant
* risks are identified
* out-of-scope boundaries are preserved
* no production code is changed

## Validation

Run:

```bash
git diff --check -- docs
```

No implementation tests are required for this planning task.

## Deliverable

Create:

```text
docs/engineering/plan/intent1/engineering-plan.md
```

Optionally create a short planning report under the canonical OSK agent report location if required by installed OSK conventions.

No commits.
