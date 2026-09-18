# TC-12 — Restore Invalid Domain State

## ID

TC-12

## Title

A granted restore against a non-ARCHIVED item yields a domain precondition failure, not a Custos
denial.

## Source Use Case

Use Case 03 — ContentItem Restore Under Secured Runtime (Alternate Flow — Domain Precondition Not
Met).

## Purpose

Prove that authorization does not replace domain state-machine validation: a caller who is
authorized to restore but targets an item that is not `ARCHIVED` receives the domain's own
precondition error.

## Preconditions

- A ContentItem exists and is in a non-`ARCHIVED` status (e.g. `DRAFT` or `PUBLISHED`).
- The caller holds restore authority at a scope covering the item, so authorization is granted.
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- A non-archived ContentItem within Site A.
- An Actor authorized to restore that item.
- The secured runtime.

## When

The caller invokes the ContentItem restore operation through the secured runtime.

## Then

- Authorization evaluation is performed normally and is granted.
- The domain restore operation itself refuses per the existing state-machine rule.
- The observable outcome is the domain's own precondition error, not a Custos denial.

## Negative / boundary notes

- Asserts the domain/authorization separation: Custos is state-blind and does not model the
  `ARCHIVED` precondition.

## Observable evidence

- The caller observes a domain precondition error (not the standard Custos denial signal).
- The ContentItem is unchanged.

## Out of scope

- Changing the domain precondition itself (out of Custos's scope).
- Role administration, decision records, metrics, persistence.
