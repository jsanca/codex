Normalize ContentItem identity generation to follow the same IdentityGenerator pattern used by Site and ContentType.

Context:
CodexContentTypeService receives:

IdentityGenerator<CreateContentTypeCommand, ContentTypeId>

and uses:

identityGenerator.nextIdentity(command)

during create().

CodexContentItemService currently generates the ContentItemId directly inside create() using:

ContentItemId.forItem(command.siteKey(), command.contentTypeKey(), command.key())

This is deterministic, but it is not symmetric with Site and ContentType identity generation.

Goal:
Introduce ContentItemIdentityGenerator and inject it into CodexContentItemService, while preserving the existing deterministic identity semantics.

Requirements:
1. Create ContentItemIdentityGenerator implementing:
   IdentityGenerator<CreateContentItemCommand, ContentItemId>

2. The generator must produce the same ContentItemId currently produced by:
   ContentItemId.forItem(siteKey, contentTypeKey, contentItemKey)

3. Update CodexContentItemService to receive:
   IdentityGenerator<CreateContentItemCommand, ContentItemId>

4. Provide constructors consistent with CodexContentTypeService:
    - default constructor path uses new ContentItemIdentityGenerator()
    - test-visible or full constructor allows injecting a custom generator

5. Replace direct usage of:
   ContentItemId.forItem(...)

   with:
   identityGenerator.nextIdentity(command)

6. Do not change ContentRevisionId generation in this task.
   It can remain:
   ContentRevisionId.forRevision(...)

7. Add focused tests for ContentItemIdentityGenerator:
    - same command produces same ContentItemId
    - different SiteKey produces different ContentItemId
    - different ContentTypeKey produces different ContentItemId
    - different ContentItemKey produces different ContentItemId
    - null command is rejected

8. Add or update CodexContentItemService tests to prove it uses the injected identity generator.

9. Do not change event payloads.
10. Do not change AuditRecordId generation in this task.
11. Do not change lifecycle semantics.

Expected result:
- ContentItem identity generation remains deterministic.
- ContentItem identity generation follows the same IdentityGenerator pattern as Site and ContentType.
- Existing lifecycle, event, cache, and audit tests continue to pass.