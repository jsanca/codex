# Checkpoint: Codex Custos Secured Runtime Composition

Date: 2026-08-06

## Summary

Phase 3.1 Secured Runtime Composition is complete.

`ConciliumRuntime` now exposes an explicit secured runtime path that enforces Custos domain
authorization on all service operations. The unsecured path is preserved for backward compatibility
and non-authorization contexts. `codex-codex` remains pure — it does not depend on `codex-custos`.

This checkpoint closes the gap identified in `CHECKPOINT-CODEX-CUSTOS-SECURED-DECORATORS.md`
under "secured runtime composition is not done."

## Completed Components

### codex-custos

- `SecuredServiceComposer` — public factory in `codex.custos.api.service`
  - Creates the default Custos authorization stack (resolver + decision service + 3 permission services)
  - Wraps raw Codex services with internal `Secured*Service` decorators
  - Zero authorization logic of its own
  - Sole public bridge between `codex-concilium` and Custos internal implementations

### codex-concilium

- `ConciliumRuntime.secured(Supplier<PermissionResolutionSnapshot>)` — secured factory
- `ConciliumRuntime.RuntimeSecurityMode` — `SECURED` / `UNSECURED` diagnostic enum
- `ConciliumRuntime.securityMode()` — returns the runtime's security mode
- `ConciliumRuntime.siteService()` / `contentTypeService()` / `contentItemService()` — top-level service accessors
- `codex-custos` dependency added to `pom.xml` and `module-info.java`

### Tests

- `ConciliumRuntimeSecuredTest` — 13 behavioral tests

Test coverage:
- factory returns non-null runtime
- null snapshot provider rejected
- secured runtime reports `SECURED` metadata
- unauthorized actor denied (`AccessDeniedException`)
- denied operation does not mutate state
- denied operation does not emit domain events
- SUPER_ADMIN can create site, content type, and content item
- authorized publish flow reaches Index and Chronicon
- snapshot provider called per operation, not at construction
- AGENT + SUPER_ADMIN throws `CustosAgentSuperAdminInvariantViolationException`
- `inMemory()` still works as unsecured back-compat path
- `close()` is idempotent on secured runtime
- `inMemory()` reports `UNSECURED` metadata

## Dependency Direction

```text
codex-concilium -> codex-custos -> codex-codex -> codex-fundamentum
```

`codex-codex` remains the domain kernel. It does not import Custos types. ✓

## Runtime Metadata Pattern

`RuntimeSecurityMode` is diagnostic metadata on `ConciliumRuntime`. Security is enforced by the
service graph, not by this enum. Callers of the secured runtime must use `runtime.siteService()`
(not `runtime.coreRuntime().siteService()`) to get the authorization-enforcing decorator.

## Authorization Posture (Unchanged from Phase 1.4)

Secured decorators follow the same posture established in Phase 1.4:

1. Validate inputs with null guards.
2. Consult the domain permission service.
3. Call `requireGranted()` on the `AccessDecision`.
4. Delegate to the underlying service only after a granted decision.
5. Denied decisions never reach the delegate.
6. Hard invariant exceptions propagate uncaught.

Fail-closed operations (unchanged):
- `ContentItem` delete → `UnsupportedOperationException`
- `ContentItem` restore → `UnsupportedOperationException`
- `Site` unarchive → `UnsupportedOperationException`

## Pass-Through Gaps (Unchanged from Phase 1.4)

The following methods still pass through without per-item authorization:
- `Site.findAll`, `Site.findByAlias`
- `ContentType.findBySiteKey`, `ContentType.findAll`
- `ContentItem.findByContentType`, `ContentItem.findAll`

These are accepted gaps. See Phase 1.4 checkpoint for rationale.

## Validation

```bash
mvn test -pl codex-custos,codex-concilium -am --no-transfer-progress
# codex-custos:    356 tests — PASS
# codex-concilium:  40 tests — PASS
```

## Accepted Gaps

- Collection read authorization (post-retrieval / query-level filtering)
- Alias-to-`SiteKey` resolution before authorization
- Delete / restore / unarchive / purge permission vocabulary
- Direct actor `PermissionGrant` support
- Explanation trace on `AccessDecision`
- Denied-operation cache/index consistency tests
- Audit/authorization integration

## Recommended Next Work

- Collection read filtering (post-retrieval authorization on list operations)
- Alias resolution strategy for `findByAlias`
- Permission vocabulary for delete, restore, unarchive operations
- Consider whether `ConciliumRuntime.inMemory()` should eventually be renamed to
  `inMemoryUnsecured()` when `secured()` becomes the default (deferred, not in scope now)

## Resume Point

If work resumes from this checkpoint:

1. The secured runtime composition path is complete. Do not re-implement it.
2. The next meaningful slice is collection read filtering, alias resolution, or retention
   vocabulary — pick based on current roadmap priority.
3. Do not add delete/restore/purge/unarchive permissions until the domain vocabulary and
   lifecycle semantics are intentionally defined.
