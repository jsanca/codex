Task 14 — Align ContentType Permission Vocabulary with Lifecycle

Context:
Task 13 introduced domain-specific permission services. Deep reviewed the task and passed it with one WARN:

ContentTypePermissionsService currently exposes canDeleteContentType backed by Permissions.CONTENT_TYPE_DELETE, but the domain lifecycle appears to use archive / ARCHIVED vocabulary:

* ContentTypeStatus has ARCHIVED, not DELETED.
* ContentTypeService uses archive(), not delete().
* The current Javadoc says “delete (or deprecate)”, which indicates unresolved vocabulary.

We want the permission service language to express domain intent clearly.

Decision for this slice:
Align ContentType authorization vocabulary to archive, not delete.

Scope:
Update codex-custos only.

Expected changes:

1. Rename permission catalog constant:

    * from CONTENT_TYPE_DELETE
    * to CONTENT_TYPE_ARCHIVE

   Keep the permission value aligned with the new vocabulary:

    * "contentType.archive"

2. Rename ContentTypePermissionsService method:

    * from canDeleteContentType(...)
    * to canArchiveContentType(...)

3. Rename implementation method:

    * from DefaultContentTypePermissionsService.canDeleteContentType(...)
    * to DefaultContentTypePermissionsService.canArchiveContentType(...)

4. Update tests accordingly:

    * test names
    * display names
    * expected PermissionKey
    * any mapping tables or fixtures

5. Update BuiltInRoles if it references CONTENT_TYPE_DELETE.
   Use CONTENT_TYPE_ARCHIVE instead.

6. Update any documentation comments in the affected API/service classes.

Do not introduce:

* contentType.delete
* contentType.destroy
* contentType.purge
* contentType.restore
* contentItem.delete
* contentItem.restore
* site.delete
* site.restore
* secured decorators
* REST
* persistence
* Archivum archive store
* Olorin / step-up behavior

Vocabulary guidance:

* archive = lifecycle operation that removes the entity from normal active use.
* restore = future retention/archive administration operation.
* purge = future irreversible retention cleanup operation.
* delete/destroy are not part of this slice.

Important:
Do not add restore or purge yet. This task only aligns the existing content type vocabulary.

Tests:
Run:

* mvn test -pl codex-custos
* optionally full reactor if the rename affects other modules

Expected output:

* files changed
* old names removed
* new names introduced
* test count
* commands run
* any references that could not be renamed
