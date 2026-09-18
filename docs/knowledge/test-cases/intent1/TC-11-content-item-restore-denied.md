# TC-11 — Denied ContentItem Restore

## ID

TC-11

## Title

A caller without sufficient authority is denied restore and the item remains ARCHIVED.

## Source Use Case

Use Case 03 — ContentItem Restore Under Secured Runtime (Alternate Flow — Unauthorized).

## Purpose

Prove that a caller lacking restore authority receives the standard Custos denial signal, the
domain operation is not invoked, and the ContentItem remains `ARCHIVED`.

## Preconditions

- A ContentItem exists and is in `ARCHIVED` status.
- The caller is a well-formed Actor without restore authority at any scope covering the target
  item (`COPYWRITER`, `REVIEWER`, or `VIEWER`, per MD-C1-01).
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- An archived ContentItem within Site A.
- An Actor whose resolved permissions do not include restore authority over that item.
- The secured runtime.

## When

The caller invokes the ContentItem restore operation through the secured runtime.

## Then

- An authorization decision is reached and is denied.
- The standard Custos denial signal is raised.
- The domain restore operation is not invoked.
- The ContentItem remains in `ARCHIVED` status.

## Negative / boundary notes

- The absence of downstream side effects is asserted jointly in TC-16.

## Observable evidence

- The caller receives the standard Custos denial signal.
- The ContentItem status remains `ARCHIVED`.

## Out of scope

- Side-effect absence detail (covered by TC-16).
- Role administration, direct actor grants, decision records, metrics, persistence.
