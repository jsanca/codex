# Task28: Move Indexing Classes to codex-index

## Objective

Move the indexing foundation and content item published indexing projection from `codex-codex` into the new `codex-index` module.

This task enforces the module responsibility boundary documented in `MODULE-RESPONSIBILITIES.md`:

```text
codex-codex
  -> canonical domain model, lifecycle, commands, domain events

codex-index
  -> indexing abstractions, index writers, search/index projections, indexing subscribers
```

The core module must not depend on codex-index.

codex-index may depend on:

codex-fundamentum
codex-codex

Do not move canonical domain classes out of codex-codex.

Do not create search APIs yet.

Do not add OpenSearch, Lucene, myIR, Elasticsearch, embeddings, Redis, Caffeine, or any external backend.

Decision Context

Indexing is a projection concern.

Canonical services emit domain events but do not perform indexing directly.

The current MVP placed indexing classes temporarily inside codex-codex while the architecture was being shaped.

Now that codex-index exists as a module skeleton, indexing classes should move there.

Important boundary:

codex-codex must not depend on codex-index.

This means:

domain events remain in codex-codex
canonical services remain in codex-codex
indexing abstractions and subscribers move to codex-index
runtime wiring may need to avoid pulling indexing back into codex-codex

If current CodexRuntime in codex-codex wires indexing subscribers directly, do not force codex-codex to depend on codex-index.

Instead, either:

remove indexing subscriber wiring from CodexRuntime for now and leave only core event dispatching, or
introduce the smallest transitional hook/factory that does not require codex-codex to know codex-index, or
document that full cross-module runtime composition needs a future codex-runtime / codex-assembly module.

Prefer option 1 or 3 unless the existing structure clearly supports option 2 cleanly.

Do not create codex-runtime in this task.

Scope

Move indexing-related classes from codex-codex to codex-index.

Likely classes/packages to move:

IndexDocument
IndexDocumentId
IndexResourceType
IndexWriter
NoOpIndexWriter
RecordingIndexWriter
ContentItemIndexDocumentMapper
ContentItemPublishedIndexingSubscriber
ContentItemProjectionSource
RepositoryContentItemProjectionSource

Adjust package names, imports, module declarations, and tests accordingly.

Do not implement:

ContentSearchService
search query APIs
OpenSearch writer
Lucene writer
myIR writer
Elasticsearch writer
embedding/vector writer
cache invalidation
audit
workflow
runtime/assembly module
dynamic subscriber registry
Spring integration
persistence
REST
Expected Package Layout

Use packages under codex-index.

Suggested API package:

codex.index.api

For backend-neutral public indexing contracts:

IndexDocument
IndexDocumentId
IndexResourceType
IndexWriter

Suggested internal package:

codex.index.internal

For internal/test/local implementations and subscribers:

NoOpIndexWriter
RecordingIndexWriter
ContentItemIndexDocumentMapper
ContentItemPublishedIndexingSubscriber
ContentItemProjectionSource
RepositoryContentItemProjectionSource

Alternative package naming is acceptable if it follows existing module conventions, but keep this rule:

API contracts go in an api package.
Implementation/subscriber/projection details go in an internal package.

Do not keep indexing classes under codex.codex.* after the move unless they are domain events or canonical content classes.

Module Dependencies

Update codex-index/pom.xml as needed.

Expected dependencies:

codex-index
-> codex-fundamentum
-> codex-codex

Rationale:

IndexWriter, IndexDocument, and event subscribers use fundamentum abstractions.
ContentItemPublishedIndexingSubscriber depends on ContentItemPublishedEvent, ContentItem, ContentRevision, and repositories/contracts from codex-codex.

Update codex-index/src/main/java/module-info.java.

Expected shape:

module codex.index {
requires codex.fundamentum;
requires codex.codex;

    exports codex.index.api;
}

Do not export internal packages.

If tests require access to internal packages, use the existing project test/module strategy. Do not export internals publicly only for tests unless that is already the project convention.

Core Dependency Rule

Do not add this dependency:

codex-codex -> codex-index

If moving classes causes codex-codex compilation failures because CodexRuntime imports index classes, fix the runtime boundary without making core depend on index.

Possible acceptable approaches:

Preferred Transitional Approach

Remove direct indexing wiring from CodexRuntime in codex-codex.

Keep CodexRuntime focused on canonical core services and core event dispatching.

Document that indexing wiring will be composed by a future runtime/assembly module.

Alternative if Already Supported Cleanly

Allow CodexRuntime to accept a generic CodexEventDispatcher or subscriber dispatcher from outside, without importing codex-index.

Example conceptual direction:

codex-codex runtime receives CodexEventDispatcher
external assembly builds:
DeferredEventDispatcher
-> CompositeCodexEventDispatcher
-> LocalCodexEventDispatcher
-> codex-index subscribers

Do not overbuild this.

Do not create a new runtime module in this task.

Files to Move

Move the following if they currently exist in codex-codex:

Index API contracts

From current package, likely:

codex.codex.api.index

To:

codex.index.api

Classes:

IndexDocument
IndexDocumentId
IndexResourceType
IndexWriter
Index implementations and subscribers

From current package, likely:

codex.codex.internal.index

To:

codex.index.internal

Classes:

NoOpIndexWriter
RecordingIndexWriter
ContentItemIndexDocumentMapper
ContentItemPublishedIndexingSubscriber
ContentItemProjectionSource
RepositoryContentItemProjectionSource

Update package declarations and imports.

Update tests accordingly.

Remove old empty packages if appropriate.

Domain Events Stay in codex-codex

Do not move:

ContentItemPublishedEvent
ContentItemCreatedEvent
ContentTypeCreatedEvent
ContentTypeActivatedEvent
SiteCreatedEvent
any canonical domain event

Domain events belong to codex-codex.

codex-index listens to those events.

Canonical Entities Stay in codex-codex

Do not move:

Site
ContentType
ContentTypeVersion
ContentItem
ContentRevision
Field
commands
canonical services
domain repositories
domain exceptions

codex-index may depend on them, but does not own them.

Repositories and Projection Source

For this task, it is acceptable that RepositoryContentItemProjectionSource in codex-index depends on repository contracts from codex-codex.

This is a projection source implementation.

Future work may introduce a more formal read-model or runtime assembly module.

Do not move canonical repositories into codex-index.

Runtime Wiring

Review CodexRuntime.

If CodexRuntime currently imports:

NoOpIndexWriter
ContentItemPublishedIndexingSubscriber
ContentItemIndexDocumentMapper
RepositoryContentItemProjectionSource
IndexWriter

then moving those to codex-index will create a dependency problem.

Do not solve it by making codex-codex depend on codex-index.

Instead:

remove direct indexing wiring from CodexRuntime, or
leave a documented TODO/future runtime composition note, or
use only fundamentum abstractions if already available.

After this task, codex-codex should compile without depending on codex-index.

If integration tests relying on runtime indexing break, move those tests to codex-index or adjust them to build their own local dispatcher and subscriber wiring inside the codex-index test scope.

Tests

Move relevant tests from codex-codex to codex-index.

Likely tests to move:

IndexDocumentIdTest
IndexDocumentTest
NoOpIndexWriterTest
RecordingIndexWriterTest
ContentItemIndexDocumentMapperTest
ContentItemPublishedIndexingSubscriberTest
RepositoryContentItemProjectionSourceTest
any publish-to-index projection integration test

Update packages/imports.

Tests in codex-index may depend on codex-codex test fixtures only if those fixtures are accessible cleanly.

Prefer local test setup in codex-index if needed.

Do not make production internals public just to reuse tests.

Integration Test Strategy

After moving indexing to codex-index, an integration-style test should exist in codex-index proving:

ContentItemPublishedEvent
-> LocalCodexEventDispatcher
-> ContentItemPublishedIndexingSubscriber
-> RecordingIndexWriter

It may manually create:

in-memory content item repository
in-memory content revision repository
RepositoryContentItemProjectionSource
ContentItemIndexDocumentMapper
RecordingIndexWriter
ContentItemPublishedIndexingSubscriber
LocalCodexEventDispatcher

This test does not need CodexRuntime unless runtime composition is cleanly available without core depending on index.

Documentation Updates

Update codex-index/README.md if needed to mention that indexing foundation classes now live in codex-index.

Update codex-docs/modules/MODULE-RESPONSIBILITIES.md if needed.

Add a short note:

Indexing foundation and content item published indexing projection have moved to codex-index.
codex-codex remains the canonical core and does not depend on codex-index.

Do not rewrite the entire documentation.

Acceptance Criteria

The task is complete when:

indexing API contracts are in codex-index
indexing internal implementations/subscribers are in codex-index
old indexing packages are removed from codex-codex or left empty only if unavoidable
codex-index depends on codex-codex and codex-fundamentum
codex-codex does not depend on codex-index
tests compile and pass
full reactor compile passes
runtime/core dependency direction remains clean
no backend-specific indexing implementation is introduced
Maven Commands

Run the smallest relevant commands first:

mvn test -pl codex-index -am

Then run full compile:

mvn compile

If test movement affects multiple modules, run:

mvn test -pl codex-fundamentum,codex-codex,codex-index -am

Report all commands run.

Post-Task Report

After implementation, report:

files moved
packages renamed
module-info changes
pom changes
tests moved or updated
runtime wiring changes, if any
whether codex-codex depends on codex-index
Maven commands run
whether tests passed
intentional deviations
open questions
recommended follow-up tasks
Constraints
Follow CLAUDE.md.
Follow CODING_IDENTITY.md.
Follow AGENT-CALIBRATION.md.
Move indexing classes only.
Do not move canonical domain classes.
Do not move domain events.
Do not move canonical services.
Do not make codex-codex depend on codex-index.
Do not create codex-runtime or codex-assembly.
Do not implement search service.
Do not implement OpenSearch.
Do not implement Lucene.
Do not implement myIR adapter.
Do not implement embeddings.
Do not implement cache invalidation.
Do not implement audit.
Do not implement workflow.
Do not add Spring.
Do not add JPA.
Do not add persistence framework.
Do not add REST.
Do not modify unrelated files.
Do not modify .idea, target, build, or generated files.
Keep comments and JavaDoc in English.
Prefer small, explicit, testable changes.