# Intent 1 Pre-Implementation Review

- **Review ID:** Intent1PreImplementationReview
- **Reviewed package:** Intent 1 (Custos C1 — Complete Secured Mutation Enforcement)
  - Intent: `docs/engineering/agents/tasks/intent1/intent.md`
  - Use cases: `docs/knowledge/use-cases/intent1/use-case-0{1,2,3}-*.md`
  - Canonical test cases: `docs/knowledge/test-cases/intent1/` (README + TC-01…TC-17)
  - Engineering plan: `docs/engineering/plan/intent1/engineering-plan.md`
- **Owner:** Deep (Architecture / Adversarial Reviewer)
- **Verdict:** CHANGES REQUIRED
- **Date:** 2026-09-18

## 1. Executive verdict

The Intent 1 package is **architecturally sound and well-specified at the technical layer**, but it is
**not yet safe to advance to Design** for two reasons that materially affect implementation and test
fixtures: (1) the built-in role mapping is a product decision the Intent and Use Cases deliberately
defer, and the plan only *recommends* it — leaving `BuiltInRoles` (Slice 2) and the authorized/denied
actors in TC-01/02/05/06/10/11 subject to a decision that has not been made; and (2) the plan's test
strategy contains an internal contradiction about "exactly one layer" that governs how Slice 6 and
Slice 7 are scoped. Neither is a defect in the approach — the permission vocabulary, scope semantics,
authorization/domain ordering, invariant handling, and side-effect safety are all correct and verified
against the current code — but both must be resolved before Design begins.

Everything else is reconciliation-level (MINOR). Findings are severity-classified in §15.

## 2. Evidence reviewed

- All four artifact layers (Intent, 3 use cases, 17 canonical test cases + index, engineering plan).
- Current Custos source, read directly to validate plan feasibility:
  - `Permissions.java` (20 keys; no unarchive/delete/restore) — confirms plan §2.
  - `BuiltInRoles.java` (6 roles; `SUPER_ADMIN`/`SITE_ADMIN`/`EDITOR`/`COPYWRITER`/`REVIEWER`/`VIEWER`) — confirms plan §4.2 baseline.
  - `DefaultResourceScopeHierarchy.java` (`ContentItem → ContentType → Site → Global`; no cross-Site parent) — confirms plan §4.3.
  - `DefaultPermissionImplicationRules.java` (update⇒read, publish⇒read, publish⇏update) — confirms plan §2.
  - `DefaultPermissionResolver.java` (invariant → SUPER_ADMIN bypass → scope walk) — confirms plan §4.5.
  - `SecuredSiteService.java` (`unarchive` throws `UnsupportedOperationException`, no null guards) and `SecuredContentItemService.java` (`delete`/`restore` throw after null guards) — confirms plan §2 and Slice 4/5 targets.
  - `SitePermissionsService.java` / `ContentItemPermissionsService.java` (+ `Default*` impls) — no `canUnarchive/canDelete/canRestore`; JavaDoc notes the absences — confirms plan §2.
- Domain semantics (codex-codex):
  - `CodexSiteService.java:108` — `unarchive` transitions `ARCHIVED → SUSPENDED` (matches Intent/UC/plan; the `SiteService.unarchive` Javadoc "active state" is imprecise — see NOTE-1).
  - `ContentItemService.java` — `delete` requires `ARCHIVED`; `restore` returns `ARCHIVED → DRAFT`; both throw on invalid state (domain precondition errors).
- Test surface: `SecuredSiteServiceTest`, `SecuredContentItemServiceTest`, `SecuredContentItemServiceAuthorizationMatrixTest` (custos) and `ConciliumRuntimeDeniedSideEffectsTest` (concilium) exist, confirming Slice 6/7 targets. No `SecuredSiteServiceAuthorizationMatrixTest` exists — the plan correctly flags it as new (Slice 6).
- Supporting context: `docs/PROJECT.md`, `docs/OSK.md`, `docs/engineering/roadmap/ROADMAP.md`, `docs/knowledge/security/CUSTOS-MODEL.md`, `docs/knowledge/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md`.

## 3. Intent alignment findings

- **Aligned.** The plan solves exactly the three named fail-closed operations and does not broaden
  scope. The non-goals list (§10) faithfully reproduces the Intent's non-goals.
- No deferred concern (role administration, direct grants, decision records/traces, Observance auth
  metrics, list filtering, alias authorization, persistence, Porta) is pulled into C1. Verified against
  the plan's §10 and the slice definitions.
- **One unverified alignment nuance (NOTE):** the plan's §4.2 states `SUPER_ADMIN` "receives every new
  permission by definition," but `BuiltInRoles.SUPER_ADMIN` is a static `Set.of(...)` enumeration — it
  does *not* auto-include new keys. Enforcement for `SUPER_ADMIN` is via resolver bypass, so this is a
  blueprint-completeness/introspection edit, not an enforcement change. The plan already says this
  ("the blueprint stays complete"); Slice 2 must explicitly edit the `SUPER_ADMIN` set. No action needed,
  but implementers must not assume the bypass makes the blueprint edit optional.

## 4. Use-case findings

- **Complete and faithful.** All three use cases carry the required dimensions: main authorized flow,
  unauthorized flow, domain-precondition flow (delete/restore), hard-invariant flow, scope boundaries,
  downstream side-effect semantics, and the explicit no-UOE goal.
- The use cases correctly preserve domain state semantics (`ARCHIVED → SUSPENDED`, delete requires
  `ARCHIVED`, `ARCHIVED → DRAFT`) and keep them separate from authorization.
- No missing, redundant, or contradictory behavior found between the three use cases and the Intent.

## 5. Test-case findings

- All 17 canonical test cases are behavioral and implementation-agnostic (no Java class names, method
  names, mock libraries, module names, or slices). Confirmed by inspection.
- Traceability is complete: every observable verification across the three use cases maps to at least
  one TC (see index). No orphaned verification.
- Role expectations are correctly marked provisional in the index and per-TC "Given" clauses.
- **TC-13 (MINOR-4):** the current TC-13 performs three sequential actions (Actor R restore; Actor R
  publish; Actor P restore) against a single item. The first action (a granted restore) mutates the item
  from `ARCHIVED` to `DRAFT`, so the subsequent assertions are no longer operating from the same initial
  state (Actor P's restore would now hit a domain precondition before authorization is even meaningful).
  **Recommendation:** give each of the three assertions its own independent archived item fixture so no
  assertion depends on the outcome of another. This is the exact coupling hazard the review scope flags.
- **TC-08 / TC-14 (satisfied):** wording already states Site-A authority must not authorize Site-B, and
  the boundary notes already clarify that broader/Global authority legitimately covers multiple Sites.
  No change required beyond a trivial wording polish if desired.
- **TC-17 (MINOR-5):** the no-fallback scenario is correct but does not state independent initial
  fixtures per invocation. Because `delete` destroys the item and `restore` changes its status, the
  "authorized vs unauthorized" pair per operation should each start from a fresh archived resource.
  **Recommendation:** add an explicit precondition that each invocation uses independent initial state.

## 6. Permission vocabulary assessment

- **Sound.** Three distinct keys — `site.unarchive`, `contentItem.delete`, `contentItem.restore` — are
  correct and consistent with the existing `<resource>.<operation>` naming (`site.archive`,
  `contentItem.archive`, etc.).
- **Reuse would be incorrect.** `delete` is irreversible (≠ `archive`); `restore` is the inverse
  transition (≠ `archive`); `unarchive` is the inverse of `archive` and moves a Site out of a
  suspended-terminal state. Collapsing any of these onto `archive` would silently broaden archivers into
  deleters/restorers/unarchivers, violating the Intent's "no accidental privilege expansion."
- **No implication rules should be added.** Correct — adding archive⇒restore or archive⇒unarchive, or
  restore⇒publish, would be silent authority expansion. The current implication rules remain untouched.
- **Minor observation:** `site.unarchive` is the only `un-` verb form in the catalog. It is acceptable
  because it mirrors the existing domain command `UnarchiveSiteCommand`; no rename required, but it is
  worth a one-line note in the constant's JavaDoc that it is the inverse of `site.archive`.

## 7. Role-mapping assessment (adversarial)

The plan recommends (and the test cases adopt provisionally):

| Permission | SUPER_ADMIN | SITE_ADMIN | EDITOR | COPYWRITER | REVIEWER | VIEWER |
| --- | --- | --- | --- | --- | --- | --- |
| `site.unarchive` | ✓ | ✓ | — | — | — | — |
| `contentItem.delete` | ✓ | ✓ | — | — | — | — |
| `contentItem.restore` | ✓ | ✓ | ✓ | — | — | — |

- **`EDITOR → restore` is defensible but product-ambiguous.** EDITOR already holds `contentItem.archive`;
  granting restore restores lifecycle symmetry (an editor who can archive should be able to return an
  item to draft). This is a *reasonable* default, not a forced conclusion: "lifecycle symmetry" is a
  convenience argument, and the Intent explicitly leaves "should restore align with update or archive?"
  open (UC3 Open Questions).
- **`SITE_ADMIN → delete` is appropriately aligned, not over-broad.** SITE_ADMIN already holds the full
  Site lifecycle and full content lifecycle including `archive`. Granting the irreversible `delete` to
  SITE_ADMIN is the natural home for destructive authority. Restricting delete to SITE_ADMIN/SUPER_ADMIN
  (denying EDITOR) makes delete effectively *stronger* than archive, which answers the "should delete be
  stronger than archive?" question coherently — **but only as a recommendation.**
- **No accidental privilege escalation** in the recommended table: no role gains a permission outside its
  current responsibility domain (VIEWER/REVIEWER/COPYWRITER unchanged; EDITOR gains only the reversible
  restore).
- **MAJOR-1: this mapping is a product decision, not a settled fact.** The Intent ("Which built-in roles
  may delete/restore/unarchive?" — "belong to use cases and design") and the use cases defer it. The plan
  only recommends. Because Slice 2 edits `BuiltInRoles` and TC-01/02/05/06/10/11 hard-code authorized
  vs denied actors from this table, an unaccepted mapping silently freezes product intent into
  implementation and tests. **An explicit maintainer decision is required before Design:** either accept
  §4.2 as-is, or specify an alternative, and record it. Until then the provisional markers in the test
  cases must remain.

## 8. Scope assessment

- **Matches current hierarchy.** Site unarchive `Site → Global`; ContentItem delete/restore
  `ContentItem → ContentType → Site → Global` — both mirror the existing `site.archive` /
  `contentItem.archive` walks and require no new scope layer. Verified against
  `DefaultResourceScopeHierarchy`.
- **No cross-Site leakage.** The hierarchy's parent construction never leaves the originating Site; the
  new permissions inherit this. Verified.
- **QA finding on ContentType/Global coverage (MINOR-3):** the canonical test cases enumerate Site-scope
  cross-boundary denial (TC-03/08/14) but do not enumerate a ContentType-scope or Global-scope grant
  explicitly. This is a *coverage completeness* gap, not an invariant gap: cross-Site isolation is the
  only scope invariant under test, and it is covered. **Recommendation:** have Slice 6's matrix
  extension include at least one ContentType-scope grant scenario (and rely on the existing SUPER_ADMIN
  bypass test for Global coverage) to close the QA finding. This can be added during Slice 6 without
  reopening the objective; it does not block Design.

## 9. Authorization / domain ordering assessment

- **Correct.** The proposed ordering — null-guard → authorize (`requireGranted`) → delegate (domain
  validates state) — matches the existing secured methods for `publish`, `archive`, etc. Verified.
- **Custos remains state-blind:** the permission services operate on identifiers + actor + snapshot, not
  entity state. Confirmed by reading `Default*PermissionsService`.
- **Invalid domain state is not transformed into a denial:** a granted delete/restore against a
  non-`ARCHIVED` item surfaces the domain's own precondition error (the domain `delete`/`restore` throw
  on invalid status). Confirmed by `ContentItemService` Javadoc and state-machine validation.
- **TC-07 / TC-12 correctly encode this separation.** No change required.

## 10. Hard-invariant assessment

- **Correct and safe.** `DefaultPermissionResolver` enforces the AGENT + SUPER_ADMIN invariant before any
  SUPER_ADMIN bypass, and it propagates `CustosAgentSuperAdminInvariantViolationException` (never
  downgraded to `Denied`). The new operations reuse the standard chain, so **no invariant code changes
  are required** — the plan's claim (§4.5) is accurate.
- **TC-04/09/15 correctly assert** the fatal signal is not converted to denial and the delegate is not
  invoked.

## 11. Side-effect safety assessment

- **Feasible and already proven for the pattern.** `ConciliumRuntimeDeniedSideEffectsTest` already proves
  denied operations produce no domain mutation, no events, no projection updates, and no Chronicon audit
  records, with the same runtime wiring the new operations will use.
- Slice 7 extends this to unarchive/delete/restore. The existing test technique (recorded-events baseline,
  `ChroniconRepository.findAll()` empty / baseline-count, state unchanged) is directly reusable for all
  three, including `delete` (whose denied path never reaches the delegate, so no `ContentItemDeletedEvent`
  and no index-removal projection occur).
- No unobserved side effect identified. The four guarantees named by the Intent are fully covered by
  TC-16 and Slice 7.

## 12. Engineering-slice assessment

- **Dependency order is correct.** Vocabulary → roles → permission-service API → decorators → matrix →
  runtime. Each slice depends only on prior ones. Slice 3 (permission service) is correctly noted as
  runtime-independent of Slice 2 (roles).
- **Slice sizes are appropriate and reviewable.** The two review checkpoints (after Slice 3, after Slice
  7) are well placed.
- **No slice combines unrelated decisions.** Vocabulary (Slice 1) is kept separate from roles (Slice 2),
  which is important precisely because the role mapping is a product decision (MAJOR-1).
- **MINOR-2: "Six bounded slices" vs seven.** §6 header says "Six bounded slices," but the plan defines
  seven (Slice 1–7) and §12 sequences seven. Correct the header to "Seven bounded slices."

## 13. Test-layer assessment

- **Layer responsibilities are clear** (permission-service unit, decorator unit, blueprint, matrix,
  runtime integration) and the intent to avoid duplication is sound.
- **MINOR-1: internal contradiction in the layering principle.** §7 asserts "each responsibility is
  asserted at exactly one layer," and §8/§11 assert every observable verification is "covered by exactly
  one test at the layer named in §8." But §8's traceability matrix maps several rows to **two** layers
  (e.g. "UC1 authorized main flow" → "Matrix + Runtime integration"). This is not a behavioral defect —
  matrix verifies role×scope grant while runtime verifies downstream side effects — but the "exactly one
  layer" wording contradicts the two-layer mapping. **Recommendation:** reword §7 and §8 to "one primary
  assertion layer per responsibility, with selective end-to-end confirmation at the runtime layer," and
  replace "exactly one test" with "at least one test at its primary layer." This is the wording the
  review scope itself suggests.

## 14. Architecture-boundary assessment

- **C1 stays within boundaries.** Production changes are confined to `codex-custos` (`api.model`,
  `api.service`, `internal.service`); Slice 7 touches `codex-concilium` test code only. No new module
  dependency, no new event choreography, and no change to Chronicon, Observance, decision records, traces,
  role administration, direct grants, list filtering, persistence, Porta, or Olorin. Verified against the
  plan §10 and the current module graph.
- The only additive public-API changes are three constants + three interface methods, all additive; the
  only implementers are the internal defaults and test stubs, so no external breakage.

## 15. Required changes before Design

- **MAJOR-1 — Role semantics decision (required before Design).** Record an explicit maintainer decision
  on the §4.2 role table: accept it as-is, or specify the alternative. This must precede Slice 2 because
  it freezes `BuiltInRoles` and the authorized/denied actors in TC-01/02/05/06/10/11. The plan's
  recommendation is coherent and may stand as the proposed default, but it must not be silently assumed.
- **MAJOR-2 — nothing else rises to MAJOR.** (The test-layering issue is MINOR because it is a wording
  reconciliation with no behavioral ambiguity; see MINOR-1.)

## 16. Optional improvements (MINOR / NOTE, not blocking)

- **MINOR-1 — test-layering wording.** Reword "exactly one layer/test" to "one primary assertion layer
  per responsibility, with selective end-to-end confirmation" (§7, §8 intro, §11).
- **MINOR-2 — slice count.** Change "Six bounded slices" to "Seven bounded slices" (§6).
- **MINOR-3 — scope coverage completeness.** Add a ContentType-scope grant scenario to Slice 6's matrix
  to close the QA finding; rely on the SUPER_ADMIN bypass test for Global coverage.
- **MINOR-4 — TC-13 fixture independence.** Split TC-13's three assertions across independent archived
  item fixtures to avoid state coupling.
- **MINOR-5 — TC-17 fixture independence.** Add an explicit precondition that each operation×authority
  invocation uses independent initial state.
- **MINOR-6 — stale ADR reference.** Plan §1 cites `docs/future-forward/ADR-009.md`, which no longer
  exists after the documentation migration. The Custos authorization model authority is now
  `docs/knowledge/security/CUSTOS-MODEL.md`. Separately, the old Custos authorization ADR (future-forward
  ADR-009) was not migrated into `docs/engineering/adr/` (whose ADR-009 is now a different topic), and
  `AGENTS.md`/`CLAUDE.md` still cite `docs/security/...` (also moved). This is a documentation-drift
  issue outside C1 but worth a follow-up.
- **NOTE-1 — pre-existing domain Javadoc imprecision (out of C1 scope).** `SiteService.unarchive`
  Javadoc says "active state," but the implementation transitions `ARCHIVED → SUSPENDED`. C1 must not
  "fix" this (domain semantics are out of scope), but a reader may be misled.

## 17. Final recommendation

**CHANGES REQUIRED.** Do not begin Design until MAJOR-1 (the role-mapping decision) is resolved and
recorded. The technical plan is sound and verified against the code; once the role table is accepted and
the MINOR wording/fixture items are reconciled, the package can proceed to Design with high confidence.
The two review checkpoints proposed in the plan (after Slice 3 and after Slice 7) should be retained.

## Validation

- `git diff --check -- docs` → clean (no output).
- No production code or test code modified; no commits made.
