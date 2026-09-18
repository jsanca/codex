# TC-14 — Restore Scope Isolation

## ID

TC-14

## Title

Restore authority scoped to one resource does not enable restore on an unrelated resource.

## Source Use Case

Use Case 03 — ContentItem Restore Under Secured Runtime (Boundaries).

## Purpose

Prove that restore authority respects the existing scope walk without leaking across unrelated
ContentItems, ContentTypes, or Sites, while broader-scope authority still permits restore within
its own scope but never across Site boundaries.

## Preconditions

- At least two archived ContentItems in different, unrelated scopes (e.g. different Sites A and
  B, or different ContentTypes).
- A caller holds restore authority at a scope covering only one of them.
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- Item X (within Site A) and Item Y (within Site B), both archived.
- An Actor authorized to restore within Site A only.
- The secured runtime.

## When

The caller invokes the ContentItem restore operation for Item Y (and, as a control, for Item X).

## Then

- The attempt on Item Y is denied; Item Y remains `ARCHIVED`.
- The attempt on Item X (control) is granted; Item X becomes `DRAFT`.
- The scope walk for Item Y consults only Item Y's scope chain and never derives authority from
  the Site A grant.

## Negative / boundary notes

- Broader-scope authority (e.g. a Site-scoped or Global grant) legitimately permits restore of
  items within that scope; the invariant under test is only that authority never crosses Site
  boundaries.

## Observable evidence

- Item Y status remains `ARCHIVED`; Item X status is `DRAFT` (control).

## Out of scope

- Collection-read authorization; cross-Site grants.
- Role administration, decision records, metrics, persistence.
