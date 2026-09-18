# Task — Intent 1 Canonical Test Cases

Status: Planned
Owner: QA Agent
Operating Role: QA / Verification Architect
Scope: Custos C1 — Complete Secured Mutation Enforcement
Target: 45–60 minutes
Hard Stop: 90 minutes

## Objective

Create the canonical behavioral test cases for Intent 1.

The test cases must translate the accepted Intent and Use Cases into explicit, verifiable Given / When / Then scenarios without prescribing implementation details.

The goal is to define what must be proven before Custos C1 can be considered complete.

## Sources of Truth

Read first:

* canonical Intent 1 document
* `docs/knowledge/use-cases/intent1/`
* `docs/engineering/plan/intent1/engineering-plan.md`
* `docs/PROJECT.md`
* `docs/OSK.md`

Behavioral authority order:

```text
Intent
  -> Use Cases
  -> Test Cases
```

The engineering plan may be used for traceability and completeness checks, but it must not override Intent or Use Case behavior.

## Operating Role

Act as QA / Verification Architect.

Focus on:

* observable behavior
* boundary conditions
* negative paths
* invariant preservation
* state transitions
* side-effect absence
* scope isolation
* traceability

Do not design production code.

Do not choose classes, packages, mocks, frameworks, or implementation techniques.

## Output Location

Create canonical test cases under:

```text
docs/knowledge/test-cases/intent1/
```

Recommended structure:

```text
docs/knowledge/test-cases/intent1/
  TC-01-site-unarchive-authorized.md
  TC-02-site-unarchive-denied.md
  ...
```

A different naming convention is acceptable if OSK specifies one.

## Required Test Case Format

Each test case should contain at minimum:

* ID
* Title
* Source Use Case
* Purpose
* Preconditions
* Given
* When
* Then
* Negative / boundary notes if relevant
* Observable evidence
* Out of scope

Keep the language behavioral.

Avoid references to:

* Java class names
* test class names
* method names
* mocking libraries
* Maven modules
* implementation slices

Those belong to engineering planning, not canonical test cases.

## Required Coverage

Create test cases covering all observable verifications from the three use cases.

### Site Unarchive

At minimum cover:

1. Authorized actor can unarchive an archived Site.
2. Unauthorized actor is denied.
3. Denied call leaves Site unchanged.
4. Denied call produces no domain side effects.
5. Authority scoped to Site A does not authorize Site B.
6. Hard invariant violation propagates and is not converted to denial.
7. Secured runtime no longer uses unsupported-operation behavior.

### ContentItem Delete

At minimum cover:

1. Authorized actor can delete an archived ContentItem.
2. Unauthorized actor is denied.
3. Denied call leaves item intact.
4. Denied call produces no domain side effects.
5. Granted actor against non-ARCHIVED item receives domain precondition failure.
6. Authorization does not replace domain state-machine validation.
7. Scope authority does not leak across unrelated ContentItems / Sites.
8. Hard invariant violation propagates.
9. Secured runtime no longer uses unsupported-operation behavior.

### ContentItem Restore

At minimum cover:

1. Authorized actor restores archived item to DRAFT.
2. Unauthorized actor is denied.
3. Denied call leaves item ARCHIVED.
4. Denied call produces no domain side effects.
5. Granted actor against invalid state receives domain precondition failure.
6. Restore does not imply publish.
7. Publish authority does not imply restore unless explicitly granted.
8. Scope authority does not leak across unrelated resources / Sites.
9. Hard invariant violation propagates.
10. Secured runtime no longer uses unsupported-operation behavior.

## Cross-Cutting Test Cases

Where useful, create shared test cases for:

### Explicit permission semantics

Each operation must result in an authorization decision rather than fallback unsupported behavior.

### Scope isolation

Authority granted for one Site must not cross into another Site.

### Fail-closed behavior

A denied decision must prevent delegate execution and all resulting domain side effects.

### Domain / Authorization separation

Custos determines whether an actor may attempt the operation.

The domain state machine determines whether the operation is valid for current state.

### Hard invariant preservation

A fatal Custos invariant must remain fatal and never become a normal denied decision.

## Role Semantics

Use the accepted engineering-plan role recommendations only as provisional expected behavior:

* `SUPER_ADMIN`
* `SITE_ADMIN`
* `EDITOR`
* `COPYWRITER`
* `REVIEWER`
* `VIEWER`

If a role expectation conflicts with the Intent or Use Cases, flag it instead of silently adopting it.

Especially verify the proposed distinction:

```text
EDITOR
  restore -> allowed
  delete  -> denied
```

Do not treat that mapping as immutable architecture; record any concern as a QA finding.

## State Semantics

Preserve current domain behavior:

```text
Site unarchive:
ARCHIVED -> SUSPENDED

ContentItem delete:
requires ARCHIVED

ContentItem restore:
ARCHIVED -> DRAFT
```

Custos test cases must not redefine these state transitions.

## Side-Effect Expectations

For denied operations, canonical test cases should require absence of:

* domain mutation
* domain event caused by the attempted mutation
* projection update caused by that event
* Chronicon domain-audit record caused by that event

Do not require security-audit records in Intent 1.

Security decision records / Aegis / Observance authorization metrics remain out of scope.

## Suggested Test Case Set

Use judgment, but a reasonable starting set is:

```text
TC-01  Authorized Site unarchive
TC-02  Denied Site unarchive
TC-03  Site unarchive scope isolation
TC-04  Site unarchive hard invariant

TC-05  Authorized ContentItem delete
TC-06  Denied ContentItem delete
TC-07  Delete invalid domain state
TC-08  Delete scope isolation
TC-09  Delete hard invariant

TC-10  Authorized ContentItem restore
TC-11  Denied ContentItem restore
TC-12  Restore invalid domain state
TC-13  Restore does not imply publish
TC-14  Restore scope isolation
TC-15  Restore hard invariant

TC-16  Denied secured mutations produce no domain side effects
TC-17  No unsupported-operation fallback remains
```

Do not mechanically create exactly 17 files if combining or splitting cases yields clearer verification.

## Traceability

Create an index or matrix mapping:

| Test Case | Source Use Case | Intent Requirement |
| --------- | --------------- | ------------------ |

Every observable verification from:

* Site unarchive use case
* ContentItem delete use case
* ContentItem restore use case

must map to at least one canonical test case.

No observable verification should be orphaned.

## Relationship To Engineering Plan

After test cases are complete, compare them against:

```text
docs/engineering/plan/intent1/engineering-plan.md
```

Report:

* any behavioral test missing from the plan
* any planned test with no behavioral source
* any contradiction
* any duplicated responsibility that may cause unnecessary test layering

Do not modify the engineering plan in this task unless explicitly required by OSK workflow.

## Out of Scope

Do not:

* implement tests
* modify production code
* modify Java test code
* choose test frameworks
* modify permission vocabulary
* change built-in role mappings
* design new authorization mechanisms
* add Observance metrics
* add security audit
* add decision records/traces
* create implementation tasks
* commit

## Deliverables

Create:

```text
docs/knowledge/test-cases/intent1/
```

containing:

* canonical test cases
* an index / traceability matrix

Optionally create a QA report under the canonical OSK agent-report path if OSK requires one.

The report should summarize:

1. number of test cases
2. use-case coverage
3. open QA questions
4. discrepancies with engineering plan
5. any missing behavioral requirement

## Acceptance Criteria

* Every Use Case observable verification is represented.
* Authorized, denied, invalid-state, scope-boundary, and invariant paths are covered.
* Test cases remain implementation-agnostic.
* Domain-state semantics remain separate from authorization semantics.
* Denied side-effect guarantees are explicit.
* Cross-Site isolation is explicit.
* No security-audit / metrics requirements leak into C1.
* Traceability from Intent → Use Case → Test Case is complete.
* Engineering-plan discrepancies are reported.
* `git diff --check -- docs` passes.
* No production or test code changed.
* No commits.

## Validation

Run:

```bash
git diff --check -- docs
```

No Maven tests are required because this task creates documentation-only behavioral test specifications.
