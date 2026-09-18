# TC-02 — Denied Site Unarchive

## ID

TC-02

## Title

A caller without sufficient authority is denied unarchive and the Site is unchanged.

## Source Use Case

Use Case 01 — Site Unarchive Under Secured Runtime (Alternate Flow — Unauthorized).

## Purpose

Prove that a caller lacking unarchive authority receives the standard Custos denial signal, the
domain operation is not invoked, and the Site remains `ARCHIVED`.

## Preconditions

- A Site exists and is in `ARCHIVED` status.
- The caller is a well-formed Actor without unarchive authority at any scope covering the target
  Site (`EDITOR`, `COPYWRITER`, `REVIEWER`, or `VIEWER`, per MD-C1-01).
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- An archived Site belonging to Site A.
- An Actor whose resolved permissions do not include unarchive authority over Site A.
- The secured runtime.

## When

The caller invokes the Site unarchive operation through the secured runtime for Site A.

## Then

- An authorization decision is reached and is denied.
- The standard Custos denial signal is raised.
- The domain unarchive operation is not invoked.
- The Site remains in `ARCHIVED` status.

## Negative / boundary notes

- Denial is a normal authorization outcome, not an exception in domain semantics.
- The absence of downstream side effects is asserted jointly in TC-16.

## Observable evidence

- The caller receives the standard Custos denial signal.
- Site status remains `ARCHIVED`.

## Out of scope

- Side-effect absence detail (covered by TC-16).
- Role administration, direct actor grants, decision records, metrics, persistence.
