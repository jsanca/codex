# TC-03 — Site Unarchive Scope Isolation

## ID

TC-03

## Title

Unarchive authority scoped to Site A does not authorize unarchiving Site B.

## Source Use Case

Use Case 01 — Site Unarchive Under Secured Runtime (Boundaries).

## Purpose

Prove that authority over one Site never crosses Site boundaries: a caller who may unarchive
Site A is still denied when attempting to unarchive Site B unless separately authorized there.

## Preconditions

- Two Sites, Site A and Site B, both in `ARCHIVED` status.
- A caller holds unarchive authority at a scope covering Site A only (e.g. a Site-scoped grant at
  Site A), and no authority at any scope covering Site B.
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- Site A and Site B, both archived.
- An Actor authorized to unarchive Site A but not Site B.
- The secured runtime.

## When

The caller invokes the Site unarchive operation for Site B (and, as a control, for Site A).

## Then

- The attempt on Site B is denied; Site B remains `ARCHIVED`.
- The attempt on Site A (control) is granted; Site A becomes `SUSPENDED`.
- The scope walk for Site B consults only Site B's scope chain (`Site B → Global`) and never
  derives authority from Site A's grant.

## Negative / boundary notes

- This is the cross-Site isolation guarantee; it must hold even when the caller is otherwise
  authorized for Site A.

## Observable evidence

- Site B status remains `ARCHIVED`; Site A status is `SUSPENDED` (control).

## Out of scope

- Broader-scope grants (Global) which legitimately cover both sites — not under test here.
- Role administration, decision records, metrics, persistence.
