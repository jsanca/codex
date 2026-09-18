# TC-01 — Authorized Site Unarchive

## ID

TC-01

## Title

A caller with sufficient authority can unarchive an archived Site.

## Source Use Case

Use Case 01 — Site Unarchive Under Secured Runtime (Main Flow — Authorized).

## Purpose

Prove that, when a caller holds the authority to unarchive a Site, the secured runtime reaches a
granted decision, invokes the domain operation, and the Site transitions `ARCHIVED → SUSPENDED`
with the same downstream effects as an unsecured invocation.

## Preconditions

- A Site exists and is in `ARCHIVED` status.
- The caller is a well-formed Actor holding authority for the unarchive operation at a scope that
  includes the target Site (`SUPER_ADMIN` or `SITE_ADMIN`, per MD-C1-01).
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- An archived Site belonging to Site A.
- An Actor whose resolved permissions include unarchive authority over Site A.
- The secured runtime.

## When

The caller invokes the Site unarchive operation through the secured runtime for Site A.

## Then

- An authorization decision is reached and is granted.
- The domain unarchive operation executes with the original arguments.
- The Site transitions from `ARCHIVED` to `SUSPENDED`.
- Every downstream effect that a normal, successful unarchive produces (events, projections, audit
  records) occurs, indistinguishable from an unsecured invocation with a valid caller.
- No denial signal is raised.

## Negative / boundary notes

- The operation is meaningful only from `ARCHIVED`; this test targets the archived starting state.
- Authorization grants "whether"; it does not alter the target status (`SUSPENDED`).

## Observable evidence

- Site status is `SUSPENDED` after the call.
- The operation's normal downstream effects are present.

## Out of scope

- Role administration, direct actor grants, decision records, decision traces, metrics, persistence.
- Behavior when the Site is not `ARCHIVED`.
