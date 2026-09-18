# TC-16 — Denied Secured Mutations Produce No Domain Side Effects

## ID

TC-16

## Title

A denied unarchive, delete, or restore produces no domain mutation, event, projection update, or
Chronicon audit record.

## Source Use Case

Use Cases 01, 02, and 03 (Alternate Flow — Unauthorized; Postconditions — After a denied call).

## Purpose

Prove the denied-operation side-effect guarantee holds uniformly for the three operations newly
brought under explicit authorization: a denied attempt leaves no trace attributable to the
attempted mutation.

## Preconditions

- For each of the three operations, a target resource exists in the correct starting state:
  - an archived Site (for unarchive);
  - an archived ContentItem (for delete and restore).
- A caller without authority for the attempted operation.
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition, wired to its normal projections and
  audit subscribers.

## Given

- A secured runtime with domain events, projections, and domain audit (Chronicon) active.
- A denied actor for each operation.
- Archived resources (one Site, one ContentItem).

## When

The caller attempts, through the secured runtime and for each operation in turn:
- Site unarchive (denied);
- ContentItem delete (denied);
- ContentItem restore (denied).

## Then

For each denied attempt:

- No domain state mutation occurs (the Site remains `ARCHIVED`; the ContentItem remains
  `ARCHIVED` and still exists).
- No domain event attributable to the attempted mutation is emitted.
- No projection update caused by such an event occurs.
- No Chronicon domain-audit record attributable to the denied attempt is written.
- The caller receives the standard Custos denial signal.

## Negative / boundary notes

- This test deliberately does not require any security-audit record; security decision records,
  Aegis, and Observance authorization metrics are out of scope for Intent 1.
- "Attributable to the denied attempt" is scoped to the mutation's own event/projection/audit
  chain; pre-existing records are unaffected.

## Observable evidence

- Domain state is unchanged for each operation.
- No event, projection change, or domain-audit record for any denied attempt.

## Out of scope

- Security decision records, decision traces, Observance authorization metrics, Aegis.
- Role administration, direct actor grants, persistence.
