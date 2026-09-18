# QA Report — Intent 1 Canonical Test Cases

## Task

`docs/engineering/agents/tasks/intent1/Task—Intent1CanonicalTestCases.md`

## Summary

Canonical behavioral test cases for Custos C1 — Complete Secured Mutation Enforcement were
produced from the accepted Intent and three Use Cases. No production code, test code, or
permission vocabulary was changed.

## Number of Test Cases

17 test cases across three operations plus two cross-cutting cases:

- Site unarchive: TC-01–TC-04 (4)
- ContentItem delete: TC-05–TC-09 (5)
- ContentItem restore: TC-10–TC-15 (6)
- Cross-cutting: TC-16 (denied side-effect absence), TC-17 (no unsupported-operation fallback) (2)

## Use-Case Coverage

Every observable verification in the three use cases maps to at least one test case (see the
traceability matrix in `docs/knowledge/test-cases/intent1/README.md`):

- UC1 (unarchive): authorized, denied, scope isolation, hard invariant, no-fallback — covered by
  TC-01/02/03/04 and TC-16/17.
- UC2 (delete): authorized, denied, invalid domain state, scope isolation, hard invariant,
  no-fallback — covered by TC-05/06/07/08/09 and TC-16/17.
- UC3 (restore): authorized, denied, invalid domain state, restore≠publish, scope isolation, hard
  invariant, no-fallback — covered by TC-10/11/12/13/14/15 and TC-16/17.

The additional Intent requirement "publish authority does not imply restore" (task requirement
restore #7) is folded into TC-13 as a three-part assertion (restore never publishes; restore ⇏
publish; publish ⇏ restore).

## Open QA Questions

1. **Role mapping is provisional.** The Intent and Use Cases defer role questions to design. The
   test cases adopt the engineering plan's recommended mapping (`SITE_ADMIN` + `SUPER_ADMIN` for
   all three; `EDITOR` for restore only). If design later changes these mappings, the
   "provisionally authorized" actors in TC-01/05/10 and the "provisionally denied" actors in
   TC-02/06/11 must be re-expressed. This is expected, but it means the test cases are not yet
   fully independent of the design role table.
2. **Destructive-authority strength.** The Intent raises, and leaves open, whether delete should
   require stronger authority than archive/update. TC-05/06/08 do not assert any specific
   strength ranking; they only assert that delete has its own explicit permission and that scope
   does not leak. If a stronger-authority requirement is later adopted, an additional test case
   would be needed.
3. **Broader-scope delete/restore.** UC2 and UC3 note broader-scope authority may permit the
   operation within scope. TC-08/14 assert the cross-Site boundary but do not enumerate
   ContentType- and Global-scope grants. This is intentionally left to design (permission
   vocabulary + scope semantics) and may warrant a follow-up case once scope levels are fixed.

## Discrepancies With Engineering Plan

The task requires comparing test cases against the engineering plan and reporting any
contradictions or duplicated responsibilities.

1. **No contradiction found.** The plan's recommended decisions (§4) are consistent with the
   Intent and Use Cases: three distinct keys, no new implications, no reuse, role table, and
   scope walks that mirror existing operations. The test cases reflect this behavior without
   prescribing the constants, API names, or slices.
2. **No orphaned plan test.** Every planned test layer named in the plan's §8 traceability matrix
   has a behavioral source in these test cases. Conversely, no test case requires a behavior the
   plan does not also anticipate.
3. **No duplicated responsibility at the behavioral level.** The plan layers the same behavior
   across unit/decorator/matrix/runtime test layers (§7). That layering is an implementation
   concern; these canonical test cases are layer-agnostic and therefore do not duplicate it. The
   plan's own instruction that "each responsibility is asserted at exactly one layer" is an
   engineering concern and does not conflict with behavioral coverage here.
4. **QA finding — role mapping provenance.** The `EDITOR: restore → allowed, delete → denied`
   distinction originates in the engineering plan's recommended role table (§4.2), not in the
   Intent or Use Cases (which defer role questions). Per the task's instruction ("do not treat
   that mapping as immutable architecture"), this is recorded as a QA finding rather than
   silently adopted as architecture. It is provisionally reflected in TC-05/06/10/11.

## Missing Behavioral Requirement

None identified. The three use cases' observable verifications, the Intent's success conditions,
and the task's minimum coverage list are all represented. The one requirement not yet fully
enumerable (broader-scope delete/restore at ContentType/Global levels) is deferred to design and
flagged above, not missing from the use cases.

## Files Created

- `docs/knowledge/test-cases/intent1/README.md` (index + traceability matrix)
- `docs/knowledge/test-cases/intent1/TC-01` … `TC-17` (17 test case files)

## Validation

No Maven tests are required (documentation-only). `git diff --check -- docs` passes.

## Out of Scope (confirmed unchanged)

No production code, test code, permission vocabulary, role mappings, Observance metrics, or
security-audit artifacts were modified.
