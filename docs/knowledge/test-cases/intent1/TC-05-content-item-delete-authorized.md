# TC-05 — Authorized ContentItem Delete

## ID

TC-05

## Title

A caller with sufficient authority can delete an archived ContentItem.

## Source Use Case

Use Case 02 — ContentItem Delete Under Secured Runtime (Main Flow — Authorized).

## Purpose

Prove that a caller holding delete authority can remove an archived ContentItem through the
secured runtime with the same downstream effects as an unsecured invocation.

## Preconditions

- A ContentItem exists, belongs to a known Site and ContentType, and is in `ARCHIVED` status
  (delete requires `ARCHIVED`).
- The caller holds delete authority at a scope covering the target item (`SUPER_ADMIN` or
  `SITE_ADMIN`, per MD-C1-01).
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- An archived ContentItem within Site A.
- An Actor whose resolved permissions include delete authority over that item's scope chain.
- The secured runtime.

## When

The caller invokes the ContentItem delete operation through the secured runtime.

## Then

- An authorization decision is reached and is granted.
- The domain delete operation executes with the original arguments.
- The ContentItem is removed per existing domain delete semantics.
- Every downstream effect that a normal successful delete produces (events, projections, audit
  records) occurs, indistinguishable from an unsecured invocation with a valid caller.
- No denial signal is raised.

## Negative / boundary notes

- Authorization grants "whether"; it does not relax the domain precondition that delete requires
  `ARCHIVED`.

## Observable evidence

- The ContentItem no longer exists after the call.
- The operation's normal downstream effects are present.

## Out of scope

- Retention/backup/post-delete recoverability semantics.
- Role administration, direct actor grants, decision records, metrics, persistence.
