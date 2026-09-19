# Intent 1 Gate D Reconciliation - Knowledge Curator Report

## Task

Reconcile Custos C1 after the Gate D Design Review without reopening Intent 1
behavior or architecture.

## Authority and Scope

The authority for this reconciliation is
`docs/engineering/agents/reviews/intent1/Intent1DesignReview.md`. Its verdict
is **PASS WITH MINOR CHANGES - implementation authorized**.

This documentation-only reconciliation updates lifecycle state and the three
non-blocking accuracy findings. It does not alter permission vocabulary,
MD-C1-01, scope hierarchy, authorization/domain ordering, hard-invariant
behavior, failure semantics, the seven-slice structure, Intent, Use Cases, or
runtime and JPMS conclusions.

## Lifecycle State Transition

| Previous state | Reconciled state |
| --- | --- |
| Current stage: DESIGN REVIEW | Current stage: IMPLEMENTATION |
| Implementation authorized: NO | Implementation authorized: YES |
| Pending gate: Gate D - Design Approved | Gate D: PASSED |
| Current technical source pending approval | Approved design: `docs/engineering/design/intent1/design.md` |

The next authorized work is **Slice 1 - Permission Vocabulary**. The retained
execution sequence is Slices 1-3, Deep checkpoint, Slices 4-7, Final
Engineering Review, QA / Knowledge Reconciliation, and Objective Closeout.

## Resolution Matrix

| Finding | Severity | Resolution |
| --- | --- | --- |
| Gate D state not yet materialized | lifecycle | IMPLEMENTATION / YES; Gate D PASSED |
| Unarchive sketch uses `siteKey()` | MINOR | Corrected to `key()` |
| Permission catalog test marked optional | MINOR | Slice 1 now requires `PermissionsTest.catalogSize()` change from 20 to 23 |
| JavaDoc cleanup over-broad | MINOR | Narrowed to the affected ContentItem absence note |

## Reconciled Findings

### MINOR-1 - Unarchive command sketch

`docs/engineering/design/intent1/design.md` now uses `command.key()` in the
`SecuredSiteService.unarchive` sketch. This matches the reviewed
`UnarchiveSiteCommand` API and does not change production code.

### MINOR-2 - Permission catalog completeness

Slice 1 now makes the existing `PermissionsTest.catalogSize()` update
mandatory: 20 built-in permissions become 23 after the three approved
constants are introduced. Slice 2 explicitly retains the required
`BuiltInRolesTest` expectation updates for the MD-C1-01 blueprint changes.

### MINOR-3 - Narrow JavaDoc cleanup

The plan and Design now state the exact delta:

- remove `contentItem.delete` from the `ContentItemPermissionsService` absence
  note when the capability is added;
- retain the `SitePermissionsService` note for `site.update` and `site.delete`,
  which remain absent after C1;
- do not imply that `contentItem.restore` was already listed in the ContentItem
  note.

## Files Changed

- `docs/engineering/plan/intent1/engineering-plan.md`
- `docs/engineering/design/intent1/design.md`
- `docs/engineering/agents/reports/knowledge/Intent1GateDReconciliation-REPORT.md`
- `docs/engineering/ENGINEERING_LOG.md`

## Validation

`git diff --check -- docs` completed successfully.

No production code or Java tests were changed. No Maven tests were run because
this is documentation and lifecycle reconciliation only.

## Open Questions

None within this reconciliation scope. Existing non-blocking documentation debt
identified by the Design Review remains outside Custos C1 unless separately
tasked.
