# TC-04 — Site Unarchive Hard Invariant

## ID

TC-04

## Title

A fatal Custos invariant violation during unarchive propagates and is not converted to denial.

## Source Use Case

Use Case 01 — Site Unarchive Under Secured Runtime (Alternate Flow — Hard Invariant Violation).

## Purpose

Prove that when authorization evaluation surfaces a hard Custos invariant violation (for example,
an agent actor improperly holding administrative authority), the failure is fatal and is not
downgraded into a normal denied decision.

## Preconditions

- A Site exists and is in `ARCHIVED` status.
- The evaluation is arranged to surface a hard invariant violation (e.g. an agent actor associated
  with the highest administrative role).
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- An archived Site belonging to Site A.
- An Actor whose evaluation triggers the hard invariant (an agent actor holding administrative
  authority).
- The secured runtime.

## When

The caller invokes the Site unarchive operation through the secured runtime for Site A.

## Then

- The hard invariant violation propagates as a fatal Custos error.
- It is not converted into a standard denied decision.
- The domain unarchive operation is not invoked.
- No domain state changes.

## Negative / boundary notes

- The fatal signal must not be swallowed or downgraded at the authorization or decorator layer.

## Observable evidence

- The caller observes the fatal invariant error (not the standard denial signal).
- Site status remains `ARCHIVED`.

## Out of scope

- The definition of which actors trigger the invariant (an existing Custos invariant).
- Decision records, metrics, persistence.
