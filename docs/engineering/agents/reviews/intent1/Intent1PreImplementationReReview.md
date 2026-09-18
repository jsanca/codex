# Intent 1 Pre-Implementation Re-Review

- **Review ID:** Intent1PreImplementationReReview
- **Reviewed against:** `docs/engineering/agents/reviews/intent1/Intent1PreImplementationReview.md`
- **Owner:** Deep (Architecture / Adversarial Reviewer)
- **Verdict:** PASS WITH MINOR CHANGES
- **Date:** 2026-09-18

## 1. Verdict

**PASS WITH MINOR CHANGES — proceed to Design.**

MAJOR-1 is resolved: the built-in role mapping is now an accepted maintainer decision (MD-C1-01),
recorded in the engineering plan and canonical test cases, and no longer presented as provisional.
No material contradiction remains in role semantics, scope semantics, canonical test behavior, the
engineering sequence, or architecture boundaries. The single residual item is editorial and does not
affect Design semantics: the documentation debt that the pre-migration Custos authorization ADR has
no migrated equivalent in `docs/engineering/adr/` is not explicitly recorded in the reconciliation
report (see §4, NOTE-R1). This can be recorded without reopening the objective.

## 2. Findings checked

Narrow re-verification only; the full architectural assessment from the original review is not
repeated.

### MAJOR-1 — Role mapping decision → RESOLVED

- MD-C1-01 is explicitly recorded in `Intent1PreImplementationReconciliation-REPORT.md` (§"Maintainer
  Decision Recorded") and in the plan §4.2 ("MD-C1-01 accepted", "no longer a provisional
  recommendation").
- The accepted mapping matches the required table exactly (`site.unarchive` = SUPER_ADMIN/SITE_ADMIN;
  `contentItem.delete` = SUPER_ADMIN/SITE_ADMIN; `contentItem.restore` = SUPER_ADMIN/SITE_ADMIN/EDITOR).
- Affected test cases no longer use provisional language: repo-wide grep for `provisional`/
  `provisionally` across `docs/knowledge/test-cases/intent1/` returns no matches. The README §"Role
  Semantics — MD-C1-01 Accepted" and TC-01/02/05/06/07/10/11 reference MD-C1-01 as accepted.
- No conflicting role mapping remains: the Intent and Use Cases retain only their original *deferral*
  language (no specific mapping), which MD-C1-01 resolves; the reconciliation report states this
  explicitly. No artifact asserts a competing mapping.

### MINOR-1 — Test layering wording → RESOLVED

Plan §7 now reads "one primary assertion layer per responsibility, with selective end-to-end
confirmation where needed" and §8 closes with "maps to at least one planned test at its primary
layer, with selective end-to-end confirmation where needed." The former "exactly one layer/test"
wording is gone, so no contradiction with the multi-layer traceability rows remains.

### MINOR-2 — Slice count → RESOLVED

Plan §6 now reads "Seven bounded slices" (line 184) and consistently defines Slice 1 through Slice 7.

### MINOR-3 — ContentType scope coverage → RESOLVED

Slice 6 §"Behaviour verified" now includes "ContentType-scoped authority authorizes delete and
restore only for ContentItems of that ContentType within the same Site," and the acceptance criteria
explicitly require "ContentType-scoped delete/restore coverage." This adds coverage, not a new scope
model (the `ContentItem → ContentType → Site → Global` walk is unchanged).

### MINOR-4 — TC-13 fixture independence → RESOLVED

TC-13 now specifies three independent archived ContentItem fixtures (restore-state, restore-authority,
publish-authority), one per assertion, with an explicit note that no assertion depends on another's
state mutation.

### MINOR-5 — TC-17 fixture independence → RESOLVED

TC-17 now requires "independent initial resource state" for every operation × authority-state
combination, both in preconditions and negative/boundary notes.

### MINOR-6 — Stale ADR reference → RESOLVED (with residual NOTE)

The stale `docs/future-forward/ADR-009.md` reference is removed; the plan §1 now cites
`docs/knowledge/security/CUSTOS-MODEL.md` as the current Custos authority. No `future-forward` or
`ADR-009` string remains in the plan, test cases, or reconciliation report (grep-verified). The old
ADR was not silently recreated. See NOTE-R1 for the residual debt-recording omission.

### NOTE-1 — SiteService.unarchive JavaDoc → TRACKED

Recorded as separate debt in the reconciliation report §"Documentation Debt" (the JavaDoc "active
state" vs the verified `ARCHIVED → SUSPENDED` transition). Correctly left outside C1.

## 3. Resolution matrix

| Previous Finding | Status | Evidence |
| --- | --- | --- |
| MAJOR-1 role mapping | RESOLVED | MD-C1-01 in plan §4.2 + README; no `provisional` wording remains in test cases |
| MINOR-1 layering wording | RESOLVED | Plan §7/§8 "one primary assertion layer … selective end-to-end confirmation" |
| MINOR-2 slice count | RESOLVED | Plan §6 "Seven bounded slices" |
| MINOR-3 ContentType scope coverage | RESOLVED | Slice 6 behaviour + acceptance criteria add ContentType-scoped delete/restore |
| MINOR-4 TC-13 fixtures | RESOLVED | Three independent archived fixtures in TC-13 |
| MINOR-5 TC-17 fixtures | RESOLVED | Independent initial state per operation × authority-state in TC-17 |
| MINOR-6 stale ADR reference | RESOLVED (with NOTE-R1) | Plan cites `knowledge/security/CUSTOS-MODEL.md`; no `future-forward` reference remains |
| NOTE-1 JavaDoc debt | TRACKED | Reconciliation report §"Documentation Debt" |

## 4. Any remaining issues

- **NOTE-R1 (editorial, non-blocking).** The reconciliation report records the stale-reference
  *replacement* but does not record the separate documentation debt that the pre-migration Custos
  authorization ADR has no migrated equivalent in `docs/engineering/adr/` — the Custos model survives
  only as knowledge (`docs/knowledge/security/CUSTOS-MODEL.md`), not as a decision record. The old ADR
  was not silently recreated, so no behavior is at risk; but a one-line debt note (either in the
  reconciliation report or the roadmap's deferred list) would make the gap visible rather than
  implicit. This does not affect role/scope/test/sequence/boundary semantics and therefore does not
  block Design.
- **NOTE-R2 (pre-existing, out of C1 scope).** `AGENTS.md`/`CLAUDE.md` still cite the moved
  `docs/security/CUSTOS-MODEL.md` path (now `docs/knowledge/security/`). This is broader
  documentation drift, unrelated to Intent 1, and worth a follow-up outside this objective.

## 5. Regression check

Reconciliation introduced no behavioral change to Intent, Use Cases, or the technical decisions:

- Permission vocabulary — still three distinct keys, no reuse, no new implication rules (§4.1 unchanged).
- Scope hierarchy — unchanged (`Site → Global`; `ContentItem → ContentType → Site → Global`); the
  ContentType-scope addition is coverage only, not a new scope model.
- Authorization/domain ordering — unchanged (§4.4, state-blind Custos, domain validates state).
- Hard-invariant semantics — unchanged (§4.5, AGENT + SUPER_ADMIN fatal, propagated).
- Side-effect guarantees — unchanged (Slice 7, four denials: no mutation/event/projection/audit).
- C1 out-of-scope boundaries — unchanged (§10).
- Intent/Use Cases — untouched; the reconciliation explicitly retains their role-deferral language as
  historical context and layers MD-C1-01 on top, consistent with `Intent → Use Cases → Test Cases`
  authority order.

## 6. Final recommendation

**Proceed to Design.** The MAJOR finding is resolved and all six MINOR findings are reconciled. The
sole residual (NOTE-R1) is editorial documentation-debt recording that can be captured during or
after Design without reopening the objective. Retain the plan's two review checkpoints (after Slice 3
and after Slice 7).

## Validation

`git diff --check -- docs` → clean (exit 0). No production code or tests modified; no commits.
