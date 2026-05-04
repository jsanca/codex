# Task27: Module Skeleton Creation

## Objective

Create the initial Maven/Jigsaw skeletons for selected Codex modules so dependency boundaries can begin to be enforced by the build.

This task should create module structure only.

Do not move existing Java classes.

Do not refactor packages.

Do not change runtime wiring.

The goal is to prepare the codebase for future module migration tasks, especially moving indexing classes into `codex-index`.

## Decision Context

Task26 documented Codex module responsibilities.

The next step is to create empty or minimal module skeletons before moving code.

This lets us separate two concerns:

```text
Task27
  -> create module boundaries

Future migration tasks
  -> move classes into those boundaries
```

This avoids mixing Maven/Jigsaw setup with large package refactors.

The most important dependency rule remains:

codex-codex is the canonical core.
codex-codex must not depend on adapter/projection modules.
adapter/projection modules may depend on codex-codex and codex-fundamentum as needed.
Scope

Create skeletons for:

codex-index
codex-archivum

Optional only if simple and consistent:

codex-chronicon

Do not create every future module yet.

Reason:

codex-index is the next likely migration target.

codex-archivum is the next likely durable persistence boundary.

codex-chronicon may come soon for audit/history, but can be skipped if it creates unnecessary Maven noise.

Required Work

For each created module:

create Maven module directory
create pom.xml
create src/main/java
create src/test/java
create module-info.java
create README.md
update parent/root pom.xml modules list
ensure Maven reactor builds

Do not add production domain logic.

Do not move classes.

Do not add dependencies unless required for compilation.

1. Create codex-index module

Create directory:

codex-index

Suggested structure:

codex-index/
pom.xml
README.md
src/
main/
java/
module-info.java
test/
java/
codex-index responsibility

codex-index will eventually own:

indexing abstractions
index documents
index writers
indexing projection subscribers
search/query abstractions
retrieval/ranking strategies
future adapters for:
Lucene
OpenSearch
Elasticsearch
Codex/myIR
vector indexes
hybrid search systems

For now, it may remain empty except for module metadata and README.

codex-index dependencies

For this skeleton task, keep dependencies minimal.

Expected future dependencies:

codex-index
-> codex-fundamentum
-> codex-codex

But do not add codex-codex dependency unless the skeleton needs it.

If empty module compilation requires no references, only depend on codex-fundamentum if consistent with module-info and project style.

Possible module-info.java for now:

module codex.index {
requires codex.fundamentum;
}

Do not export packages if there are no packages yet.

If the compiler complains about an empty module with only module-info.java, adjust minimally.

2. Create codex-archivum module

Create directory:

codex-archivum

Suggested structure:

codex-archivum/
pom.xml
README.md
src/
main/
java/
module-info.java
test/
java/
codex-archivum responsibility

codex-archivum will eventually own:

durable repository implementations
relational database adapters
filesystem-backed storage
object/blob storage
S3/MinIO-style storage
transaction adapters
migration support
outbox storage in the future

For now, it may remain empty except for module metadata and README.

codex-archivum dependencies

For this skeleton task, keep dependencies minimal.

Expected future dependencies:

codex-archivum
-> codex-fundamentum
-> codex-codex

But do not add codex-codex dependency unless needed.

Possible module-info.java for now:

module codex.archivum {
requires codex.fundamentum;
}

Do not add database dependencies.

Do not add JDBC, JPA, Flyway, Liquibase, HikariCP, Postgres, S3, MinIO, or filesystem implementation code.

3. Optional: codex-chronicon module

Only if the existing project/module conventions make it easy, create:

codex-chronicon

Responsibility:

audit trails
event history
historical timelines
change narratives
temporal projections
restoration views

For now, no implementation.

Possible module declaration:

module codex.chronicon {
requires codex.fundamentum;
}

Skip this module if it would increase scope or break the build.

4. POM requirements

Each new module pom.xml should:

inherit from the root/parent project
use the same packaging style as existing modules
follow naming conventions used by existing modules
avoid unnecessary dependencies
include test dependencies only if existing module convention requires them
not introduce plugins unless inherited config is insufficient

Parent/root pom.xml should include new modules in the correct section/order.

Suggested order:

<module>codex-fundamentum</module>
<module>codex-codex</module>
<module>codex-index</module>
<module>codex-archivum</module>

If codex-chronicon is created, place it near other projection/support modules.

5. README requirements

Each new module should include a short README.md.

codex-index README should explain
The Index makes knowledge discoverable.
It does not own canonical content.
It will receive projections from domain events.
Core must not depend on it.
Search/query APIs are future work.
Backends such as Lucene, OpenSearch, myIR, and embeddings are future adapters.
codex-archivum README should explain
The Archivum stores the manuscripts.
It owns durable storage adapters.
It does not own domain lifecycle.
It will eventually provide durable repository implementations and transaction adapters.
No persistence backend is implemented yet.
codex-chronicon README if created
The Chronicon preserves the memory of the manuscript.
It will own audit/history/timeline projections.
It listens to events.
It does not own canonical lifecycle.
6. Module info requirements

Create valid module-info.java files.

Requirements:

module names should follow existing naming style
do not export nonexistent packages
do not require modules unnecessarily
do not add transitive requirements unless needed
keep declarations minimal

If the module currently has no packages, a minimal module declaration is acceptable.

Examples:

module codex.index {
requires codex.fundamentum;
}
module codex.archivum {
requires codex.fundamentum;
}
7. Do not move existing indexing code

Even though codex-index is being created, do not move these yet:

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

Moving indexing code will be a separate task.

8. Do not alter runtime wiring

Do not change:

CodexRuntime
event dispatcher wiring
indexing subscriber wiring
service construction
repositories
cache wiring

This task is structural only.

9. Tests / Build

Run the smallest meaningful Maven command.

Preferred:

mvn test

or, if the project is large and convention supports it:

mvn -pl codex-index,codex-archivum test

But ensure the full reactor still recognizes the new modules.

If empty modules have no tests, compilation should still pass.

10. Documentation update

Update codex-docs/modules/MODULE-RESPONSIBILITIES.md only if needed to mention that skeletons now exist.

Do not rewrite the document.

A small note is enough:

Module skeletons currently exist for codex-index and codex-archivum, but implementation migration is future work.

Only add this if it feels helpful.

11. Post-Task Report

After implementation, report:

modules created
files created
parent POM changes
Maven command run
whether tests/build passed
whether optional codex-chronicon was created or skipped
any intentional deviations
any follow-up tasks recommended
12. Constraints
    Follow CLAUDE.md.
    Follow CODING_IDENTITY.md.
    Follow AGENT-CALIBRATION.md.
    Structural task only.
    Do not move existing Java classes.
    Do not refactor packages.
    Do not change runtime wiring.
    Do not add production logic.
    Do not add database dependencies.
    Do not add OpenSearch/Lucene/myIR/embedding dependencies.
    Do not add Spring.
    Do not add JPA.
    Do not add REST.
    Do not add persistence implementation.
    Do not modify unrelated files.
    Do not modify .idea, target, build, or generated files.
    Keep README documentation in English.
    Keep module declarations minimal.
    Run the smallest relevant Maven build/test command.