# TC-13 — Restore Does Not Imply Publish

## ID

TC-13

## Title

Restore authority and publish authority are independent; restore never causes the item to become
PUBLISHED.

## Source Use Case

Use Case 03 — ContentItem Restore Under Secured Runtime (Boundaries).

## Purpose

Prove that restore is distinct from publish: authority to restore must not implicitly grant
authority to publish, and vice versa, and a restore never publishes the item.

## Preconditions

- Three independent ContentItems exist and each is in `ARCHIVED` status.
- Callers with restore-only and publish-only authority are available.
- A permission resolution snapshot exists for the evaluation.
- The runtime under exercise is the secured composition.

## Given

- An archived ContentItem for restore-state verification within Site A.
- A separate archived ContentItem for the restore-authority-does-not-imply-publish check within Site A.
- A separate archived ContentItem for the publish-authority-does-not-imply-restore check within Site A.
- Actor R: holds restore authority but not publish authority.
- Actor P: holds publish authority but not restore authority.
- The secured runtime.

## When

- Actor R invokes restore on the restore-state fixture.
- Actor R invokes publish on the restore-authority fixture.
- Actor P invokes restore on the publish-authority fixture.

## Then

- Actor R's restore is granted; the restore-state fixture becomes `DRAFT` (not `PUBLISHED`).
- Actor R's publish is denied (restore authority does not imply publish).
- Actor P's restore is denied (publish authority does not imply restore).

## Negative / boundary notes

- This is a three-part assertion: restore never publishes, restore ⇏ publish, publish ⇏ restore.
- Each assertion uses an independent archived fixture so a state mutation or a failed operation in
  one assertion cannot affect another.
- The restore-state fixture must remain `DRAFT` after Actor R's restore; it must not be promoted
  to `PUBLISHED`.

## Observable evidence

- After Actor R restores the restore-state fixture: status is `DRAFT`.
- Actor R's publish attempt: standard denial signal.
- Actor P's restore attempt: standard denial signal.

## Out of scope

- Role administration, decision records, metrics, persistence.
