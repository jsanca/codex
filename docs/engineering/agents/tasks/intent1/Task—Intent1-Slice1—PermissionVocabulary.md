# Task — Intent 1 / Slice 1 — Permission Vocabulary

Status: Authorized
Owner: Clio
Role: Software Engineer
Objective: Custos C1 — Complete Secured Mutation Enforcement
Slice: 1 of 7

## Lifecycle State

Gate D has passed.

```text
Current stage:
  IMPLEMENTATION

Implementation authorized:
  YES

Approved design:
  docs/engineering/design/intent1/design.md

Next authorized work:
  Slice 1 — Permission Vocabulary
```

This task is authorized to modify production and test code only within the boundaries of Slice 1.

## Authority Order

When interpreting this task, use the following authority hierarchy:

```text
Behavior authority:
  Intent
    → Use Cases
      → Canonical Test Cases

Execution authority:
  Engineering Plan

Technical authority:
  Approved Design

Progress authority:
  Review Gates
```

The Engineering Plan defines what this slice must execute.

The approved Design defines how the technical contracts for Intent 1 are shaped.

Intent, Use Cases, and Canonical Test Cases define behavior that implementation must not contradict.

## Required Reading

Read before changing code:

### Execution source of truth

```text
docs/engineering/plan/intent1/engineering-plan.md
```

Focus on:

* accepted decisions
* Slice 1
* lifecycle/execution gates
* Definition of Done
* implementation/review sequence

### Approved technical design

```text
docs/engineering/design/intent1/design.md
```

Focus on:

* §2 Accepted Decisions
* §5 TARGET
* §6 Delta Summary
* §7 Public Contract Impact
* §14 Implementation-Readiness Conclusion

### Behavioral context

Read the Intent 1 permission-vocabulary portions and the three Use Cases sufficiently to confirm that the new permissions remain distinct.

Canonical behavioral artifacts include:

```text
docs/knowledge/use-cases/intent1/
docs/knowledge/test-cases/intent1/
```

For Slice 1, a full re-analysis of all 17 test cases is not required unless implementation uncovers an ambiguity.

Do not reinterpret already-approved product decisions.

## Objective

Introduce the explicit Custos permission vocabulary required by Intent 1.

Add exactly three new permission keys:

```text
site.unarchive
contentItem.delete
contentItem.restore
```

using the existing `PermissionKey` / `Permissions` conventions.

Expected constants:

```text
SITE_UNARCHIVE
CONTENT_ITEM_DELETE
CONTENT_ITEM_RESTORE
```

## Required Production Change

Modify:

```text
codex-custos/src/main/java/codex/custos/api/model/Permissions.java
```

Add the three new permission constants using the same style and construction pattern as neighboring permissions.

Keep naming and grouping consistent with the existing catalog.

### Semantic constraints

These permissions are intentionally distinct.

Do not:

* reuse `site.archive`
* reuse `contentItem.archive`
* reuse `contentItem.update`
* reuse `contentItem.publish`
* introduce implication rules
* introduce generic lifecycle permissions

The approved Design requires three explicit capabilities.

## Required Test Change

Update the existing permission catalog test.

Repository evidence from Design Review confirms:

```text
PermissionsTest.catalogSize()
```

currently expects:

```text
20
```

After this slice it must expect:

```text
23
```

Also ensure the existing uniqueness/reflection coverage continues to pass for all permission constants.

Do not add redundant tests if the existing catalog tests already prove:

* expected catalog size
* permission-key uniqueness

## Explicit Non-Goals

Do not modify:

* `BuiltInRoles`
* `SitePermissionsService`
* `ContentItemPermissionsService`
* default permission-service implementations
* secured decorators
* `DefaultPermissionImplicationRules`
* resolver logic
* scope hierarchy
* runtime composition
* JPMS/module-info
* domain services
* Chronicon
* Observance
* Concilium runtime integration tests

Those belong to later slices.

In particular:

```text
Slice 2 owns BuiltInRoles.
Slice 3 owns permission-service APIs/defaults.
Slices 4–5 own secured decorators.
Slices 6–7 own integration/runtime verification.
```

Do not pull future-slice work forward.

## Tests

Run the focused Custos test suite:

```bash
mvn -pl codex-custos test
```

If project conventions require dependent modules through `-am`, follow the repository's established command.

Also run:

```bash
git diff --check
```

## Acceptance Criteria

Slice 1 is complete when:

* `SITE_UNARCHIVE` exists.
* `CONTENT_ITEM_DELETE` exists.
* `CONTENT_ITEM_RESTORE` exists.
* Their external key values are exactly:

    * `site.unarchive`
    * `contentItem.delete`
    * `contentItem.restore`
* Permission catalog size expectation is updated from 20 to 23.
* Existing permission uniqueness tests pass.
* No implication rule is added.
* No role gains a permission yet.
* No permission-service API changes yet.
* No secured operation becomes executable yet.
* `mvn -pl codex-custos test` passes.
* `git diff --check` passes.
* Changes remain strictly within Slice 1.

## Report

At completion, report:

1. files changed
2. exact permission constants added
3. test changes
4. validation commands/results
5. confirmation that no Slice 2+ work was introduced
6. any unexpected repository discrepancy

Do not commit.
