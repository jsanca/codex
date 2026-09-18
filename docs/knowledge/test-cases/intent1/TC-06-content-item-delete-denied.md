# TC-06 — Denied ContentItem Delete

## ID

TC-06

## Title

A caller without sufficient authority is denied delete and the item is intact.

## Source Use Case

Use Case 02 — ContentItem Delete Under Secured Runtime (Alternate Flow — Unauthorized).

## Purpose

Prove that a caller lacking delete authority receives the standard Custos denial signal, the
domain operation is not invoked, and the ContentItem continues to exist unchanged.

## Preconditions

- A ContentItem exists and is in `ARCHIVED` status.
- The caller is a well-formed Actor without delete authority at any scope covering the target
  item (`EDITOR`, `COPYWRITER`, `REVIEWER`, or `VIEWER`, per MD-C1-01).
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- An archived ContentItem within Site A.
- An Actor whose resolved permissions do not include delete authority over that item.
- The secured runtime.

## When

The caller invokes the ContentItem delete operation through the secured runtime.

## Then

- An authorization decision is reached and is denied.
- The standard Custos denial signal is raised.
- The domain delete operation is not invoked.
- The ContentItem continues to exist and its status is unchanged (`ARCHIVED`).

## Negative / boundary notes

- The absence of downstream side effects is asserted jointly in TC-16.

## Observable evidence

- The caller receives the standard Custos denial signal.
- The ContentItem still exists, status unchanged.

## Out of scope

- Side-effect absence detail (covered by TC-16).
- Role administration, direct actor grants, decision records, metrics, persistence.
