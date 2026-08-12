# Review Report — CODEX-016: Secured Runtime Composition

**Report type:** review
**Date:** 2026-08-06
**Reviewer:** Deep
**Task:** `docs/agents/tasks/runtime/Task-CODEX-016—SecuredRuntimeComposition.md`
**Implementation report:** `docs/agents/reports/runtime/CODEX-016-SecuredRuntimeComposition-REPORT.md`
**Checkpoint:** `docs/agents/checkpoints/CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md` (Phase 1.4, completed)

---

## PASS / WARN / BLOCKER Summary

| Verdict | Count | Items |
|---------|-------|-------|
| **PASS** | 13 | Module boundaries, dependency direction, factory purity, service accessors, snapshot semantics, back-compat, metadata design, authorization enforcement, state/event integrity, invariant propagation, test quality |
| **WARN** | 2 | `coreRuntime()` bypass risk not documented in Javadoc; `compose()` receives `RuntimeSecurityMode.UNSECURED` even when caller manually wraps services |
| **BLOCKER** | 0 | — |

**Verdict:** CODEX-016 accepted with follow-up on `coreRuntime()` Javadoc hardening.

---

## Validation

### Commands executed and results

```bash
$ git diff --check -- "*.java" "*.xml"
# EXIT: 0 — no whitespace errors

$ mvn test -pl codex-custos,codex-concilium -am --no-transfer-progress
# codex-custos:    BUILD SUCCESS
# codex-concilium: 40 tests — BUILD SUCCESS
#   ConciliumRuntimeTest:         26 tests
#   ConciliumRuntimeEndToEndTest:  1 test
#   ConciliumRuntimeSecuredTest:  13 tests
# Total time: ~12s
```

### Evidence: files inspected

| File | Purpose |
|------|---------|
| `codex-custos/src/main/java/codex/custos/api/service/SecuredServiceComposer.java` | Public factory bridge |
| `codex-custos/src/main/java/module-info.java` | Exports verification |
| `codex-custos/src/main/java/codex/custos/internal/service/SecuredSiteService.java` | Authorization decorator |
| `codex-custos/src/main/java/codex/custos/internal/service/SecuredContentTypeService.java` | Authorization decorator |
| `codex-custos/src/main/java/codex/custos/internal/service/SecuredContentItemService.java` | Authorization decorator |
| `codex-concilium/pom.xml` | Dependency graph |
| `codex-concilium/src/main/java/module-info.java` | Module requires |
| `codex-concilium/src/main/java/codex/concilium/api/runtime/ConciliumRuntime.java` | Composition logic |
| `codex-concilium/src/main/java/codex/concilium/api/runtime/RuntimeSecurityMode.java` | Diagnostic enum |
| `codex-concilium/src/test/java/codex/concilium/internal/ConciliumRuntimeSecuredTest.java` | Behavioral tests (13) |
| `codex-codex/src/main/java/module-info.java` | Core purity verification |
| `docs/agents/tasks/runtime/Task-CODEX-016—SecuredRuntimeComposition.md` | Task definition |
| `docs/agents/reports/runtime/CODEX-016-SecuredRuntimeComposition-REPORT.md` | Implementation report |

---

## 1. Module Boundary Assessment

### Q1: Does `codex-codex` remain independent of `codex-custos`?

**PASS.** `codex-codex/module-info.java` has no `requires codex.custos`. `codex-codex/pom.xml` depends only on `codex-fundamentum`. No Custos types appear in any codex-codex source file.

### Q2: Does `codex-concilium` depend on `codex-custos` without creating cycles?

**PASS.** Dependency chain is linear:
```
codex-concilium → codex-custos → codex-codex → codex-fundamentum
```
No back-edges. `pom.xml` adds one `<dependency>` on `codex-custos`. `module-info.java` adds `requires codex.custos`.

### Q3: Does `codex-custos` avoid exporting `codex.custos.internal.service`?

**PASS.** `module-info.java` exports only three public packages: `api.exception`, `api.model`, `api.service`. `Secured*Service` classes remain in `codex.custos.internal.service` — unreachable from outside.

---

## 2. Runtime Composition Assessment

### Q4: Is `SecuredServiceComposer` a thin public bridge with zero authorization logic?

**PASS.** The class contains 136 lines, none of which perform authorization. Each method is a pure delegation:
- Factory methods instantiate internal classes and return interface types
- Wrap methods call `new Secured*Service(delegate, perms, provider)` and return the interface

The class imports `Default*PermissionsService` and `Secured*Service` from the internal package — this is correct because it lives in codex-custos itself and acts as the controlled boundary. Callers receive only interface types.

### Q5: Does `ConciliumRuntime.secured(...)` expose secured services through the new accessors?

**PASS.** The `secured()` factory (line 186-224) creates raw CodexRuntime services, builds the Custos stack via `SecuredServiceComposer`, wraps all three services, and stores them in the instance fields `siteService`, `contentTypeService`, `contentItemService`. The accessors `siteService()`, `contentTypeService()`, `contentItemService()` return these fields.

### Q6: Does `ConciliumRuntime.inMemory()` still behave as unsecured/back-compat?

**PASS.** Both `inMemory()` and `inMemory(Observance)` pass `core.siteService()`, `core.contentTypeService()`, `core.contentItemService()` directly as the service fields. The `RuntimeSecurityMode.UNSECURED` is stored. Test `inMemoryRuntimeStillWorksAsUnsecuredPath` confirms an unauthorized actor can create a site through the unsecured path.

### Q7: Is `RuntimeSecurityMode` diagnostic only, not used as a security gate?

**PASS.** The enum's Javadoc states "This is diagnostic metadata only — security is enforced by the service graph." The `securityMode()` accessor's Javadoc repeats this. No production code gates behavior on `securityMode()`. The enum is returned-only (no `if (securityMode == SECURED) { ... }` patterns).

### Q8: Is `snapshotProvider` preserved as per-operation supplier and not evaluated eagerly?

**PASS.** Proven by test `snapshotProviderIsCalledPerOperation`:
- `callCount` is 0 immediately after `ConciliumRuntime.secured(countingProvider)` returns
- `callCount` increments after the first operation
- `callCount` increments further after a second operation

This confirms the supplier is stored as a reference and called lazily per secured method invocation, not at construction time.

---

## 3. Security Bypass Assessment

### Q14: Is `coreRuntime().siteService()` bypass risk documented and acceptable?

**WARN.** The implementation report documents this risk clearly: "`runtime.coreRuntime().siteService()` still returns raw service." However, the `coreRuntime()` Javadoc in `ConciliumRuntime` does not mention the bypass risk. On a secured runtime, this method silently returns the raw unsecured service — a caller using `runtime.coreRuntime().siteService()` instead of `runtime.siteService()` gets no authorization.

### Q15: Should `coreRuntime()` remain publicly exposed on secured runtimes?

**Assessment:** Acceptable with hardening. Rationale:

1. `coreRuntime()` has legitimate non-bypass uses: accessing `contentItemProjectionReader()`, `recordedEvents()`, lifecycle shutdown, subscriber access
2. Renaming or hiding it would break the test `deniedOperationDoesNotEmitDomainEvents` which reads `runtime.coreRuntime().recordedEvents()`
3. The adapter layer (`codex-porta`, future) should use `runtime.siteService()`, not `runtime.coreRuntime().siteService()`

**Recommended hardening (follow-up, not blocker):**
- Add `@implNote This returns the raw core runtime. Prefer {@link #siteService()}, {@link #contentTypeService()}, {@link #contentItemService()} for domain operations.` to `coreRuntime()` Javadoc
- Document in `MODULE-RESPONSIBILITIES.md` / AGENTS.md that adapters must use secured accessors

---

## 4. Test Quality Assessment

**PASS.** `ConciliumRuntimeSecuredTest` has 13 tests across 5 `@Nested` groups, following codex-custos test conventions:

| Group | Tests | Coverage |
|-------|-------|----------|
| `Factory` | 3 | Non-null creation, null rejection, SECURED metadata |
| `AuthorizationDeny` | 3 | `AccessDeniedException`, no state mutation, no event emission |
| `AuthorizationGrant` | 2 | Full authoring flow, Index+Chronicon integrity |
| `SnapshotProvider` | 1 | Per-operation lazy evaluation |
| `HardInvariants` | 1 | `CustosAgentSuperAdminInvariantViolationException` propagation |
| `Lifecycle` | 3 | Back-compat unsecured path, UNSECURED metadata, idempotent close |

All tests are behavioral — no assertions on concrete `Secured*Service` class types. This avoids coupling to internal implementation classes. ✓

Tests use three snapshots (`SUPER_ADMIN_SNAPSHOT`, `EMPTY_SNAPSHOT`, `AGENT_SUPER_ADMIN_SNAPSHOT`) built from `BuiltInRoles.SUPER_ADMIN` + `RoleAssignment`, exercising the full real Custos chain — not stubs.

---

## 5. Review Questions — All Answers

| # | Question | Verdict | Evidence |
|---|----------|---------|----------|
| 1 | codex-codex independent of codex-custos? | PASS | `module-info.java` — no requires custos; `pom.xml` — no custos dep |
| 2 | concilium → custos without cycles? | PASS | Linear chain: concilium → custos → codex → fundamentum |
| 3 | custos avoids exporting internal.service? | PASS | Exports only api.exception, api.model, api.service |
| 4 | SecuredServiceComposer has zero auth logic? | PASS | 136 lines, all delegation/wrapping, no permission checks |
| 5 | runtime.siteService()/contentTypeService()/contentItemService() return secured? | PASS | `secured()` stores wrapped services; accessors return them |
| 6 | inMemory() still unsecured/back-compat? | PASS | Passes raw core services; test proves unauthorized actor succeeds |
| 7 | RuntimeSecurityMode diagnostic only? | PASS | Enum+accessor Javadoc confirms; no gating logic uses it |
| 8 | snapshotProvider lazy per-operation? | PASS | Test proves callCount=0 post-construction, increments per-op |
| 9 | Tests prove unauthorized actors denied? | PASS | `unauthorizedActorCannotCreateSite` — AccessDeniedException |
| 10 | Tests prove deny → no state mutation? | PASS | `deniedSiteCreateDoesNotMutateSiteState` — findAll empty |
| 11 | Tests prove deny → no events? | PASS | `deniedOperationDoesNotEmitDomainEvents` — recordedEvents empty |
| 12 | Authorized ops preserve Index/Chronicon? | PASS | `authorizedPublishFlowReachesIndexAndChronicon` — audit+events non-empty |
| 13 | AGENT+SUPER_ADMIN invariant propagates? | PASS | `agentWithSuperAdminThrowsInvariantViolation` — CustosAgentSuperAdminInvariantViolationException |
| 14 | Bypass risk documented/acceptable? | WARN | Report documents it; Javadoc on coreRuntime() does not |
| 15 | coreRuntime() remain public? | ACCEPT follow-up | Has legitimate non-bypass uses; needs Javadoc hardening |

---

## 6. Accepted Gaps (Carried Forward from Phase 1.4)

- Collection reads (`findAll`, `findByContentType`, `findBySiteKey`) pass through without authorization
- `findByAlias` lacks alias-to-SiteKey authorization
- `delete`, `restore`, `unarchive` are fail-closed pending permission vocabulary
- Direct actor `PermissionGrant` support and explanation trace are pending
- Denied-operation cache/index consistency tests are pending
- Audit/authorization integration is pending

None of these are introduced by CODEX-016 — all were accepted at Phase 1.4.

---

## 7. Recommendation

**CODEX-016 accepted with follow-up.**

The implementation complies with all acceptance criteria:
- `codex-codex` remains pure ✓
- `codex-custos` exposes a public composition entry point ✓
- `Secured*Service` implementations remain internal ✓
- Concilium can create an explicit secured runtime path ✓
- `inMemory()` remains unsecured and backward compatible ✓
- Runtime metadata distinguishes SECURED from UNSECURED ✓
- Secured runtime services enforce Custos decorators ✓
- Unsecured runtime services preserve current behavior ✓
- Tests cover secured and unsecured composition paths ✓

### Required follow-up (not blocking)

1. Add bypass-risk warning to `coreRuntime()` Javadoc — explicitly state that `siteService()` / `contentTypeService()` / `contentItemService()` are the preferred accessors
2. Consider a near-future task to rename `inMemory()` → `inMemoryUnsecured()` once adoption of `secured()` is established (explicitly out of scope for CODEX-016)

### No fixes required

No production code changes, no test changes, no module-info changes, no pom.xml changes.
