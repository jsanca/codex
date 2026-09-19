# Engineering Plan — Intent 1: Complete Secured Mutation Enforcement

Status: Draft
Operating Role: Product Architect
Scope: Custos C1 — Complete Secured Mutation Enforcement

## 1. Objective and Source Documents

Replace the three fail-closed `UnsupportedOperationException` placeholders in the secured Codex runtime with explicit, distinct Custos authorization semantics for:

- `SiteService.unarchive`
- `ContentItemService.delete`
- `ContentItemService.restore`

Each operation must reach an explicit grant/deny decision through the existing Custos chain, without altering domain lifecycle semantics, scope isolation, hard-invariant propagation, or the denied-operation side-effect guarantees already established for other secured mutations.

### Sources of truth consulted

- `docs/engineering/agents/tasks/intent1/intent.md` — accepted intent for Custos C1.
- `docs/knowledge/use-cases/intent1/use-case-01-site-unarchive.md`
- `docs/knowledge/use-cases/intent1/use-case-02-content-item-delete.md`
- `docs/knowledge/use-cases/intent1/use-case-03-content-item-restore.md`
- `docs/PROJECT.md`, `docs/OSK.md`
- `docs/engineering/roadmap/ROADMAP.md` — Deferred item: "Permission vocabulary and gated secured operations for item delete/restore and site unarchive."
- `docs/knowledge/security/CUSTOS-MODEL.md` — current Custos model and authorization vocabulary.
- Repository evidence for Custos: `codex-custos/src/main/java/codex/custos/` and matching tests.

## 1.1 Lifecycle / Execution Gates

**Current stage:** IMPLEMENTATION

**Implementation authorized:** YES

**Approved design:** `docs/engineering/design/intent1/design.md`

**Gate D:** PASSED

**Next authorized work:** Slice 1 - Permission Vocabulary

### Approved planning evidence

- Intent: `docs/engineering/agents/tasks/intent1/intent.md`
- Use Cases: `docs/knowledge/use-cases/intent1/`
- Canonical Test Cases: `docs/knowledge/test-cases/intent1/`
- Engineering Plan: this document
- Pre-Implementation Review:
  `docs/engineering/agents/reviews/intent1/Intent1PreImplementationReview.md`
- Reconciliation:
  `docs/engineering/agents/reports/knowledge/Intent1PreImplementationReconciliation-REPORT.md`
- Pre-Implementation Re-Review:
  `docs/engineering/agents/reviews/intent1/Intent1PreImplementationReReview.md`

### Approved technical source

`docs/engineering/design/intent1/design.md` is the approved technical source.
`docs/engineering/agents/reviews/intent1/Intent1DesignReview.md` satisfies
Gate D and authorizes the planned implementation sequence.

### Authorized execution sequence

```text
Slice 1
  -> Slice 2
  -> Slice 3
  -> Deep checkpoint
  -> Slice 4
  -> Slice 5
  -> Slice 6
  -> Slice 7
  -> Final Engineering Review
  -> QA / Knowledge Reconciliation
  -> Objective Closeout
```

Required review checkpoints are the Deep checkpoint after Slice 3 and the Final
Engineering Review after Slice 7. Gate D has passed; Slice 1 is the next
authorized work.

## 2. Current-State Summary

### Permission catalog (`codex.custos.api.model.Permissions`)

20 permission keys exist today, grouped as:

- Site: `site.read`, `site.create`, `site.start`, `site.suspend`, `site.archive`
- ContentType: `contentType.read/create/update/archive`
- ContentItem: `contentItem.read/create/update/publish/unpublish/archive`
- Permission mgmt: `permission.read/grant/revoke`
- Role mgmt: `role.assign/role.revoke`

**No `site.unarchive`, `contentItem.delete`, or `contentItem.restore` exists.** The interfaces `SitePermissionsService` and `ContentItemPermissionsService` explicitly note these absences in their JavaDoc.

### Built-in roles (`codex.custos.api.model.BuiltInRoles`)

| Role | Site perms | ContentType perms | ContentItem perms | Mgmt perms |
|---|---|---|---|---|
| `SUPER_ADMIN` | read, create, start, suspend, archive | read, create, update, archive | read, create, update, publish, unpublish, archive | perm.{read,grant,revoke}, role.{assign,revoke} |
| `SITE_ADMIN` | read, start, suspend, archive | read, create, update, archive | read, create, update, publish, unpublish, archive | perm.read, role.{assign,revoke} |
| `EDITOR` | — | — | read, create, update, publish, unpublish, archive | — |
| `COPYWRITER` | — | — | read, create, update | — |
| `REVIEWER` | — | — | read, publish, unpublish | — |
| `VIEWER` | — | — | read | — |

`SUPER_ADMIN` bypass logic is not encoded in the blueprint — it lives in resolver/evaluator behaviour. Hard invariants (AGENT + SUPER_ADMIN) run before any bypass and propagate fatally.

### Implication rules (`DefaultPermissionImplicationRules`)

- `contentItem.update` ⇒ `contentItem.read`
- `contentItem.publish` ⇒ `contentItem.read`
- `contentItem.publish` ⇏ `contentItem.update`

No archive/lifecycle implications exist. No cross-family implications exist.

### Permission services

`SitePermissionsService` and `ContentItemPermissionsService` (in `codex.custos.api.service`) each expose one `can*` method per authorized operation. Default implementations delegate uniformly to `AccessDecisionService.evaluate(request, snapshot)` with a permission key, a `ResourceRef`, and a matching `ResourceScope`. Neither interface declares an unarchive, delete, or restore method today.

### Secured decorators

- `SecuredSiteService.unarchive` throws `UnsupportedOperationException("Secured unarchive is not supported until site unarchive permission semantics are defined")`. Null checks omitted; delegate never reached.
- `SecuredContentItemService.delete` throws `UnsupportedOperationException("... content item delete permission semantics are defined")`. Null-arg guards run first, then throws.
- `SecuredContentItemService.restore` throws `UnsupportedOperationException("... content item restore permission semantics are defined")`. Same null-guard-then-throw shape.

The runtime assembles these decorators through `SecuredServiceComposer.wrap*Service` at `ConciliumRuntime.secured(snapshotProvider)`. The unsecured `ConciliumRuntime.inMemory()` path bypasses them entirely.

### Domain semantics (unchanged by this plan)

- `SiteStatus`: `ARCHIVED → SUSPENDED` via unarchive; no skipping.
- `ContentItemStatus`: delete requires `ARCHIVED`; `ARCHIVED → DRAFT` via restore.
- Custos governs whether a call may proceed; the state machine governs whether it is valid.

### Existing test surface

- `SecuredSiteServiceTest` — nested per-method suites with stub permissions service + spy delegate. Includes an existing `Unarchive — fail closed` section (throws UOE, does not call delegate). Constants: `SITE_A`, `ALICE`, `BOT`, `EMPTY_SNAPSHOT`, `GRANTED`, `DENIED`.
- `SecuredContentItemServiceTest` — matching structure. Includes `Delete — fail closed` and `Restore — fail closed` sections.
- `SecuredContentItemServiceAuthorizationMatrixTest` — integration test wiring `DefaultPermissionResolver → DefaultAccessDecisionService → DefaultContentItemPermissionsService → SecuredContentItemService`. Actors carry realistic built-in roles (`ALICE`=VIEWER, `BOB`=COPYWRITER, `CAROL`=REVIEWER, `DAN`=EDITOR, `EVE`=EDITOR on site-a only, `BOT`=AGENT+SUPER_ADMIN). Includes cross-Site (`SITE_A`/`SITE_B`) scenarios.
- `DefaultSitePermissionsServiceTest`, `DefaultContentItemPermissionsServiceTest` — unit coverage against a stub `AccessDecisionService`.

## 3. Decisions Required

The plan must resolve, before any code is written, decisions about:

1. Permission vocabulary (new keys vs. reuse).
2. Built-in role mappings for each new permission.
3. Scope semantics per operation.
4. Ordering between authorization and domain-state validation.
5. Hard-invariant propagation posture.

Recommendations follow.

## 4. Recommended Decisions

### 4.1 Permission vocabulary — three new distinct keys

Introduce three new `PermissionKey` constants in `Permissions`:

| Constant | Key value | Justification |
|---|---|---|
| `SITE_UNARCHIVE` | `site.unarchive` | Unarchive is the reversal of archive and moves the Site out of a suspended-terminal state. Reusing `site.archive` would let an archiver bring Sites back into circulation without an explicit authority statement, violating "no accidental privilege expansion" and "explicit authority". |
| `CONTENT_ITEM_DELETE` | `contentItem.delete` | Delete is destructive and terminal; unlike archive it cannot be reversed via a domain operation. Reusing `contentItem.archive` would silently convert every archiver into a deleter. |
| `CONTENT_ITEM_RESTORE` | `contentItem.restore` | Restore is the reversal of archive and moves the item back to `DRAFT`. Reusing `contentItem.archive` would collapse two distinct lifecycle capabilities. Restore is also *not* publish and must not imply publish (Use Case 3, Observable Verifications). |

**No reuse.** Each new key represents a semantically distinct capability; reusing an existing key to avoid vocabulary is disallowed by the intent.

**No new implication rules.** The three new keys are self-contained: archive does not imply delete; archive does not imply restore; archive does not imply unarchive; restore does not imply publish. Adding an implication would be a silent authority expansion.

### 4.2 Built-in role mappings — MD-C1-01 accepted

**MD-C1-01 — Built-In Role Mapping** is the accepted maintainer decision for Intent 1. The
following mapping is no longer a provisional recommendation.

| Permission | `SUPER_ADMIN` | `SITE_ADMIN` | `EDITOR` | `COPYWRITER` | `REVIEWER` | `VIEWER` |
|---|---|---|---|---|---|---|
| `site.unarchive` | ✓ | ✓ | — | — | — | — |
| `contentItem.delete` | ✓ | ✓ | — | — | — | — |
| `contentItem.restore` | ✓ | ✓ | ✓ | — | — | — |

Accepted rationale:

- **`SUPER_ADMIN`**: receives every new permission by definition — the blueprint enumerates all built-in domain capabilities (bypass behaviour lives elsewhere; the blueprint stays complete).
- **`SITE_ADMIN`**: already holds full Site lifecycle authority (`start`, `suspend`, `archive`). Adding `unarchive` restores lifecycle symmetry with no capability expansion beyond Site administration. Also holds full content lifecycle (`archive`), so gaining `delete` and `restore` is consistent with the role's existing destructive/lifecycle authority. This is the correct level for destructive operations.
- **`EDITOR`**: holds `contentItem.archive`. Granting `restore` restores lifecycle symmetry with archive — an editor who can send an item to archive should be able to bring it back to draft. Granting `delete`, however, would escalate the editor from a reversible destructive operation (archive/restore) to an irreversible one, violating "no accidental privilege expansion". Delete should stay with `SITE_ADMIN` and `SUPER_ADMIN`.
- **`COPYWRITER`**: does not hold archive today; adding restore would be inconsistent. Does not receive delete for the same reason as EDITOR (destructive escalation).
- **`REVIEWER`**: is a publish/unpublish role, not a lifecycle role. No new permissions.
- **`VIEWER`**: read-only. No new permissions.

**Deferred question**: whether a dedicated destructive-authority role (e.g. `CONTENT_ARCHIVIST`) should be introduced as a stronger authority than SITE_ADMIN. Not recommended for this Intent — introducing new built-in roles broadens the surface area beyond C1. Belongs to a later objective.

### 4.3 Scope semantics

Each new permission uses the same scope walk pattern as its resource family already uses. No new scope layer is introduced; the existing bottom-up walk in `DefaultResourceScopeHierarchy` stands.

| Permission | `ResourceRef` shape | `ResourceScope` levels the walk may consult (finest → broadest) |
|---|---|---|
| `site.unarchive` | `SiteResourceRef(siteKey)` | `SiteScope(siteKey)` → `GlobalScope` |
| `contentItem.delete` | `ContentItemResourceRef(siteKey, contentTypeKey, contentItemKey)` | `ContentItemScope` → `ContentTypeScope` → `SiteScope` → `GlobalScope` |
| `contentItem.restore` | `ContentItemResourceRef(...)` | `ContentItemScope` → `ContentTypeScope` → `SiteScope` → `GlobalScope` |

Each mirrors an existing permission of the same shape (`site.archive`, `contentItem.archive`) so scope-hierarchy behaviour is not re-designed.

**Cross-Site isolation**: unchanged. `DefaultResourceScopeHierarchy` already prevents cross-Site walks; the new permissions inherit this guarantee. Authority over Site A never yields Site B unarchive/delete/restore.

### 4.4 Authorization / domain-precondition ordering

Preserve the existing separation exactly as it is used by `publish`, `archive`, etc.:

```
secured decorator
  1. null-check inputs
  2. call the permissions service → obtain AccessDecision
  3. decision.requireGranted()   // throws AccessDeniedException on denial
  4. delegate.<operation>(command, actor)   // domain state machine validates here
```

Consequences the plan must protect:

- A denied authorization never reaches step 4. No domain state, event, projection, or Chronicon audit record is produced.
- A granted authorization may still fail domain preconditions (e.g. delete against a non-`ARCHIVED` item). That failure is a domain error, not a Custos denial, and Custos does not attempt to model it.
- Custos never inspects domain state. `SitePermissionsService` and `ContentItemPermissionsService` operate on identifiers, actor, and snapshot — not on entity state.

### 4.5 Hard-invariant propagation

The AGENT + `SUPER_ADMIN` invariant continues to propagate as `CustosAgentSuperAdminInvariantViolationException` and is never downgraded to a `Denied` decision. The plan does not modify any invariant code path; it inherits current behaviour because the new operations use the standard chain.

## 5. Architecture Impact

- **Layers touched**: `codex.custos.api.model` (constants + role blueprints), `codex.custos.api.service` (interface methods), `codex.custos.internal.service` (default implementations + secured decorators + implication rules — no change to rules).
- **Module boundaries**: unchanged. `codex-custos` remains the sole home of authorization vocabulary. `codex-concilium` assembly (`ConciliumRuntime.secured`) does not change because it wires services by interface, not by method.
- **Public API surface**: grows by three constants and three interface methods, all additive. No removals. Consumers of the current interfaces remain source-compatible except that new methods on the interfaces will require implementers — the only two implementers are `Default*PermissionsService` and (in tests) the stub doubles. No `default` methods on the interface; this preserves the "implement or fail to compile" rigor already used for the other capabilities.
- **Decorator chain**: unchanged. Existing wrap order (`TimedSiteService → Caching → SecuredSiteService → EventPublishingSiteService → CodexSiteService`) is preserved. Secured decorators only stop throwing UOE — the surrounding decorators are unaffected.
- **JavaDoc drift**: remove `contentItem.delete` from the `ContentItemPermissionsService`
  absence note when Slice 3 adds that capability. Retain the
  `SitePermissionsService` note for `site.update` and `site.delete`, which C1
  does not add.

## 6. Engineering Slices

Seven bounded slices, sequenced so each is reviewable in isolation and each depends only on prior ones.

### Slice 1 — Permission vocabulary

- **Objective**: introduce the three new `PermissionKey` constants.
- **Areas affected**: `codex-custos/src/main/java/codex/custos/api/model/Permissions.java`.
- **Behaviour introduced**: three new constants — `SITE_UNARCHIVE`, `CONTENT_ITEM_DELETE`, `CONTENT_ITEM_RESTORE` — matching the existing constant/JavaDoc style.
- **Tests required**: update `PermissionsTest.catalogSize()` from 20 to 23 in
  the existing permission-catalog completeness test. The three new constants
  are also exercised by later slices.
- **Dependencies**: none.
- **Acceptance criteria**: three constants exist, are exported, and
  `PermissionsTest.catalogSize()` expects 23 built-in permissions.
- **Out of scope**: implication rules, role assignments, permission service methods.

### Slice 2 — Built-in role updates

- **Objective**: extend `BuiltInRoles` blueprints per §4.2.
- **Areas affected**: `codex-custos/src/main/java/codex/custos/api/model/BuiltInRoles.java`.
- **Behaviour introduced**:
  - `SUPER_ADMIN` receives `site.unarchive`, `contentItem.delete`, `contentItem.restore`.
  - `SITE_ADMIN` receives `site.unarchive`, `contentItem.delete`, `contentItem.restore`.
  - `EDITOR` receives `contentItem.restore` only.
- **Tests required**: update `BuiltInRolesTest` expectations for the
  MD-C1-01 blueprint changes, including exact permission sets and affected
  counts. Regression check that no unmodified role gained a new permission.
- **Dependencies**: Slice 1.
- **Acceptance criteria**: blueprint permission sets exactly match §4.2; no other role composition changes.
- **Out of scope**: `SUPER_ADMIN` bypass behaviour (unchanged).

### Slice 3 — Permission service API + default implementation

- **Objective**: add `canUnarchiveSite`, `canDeleteContentItem`, `canRestoreContentItem` and their default implementations.
- **Areas affected**:
  - `codex-custos/src/main/java/codex/custos/api/service/SitePermissionsService.java`
  - `codex-custos/src/main/java/codex/custos/api/service/ContentItemPermissionsService.java`
  - `codex-custos/src/main/java/codex/custos/internal/service/DefaultSitePermissionsService.java`
  - `codex-custos/src/main/java/codex/custos/internal/service/DefaultContentItemPermissionsService.java`
  - Remove `contentItem.delete` from the `ContentItemPermissionsService`
    absence note. Retain the `SitePermissionsService` absence note for
    `site.update` and `site.delete`, which remain unsupported.
- **Behaviour introduced**: three new `can*` methods, each building an `AccessDecisionRequest` and delegating to `AccessDecisionService.evaluate(request, snapshot)`, mirroring the exact shape of `canArchiveSite` / `canArchiveContentItem`.
- **Tests required**:
  - Extend `DefaultSitePermissionsServiceTest` — a nested suite for `canUnarchiveSite` with null-guard tests plus grant/deny passthrough via a stub `AccessDecisionService`.
  - Extend `DefaultContentItemPermissionsServiceTest` for `canDeleteContentItem` and `canRestoreContentItem` with the same pattern.
- **Dependencies**: Slice 1 (permission constants). Independent of Slice 2 at runtime — resolver output depends on roles but the permission service just constructs and delegates a request.
- **Acceptance criteria**: three new methods present; each performs required
  null guards, logs at DEBUG, and returns whatever the decision service
  returns. `ContentItemPermissionsService` no longer lists
  `contentItem.delete` as absent; `SitePermissionsService` retains its
  accurate `site.update` / `site.delete` absence note.
- **Out of scope**: decorator behaviour.

### Slice 4 — `SecuredSiteService.unarchive` gating

- **Objective**: replace the `UnsupportedOperationException` with a real Custos check.
- **Areas affected**: `codex-custos/src/main/java/codex/custos/internal/service/SecuredSiteService.java`.
- **Behaviour introduced**: `unarchive` performs null guards, calls `permissionsService.canUnarchiveSite(actor, command.siteKey(), snapshotProvider.get())`, invokes `decision.requireGranted()`, and only then delegates to `SiteService.unarchive`.
- **Tests required**: replace the current `Unarchive — fail closed` section in `SecuredSiteServiceTest` with a proper nested suite mirroring the `Archive` suite:
  - null guards
  - denies when permission service returns `Denied` — asserts `AccessDeniedException`, asserts delegate not called
  - grants when permission service returns `Granted` — asserts delegate called exactly once, return value preserved
  - snapshot provider invoked exactly once per call.
- **Dependencies**: Slices 1, 3.
- **Acceptance criteria**: no `UnsupportedOperationException` remains for `unarchive`; observable behaviour matches Use Case 1.
- **Out of scope**: matrix/scope-boundary coverage (Slice 6), runtime-integration side-effect verification (Slice 6).

### Slice 5 — `SecuredContentItemService.delete` and `.restore` gating

- **Objective**: replace both `UnsupportedOperationException`s with real Custos checks.
- **Areas affected**: `codex-custos/src/main/java/codex/custos/internal/service/SecuredContentItemService.java`.
- **Behaviour introduced**:
  - `delete` calls `permissionsService.canDeleteContentItem(actor, siteKey, contentTypeKey, contentItemKey, snapshot)`, `requireGranted()`, then delegates.
  - `restore` calls `permissionsService.canRestoreContentItem(...)`, `requireGranted()`, then delegates.
  - Both preserve current null guards.
- **Tests required**: in `SecuredContentItemServiceTest`, replace the `Delete — fail closed` and `Restore — fail closed` sections with suites mirroring `Publish` and `Archive`:
  - null guards; denies (delegate untouched, `AccessDeniedException`); grants (delegate invoked exactly once, return preserved for `restore`); snapshot provider called exactly once.
- **Dependencies**: Slices 1, 3.
- **Acceptance criteria**: no `UnsupportedOperationException` remains for `delete` or `restore`; observable behaviour matches Use Cases 2 and 3.
- **Out of scope**: authorization-matrix and side-effect integration coverage.

### Slice 6 — Authorization-matrix and scope-boundary coverage

- **Objective**: prove role and scope correctness end-to-end through the real authorization chain.
- **Areas affected**:
  - `codex-custos/src/test/java/codex/custos/internal/service/SecuredContentItemServiceAuthorizationMatrixTest.java` — extend with scenarios for delete and restore.
  - New: `SecuredSiteServiceAuthorizationMatrixTest` (only if one does not yet exist), following the same pattern, exercising unarchive across `SITE_ADMIN`, `EDITOR` (denied), cross-Site leakage (authority on Site A does not permit unarchive on Site B), and AGENT + SUPER_ADMIN invariant propagation.
- **Behaviour verified**:
  - `SITE_ADMIN` unarchives its own Site.
  - `SITE_ADMIN` deletes / restores content items in its own Site.
  - `EDITOR` restores but cannot delete; cannot unarchive.
  - `COPYWRITER`, `REVIEWER`, `VIEWER` denied on all three.
  - Site-scoped authority on Site A never authorizes an operation on Site B.
  - ContentType-scoped authority authorizes delete and restore only for ContentItems of that
    ContentType within the same Site.
  - AGENT actor holding `SUPER_ADMIN` triggers the fatal invariant; the exception propagates and is not downgraded.
  - Implication rules do not silently authorize any of the three new operations from any existing permission.
- **Tests required**: new nested suites in the matrix tests. Do not duplicate what Slices 4 and 5 already cover at the decorator layer.
- **Dependencies**: Slices 1–5.
- **Acceptance criteria**: matrix coverage exists for every role-permission pair in §4.2 plus
  ContentType-scoped delete/restore coverage, explicit cross-Site denial, and invariant-propagation scenarios.
- **Out of scope**: runtime-level side-effect verification (Slice 7).

### Slice 7 — Denied-operation side-effect runtime verification

- **Objective**: prove at the runtime layer that a denied unarchive/delete/restore produces no domain events, no projection updates, and no Chronicon audit records — the guarantee named in §3 of the intent and in each use case's postconditions.
- **Areas affected**: existing Concilium runtime integration tests (the "denied side-effect" style tests referenced in the intent). Add cases, do not fork.
- **Behaviour verified**: for each of the three operations, given an actor without authority, the secured runtime raises `AccessDeniedException` and:
  - no `CodexEvent` corresponding to the operation is dispatched;
  - no projection change is observable;
  - no audit record attributable to the denied attempt is written;
  - domain state is unchanged.
- **Dependencies**: Slices 4, 5.
- **Acceptance criteria**: assertions exist for each of the three operations, aligned with the existing denied-side-effect test style. Any pre-existing "fail-closed" assertions that expected `UnsupportedOperationException` are removed or replaced.
- **Out of scope**: adding new event, projection, or audit types.

## 7. Test Strategy

### Test layers and responsibilities

| Layer | Responsibility | Where |
|---|---|---|
| Unit — permission service | Each `can*` method builds the correct `AccessDecisionRequest` (permission key, ref, scope), performs null guards, returns whatever the decision service returns. | `DefaultSitePermissionsServiceTest`, `DefaultContentItemPermissionsServiceTest` |
| Unit — secured decorator | Each secured method: null-guards, calls the permissions service once, calls the snapshot provider once, throws `AccessDeniedException` on `Denied`, invokes the delegate exactly once on `Granted`, does not invoke the delegate on `Denied`. | `SecuredSiteServiceTest`, `SecuredContentItemServiceTest` |
| Integration — authorization matrix | Role × permission × scope correctness through the real chain. Cross-Site isolation. AGENT + SUPER_ADMIN invariant. | `SecuredContentItemServiceAuthorizationMatrixTest` (extended); new `SecuredSiteServiceAuthorizationMatrixTest` if absent |
| Integration — Concilium runtime | Denied-operation side-effect safety: no events, no projections, no audit records on denial. | Existing Concilium runtime integration tests |
| Blueprint tests | `BuiltInRoles` permission sets match §4.2 exactly. | Existing `BuiltInRolesTest` (or equivalent), extended |

Use one primary assertion layer per responsibility, with selective end-to-end confirmation where
needed. Cross-Site isolation has its primary assertion at the matrix layer; denied side-effect
safety has its primary assertion at the runtime layer. The traceability matrix intentionally uses
multiple layers when they prove distinct aspects of the same observable behavior.

### Test data conventions

Follow the existing `codex-custos` test style (see AGENTS.md test conventions):

- `@Nested` per scenario family with `@DisplayName`.
- Fixtures as `private static final UPPER_SNAKE_CASE` constants; reuse the existing actor and identifier constants where possible.
- AssertJ (`assertThatThrownBy`, `assertThatNullPointerException`).
- Never rely on unordered repository/list results.
- New abstractions (e.g. new stub helper for the extended permissions services) get focused tests; do not just cover them transitively.

## 8. Use-Case-to-Test Traceability Matrix

| Use Case § | Observable behaviour | Test layer | Slice |
|---|---|---|---|
| UC1 authorized main flow | Authorized actor unarchives; Site becomes `SUSPENDED`; downstream effects match unsecured runtime | Matrix + Runtime integration | 6, 7 |
| UC1 denied flow | Denied actor cannot unarchive; Site remains `ARCHIVED`; delegate not invoked; no events/projections/audit | Decorator unit + Runtime integration | 4, 7 |
| UC1 cross-Site isolation | Authority on Site A does not enable unarchive on Site B | Matrix | 6 |
| UC1 invariant propagation | AGENT + SUPER_ADMIN yields fatal exception, not `Denied` | Matrix | 6 |
| UC1 no UOE fallback | Secured runtime never throws `UnsupportedOperationException` for unarchive | Decorator unit | 4 |
| UC2 authorized delete of archived item | SITE_ADMIN deletes archived item; delegate invoked; state matches unsecured behaviour | Matrix + Runtime integration | 6, 7 |
| UC2 unauthorized delete denied | EDITOR/COPYWRITER/REVIEWER/VIEWER denied; delegate untouched | Decorator unit + Matrix | 5, 6 |
| UC2 denied — no side effects | No event, projection, or audit record on denied delete | Runtime integration | 7 |
| UC2 granted but non-ARCHIVED | Domain precondition failure surfaces from the delegate, not from Custos | Runtime integration | 7 (assertion-only, no new Custos code) |
| UC2 scope boundaries | ContentItem authority does not leak; cross-Site isolation intact | Matrix | 6 |
| UC2 invariant propagation | AGENT + SUPER_ADMIN fatal | Matrix | 6 |
| UC2 no UOE fallback | Secured runtime never throws `UnsupportedOperationException` for delete | Decorator unit | 5 |
| UC3 authorized restore | SITE_ADMIN / EDITOR restores archived item; item becomes `DRAFT` | Matrix + Runtime integration | 6, 7 |
| UC3 unauthorized restore denied | COPYWRITER/REVIEWER/VIEWER denied; delegate untouched | Decorator unit + Matrix | 5, 6 |
| UC3 denied — no side effects | No event, projection, or audit record on denied restore | Runtime integration | 7 |
| UC3 granted but non-ARCHIVED | Domain precondition failure surfaces from the delegate | Runtime integration | 7 |
| UC3 restore ≠ publish | Restore authority does not enable publish; publish authority does not enable restore | Matrix | 6 |
| UC3 scope boundaries | Cross-Site isolation intact | Matrix | 6 |
| UC3 invariant propagation | AGENT + SUPER_ADMIN fatal | Matrix | 6 |
| UC3 no UOE fallback | Secured runtime never throws `UnsupportedOperationException` for restore | Decorator unit | 5 |
| All UCs — permission vocabulary is explicit | Three distinct keys, distinct from archive | Blueprint / catalog tests | 1, 2 |
| All UCs — role mappings deliberate | Only the roles named in §4.2 gain the new permissions | Blueprint tests | 2 |

Every observable verification enumerated in the three use cases maps to at least one planned test
at its primary layer, with selective end-to-end confirmation where needed.

## 9. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Accidental privilege expansion — EDITOR gaining delete because it "feels like" archive | Explicit role table in §4.2; blueprint test asserts exact permission sets; matrix test asserts EDITOR is denied on delete |
| Delete authority too broad — every "admin-shaped" role receiving delete | MD-C1-01 restricts delete to `SITE_ADMIN` and `SUPER_ADMIN`; matrix test proves other roles are denied |
| Cross-Site leakage — Site-A authority permitting operations on Site B | Cross-Site scenarios required in Slice 6 matrix tests; scope hierarchy unchanged and inherited |
| Vocabulary inconsistency — reusing `contentItem.archive` for restore or delete | Explicit distinct keys per §4.1; JavaDoc distinguishes purpose of each |
| Conflating authorization with domain-state validation — Custos rejecting non-`ARCHIVED` delete | Ordering in §4.4 keeps Custos state-blind; a granted call is expected to still fail at the state machine for invalid inputs; runtime integration test in Slice 7 asserts that failure is a domain error, not `AccessDeniedException` |
| Regression in fail-closed posture — a partially wired slice inadvertently allows a call through without authorization | Slice 4/5 tests assert both null-guarding and denied-delegate-untouched invariants; Slice 6 matrix confirms end-to-end; the intent's "no UOE fallback" verification is direct — either the slice authorizes or the test fails |
| Unintended side effects on denial | Runtime-integration Slice 7 explicitly asserts no event/projection/audit on denied attempts, aligned with existing denied-side-effect assertions for other operations |
| Silent implication drift — adding a rule to `DefaultPermissionImplicationRules` "just in case" | No implication changes are permitted by this plan; §4.1 states explicitly that no new implications are introduced |
| Interface-compat surprise — new methods break external implementers | The only implementers are internal defaults and test stubs; no `default` methods are added, so any future external implementer sees a compile-time signal rather than a silent behavioural gap |

## 10. Out of Scope

Explicitly not addressed by this plan (preserved from the intent and roadmap):

- RoleAssignment management, permission grant/revoke management, direct actor grants.
- Collection-read authorization / filtering (`findAll`, `findByAlias`, `findByContentType` remain pass-through).
- Authorization decision records, structured decision traces, Observance authorization metrics, Aegis / security-audit stream.
- Permission-decision caching, step-up approval, workflow.
- Persistence, Porta / REST exposure, Olorin / AI authorization.
- New built-in roles (e.g. a dedicated destructive-authority role).
- Any change to Chronicon, Observance, or projection code.
- Any change to `ConciliumRuntime` composition, decorator ordering, or module boundaries.
- Alias-to-`SiteKey` authorization.
- Modification of existing implication rules.

## 11. Definition of Done

The engineering plan is done when, by the end of Slice 7, the following are all true:

- Three new permission keys exist in `Permissions` and are referenced only where §4 permits.
- `BuiltInRoles` permission sets match §4.2 exactly; no other role has changed.
- `SitePermissionsService` and `ContentItemPermissionsService` expose the three new `can*` methods; defaults delegate through `AccessDecisionService`.
- `SecuredSiteService.unarchive`, `SecuredContentItemService.delete`, and `SecuredContentItemService.restore` no longer throw `UnsupportedOperationException`.
- Each of the three secured methods performs null guards, invokes the permissions service, requires a granted decision, calls the snapshot provider exactly once, and delegates only on grant.
- Every observable verification listed across the three use cases has one primary assertion layer,
  with selective end-to-end confirmation where needed.
- No test asserts `UnsupportedOperationException` for these three operations.
- No change is made to Chronicon, Observance, projections, `ConciliumRuntime` composition, module boundaries, existing implication rules, or any deferred concern in §10.
- `mvn clean verify` passes.

## 12. Recommended Implementation / Review Sequence

Sequenced so each slice is a small, reviewable PR-shaped unit, and so review agents can validate slice-by-slice without setup drift.

1. **Slice 1** — permission constants only. Trivially reviewable.
2. **Slice 2** — role blueprint updates. Reviewer checks the exact permission-set diffs against §4.2.
3. **Slice 3** — permission service API and defaults. Reviewer checks that each new method mirrors the shape of `canArchive*` and that null guards + delegation are correct.
4. **Slice 4** — `SecuredSiteService.unarchive` gating with unit tests. Removes the first UOE.
5. **Slice 5** — `SecuredContentItemService.delete` and `.restore` gating with unit tests. Removes the remaining UOEs.
6. **Slice 6** — matrix and scope-boundary coverage. Reviewer verifies role table, cross-Site denial, and invariant propagation.
7. **Slice 7** — runtime-level denied-side-effect coverage. Final proof that Intent 1's success conditions hold.

An independent-review checkpoint is appropriate after Slice 3 (vocabulary + role + service API frozen) and again after Slice 7 (final).

### Validation to run per slice

```bash
mvn -pl codex-custos test        # slices 1–6
mvn -pl codex-concilium test     # slice 7 additions
mvn clean verify                 # before closing the objective
```

No production configuration changes, no persistence changes, no transport changes are required.
