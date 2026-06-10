Fix the ContentItem restore lifecycle operation so the restored item and its working revision are consistent.

Context:
After the archive/restore implementation, Clio identified this edge case:

In the PUBLISHED → ARCHIVED → DRAFT path, after restore the ContentItem status becomes DRAFT, but currentWorkingRevisionId still points to a ContentRevision with status ARCHIVED.

This means the item appears editable, but its working revision still carries an ARCHIVED marker. It does not fail today, but it can break future operations that validate the working revision status, such as re-publishing after restore.

Goal:
When restoring a ContentItem from ARCHIVED to DRAFT, ensure the current working revision is also restored to a valid editable status.

Requirements:
1. Inspect the current archive implementation and how it changes ContentItem status and ContentRevision status.
2. Inspect the current restore implementation.
3. Update restore semantics so that:
    - ContentItem.status transitions ARCHIVED → DRAFT.
    - The current working revision status transitions ARCHIVED → WORKING, if the current working revision is archived.
    - restore does not automatically publish the item.
    - currentPublishedRevisionId remains empty unless the existing model explicitly requires otherwise.
4. Preserve existing invalid transition behavior:
    - restore should reject non-ARCHIVED items.
5. Update audit fields consistently with the existing restore implementation.
6. Add or update tests proving:
    - restoring an archived draft returns the item to DRAFT and the working revision to WORKING.
    - restoring an item that followed PUBLISHED → ARCHIVED → RESTORED returns the item to DRAFT and the working revision to WORKING.
    - the restored item can be published again if the current publish semantics allow it.
7. Keep ContentItemRestoredEvent dispatching behavior unchanged.
8. Keep cache invalidation behavior unchanged.
9. Do not implement delete in this task.
10. Do not add TTL, metrics, cache administration, or new event infrastructure.

Expected result:
- Restore leaves no mismatch between ContentItem status and current working revision status.
- The PUBLISHED → ARCHIVED → RESTORED path produces a clean editable draft.
- Existing tests continue to pass.