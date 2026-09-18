# CODEX-OSK-001 — Current-State Reality Check — REVIEW

- **Task:** CODEX-OSK-001 (Planned) — repository-grounded reality check + roadmap reconciliation
- **Role:** Architecture Reviewer / Auditor
- **Verdict:** CHANGES REQUIRED (documentation only — no production finding; see §7)
- **Date:** 2026-09-18
- **Validation:** `mvn test -pl codex-custos,codex-concilium -am` → BUILD SUCCESS, 1749 tests, 0 failures (see §6). `git diff --check` clean. No production code modified, no commits.
- **Method:** three parallel evidence streams (Custos code, runtime + observability, docs + OSK), each grounded in files read directly; high-impact claims re-verified by reviewer (repo-wide Java grep, full module-info reads, Maven run).
- **Record-location note:** this file lives at the task-mandated path `docs/engineering/agents/reviews/transversal/`. That conflicts with the OSK convention (`docs/engineering/agents/reviews/`, currently empty except `README.md`). Task requirement wins; the conflict itself is recorded as drift item D11.

## 1. Executive summary

Codex today is a **working in-memory CMS kernel with a complete authorization enforcement chain and a composed runtime — but with no persistence, no API exposure, no workflow, and documentation that overstates completion in at least one load-bearing place.**

What actually exists and passes tests:

- Domain kernel (`codex-codex`): sites, content types, items, revisions, lifecycle state machines, deferred event dispatch, cache + invalidation, Observance metrics. 693 tests green.
- Authorization (`codex-custos`): permission model, resolver with scope walk + implications + AGENT+SUPER_ADMIN invariant, decision service, 3 domain permission services, 3 secured decorators, full-chain matrix test. 356 tests green.
- Projections (`codex-index`, `codex-chronicon`): 4 indexing subscribers, 15 audit subscribers, append-only audit repository. 130 + 281 tests green.
- Composition (`codex-concilium`): `ConciliumRuntime.secured(...)` / `inMemory()`, 19-subscriber event pipeline, denied-operation side-effect tests. 57 tests green.
- Module hygiene is real: zero qualified exports remain, core depends only on fundamentum, all runtimes in public `*.api.runtime`.

What is design-only or missing: authorization decision records / trace / dispatcher / consumers, Observance authorization metrics, permission-decision caching (ADR-012), direct-grant support, list-read filtering, delete/restore/unarchive permission vocabulary (fail-closed `UnsupportedOperationException` edges), role-assignment management, Phase 4 workflow. Seven of eight remaining modules are empty skeletons.

The single most important finding: **`docs/security/CUSTOS-IMPLEMENTATION-CHECKLIST.md` marks `RoleAssignmentPermissionsService` done, but no such class exists anywhere** — Phase 2 "complete" is overstated and `role.assign/revoke` + `permission.grant/revoke/read` keys are enforced nowhere. Everything else checked in the list verified clean.

OSK is installed and referenced but not yet workflow-canonical: canonical locations (`docs/roadmap/ROADMAP.md`, `docs/adr/`, `docs/knowledge/`, `docs/engineering/agents/`, `docs/PROJECT.md`) are empty or template-only while the live content still sits in legacy `docs/engineering/agents/` and `docs/future-forward/`.

## 2. Capability matrix

| Capability | Status | One-line evidence |
|---|---|---|
| Domain lifecycle (site/type/item/revision state machines) | IMPLEMENTED + VERIFIED | 693 codex tests green; transitions validated in dedicated methods |
| Deferred event dispatch (buffer → flush) | IMPLEMENTED + VERIFIED | `DeferredEventDispatcher` + observance-pinned tests |
| Cache + invalidation subscribers | IMPLEMENTED + VERIFIED | `ObservingCacheRegion` wired in `CodexRuntime`; 7 invalidation subscriber suites |
| Custos kernel (keys, refs, scopes, decisions) | IMPLEMENTED + VERIFIED | Sealed `AccessDecision`/`PermissionResolution`; 356 custos tests green |
| Permission resolution (walk, implications, invariant) | IMPLEMENTED + VERIFIED | `DefaultPermissionResolver` read in full; order invariant → bypass → walk confirmed |
| AccessDecisionService | IMPLEMENTED + VERIFIED | `DefaultAccessDecisionService.evaluate` re-wraps with request `ResourceRef`; invariant propagates |
| Domain permission services (site/type/item) | IMPLEMENTED + VERIFIED | 15 `canX` methods forward exact key/ref/scope (recording-service tests) |
| RoleAssignment management service | STALE / CONTRADICTED | Checklist claims done (§2); **no such class exists** (grep-verified) |
| Secured decorators (site/type/item) | IMPLEMENTED + PARTIALLY VERIFIED | 17 ops gated; fail-closed + pass-through edges documented in code; matrix test covers item only |
| Secured runtime composition | IMPLEMENTED + VERIFIED | `secured()` builds decorated graph; `coreRuntime()` bypass explicitly documented |
| Denied-op side-effect safety | IMPLEMENTED + VERIFIED | `ConciliumRuntimeDeniedSideEffectsTest` (353 lines): no events/audit/index/state change |
| Index projections | IMPLEMENTED + VERIFIED | 4 subscribers (published/unpublished/archived/deleted); `ObservingIndexWriter` |
| Chronicon domain audit | IMPLEMENTED + VERIFIED | 15 subscribers → `MemoryChroniconRepository`; direct-write by design, no sink layer |
| Observance metrics (core/index/events/cache) | IMPLEMENTED + VERIFIED | `Timed*Service`, dispatcher, cache, index writers all record; naming pinned to ADR-011 |
| Observance on secured path | PARTIAL | `secured()` hardcodes `Observance.noop()` — metrics dead where authorization is live |
| Direct actor permission grants | DEFERRED | `PermissionGrant` record exists, data-only; resolver reads assignments + registry only |
| Explanation trace (structured) | DESIGN/DOCS ONLY | Only free-text `reason()` strings; proposal in CODEX-018B report, no code |
| AuthorizationDecisionRecord + dispatcher/consumers | DESIGN/DOCS ONLY | Zero main-source classes (grep-verified); ADR-012 + CODEX-018A/C reports only |
| Observance authorization metrics | DESIGN/DOCS ONLY | Zero `Observance` references in `codex-custos` main; design doc CODEX-018C only |
| Permission-decision caching (ADR-012) | DESIGN/DOCS ONLY | ADR status "Proposed / Future-forward"; `DefaultAccessDecisionService` cache-free |
| Security audit stream | DESIGN/DOCS ONLY | `CUSTOS-MODEL.md` "future…may exist"; no repository/stream class |
| "Aegis" | DESIGN/DOCS ONLY | Candidate name only (KNOW-005); zero source hits |
| List-read authorization (`findAll`/`findBy*`) | DEFERRED | Pass-through with "future filtering" comments in all 3 decorators |
| Alias-to-SiteKey authorization | DEFERRED | `findByAlias` unchecked; two-phase resolve-then-check documented, not built |
| Delete/restore/unarchive permission vocabulary | DEFERRED | No keys, no `canX`; decorators throw `UnsupportedOperationException` (fail-closed) |
| `PermissionEvaluator` / `SecurityEvaluationContext` | PARTIAL | Interface exists; zero implementations; context never consumed outside its test |
| Phase 4 permission-change workflow | DEFERRED | Correctly unchecked; `PermissionChangePlan`/`StepUpApproval` absent from code |
| Porta / Archivum / Iter / Scriptorium / Illuminarium / Imaginarium / Olorin | DEFERRED | Skeletons; no implementation (expected) |

## 3. Module/runtime diagram (actual, verified from module-info + runtime sources)

```text
codex-fundamentum  (no Codex deps; exports api, cache, concurrent, event,
                     exception, lifecycle, model, observance, runtime, tx)
        ^
        | requires (transitive where API exposes types)
        |
codex-codex  (kernel: services, repos, events, cache; exports api.* incl. projection, runtime)
   ^    ^
   |    | requires (index: transitive; chronicon/custos: plain + transitive for custos)
   |    |
codex-index   codex-chronicon   codex-custos
(subscribers,  (15 subscribers,  (resolver, decisions,
 mapper,        audit repo)       perms services,
 writer)                         secured decorators)
   ^            ^                  ^
   |            |                  |
   +------------+------------------+
   |  codex-concilium (plain requires on all five + slf4j; exports api.runtime)
   |  ConciliumRuntime.secured(snapshotProvider)  -> decorated service graph [SECURED]
   |  ConciliumRuntime.inMemory([Observance])     -> raw core services [UNSECURED]
   |  event pipeline: DeferredEventDispatcher -> Composite -> LocalCodexEventDispatcher
   |                 (19 module subscribers: 4 index + 15 chronicon)
   v
edge modules (skeletons, no wiring): porta, archivum, iter, scriptorium,
                                     illuminarium, imaginarium, olorin
```

Verified edge facts: **zero `exports … to …` qualified exports** in any of the five implemented module-infos (historical ones removed per calibration Tasks 38b/39/41). Three `requires transitive` edges remain, all justified: `codex→fundamentum`, `index→codex.codex`, `custos→fundamentum + codex.codex`. Circular-dependency break in `ConciliumRuntime` via `AtomicReference` forwarding lambda is documented transitional design. `RuntimeSecurityMode` is diagnostic metadata; enforcement lives in the service graph. Shutdown is reverse-order (`chronicon → index → core`), idempotent.

## 4. Custos status

- **Model — IMPLEMENTED + VERIFIED.** Sealed `ResourceRef` (4 refs) / `ResourceScope` (4 scopes); `PermissionKey` (trimmed, blank-rejected); `Permissions` catalog (20 keys — notably *no* `site.update/delete/unarchive`, `contentItem.delete/restore/purge`); `Role` (blueprint only, `Set.copyOf`); `PermissionGrant` (data-only, **unused by any evaluation path**); `RoleAssignment`; `BuiltInRoles` (6 roles; bypass explicitly *not* encoded). `AccessDecision`/`PermissionResolution` sealed Granted/Denied with string `reason()` only.
- **Resolver — IMPLEMENTED + VERIFIED.** `DefaultPermissionResolver.resolve()`: (1) `enforceAgentSuperAdminInvariant` first, (2) scoped SUPER_ADMIN bypass for non-AGENTs, (3) bottom-up `resolveByScope` with exact-scope match + `parentOf` walk, first grant wins. `covers()` is used only for the bypass; the walk uses equality + parent iteration (equivalent, verified). No site crossing possible: parents are built from the child's own `siteKey`.
- **Decision service — IMPLEMENTED + VERIFIED.** `DefaultAccessDecisionService.evaluate` builds a `PermissionResolutionRequest` from the request's target scope, delegates, and re-wraps with the original `ResourceRef`. The AGENT+SUPER_ADMIN exception propagates uncaught — never converted to `Denied`.
- **Permissions services — IMPLEMENTED + VERIFIED (3 of claimed 4).** `Site` (5 methods), `ContentType` (4), `ContentItem` (6) are thin correct mappers (recording-service tests pin exact key/ref/scope per method). **`RoleAssignmentPermissionsService` does not exist** — checklist line 108 is a false positive; `role.assign/revoke` and `permission.*` keys are enforced nowhere.
- **Secured decorators — IMPLEMENTED + PARTIALLY VERIFIED.** Gated: site 5 ops, content-type 6 ops, item 6 ops (denied → `requireGranted()` → `AccessDeniedException`, delegate unreached). Fail-closed via `UnsupportedOperationException`: site `unarchive`, item `delete`/`restore`. Pass-through: all `findAll`/`findBy*`/`findByAlias`. Schema mutations (`activate`/`addField`/`removeField`) map to `canUpdateContentType` (documented). No `purge` method exists anywhere, so no purge gap.
- **Runtime composition — IMPLEMENTED + VERIFIED.** §3 + Area notes in §2.
- **Integration coverage — VERIFIED.** `SecuredContentItemServiceAuthorizationMatrixTest` (424 lines, 6 scenarios: viewer/copywriter/reviewer/editor/site-boundary/AGENT+SUPER_ADMIN) runs the real chain resolver→decision→perms→decorator. No equivalent matrix exists for site or content-type decorators (per-op stub tests only).
- **Known gaps (all confirmed in code, do not "fix" opportunistically):** direct grants; structured trace; list-read filtering; alias two-phase check; delete/restore/unarchive vocabulary; role-assignment management; `PermissionEvaluator` implementation; site/type authorization matrices.

## 5. Audit/observability status

| Concern | Implementation | Design-only |
|---|---|---|
| Chronicon domain audit | 15 subscribers → `MemoryChroniconRepository` (append-only contract); `ChroniconRuntime` composition root; 281 tests | Service/sink layer (never built; direct `repository.save()` in subscribers is the current design) |
| Observance metrics | `Observance`/`NoOp`/`InMemory` in fundamentum; consumed by `Timed*Service`, `DeferredEventDispatcher`, `LocalCodexEventDispatcher`, `ObservingCacheRegion`, `ObservingIndexWriter`; 6 `*MetricNames` classes, ADR-011 naming | Chronicon metrics (runtime takes no `Observance` — by design); any metrics on the `secured()` path (hardcoded noop) |
| Authorization decisions | Sealed decisions with human-readable `reason()` | `AuthorizationDecisionRecord`, trace model, dispatcher/router, consumers/sinks, authorization counters/timers, caching decorator, security audit stream, "Aegis" |
| Logging | SLF4J at service boundaries/state transitions (observed live in test output) | Redaction policy (ADR-006, not assessed for compliance) |

Denied operations currently write **no** Chronicon record and **no** security record — CODEX-017 explicitly scoped denied-decision audit logging out as a separate future task. That is a conscious gap, not an oversight, but it means denied attempts are invisible everywhere.

## 6. Test reality

Observed run (`mvn test -pl codex-custos,codex-concilium -am --no-transfer-progress`): **BUILD SUCCESS, 1749 tests, 0 failures** — fundamentum 232, codex 693, chronicon 281, index 130, custos 356, concilium 57. (Skeletons have no tests; full 14-module reactor not run — nothing else contains tests to run.)

- **Important integration scenarios covered:** full-chain authorization matrix (item); denied-op side effects across 4 operation families (no events, no audit, no index update, state unchanged — `ConciliumRuntimeDeniedSideEffectsTest`, 353 lines); secured grant path reaching index + Chronicon (`ConciliumRuntimeSecuredTest`, `ConciliumRuntimeEndToEndTest`); per-operation snapshot-provider invocation; observance wiring on the `inMemory(Observance)` path (`ConciliumRuntimeTest`, `CodexRuntimeObservanceTest`).
- **Important scenarios still missing:** site and content-type authorization matrices (item-only coverage); direct index-writer assertion on denied publish (currently inferred from absent events); denied-decision audit logging (scoped out, unwritten); secured-path observability (noop, unasserted); list-read filtering (no strategy, nothing to test); role-assignment management (no code).

## 7. Documentation drift

Numbered findings (claim → fact). Item D1 is the only one affecting a completion claim; the rest are staleness/placement.

- **D1 (MAJOR — false completion):** checklist line 108 `[x] RoleAssignmentPermissionsService` → no such class in `api/service`, `internal/service`, or anywhere in Java sources (reviewer grep-verified). Phase 2 "complete" is therefore overstated.
- **D2:** checklist Open Questions (line 192) lists `AccessDecisionService` wiring as a pending choice → line 99 marks it done (it is done).
- **D3:** `MODULE-RESPONSIBILITIES.md` "indexing lives in codex-codex during MVP" (§§ lines 92–98, 135–137, 447–448, 489–493) → migration to `codex-index` is done; zero `Index*.java` remain in `codex-codex`.
- **D4:** same file's skeleton note ("Task 27", lines 8–9) and "module skeleton creation" near-future item (line 462) → 14-module pom exists.
- **D5:** same file's present-tense `codex-nuntius` / `codex-speculum` sections → absent from `pom.xml`.
- **D6:** same file's concilium status (inMemory-only) → omits implemented `secured(...)`, `RuntimeSecurityMode`, `SecuredServiceComposer`.
- **D7:** same file's Custos "users/groups/teams, LDAP/OAuth/SAML" → contradicted by kernel-only scope (checklist line 11, AGENTS.md).
- **D8:** `docs/roadmap/ROADMAP.md` empty (13 lines, placeholder comments) → real direction in legacy `docs/roadmap/history/CODEX-ROADMAP-PHASES.md`.
- **D9:** `docs/roadmap/future/` referenced by `docs/OSK.md` and `AGENTS.md` → does not exist on disk.
- **D10:** `docs/adr/` holds only `README.md` → 9 ADRs in `docs/future-forward/`, 6 in `docs/ADR-corpus/` (008/009/012 duplicated; content equivalence unverified). `ADR-007.md` filename has a leading space.
- **D11:** `docs/engineering/agents/*` hold only READMEs → populated `docs/engineering/agents/*` (19+ reports, 2 runtime reviews, checkpoints through Aug 8); no migration note (only KNOW-002:66 defers migration to a future request). This review's own path conflict is an instance of D11.
- **D12:** `ENGINEERING_LOG.md` ends Aug 6 ("next: CODEX-017") → CODEX-017 report + review exist (Aug 8) with no log entry, violating `reports/README.md`.
- **D13:** `CLAUDE.md:49` Custos "early Phase 1" → Phases 0–3 implemented per code/checklist/model.
- **D14:** `docs/PROJECT.md` still an empty template → OSK's "Start Here: read PROJECT.md" entrypoint yields nothing; `docs/knowledge/` holds only `README.md`.

Working-tree note: `M AGENTS.md` (uncommitted, from prior session work) was the only modification present; `git diff --check` clean.

## 8. Recommended next objective

**Define and implement the delete/restore/unarchive permission vocabulary and gate the three fail-closed decorator methods** (one bounded task):

- Add `site.unarchive`, `contentItem.delete`, `contentItem.restore` keys to `Permissions` (+ `BuiltInRoles` sets, following existing conventions).
- Add `canUnarchiveSite` / `canDeleteContentItem` / `canRestoreContentItem` to the domain permission services with recording-service tests.
- Replace the three `UnsupportedOperationException` edges with gated delegations; extend the authorization matrix (or add site/item matrices) with grant/deny/no-side-effect cases.

Why this one: it is the smallest slice that removes real functional blockage (a secured runtime today *cannot* delete, restore, or unarchive at all), completes the Phase 3 leftovers both checklists already frame as pending, and needs no prior design decision — unlike list-read filtering (strategy undecided) or decision trace (018-series design still settling). Runner-up: implement the structured `AccessDecision` trace per the CODEX-018B proposal.

Explicitly not recommended yet: Porta exposure, persistence, workflow engine, decision-record dispatcher/consumers — each needs its deferred design resolved first.

## 9. Risks / blockers

- **R1 — False completion signal (D1):** any planning that trusts "Phase 2 complete" will miss that privilege-management operations (`role.assign/revoke`, `permission.grant/revoke`) are entirely unenforced. Correct the checklist checkbox before scoping role-management work.
- **R2 — Enumeration exposure:** all list reads are pass-through in secured mode; any consumer of `secured()` leaks cross-scope visibility until a read-filtering strategy is chosen. Keep untrusted callers off list endpoints or treat this as the follow-up objective.
- **R3 — Blind secured path:** `secured()` hardcodes noop Observance, so production-like deployments emit no metrics; do not reason about secured-path performance from `inMemory(Observance)` numbers.
- **R4 — `coreRuntime()` bypass:** any holder of the runtime can sidestep authorization; discipline is documentary only. Fine for now, must be revisited before Porta exposes the runtime.
- **R5 — No canonical roadmap:** with `ROADMAP.md` empty and direction split across legacy docs, the next objective's authority is this review plus maintainer judgment — adopt the recommended slice explicitly rather than inferring it.
- **R6 — Denied attempts are invisible:** no decision log, no security stream. Acceptable while single-node/in-memory; becomes a blocker before any multi-actor deployment.
- **Limits of this review:** full 14-module reactor not run (skeletons contain no tests); ADR-corpus vs future-forward equivalence unverified; `CODEX-ROADMAP-PHASES.md` / `CODEX-BLUEPRINT.md` / `CODEX-OSK-ALIGNMENT.md` listed but not read; test counts are point-in-time (2026-09-18).
