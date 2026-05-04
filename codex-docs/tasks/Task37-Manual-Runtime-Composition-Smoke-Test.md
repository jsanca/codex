
# Task38: Create codex-concilium Module Skeleton

## Objective

Create the initial Maven/Jigsaw skeleton for `codex-concilium`.

`codex-concilium` will be the future local runtime composition module for Codex.

It will eventually compose module runtimes such as:

- `CodexRuntime`
- `IndexRuntime`
- `ChroniconRuntime`
- future `ArchivumRuntime`
- future `CustosRuntime`
- future `IterRuntime`
- future `PortaRuntime`

This task creates the module skeleton only.

Do not implement global runtime composition yet.

Do not wire existing runtimes together yet.

Do not create ServiceLoader discovery yet.

Do not introduce Spring.

## Decision Context

Codex now has module-level runtimes:

```text
codex-codex
  -> CodexRuntime / canonical core runtime

codex-index
  -> IndexRuntime / indexing projection runtime

codex-chronicon
  -> ChroniconRuntime / audit-history projection runtime
````

We need a future place where these module runtimes can be composed without making `codex-codex` depend on projection/adapter modules.

`codex-porta` already exists, but Porta semantically represents the external gate:

```text
REST
GraphQL
WebSocket
SSE
external API adapters
```

Porta may become one consumer of a composed runtime, but it should not be the only possible composition root.

Codex may also run as:

* CLI
* embedded engine
* background worker
* sync daemon
* agent runtime
* test application
* future REST/GraphQL server

Therefore, we are introducing `codex-concilium`.

## Meaning of Concilium

`Concilium` means council, assembly, or gathering.

In Codex, `codex-concilium` represents the place where module runtimes meet and are composed into a coherent local application runtime.

It is not the canonical core.

It is not REST/GraphQL.

It is not cluster coordination.

It is not workflow.

It is the local runtime council.

## Scope

Create:

* `codex-concilium/`
* `codex-concilium/pom.xml`
* `codex-concilium/README.md`
* `codex-concilium/src/main/java/module-info.java`
* `codex-concilium/src/main/java/...` minimal marker/placeholder only if consistent with existing module style
* `codex-concilium/src/test/java/`
* update parent/root `pom.xml` module list
* update module documentation if useful

Do not implement:

* `ConciliumRuntime`
* application runtime
* runtime composition logic
* ServiceLoader provider discovery
* Spring configuration
* REST/GraphQL
* CLI
* runtime lifecycle manager
* dynamic registry
* OSGi-like registry
* dependency injection container
* cache wiring
* index wiring
* chronicon wiring
* durable persistence wiring
* workflow wiring

## Existing Module Style

Follow the same Maven/Jigsaw skeleton style already used by existing modules such as:

* `codex-index`
* `codex-archivum`
* `codex-chronicon`
* `codex-custos`
* `codex-porta`
* `codex-iter`
* `codex-scriptorium`

Match the existing parent inheritance, artifact naming, Java module naming, dependency style, README style, and directory structure.

Do not invent a different module structure.

## 1. Create Module Directory

Create:

```text
codex-concilium/
```

Suggested structure:

```text
codex-concilium/
  pom.xml
  README.md
  src/
    main/
      java/
        module-info.java
    test/
      java/
```

If existing skeleton modules include marker classes, add a minimal marker only if consistent.

If recent tasks removed marker classes once real code existed, keep this skeleton minimal and avoid unnecessary markers unless the build requires a package.

## 2. Parent POM Update

Update the root/parent `pom.xml` and add:

```xml
<module>codex-concilium</module>
```

Place it near other runtime/edge/composition modules.

Suggested order:

```text
codex-fundamentum
codex-codex
codex-index
codex-chronicon
codex-archivum
...
codex-concilium
codex-porta
```

Use the ordering convention already present in the parent POM.

Do not reorder the entire module list unless necessary.

## 3. codex-concilium pom.xml

Create `codex-concilium/pom.xml`.

Requirements:

* inherit from the existing root parent
* use artifact id `codex-concilium`
* packaging should follow existing module convention
* keep dependencies minimal
* add `codex-fundamentum` dependency because runtime abstractions live there
* do not add `codex-codex`, `codex-index`, or `codex-chronicon` yet unless compilation requires it
* do not add Spring
* do not add REST dependencies
* do not add persistence dependencies

Expected dependency direction later:

```text
codex-concilium
  -> codex-fundamentum
  -> codex-codex
  -> codex-index
  -> codex-chronicon
  -> maybe codex-archivum/custos/iter/etc.
```

But for this skeleton, keep it minimal.

## 4. module-info.java

Create:

```text
codex-concilium/src/main/java/module-info.java
```

Suggested minimal shape:

```java
module codex.concilium {
    requires codex.fundamentum;
}
```

Do not export packages if no public package exists.

Do not add `provides` declarations.

Do not require `codex.codex`, `codex.index`, or `codex.chronicon` yet unless the skeleton needs them.

Do not add `requires transitive` unless required by public exported APIs.

## 5. README.md

Create `codex-concilium/README.md`.

The README should explain:

```text
# codex-concilium
```

Include:

* Concilium means council/assembly/gathering.
* The module will eventually compose Codex module runtimes.
* It is the local runtime composition layer.
* It is not the canonical core.
* It is not Porta.
* It is not cluster coordination.
* It is not ServiceLoader yet.
* It is not Spring configuration.
* It does not own domain behavior.

Mention future composition:

```text
CodexRuntime
  + IndexRuntime
  + ChroniconRuntime
  + future ArchivumRuntime
  + future CustosRuntime
  + future IterRuntime
  -> Concilium application runtime
```

Mention future responsibilities:

* compose module runtimes
* collect module subscribers
* create global/local event dispatcher
* coordinate lifecycle close
* expose assembled runtime to edge modules such as Porta
* potentially support ServiceLoader-based runtime provider discovery later

Mention explicitly:

```text
This module is currently a skeleton.
No runtime composition is implemented yet.
```

## 6. Documentation Update

Update `codex-docs/modules/MODULE-RESPONSIBILITIES.md` if useful.

Add or update a section for `codex-concilium`.

Suggested text:

```text
## codex-concilium

The Concilium is the council of module runtimes.

It is responsible for future local runtime composition. It may compose the canonical core runtime, index runtime, chronicon runtime, and other module runtimes into an application runtime.

It does not own canonical domain behavior, REST/GraphQL APIs, workflow, persistence, search, audit, or AI logic.

It is distinct from codex-porta, which exposes external protocols, and from a future codex-concordia, which may coordinate distributed/cluster behavior.
```

Also update any implementation classification section:

```text
Near-Future:
- codex-concilium runtime composition
```

or:

```text
Active:
- codex-concilium skeleton exists
```

Do not rewrite the entire document.

## 7. Relationship to Porta

Document the distinction:

```text
codex-concilium
  -> composes local module runtimes

codex-porta
  -> exposes external API protocols
```

Porta may depend on Concilium later.

Concilium should not depend on Porta.

Do not implement that dependency in this task.

## 8. Relationship to Concordia

Document the distinction:

```text
codex-concilium
  -> local runtime composition

future codex-concordia
  -> possible cluster/distributed coordination
```

Do not create `codex-concordia`.

Do not implement cluster coordination.

## 9. Tests / Build

If the module contains no code besides `module-info.java`, tests may not be needed.

Run:

```bash
mvn test -pl codex-concilium -am
```

Also run:

```bash
mvn compile
```

or the smallest full-reactor command consistent with the project.

Report all commands and results.

## 10. Acceptance Criteria

Task is complete when:

* `codex-concilium` module exists
* root parent POM includes `codex-concilium`
* module has valid `pom.xml`
* module has valid `module-info.java`
* module has a clear `README.md`
* documentation mentions module responsibility if appropriate
* Maven builds successfully
* no runtime composition is implemented
* no ServiceLoader discovery is implemented
* no Spring integration is introduced
* no global runtime is created

## 11. Post-Task Report

After implementation, report:

* files created
* files modified
* parent POM changes
* module-info contents
* dependencies added
* README summary
* documentation updates
* Maven commands run
* whether build/tests passed
* intentional deviations
* open questions
* recommended follow-up tasks

## 12. Constraints

* Follow CLAUDE.md.
* Follow CODING_IDENTITY.md.
* Follow AGENT-CALIBRATION.md.
* Structural task only.
* Do not implement runtime composition.
* Do not create ConciliumRuntime yet.
* Do not create ServiceLoader provider discovery.
* Do not introduce Spring.
* Do not introduce Service Locator.
* Do not introduce OSGi-like registry.
* Do not wire CodexRuntime, IndexRuntime, or ChroniconRuntime together yet.
* Do not modify codex-codex runtime.
* Do not modify codex-index runtime.
* Do not modify codex-chronicon runtime.
* Do not add REST/GraphQL.
* Do not add persistence.
* Do not add cache/search/audit/workflow behavior.
* Do not modify unrelated files.
* Do not modify `.idea`, `target`, `build`, or generated files.
* Keep documentation in English.
* Keep the module skeleton minimal and consistent with existing modules.
