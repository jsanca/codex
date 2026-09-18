# Intent 1 Pre-Implementation Reconciliation Report

## Scope

This report reconciles the Intent 1 knowledge package after Deep's
[pre-implementation review](../../reviews/intent1/Intent1PreImplementationReview.md). It is a
documentation-only reconciliation for Custos C1: Complete Secured Mutation Enforcement.

No production code, Java tests, runtime behavior, permission vocabulary, scope hierarchy, or
authorization ordering changed.

## Maintainer Decision Recorded

**MD-C1-01 — Built-In Role Mapping** is accepted for Intent 1:

| Permission | SUPER_ADMIN | SITE_ADMIN | EDITOR | COPYWRITER | REVIEWER | VIEWER |
| --- | --- | --- | --- | --- | --- | --- |
| `site.unarchive` | Yes | Yes | No | No | No | No |
| `contentItem.delete` | Yes | Yes | No | No | No | No |
| `contentItem.restore` | Yes | Yes | Yes | No | No | No |

The engineering plan records the decision and its rationale. `SUPER_ADMIN` blueprint completeness
remains separate from resolver bypass behavior.

## Resolution Matrix

| Review finding | Severity | Resolution |
| --- | --- | --- |
| Role mapping not accepted | MAJOR | Resolved by MD-C1-01 in the engineering plan and canonical test-case role expectations. |
| Test layering wording | MINOR | Reconciled to one primary assertion layer per responsibility, with selective end-to-end confirmation. |
| Slice count | MINOR | Corrected from six to seven bounded slices. |
| ContentType scope coverage | MINOR | Added to Slice 6 for ContentItem delete and restore. |
| TC-13 fixture coupling | MINOR | Three independent archived ContentItem fixtures now required. |
| TC-17 fixture coupling | MINOR | Independent initial resource state required for every operation × authority-state invocation. |
| Stale ADR reference | MINOR | Replaced with the current [Custos model](../../../../knowledge/security/CUSTOS-MODEL.md). |
| Site unarchive JavaDoc | NOTE | Deferred outside C1; documented below as separate debt. |

## Files Reconciled

- `docs/engineering/plan/intent1/engineering-plan.md`
- `docs/knowledge/test-cases/intent1/README.md`
- `docs/knowledge/test-cases/intent1/TC-01-site-unarchive-authorized.md`
- `docs/knowledge/test-cases/intent1/TC-02-site-unarchive-denied.md`
- `docs/knowledge/test-cases/intent1/TC-05-content-item-delete-authorized.md`
- `docs/knowledge/test-cases/intent1/TC-06-content-item-delete-denied.md`
- `docs/knowledge/test-cases/intent1/TC-10-content-item-restore-authorized.md`
- `docs/knowledge/test-cases/intent1/TC-11-content-item-restore-denied.md`
- `docs/knowledge/test-cases/intent1/TC-13-content-item-restore-does-not-imply-publish.md`
- `docs/knowledge/test-cases/intent1/TC-17-no-unsupported-operation-fallback-remains.md`

## Documentation Debt

`SiteService.unarchive` JavaDoc describes a return to an "active state," while the verified domain
transition is `ARCHIVED -> SUSPENDED`. This is a pre-existing wording issue, not an Intent 1
behavior change. It remains deferred outside C1 as required by the review and this reconciliation.

The Intent and Use Cases retain their original role-decision deferral language as historical
requirements context. MD-C1-01 is the later accepted decision that resolves that deferral for
Intent 1; the engineering plan and canonical test cases are the reconciled artifacts.

## Validation

```text
git diff --check -- docs
```

No Maven tests were run because this task changed documentation only.

## Recommendation

The MAJOR finding is resolved and all six MINOR findings are corrected or reconciled. Deep can
perform a narrow re-review of the Intent 1 package for approval to advance to Design.
