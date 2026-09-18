# TC-15 — Restore Hard Invariant

## ID

TC-15

## Title

A fatal Custos invariant violation during restore propagates and is not converted to denial.

## Source Use Case

Use Case 03 — ContentItem Restore Under Secured Runtime (Alternate Flow — Hard Invariant
Violation).

## Purpose

Prove that when authorization evaluation surfaces a hard Custos invariant violation, the failure
is fatal and is not downgraded into a normal denied decision.

## Preconditions

- A ContentItem exists and is in `ARCHIVED` status.
- The evaluation is arranged to surface a hard invariant violation (e.g. an agent actor associated
  with the highest administrative role).
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- An archived ContentItem within Site A.
- An Actor whose evaluation triggers the hard invariant.
- The secured runtime.

## When

The caller invokes the ContentItem restore operation through the secured runtime.

## Then

- The hard invariant violation propagates as a fatal Custos error.
- It is not converted into a standard denied decision.
- The domain restore operation is not invoked.
- The ContentItem remains in `ARCHIVED` status.

## Negative / boundary notes

- The fatal signal must not be swallowed or downgraded at the authorization or decorator layer.

## Observable evidence

- The caller observes the fatal invariant error (not the standard denial signal).
- The ContentItem status remains `ARCHIVED`.

## Out of scope

- The definition of which actors trigger the invariant (an existing Custos invariant).
- Decision records, metrics, persistence.
