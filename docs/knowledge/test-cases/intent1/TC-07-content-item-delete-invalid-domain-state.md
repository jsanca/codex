# TC-07 — Delete Invalid Domain State

## ID

TC-07

## Title

A granted delete against a non-ARCHIVED item yields a domain precondition failure, not a Custos
denial.

## Source Use Case

Use Case 02 — ContentItem Delete Under Secured Runtime (Alternate Flow — Domain Precondition Not
Met).

## Purpose

Prove that authorization does not replace domain state-machine validation: a caller who is
authorized to delete but targets an item that is not `ARCHIVED` receives the domain's own
precondition error, and the separation between authorization and domain semantics is preserved.

## Preconditions

- A ContentItem exists and is in a non-`ARCHIVED` status (e.g. `DRAFT` or `PUBLISHED`).
- The caller holds delete authority at a scope covering the item (`SUPER_ADMIN` or `SITE_ADMIN`,
  per MD-C1-01), so authorization is granted.
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- A non-archived ContentItem within Site A.
- An Actor authorized to delete that item.
- The secured runtime.

## When

The caller invokes the ContentItem delete operation through the secured runtime.

## Then

- Authorization evaluation is performed normally and is granted (granted authority does not
  bypass domain preconditions).
- The domain delete operation itself refuses per the existing state-machine rule.
- The observable outcome is the domain's own precondition error, not a Custos denial.

## Negative / boundary notes

- This asserts the domain/authorization separation: Custos is state-blind and does not model the
  `ARCHIVED` precondition.
- The control case (delete an actually-archived item with the same authority) should still
  succeed; only the non-`ARCHIVED` target fails.

## Observable evidence

- The caller observes a domain precondition error (not the standard Custos denial signal).
- The ContentItem is unchanged.

## Out of scope

- Changing the domain precondition itself (out of Custos's scope).
- Role administration, decision records, metrics, persistence.
