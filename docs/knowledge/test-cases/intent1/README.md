# Intent 1 — Canonical Test Cases

Status: Draft
Scope: Custos C1 — Complete Secured Mutation Enforcement
Sources of truth: `intent.md` (accepted intent), the three use cases under
`docs/knowledge/use-cases/intent1/`, and `docs/engineering/plan/intent1/engineering-plan.md`
(traceability only — behavior comes from Intent + Use Cases).

## Purpose

These test cases translate the accepted Intent and Use Cases into explicit, verifiable
Given / When / Then scenarios. They are behavioral and implementation-agnostic: they define
what must be proven, not how it is implemented or tested.

Behavioral authority order is `Intent → Use Cases → Test Cases`. The engineering plan does not
override Intent or Use Case behavior.

## Test Case List

| ID | Title | Source Use Case | Operation |
| --- | --- | --- | --- |
| TC-01 | Authorized Site unarchive | UC1 | Site unarchive |
| TC-02 | Denied Site unarchive | UC1 | Site unarchive |
| TC-03 | Site unarchive scope isolation | UC1 | Site unarchive |
| TC-04 | Site unarchive hard invariant | UC1 | Site unarchive |
| TC-05 | Authorized ContentItem delete | UC2 | ContentItem delete |
| TC-06 | Denied ContentItem delete | UC2 | ContentItem delete |
| TC-07 | Delete invalid domain state | UC2 | ContentItem delete |
| TC-08 | Delete scope isolation | UC2 | ContentItem delete |
| TC-09 | Delete hard invariant | UC2 | ContentItem delete |
| TC-10 | Authorized ContentItem restore | UC3 | ContentItem restore |
| TC-11 | Denied ContentItem restore | UC3 | ContentItem restore |
| TC-12 | Restore invalid domain state | UC3 | ContentItem restore |
| TC-13 | Restore does not imply publish | UC3 | ContentItem restore |
| TC-14 | Restore scope isolation | UC3 | ContentItem restore |
| TC-15 | Restore hard invariant | UC3 | ContentItem restore |
| TC-16 | Denied secured mutations produce no domain side effects | UC1, UC2, UC3 | all three |
| TC-17 | No unsupported-operation fallback remains | UC1, UC2, UC3 | all three |

## Traceability Matrix

Each observable verification from the three use cases maps to at least one test case.

| Test Case | Source Use Case | Observable verification covered |
| --- | --- | --- |
| TC-01 | UC1 | Sufficient authority can complete unarchive on an archived Site |
| TC-02 | UC1 | Insufficient authority cannot complete unarchive; Site unchanged |
| TC-03 | UC1 | Authority on one Site does not enable unarchive on another Site |
| TC-04 | UC1 | Hard invariant violation is fatal and not converted to denial |
| TC-05 | UC2 | Sufficient authority can delete an archived ContentItem |
| TC-06 | UC2 | Insufficient authority cannot delete; item intact and unchanged |
| TC-07 | UC2 | Granted call against non-ARCHIVED item yields a domain precondition failure; authorization does not replace state-machine validation |
| TC-08 | UC2 | Authority on one resource does not enable delete on an unrelated resource; broader-scope authority stays within Site boundaries |
| TC-09 | UC2 | Hard invariant violation is fatal and not converted to denial |
| TC-10 | UC3 | Sufficient authority can restore an archived item to DRAFT |
| TC-11 | UC3 | Insufficient authority cannot restore; status remains ARCHIVED |
| TC-12 | UC3 | Granted call against invalid state yields a domain precondition failure |
| TC-13 | UC3 | Restore authority does not imply publish; publish authority does not imply restore |
| TC-14 | UC3 | Authority on one resource does not enable restore on an unrelated resource; broader-scope authority stays within Site boundaries |
| TC-15 | UC3 | Hard invariant violation is fatal and not converted to denial |
| TC-16 | UC1, UC2, UC3 | Denied operations produce no domain mutation, event, projection update, or Chronicon audit record |
| TC-17 | UC1, UC2, UC3 | Secured runtime never falls back to an unsupported-operation refusal; every mutation yields an explicit decision |

## Role Semantics — MD-C1-01 Accepted

The following role expectations are the accepted maintainer decision for Intent 1 (MD-C1-01):

| Permission | SUPER_ADMIN | SITE_ADMIN | EDITOR | COPYWRITER | REVIEWER | VIEWER |
| --- | --- | --- | --- | --- | --- | --- |
| site unarchive | granted | granted | denied | denied | denied | denied |
| content item delete | granted | granted | denied | denied | denied | denied |
| content item restore | granted | granted | granted | denied | denied | denied |

`EDITOR` may restore but may not delete. `SITE_ADMIN` may unarchive Sites and delete or restore
ContentItems within its valid scope. `SUPER_ADMIN` retains the complete built-in permission
vocabulary; resolver bypass behavior remains separate from blueprint completeness.

## Cross-cutting invariants

The following apply to every mutation covered here and are asserted across the set:

- **Explicit permission semantics** — each operation yields an authorization decision, never a
  fallback unsupported-operation behavior (TC-17).
- **Fail closed** — a denied decision prevents delegate execution and all resulting domain side
  effects (TC-02/06/11 and TC-16).
- **Domain / authorization separation** — Custos decides whether a call may proceed; the domain
  state machine decides whether it is valid for the current state (TC-07, TC-12).
- **Scope isolation** — authority for one Site never crosses into another Site (TC-03/08/14).
- **Hard invariant preservation** — a fatal Custos invariant stays fatal and never becomes a
  normal denied decision (TC-04/09/15).

## State semantics (not redefined here)

- Site unarchive: `ARCHIVED → SUSPENDED`.
- ContentItem delete: requires `ARCHIVED`.
- ContentItem restore: `ARCHIVED → DRAFT`.

These test cases must not redefine these transitions; they assert them.
