# Task31: Public Projection Read Contract

## Objective

Remove the current qualified export from `codex-codex` to `codex-index`:

```java
exports codex.codex.internal.repository to codex.index;
```
Replace it with a public, narrow projection/read contract in codex-codex API.

codex-index should depend on this public contract instead of depending on internal repository interfaces.

This task fixes the controlled technical debt introduced during Task28.

Decision Context

codex-index needs to read canonical content state in order to build indexing projections.

Today it reaches into codex-codex internals through a qualified export:

exports codex.codex.internal.repository to codex.index;

That works, but it weakens the meaning of internal.

The correct boundary is:

codex-codex
-> owns canonical content and exposes narrow public read/projection contracts

codex-index
-> consumes those contracts to build index projections

runtime / assembly / porta / future Spring wiring
-> provides the concrete implementation

Do not make all repositories public.

Do not expose internal repositories broadly.

Do not use a Service Locator.

Do not introduce runtime assembly yet.

The goal is only to introduce the public projection/read contract and make codex-index depend on it.

Architectural Direction

Use a narrow public API contract.

Do not expose general repositories.

Do not move repositories out of internal.

Do not make codex-index depend on codex.codex.internal.repository.

Do not make codex-codex depend on codex-index.

Expected direction:

codex.codex.api.projection.ContentItemProjectionReader
-> public read/projection contract

codex.index.internal.ContentItemProjectionSource
-> index-side abstraction

codex.index.internal.ReaderContentItemProjectionSource
-> adapts ContentItemProjectionReader to ContentItemProjectionSource

Concrete repository-backed implementations may exist inside codex-codex.internal, but codex-index must not import them.

Scope

Implement:

public projection/read contract in codex-codex
internal repository-backed implementation in codex-codex
update codex-index to use the public contract
remove qualified export of codex.codex.internal.repository to codex.index
update tests
update documentation if useful

Do not implement:

runtime/assembly module
ServiceLoader runtime discovery
CodexModuleRuntime
CodexModuleRuntimeProvider
Spring wiring
cache integration
read-only unit of work
search service
audit subscribers
workflow
durable persistence
REST/GraphQL
dynamic service registry
Location
New public API package in codex-codex

Suggested package:

codex.codex.api.projection

Create:

codex.codex.api.projection.ContentItemProjectionReader

Alternative package name is acceptable if it matches existing conventions, but prefer api.projection because this contract exists for projection modules such as index, chronicon, cache invalidation, or future read models.

Internal implementation in codex-codex

Suggested package:

codex.codex.internal.projection

Create:

codex.codex.internal.projection.RepositoryContentItemProjectionReader

This class may depend on internal repositories because it lives inside codex-codex.

Index adapter in codex-index

Suggested package:

codex.index.internal

Replace or refactor:

RepositoryContentItemProjectionSource

into something like:

ReaderContentItemProjectionSource

or update the existing class to depend on ContentItemProjectionReader instead of repositories.

Prefer renaming if the old name becomes misleading.

1. Create ContentItemProjectionReader

Create:

codex.codex.api.projection.ContentItemProjectionReader

Contract:

Optional<ContentItem> findContentItem(
SiteKey siteKey,
ContentTypeKey contentTypeKey,
ContentItemKey key
)

Optional<ContentRevision> findContentRevision(
ContentRevisionId revisionId
)

Required imports likely include:

codex.codex.api.model.entity.ContentItem
codex.codex.api.model.entity.ContentRevision
codex.codex.api.model.identity.SiteKey
codex.codex.api.model.identity.ContentTypeKey
codex.codex.api.model.identity.ContentItemKey
codex.codex.api.model.identity.ContentRevisionId
java.util.Optional

Requirements:

interface only
no implementation logic
JavaDoc explaining it is a narrow read contract for projection modules
no write methods
no generic query DSL
no search methods
no cache methods
no transaction methods
no actor parameter for now unless existing read conventions require it

Rationale:

This contract allows projection modules to load canonical state without depending on internal repositories.

2. Export projection API

Update codex-codex/src/main/java/module-info.java.

Add:

exports codex.codex.api.projection;

Do not export internal projection packages.

Do not export internal repositories.

Remove:

exports codex.codex.internal.repository to codex.index;

The build should still pass after codex-index is updated to use the public contract.

3. Create RepositoryContentItemProjectionReader

Create:

codex.codex.internal.projection.RepositoryContentItemProjectionReader

It should implement:

ContentItemProjectionReader

Constructor dependencies:

ContentItemRepository
ContentRevisionRepository

Behavior:

findContentItem(siteKey, contentTypeKey, key)
-> validates arguments
-> delegates to contentItemRepository.findByKey(...)

findContentRevision(revisionId)
-> validates argument
-> delegates to contentRevisionRepository.findById(...)

Requirements:

validate constructor dependencies
validate method arguments
return repository results as-is
no caching
no read-only transaction handling
no indexing
no event dispatch
no service locator
no Spring
JavaDoc explaining this is the repository-backed implementation of the public projection reader

This class remains internal to codex-codex.

4. Update CodexRuntime / Core Runtime Exposure

If the existing CodexRuntime currently exposes or can expose a projection reader, add a method:

ContentItemProjectionReader contentItemProjectionReader()

Requirements:

return the repository-backed reader created from the same repositories as core services
do not import codex-index
do not wire indexing subscribers
do not create codex-runtime or codex-assembly
do not introduce ServiceLoader
do not introduce Spring

If adding this method creates too much runtime churn, it may be deferred, but the preferred result is that core runtime can provide the public reader to future assembly layers:

var core = CodexRuntime.inMemory();
var index = IndexRuntime.inMemory(core.contentItemProjectionReader());

This keeps runtime composition explicit.

5. Update codex-index projection source

Update codex-index.

Current likely class:

codex.index.internal.RepositoryContentItemProjectionSource

It probably depends on:

codex.codex.internal.repository.ContentItemRepository
codex.codex.internal.repository.ContentRevisionRepository

Remove those dependencies.

Make it depend on:

codex.codex.api.projection.ContentItemProjectionReader

Suggested rename:

ReaderContentItemProjectionSource

Behavior:

loadItem(event)
-> validate event
-> projectionReader.findContentItem(event.siteKey(), event.contentTypeKey(), event.key())
-> if empty, throw IllegalStateException

loadPublishedRevision(event)
-> validate event
-> projectionReader.findContentRevision(event.publishedRevisionId())
-> if empty, throw IllegalStateException

Requirements:

no direct repository imports in codex-index
preserve previous exception behavior
preserve ContentItemProjectionSource abstraction
preserve tests, updated to use reader stubs
JavaDoc should explain it adapts the public core projection reader to the index projection source

If keeping the old class name, update JavaDoc so it is not misleading. Prefer renaming to avoid confusion.

6. Update codex-index module-info

Update codex-index/src/main/java/module-info.java as needed.

It should require codex.codex.

It should not require access to internal repository packages.

No qualified export from core should be needed.

Expected:

module codex.index {
requires codex.fundamentum;
requires transitive codex.codex;
requires org.slf4j;

    exports codex.index.api;
}

Use requires transitive codex.codex only if codex.index.api exposes codex-codex API types such as SiteKey.

If only internal classes use codex-codex, prefer:

requires codex.codex;

Do not export codex.index.internal.

7. Update tests
   codex-codex tests

Add tests for RepositoryContentItemProjectionReader.

Test cases:

constructor rejects null content item repository
constructor rejects null content revision repository
findContentItem rejects null siteKey
findContentItem rejects null contentTypeKey
findContentItem rejects null key
findContentRevision rejects null revisionId
findContentItem returns existing item
findContentItem returns empty when missing
findContentRevision returns existing revision
findContentRevision returns empty when missing

If CodexRuntime exposes contentItemProjectionReader(), add a small runtime test verifying it is non-null and can read a created item/revision if straightforward.

Do not overbuild runtime tests.

codex-index tests

Update projection source tests.

Use a fake/stub ContentItemProjectionReader.

Do not use internal repositories from codex-codex.

Test cases:

constructor rejects null reader
loadItem rejects null event
loadPublishedRevision rejects null event
loadItem returns item from reader
loadPublishedRevision returns revision from reader
loadItem throws IllegalStateException when reader returns empty
loadPublishedRevision throws IllegalStateException when reader returns empty
no index write happens when source throws, if covered in subscriber tests

Update integration tests in codex-index to use a stub reader or local reader implementation.

Do not make codex-index tests depend on codex-codex internal repositories.

8. Remove qualified export

After updating code and tests, remove from codex-codex/module-info.java:

exports codex.codex.internal.repository to codex.index;

Acceptance criterion:

codex-index should compile without any import from:

codex.codex.internal.repository

Run a search to verify.

9. Documentation

Update relevant docs briefly.

Suggested update to MODULE-RESPONSIBILITIES.md or module notes:

codex-codex exposes narrow projection/read contracts for modules that need canonical state for projections.
codex-index consumes ContentItemProjectionReader instead of internal repositories.
Internal repositories remain internal to codex-codex.

Do not rewrite large docs.

10. Acceptance Criteria

Task is complete when:

ContentItemProjectionReader exists in a public codex-codex API package
RepositoryContentItemProjectionReader exists internally in codex-codex
codex-index no longer imports codex.codex.internal.repository
qualified export to codex.index is removed
codex-codex does not depend on codex-index
codex-index still compiles and tests pass
indexing subscriber behavior remains unchanged
no runtime assembly module is introduced
no ServiceLoader implementation is introduced
no cache/read-only unit-of-work behavior is introduced
11. Maven Commands

Run:

mvn test -pl codex-codex,codex-index -am

Then, if practical:

mvn compile

Report commands and results.

12. Post-Task Report

After implementation, report:

files created
files modified
files renamed/deleted
package changes
module-info changes
whether the qualified export was removed
whether codex-index imports any codex.codex.internal.repository classes
tests added/updated
Maven commands run
whether tests passed
intentional deviations
open questions
recommended follow-up tasks
13. Constraints
    Follow CLAUDE.md.
    Follow CODING_IDENTITY.md.
    Follow AGENT-CALIBRATION.md.
    Remove internal repository dependency from codex-index.
    Do not make repositories public.
    Do not move repositories out of internal.
    Do not make codex-codex depend on codex-index.
    Do not introduce runtime/assembly module.
    Do not introduce ServiceLoader.
    Do not introduce Spring.
    Do not introduce Service Locator.
    Do not pass runtime context into domain services.
    Do not implement cache.
    Do not implement read-only unit of work.
    Do not implement search service.
    Do not implement audit subscribers.
    Do not implement workflow.
    Do not implement persistence.
    Do not modify unrelated files.
    Keep comments and JavaDoc in English.
    Prefer small, explicit, testable changes.