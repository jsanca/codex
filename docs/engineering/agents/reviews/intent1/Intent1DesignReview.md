# Intent 1 Design Review — Gate D (Design Approved)

- **Review ID:** Intent1DesignReview
- **Reviewed artifact:** `docs/engineering/design/intent1/design.md` (Narrow Custos Design)
- **Owner:** Deep (Architecture Reviewer / Adversarial Reviewer)
- **Scope:** Custos C1 — Design Approval Gate
- **Verdict:** PASS WITH MINOR CHANGES — implementation authorized
- **Date:** 2026-09-18

## 1. Executive verdict

The Design is a faithful and technically sound realization of the approved planning package. It
introduces exactly the planned delta — three permission keys, three `can*` methods with default
implementations, three `BuiltInRoles` blueprint updates per MD-C1-01, and the replacement of three
`UnsupportedOperationException` bodies with the standard secured-mutation body — and it introduces
no hidden architectural change. Every surrounding layer (resolver, decision service, scope
hierarchy, implication rules, composer, runtime) is already correct and is correctly identified as
unchanged.

Gate D is satisfied. Implementation is authorized, with three MINOR (reconciliation-level) accuracy
corrections that do not alter implementation semantics and are folded into Slice 1–3 execution
rather than reopened.

## 2. Evidence reviewed

Planning package:

- `docs/engineering/agents/tasks/intent1/intent.md` — accepted intent.
- `docs/knowledge/use-cases/intent1/use-case-0{1,2,3}-*.md` — three canonical use cases.
- `docs/knowledge/test-cases/intent1/` — README + TC-01 through TC-17.
- `docs/engineering/plan/intent1/engineering-plan.md` — including MD-C1-01 and §1.1 Lifecycle / Execution Gates.
- `docs/engineering/agents/reviews/intent1/Intent1PreImplementationReview.md` and `Intent1PreImplementationReReview.md`.
- `docs/engineering/agents/reports/knowledge/Intent1PreImplementationReconciliation-REPORT.md`.

Design and lifecycle policy:

- `docs/engineering/design/intent1/design.md` — the artifact under review.
- `docs/engineering/ENGINEERING-LIFECYCLE.md` — Codex lifecycle policy (four-authority model + gates P/D/I/Q).
- `docs/engineering/agents/reports/knowledge/CodexEngineeringLifecycle-REPORT.md` — Elito's curation record.

Repository source (read directly, not trusted to prose):

- `Permissions.java`, `BuiltInRoles.java`, `Role.java`, `AccessDecision.java`, `AccessDecisionRequest.java`, `SiteResourceRef`, `SiteScope`, `ContentItemResourceRef`, `ContentItemScope`.
- `SitePermissionsService`, `ContentItemPermissionsService`, `DefaultSitePermissionsService`, `DefaultContentItemPermissionsService`.
- `SecuredSiteService`, `SecuredContentItemService`, `SecuredServiceComposer`.
- `DefaultPermissionResolver`, `DefaultResourceScopeHierarchy`, `DefaultPermissionImplicationRules`.
- `codex-custos/src/main/java/module-info.java`.
- `codex-concilium/.../ConciliumRuntime.java`.
- Domain: `SiteService`, `ContentItemService`, `CodexSiteService`, `UnarchiveSiteCommand`, `DeleteContentItemCommand`, `RestoreContentItemCommand`, `ArchiveSiteCommand`.
- Tests: `SecuredSiteServiceTest`, `SecuredContentItemServiceTest`, `SecuredContentItemServiceAuthorizationMatrixTest`, `DefaultSitePermissionsServiceTest`, `DefaultContentItemPermissionsServiceTest`, `BuiltInRolesTest`, `PermissionsTest`, `StubSitePermissionsService`, `StubContentItemPermissionsService`, and `ConciliumRuntimeDeniedSideEffectsTest`.

## 3. CURRENT accuracy

Verified against repository code. The CURRENT diagram and §3 analysis are accurate.

- **Three UOE gaps exist exactly where claimed.**
  - `SecuredSiteService.unarchive` (lines 120–123): throws `UnsupportedOperationException`, no null guards, delegate never reached. Confirmed.
  - `SecuredContentItemService.delete` (lines 156–162): null-guards `command` and `actor`, then throws. Confirmed.
  - `SecuredContentItemService.restore` (lines 164–171): null-guards, then throws. Confirmed.
- **Neighbouring secured operations use the stated pattern.** `create`, `findByKey`, `start`, `suspend`, `archive` (Site) and `create`, `findByKey`, `update`, `publish`, `unpublish`, `archive` (ContentItem) each null-guard, call a `can*` method, `requireGranted()`, then delegate. Confirmed.
- **Permission service method counts/shapes are correct.** `SitePermissionsService` has 5 methods; `ContentItemPermissionsService` has 6; each `Default*` implementation builds an `AccessDecisionRequest` (permission key + `ResourceRef` + `ResourceScope`) and delegates to `AccessDecisionService.evaluate`. Confirmed.
- **Composer/runtime topology is accurate.** `SecuredServiceComposer` returns decorators by interface (`wrapSiteService`, `wrapContentItemService`, plus default permission/decision factories); `ConciliumRuntime.secured(snapshotProvider)` wires services by interface, never by method. Confirmed.

Diagram simplifications (the CURRENT `Permissions` box omits the `CONTENT_TYPE_*`, `PERMISSION_*`, and `ROLE_*` constants) are cosmetic and explicitly scoped ("focused on the C1 authorization path"); they do not mislead implementation, so they are not flagged.

## 4. TARGET accuracy

The TARGET diagram introduces only the planned delta and no hidden architectural change.

- Three new `PermissionKey` constants (`SITE_UNARCHIVE`, `CONTENT_ITEM_DELETE`, `CONTENT_ITEM_RESTORE`). Confirmed absent today.
- `BuiltInRoles` updates match MD-C1-01 exactly (SUPER_ADMIN +3, SITE_ADMIN +3, EDITOR +1 restore; COPYWRITER/REVIEWER/VIEWER unchanged).
- `canUnarchiveSite`, `canDeleteContentItem`, `canRestoreContentItem` with default implementations mirroring `canArchive*`.
- Three secured-decorator bodies replaced with the standard authorized-mutation body.

No change to resolver, decision service, hierarchy, implication rules, composer, runtime, or domain services is required or implied. The "Deliberately not changed" list (§6) is accurate.

## 5. Contract review

The three proposed signatures mirror the neighbouring archive methods exactly and require no new
value objects, context objects, default interface methods, changed return types, or new exceptions:

- `SitePermissionsService.canUnarchiveSite(Actor, SiteKey, PermissionResolutionSnapshot) : AccessDecision` — same shape as `canArchiveSite`.
- `ContentItemPermissionsService.canDeleteContentItem(Actor, SiteKey, ContentTypeKey, ContentItemKey, PermissionResolutionSnapshot) : AccessDecision` — same shape as `canArchiveContentItem`.
- `ContentItemPermissionsService.canRestoreContentItem(...)` — same shape.

No `default` method body is added; the interfaces stay strict. The only implementers are the two
`Default*PermissionsService` classes and the two test stubs (`StubSitePermissionsService`,
`StubContentItemPermissionsService`), both of which are internal/test-only, so the additive methods
cause compile-time, not runtime, impact. Confirmed.

**MINOR-1 (code sketch):** Design §8 writes `canUnarchiveSite(actor, command.siteKey(), ...)` for the
unarchive decorator body, but `UnarchiveSiteCommand` exposes `key()`, not `siteKey()` (see
`UnarchiveSiteCommand.java:14`; `SecuredSiteService.archive` already uses `archiveSiteCommand.key()`).
The textual instruction "mirror `SecuredSiteService.archive`" is correct and would lead an implementer
to `command.key()`; only the literal sketch is wrong.

## 6. Resource / scope review

Exact mapping confirmed against the resource/scope model:

| Operation | Permission | ResourceRef | ResourceScope | Walk |
|---|---|---|---|---|
| Site unarchive | `SITE_UNARCHIVE` | `SiteResourceRef(siteKey)` | `SiteScope(siteKey)` | Site → Global |
| ContentItem delete | `CONTENT_ITEM_DELETE` | `ContentItemResourceRef(site, type, item)` | `ContentItemScope(site, type, item)` | Item → Type → Site → Global |
| ContentItem restore | `CONTENT_ITEM_RESTORE` | `ContentItemResourceRef(...)` | `ContentItemScope(...)` | Item → Type → Site → Global |

`DefaultResourceScopeHierarchy.parentOf` and `covers` never cross `SiteKey` boundaries, so
cross-Site isolation is inherited unchanged. The existing hierarchy is sufficient; no new scope
layer or ref/scope type is needed. Confirmed.

## 7. Failure semantics

The Design preserves exactly three outcomes, introducing no fourth and requiring no exception
translation or new failure mechanism:

1. **Denied** — `AccessDecisionService.evaluate` returns `Denied`; `requireGranted()` throws `AccessDeniedException`; delegate not invoked; no mutation/event/projection/Chronicon audit. Confirmed (`AccessDecision.Denied.requireGranted` throws `AccessDeniedException`).
2. **Hard invariant** — `DefaultPermissionResolver.enforceAgentSuperAdminInvariant` runs before any `SUPER_ADMIN` bypass and throws `CustosAgentSuperAdminInvariantViolationException`; it propagates through the decorator unchanged and is never downgraded. Confirmed.
3. **Granted + invalid domain state** — `requireGranted()` returns normally, the delegate is invoked, and the domain state machine rejects the transition (e.g. `delete` on non-`ARCHIVED`; `CodexSiteService` `ARCHIVED → SUSPENDED` only). The domain error is not a Custos denial. Confirmed.

## 8. Runtime / JPMS impact

The non-change claims are verified.

- **`SecuredServiceComposer`** — no structural change. Factory methods return `SiteService`/`ContentItemService` wired by the same three constructor arguments; adding methods inside the decorators is invisible to the composer. Confirmed.
- **`ConciliumRuntime.secured(...)`** — no structural change. Composition wires by interface. Confirmed (`ConciliumRuntime.java:205–224`).
- **`ConciliumRuntime.inMemory()`** — unchanged; unsecured path continues to bypass Custos. Confirmed.
- **JPMS** — `codex-custos/module-info.java` already `exports codex.custos.api.model` and `codex.custos.api.service`. New constants and methods live in those exported packages; no `requires`, `requires transitive`, qualified export, or new dependency is introduced. Confirmed.

## 9. Test seams

All five seams exist and are reusable without new test infrastructure:

- Permission-service unit: `DefaultSitePermissionsServiceTest`, `DefaultContentItemPermissionsServiceTest` exist.
- Secured-decorator unit: `SecuredSiteServiceTest` (has `unarchive — fail closed`), `SecuredContentItemServiceTest` (has `delete — fail closed` and `restore — fail closed`) exist.
- Blueprint: `BuiltInRolesTest` exists and already asserts exact permission sets.
- Matrix: `SecuredContentItemServiceAuthorizationMatrixTest` exists; `SecuredSiteServiceAuthorizationMatrixTest` does **not** exist, which the plan correctly flags as new.
- Runtime denied-side-effect: `ConciliumRuntimeDeniedSideEffectsTest` exists and its recorded-events/baseline technique is directly reusable.

The wording "one primary assertion layer per responsibility, with selective end-to-end confirmation"
is compatible with TC-01…TC-17: it avoids redundant same-assertion duplication while permitting the
matrix layer to prove role×scope and the runtime layer to prove denied side-effect absence as
distinct aspects of the same observable behavior. Confirmed.

**MINOR-2 (test-catalog completeness):** The existing `PermissionsTest.catalogSize()` asserts the
catalog has "exactly 20" keys and `PermissionsTest.allPermissionsAreDistinct()` reflects over all
public `PermissionKey` constants. Adding the three constants in Slice 1 will fail `catalogSize()`
unless it is updated to 23. The plan's Slice 1 describes this update as optional ("Optionally extend
an existing catalog-completeness test if one exists"); it is in fact mandatory, and `BuiltInRolesTest`
size assertions (`SUPER_ADMIN` = 20, `SITE_ADMIN` = 17, `EDITOR` `containsExactlyInAnyOrder` of 6)
will likewise fail in Slice 2. Slice 2 already requires updating `BuiltInRolesTest`; only Slice 1's
"optional" framing and the plan's failure to name `PermissionsTest.catalogSize()` are the gap.

## 10. Slice compatibility

The Design is implementable through the approved seven slices without changing their meaning.
Slices 1–3 (vocabulary → role blueprints → permission API/defaults) form the vocabulary/role/API
freeze before the Deep checkpoint; Slices 4–7 (secured decorators → authorization matrix → runtime
guarantees) follow. The design adds no work outside the seven slices and no slice reordering is
required.

No implementation work has begun: the working tree contains no Java source or test changes
(see §12 lifecycle note for the documentation-only edits that are in flight).

## 11. Lifecycle / gate verification

- `docs/engineering/ENGINEERING-LIFECYCLE.md` correctly distinguishes the four authorities:
  - **Behavior authority:** Intent → Use Cases → Canonical Test Cases.
  - **Execution authority:** Engineering Plan.
  - **Technical authority:** Approved Design.
  - **Progress authority:** Review Gates.
- It defines Gate P (Planning Approved), Gate D (Design Approved), Gate I (Implementation Complete), and Gate Q (Objective Complete), and states "Implementation must not begin before Gate D is satisfied."
- Intent 1 currently states the correct lifecycle position in plan §1.1:
  - **Current stage:** DESIGN REVIEW
  - **Implementation authorized:** NO
  - **Pending gate:** Gate D - Design Approved

This is consistent with the lifecycle policy and with the fact that this Design Review is the
Gate D input. On approval, the plan state should advance to IMPLEMENTATION / YES, with the approved
design recorded and Slice 1 as next authorized work (see §14). Per repository convention,
gate-state reconciliation is performed by the knowledge/curation role, not the reviewer, so this
review recommends the change without making it.

## 12. Findings

| ID | Severity | Location | Finding |
|---|---|---|---|
| MINOR-1 | MINOR | `design.md` §8 | Unarchive code sketch uses `command.siteKey()`; `UnarchiveSiteCommand` exposes `key()`. Should read `command.key()` to mirror `SecuredSiteService.archive`. |
| MINOR-2 | MINOR | `engineering-plan.md` Slice 1 / `design.md` §12 | `PermissionsTest.catalogSize()` asserts exactly 20 keys and will fail after Slice 1; the plan labels the catalog-test update "optional". Updating to 23 is mandatory. `BuiltInRolesTest` size/count assertions also change in Slice 2 (already covered by Slice 2's test requirement). |
| MINOR-3 | MINOR | `design.md` §3.3/§6/§7, `engineering-plan.md` §2/§5 | The "remove the absent-from-this-service JavaDoc notes" delta is over-broad. `SitePermissionsService`'s note flags `site.update`/`site.delete` (still absent, not `site.unarchive`); `ContentItemPermissionsService`'s note flags `contentItem.delete` (being added) but never mentions `contentItem.restore`. The correct delta is to remove `contentItem.delete` from the ContentItem note; the Site note should not be removed wholesale. |

No BLOCKER or MAJOR findings. No finding alters implementation semantics, the resource/scope
mapping, failure semantics, runtime/JPMS non-change claims, implication rules, or slice structure.

## 13. Gate D decision

**PASS WITH MINOR CHANGES — implementation is authorized.**

The Design is faithful to the planning package and technically sound against repository evidence.
The three MINOR findings are documentation/sketch-precision issues and are non-blocking; they do
not alter what is implemented and are reconciled during Slice 1–3 execution without reopening the
objective. Gate D is satisfied.

## 14. Next authorized action

Recommended plan-state advance (to be performed by the curation role, not this review):

```text
Current stage:            IMPLEMENTATION
Implementation authorized: YES
Approved design:          docs/engineering/design/intent1/design.md
Next authorized work:     Slice 1 (permission vocabulary)
```

Retained review checkpoints: Deep checkpoint after Slice 3, Final Engineering Review after Slice 7.

## Validation

- `git diff --check -- docs` → clean (exit 0).
- No production or test code changed; no commits made.
