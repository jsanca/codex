# TC-17 — No Unsupported-Operation Fallback Remains

## ID

TC-17

## Title

The secured runtime reaches an explicit authorization decision for unarchive, delete, and
restore; it never falls back to unsupported-operation behavior.

## Source Use Case

Use Cases 01, 02, and 03 (Observable Verifications — "the secured runtime never falls back to an
'unsupported operation' style refusal").

## Purpose

Prove the completeness outcome of Intent 1: each of the three operations now participates in the
normal grant/deny model rather than a fail-closed placeholder. There is no code path in the
secured runtime that refuses these operations with an unsupported-operation signal instead of an
authorization decision.

## Preconditions

- The secured runtime is composed with the full authorization chain.
- The three operations are wired to explicit permission semantics.
- Every operation × authority-state combination has an independent initial resource state:
  an archived Site for unarchive, and an archived ContentItem for delete or restore.

## Given

- The secured runtime.
- Actors with and without authority for each operation.
- Independent archived resources for every operation × authority-state invocation.

## When

Each of the three operations is invoked, for both an authorized and an unauthorized actor:
- Site unarchive;
- ContentItem delete;
- ContentItem restore.

## Then

- Every invocation yields an explicit authorization decision: granted for the authorized actor,
  denied (standard Custos denial signal) for the unauthorized actor.
- No invocation yields an "unsupported operation" style refusal.
- For granted calls, the domain operation executes; for denied calls, the delegate is not invoked.

## Negative / boundary notes

- This is a no-fallback assertion: the presence of any unsupported-operation refusal for these
  three operations, in any path, fails this test case.
- Independent fixtures prevent granted delete or restore calls from contaminating later denied
  controls or altering the initial state required by another invocation.
- It is the direct expression of Intent 1's "explicit authority" and "consistency" principles.

## Observable evidence

- Three operations × two authority states produce only granted/denied outcomes, never an
  unsupported-operation signal.

## Out of scope

- Role administration, decision records, metrics, persistence.
