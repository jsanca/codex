# TC-10 — Authorized ContentItem Restore

## ID

TC-10

## Title

A caller with sufficient authority can restore an archived ContentItem to DRAFT.

## Source Use Case

Use Case 03 — ContentItem Restore Under Secured Runtime (Main Flow — Authorized).

## Purpose

Prove that a caller holding restore authority can return an archived ContentItem to `DRAFT`
through the secured runtime with the same downstream effects as an unsecured invocation.

## Preconditions

- A ContentItem exists, belongs to a known Site and ContentType, and is in `ARCHIVED` status
  (restore is meaningful only from `ARCHIVED`).
- The caller holds restore authority at a scope covering the target item (`SUPER_ADMIN`,
  `SITE_ADMIN`, or `EDITOR`, per MD-C1-01).
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- An archived ContentItem within Site A.
- An Actor whose resolved permissions include restore authority over that item's scope chain.
- The secured runtime.

## When

The caller invokes the ContentItem restore operation through the secured runtime.

## Then

- An authorization decision is reached and is granted.
- The domain restore operation executes with the original arguments.
- The ContentItem transitions from `ARCHIVED` to `DRAFT`, per the existing state machine.
- Every downstream effect that a normal successful restore produces (events, projections, audit
  records) occurs, indistinguishable from an unsecured invocation with a valid caller.
- No denial signal is raised.

## Negative / boundary notes

- Restore returns the item to `DRAFT`, never to `PUBLISHED` (see TC-13).

## Observable evidence

- The ContentItem status is `DRAFT` after the call.
- The operation's normal downstream effects are present.

## Out of scope

- Publish semantics (covered by TC-13).
- Role administration, direct actor grants, decision records, metrics, persistence.
