# Implementation Report — CODEX-016.1: Harden coreRuntime() Javadoc Bypass Warning

**Task:** CODEX-016.1
**Date:** 2026-08-06
**Implementer:** Clio
**Status:** COMPLETE

## Change

Updated the Javadoc of `ConciliumRuntime.coreRuntime()` to document the authorization bypass risk
on secured runtimes.

**File:** `codex-concilium/src/main/java/codex/concilium/api/runtime/ConciliumRuntime.java`

The new Javadoc:
- Names the bypass risk explicitly with a **Security note** callout
- Directs domain callers and adapters to use `siteService()`, `contentTypeService()`, `contentItemService()` instead
- Documents the legitimate uses of `coreRuntime()` (projection readers, recorded events, lifecycle, internal composition)

## Validation

```
git diff --check -- "*.java"   → PASS (no whitespace errors)
mvn test -pl codex-concilium -am --no-transfer-progress
  → 40 tests, 0 failures — BUILD SUCCESS
```

## No Behavior Changed

No runtime behavior, test logic, or public API surface was modified.
